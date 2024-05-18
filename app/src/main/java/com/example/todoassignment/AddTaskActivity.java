package com.example.todoassignment;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class AddTaskActivity extends AppCompatActivity {
    private EditText taskNameEditText, taskDescriptionEditText, dueDateEditText;
    private Spinner prioritySpinner;
    private Button saveButton, setRemindersButton;
    private long dueDate;
    private Task taskToEdit;
    private List<Long> reminderTimes = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add Task");
        }

        taskNameEditText = findViewById(R.id.edittext_task_name);
        taskDescriptionEditText = findViewById(R.id.edittext_task_description);
        dueDateEditText = findViewById(R.id.edittext_due_date);
        prioritySpinner = findViewById(R.id.spinner_priority);
        saveButton = findViewById(R.id.button_save_task);
        setRemindersButton = findViewById(R.id.button_set_reminders);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.priority_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(adapter);

        dueDateEditText.setOnClickListener(v -> showDatePickerDialog());
        saveButton.setOnClickListener(v -> saveTask());
        setRemindersButton.setOnClickListener(v -> showReminderSetupDialog());

        db = FirebaseFirestore.getInstance();

        taskToEdit = (Task) getIntent().getSerializableExtra("task");
        if (taskToEdit != null) {
            populateTaskDetails(taskToEdit);
        }
    }

    private void populateTaskDetails(Task task) {
        taskNameEditText.setText(task.getName());
        taskDescriptionEditText.setText(task.getDescription());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(task.getDueDate());
        dueDateEditText.setText(getString(R.string.date_format, calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR)));
        prioritySpinner.setSelection(task.getPriority());
        dueDate = task.getDueDate();
        reminderTimes = task.getReminderTimes();
        saveButton.setText(R.string.update_task);
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year1, month1, dayOfMonth) -> showTimePickerDialog(year1, month1, dayOfMonth),
                year, month, day);
        datePickerDialog.show();
    }

    private void showTimePickerDialog(int year, int month, int day) {
        final Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minuteOfHour) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, day, hourOfDay, minuteOfHour);
                    dueDate = selectedDate.getTimeInMillis();
                    dueDateEditText.setText(getString(R.string.date_format, day, month + 1, year) + " " + getString(R.string.time_format, hourOfDay, minuteOfHour));
                },
                hour, minute, true);
        timePickerDialog.show();
    }

    private void showReminderSetupDialog() {
        final CharSequence[] items = {"10 minutes before", "1 hour before", "1 day before"};
        final boolean[] checkedItems = new boolean[items.length];

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Reminders");
        builder.setMultiChoiceItems(items, checkedItems, (dialog, indexSelected, isChecked) -> {
            long timeOffset = 0;
            switch (indexSelected) {
                case 0: timeOffset = 600000; break;
                case 1: timeOffset = 3600000; break;
                case 2: timeOffset = 86400000; break;
            }
            if (isChecked) {
                reminderTimes.add(dueDate - timeOffset);
            } else {
                reminderTimes.remove(dueDate - timeOffset);
            }
        });
        builder.setPositiveButton("OK", (dialog, id) -> {});
        builder.setNegativeButton("Cancel", (dialog, id) -> {});
        builder.create().show();
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void setTaskReminders(Task task) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (alarmManager.canScheduleExactAlarms()) {
                for (long reminderTime : task.getReminderTimes()) {
                    Intent intent = new Intent(this, TaskReminderReceiver.class);
                    intent.putExtra("task_name", task.getName());
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(this, (int) reminderTime, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    if (reminderTime > System.currentTimeMillis()) {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
                        Log.d("ReminderSet", "Reminder set for: " + new Date(reminderTime).toString());
                    } else {
                        Log.d("ReminderSet", "Reminder time is in the past: " + new Date(reminderTime).toString());
                    }
                }
            } else {
                // Prompt the user to grant permission
                new AlertDialog.Builder(this)
                        .setTitle("Permission Required")
                        .setMessage("To schedule exact alarms, please grant the permission in settings.")
                        .setPositiveButton("Settings", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        } else {
            Toast.makeText(this, "AlarmManager is not available.", Toast.LENGTH_LONG).show();
        }
    }

    private void removeTaskReminders(Task task) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            for (long reminderTime : task.getReminderTimes()) {
                Intent intent = new Intent(this, TaskReminderReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, (int) reminderTime, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                alarmManager.cancel(pendingIntent);
                Log.d("ReminderRemove", "Reminder removed for: " + new Date(reminderTime).toString());
            }
        }
    }

    private void saveTask() {
        String taskName = taskNameEditText.getText().toString().trim();
        String taskDescription = taskDescriptionEditText.getText().toString().trim();
        int priority = prioritySpinner.getSelectedItemPosition();

        if (taskName.isEmpty() || taskDescription.isEmpty()) {
            Toast.makeText(this, "Task name and description cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dueDate <= System.currentTimeMillis()) {
            Toast.makeText(this, "Due date must be in the future.", Toast.LENGTH_SHORT).show();
            return;
        }

        Task task;
        if (taskToEdit != null) {
            removeTaskReminders(taskToEdit);
            task = taskToEdit;
            task.setName(taskName);
            task.setDescription(taskDescription);
            task.setDueDate(dueDate);
            task.setPriority(priority);
            task.setReminderTimes(reminderTimes);
            updateTaskInFirestore(task);
        } else {
            String id = FirebaseFirestore.getInstance().collection("tasks").document().getId();
            task = new Task(id, taskName, taskDescription, dueDate, false, priority, reminderTimes);
            saveTaskToFirestore(task);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setTaskReminders(task);
        } else {
            for (long reminderTime : task.getReminderTimes()) {
                setLegacyTaskReminder(reminderTime, task.getName());
            }
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra(taskToEdit != null ? "edited_task" : "new_task", task);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void setLegacyTaskReminder(long reminderTime, String taskName) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            Intent intent = new Intent(this, TaskReminderReceiver.class);
            intent.putExtra("task_name", taskName);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, (int) reminderTime, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            if (reminderTime > System.currentTimeMillis()) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
                Log.d("ReminderSet", "Reminder set for: " + new Date(reminderTime).toString());
            } else {
                Log.d("ReminderSet", "Reminder time is in the past: " + new Date(reminderTime).toString());
            }
        } else {
            Toast.makeText(this, "AlarmManager is not available.", Toast.LENGTH_LONG).show();
        }
    }

    private void saveTaskToFirestore(Task task) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        db.collection("users").document(userId).collection("tasks").document(task.getId())
                .set(task)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Task saved successfully.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateTaskInFirestore(Task task) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        db.collection("users").document(userId).collection("tasks").document(task.getId())
                .set(task)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Task updated successfully.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
