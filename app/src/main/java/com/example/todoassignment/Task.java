package com.example.todoassignment;

import java.io.Serializable;
import java.util.List;

public class Task implements Serializable {
    private String id;
    private String name;
    private String description;
    private long dueDate;
    private boolean isCompleted;
    private int priority;
    private List<Long> reminderTimes;

    // No-argument constructor for Firestore
    public Task() {
    }

    // Constructor for a new or existing task with reminders
    public Task(String name, String description, long dueDate, boolean isCompleted, int priority, List<Long> reminderTimes) {
        this.name = name;
        this.description = description;
        this.dueDate = dueDate;
        this.isCompleted = isCompleted;
        this.priority = priority;
        this.reminderTimes = reminderTimes;
    }

    public Task(String id, String name, String description, long dueDate, boolean isCompleted, int priority, List<Long> reminderTimes) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.dueDate = dueDate;
        this.isCompleted = isCompleted;
        this.priority = priority;
        this.reminderTimes = reminderTimes;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getDueDate() { return dueDate; }
    public void setDueDate(long dueDate) { this.dueDate = dueDate; }
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public List<Long> getReminderTimes() { return reminderTimes; }
    public void setReminderTimes(List<Long> reminderTimes) { this.reminderTimes = reminderTimes; }

    // Check if the task is overdue
    public boolean isOverdue() {
        return !isCompleted && System.currentTimeMillis() > dueDate;
    }
}
