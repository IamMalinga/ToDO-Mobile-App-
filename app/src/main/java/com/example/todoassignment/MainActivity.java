// MainActivity.java

package com.example.todoassignment;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class MainActivity extends AppCompatActivity {
    private ArrayList<Task> taskList;
    private TaskAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FirebaseFirestore db;
    private SharedPreferences sharedPreferences;

    private static final String TAG = "MainActivity";

    private final ActivityResultLauncher<Intent> addTaskLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task task = (Task) result.getData().getSerializableExtra("new_task");
                    if (task != null) {
                        addTaskInSortedOrder(task);
                        adapter.notifyDataSetChanged();
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> editTaskLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task updatedTask = (Task) result.getData().getSerializableExtra("edited_task");
                    if (updatedTask != null) {
                        int index = findTaskIndexById(updatedTask.getId());
                        if (index != -1) {
                            taskList.set(index, updatedTask);
                            adapter.notifyItemChanged(index);
                        }
                    }
                }
            }
    );

    private void addTaskInSortedOrder(Task newTask) {
        boolean added = false;
        for (int i = 0; i < taskList.size(); i++) {
            if (newTask.getPriority() < taskList.get(i).getPriority()) {
                taskList.add(i, newTask);
                added = true;
                break;
            }
        }
        if (!added) {
            taskList.add(newTask);
        }
        adapter.notifyDataSetChanged();
    }

    private int findTaskIndexById(String taskId) {
        for (int i = 0; i < taskList.size(); i++) {
            if (taskList.get(i).getId().equals(taskId)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE);
        setTheme(sharedPreferences.getBoolean("DarkMode", false) ? R.style.AppTheme_Dark : R.style.AppTheme_Light);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);

        db = FirebaseFirestore.getInstance();
        taskList = new ArrayList<>();
        adapter = new TaskAdapter(this, taskList, editTaskLauncher);

        RecyclerView recyclerView = findViewById(R.id.recycler_view_tasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this::refreshTasks);

        FloatingActionButton addTaskButton = findViewById(R.id.button_add_task);
        addTaskButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
            addTaskLauncher.launch(intent);
        });

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        FirebaseApp.initializeApp(this);
        fetchTasksFromFirestore();
        sortTasksByPriority();
    }

    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            Task task = taskList.get(position);

            if (direction == ItemTouchHelper.LEFT) {
                task.setCompleted(!task.isCompleted());
                updateTaskInFirestore(task);
                adapter.notifyItemChanged(position);
                Log.d(TAG, "Task marked as completed: " + task.getName());
            } else if (direction == ItemTouchHelper.RIGHT) {
                showDeleteConfirmationDialog(position, task);
            }
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                float dX, float dY, int actionState, boolean isCurrentlyActive) {
            new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    .addSwipeLeftBackgroundColor(Color.GREEN)
                    .addSwipeLeftLabel("Complete")
                    .setSwipeLeftLabelColor(Color.WHITE)
                    .addSwipeLeftActionIcon(R.drawable.ic_done)
                    .addSwipeRightBackgroundColor(Color.RED)
                    .addSwipeRightLabel("Delete")
                    .setSwipeRightLabelColor(Color.WHITE)
                    .addSwipeRightActionIcon(R.drawable.ic_delete)
                    .create()
                    .decorate();

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    };


    private void showDeleteConfirmationDialog(int position, Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
        builder.setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    taskList.remove(position);
                    deleteTaskFromFirestore(task.getId());
                    adapter.notifyItemRemoved(position);
                    Log.d(TAG, "Task deleted: " + task.getName());
                })
                .setNegativeButton(android.R.string.no, (dialog, which) -> {
                    adapter.notifyItemChanged(position);
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void saveTaskToFirestore(Task task) {
        db.collection("tasks").add(task)
                .addOnSuccessListener(documentReference -> {
                    task.setId(documentReference.getId());
                    Toast.makeText(this, "Task saved successfully.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateTaskInFirestore(Task task) {
        db.collection("tasks").document(task.getId())
                .set(task)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Task updated successfully.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteTaskFromFirestore(String taskId) {
        db.collection("tasks").document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Task deleted successfully.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchTasksFromFirestore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, SignInActivity.class));
            finish();
            return;
        }

        String userId = currentUser.getUid();
        db.collection("users").document(userId).collection("tasks").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        taskList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Task fetchedTask = document.toObject(Task.class);
                            fetchedTask.setId(document.getId());
                            taskList.add(fetchedTask);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.w("Firestore", "Error getting documents.", task.getException());
                    }
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    private void refreshTasks() {
        fetchTasksFromFirestore();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sortTasksByPriority() {
        Collections.sort(taskList, (task1, task2) -> Integer.compare(task2.getPriority(), task1.getPriority()));
        adapter.notifyDataSetChanged();
    }
}
