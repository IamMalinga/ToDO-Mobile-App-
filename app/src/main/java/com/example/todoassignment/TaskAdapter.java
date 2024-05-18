package com.example.todoassignment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private final Context context;
    private final ArrayList<Task> tasks;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final ActivityResultLauncher<Intent> editTaskLauncher;

    public TaskAdapter(Context context, ArrayList<Task> tasks, ActivityResultLauncher<Intent> editTaskLauncher) {
        this.context = context;
        this.tasks = tasks;
        this.editTaskLauncher = editTaskLauncher;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.task_item, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.taskName.setText(task.getName());
        holder.taskDescription.setText(task.getDescription());
        holder.taskDueDate.setText(dateFormat.format(task.getDueDate()));
        holder.taskDueTime.setText(timeFormat.format(task.getDueDate())); // Set the due time here
        holder.taskPriorityIcon.setImageResource(getPriorityIcon(task.getPriority()));
        holder.taskPriorityText.setText(getPriorityText(task.getPriority()));
        holder.taskCompleted.setChecked(task.isCompleted());

        if (task.isCompleted()) {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.completed_task_background));
            holder.taskName.setPaintFlags(holder.taskName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.taskDescription.setPaintFlags(holder.taskDescription.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.taskName.setPaintFlags(holder.taskName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.taskDescription.setPaintFlags(holder.taskDescription.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.light_green));
        }

        if (task.isOverdue()) {
            holder.taskDueDate.setTextColor(Color.YELLOW);
            holder.taskDueTime.setTextColor(Color.YELLOW);
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.overdue_task_background));
        } else {
            holder.taskDueDate.setTextColor(ContextCompat.getColor(context, android.R.color.black));
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddTaskActivity.class);
            intent.putExtra("task", task);
            editTaskLauncher.launch(intent);
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskName, taskDescription, taskDueDate, taskDueTime, taskPriorityText;
        ImageView taskPriorityIcon;
        CheckBox taskCompleted;
        CardView cardView;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskName = itemView.findViewById(R.id.task_name);
            taskDescription = itemView.findViewById(R.id.task_description);
            taskDueDate = itemView.findViewById(R.id.task_due_date);
            taskDueTime = itemView.findViewById(R.id.task_due_time);
            taskPriorityIcon = itemView.findViewById(R.id.task_priority_icon);
            taskPriorityText = itemView.findViewById(R.id.task_priority_text);
            taskCompleted = itemView.findViewById(R.id.task_completed);
            cardView = (CardView) itemView;
        }
    }

    private int getPriorityIcon(int priority) {
        switch (priority) {
            case 0:
                return R.drawable.ic_priority_low;
            case 1:
                return R.drawable.ic_priority_medium;
            case 2:
                return R.drawable.ic_priority_high;
            default:
                return R.drawable.ic_priority_low;
        }
    }

    private String getPriorityText(int priority) {
        switch (priority) {
            case 0:
                return "Low";
            case 1:
                return "Medium";
            case 2:
                return "High";
            default:
                return "Unknown";
        }
    }
}
