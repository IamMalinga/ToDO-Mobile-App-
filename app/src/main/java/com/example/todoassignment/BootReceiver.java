package com.example.todoassignment;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("tasks").get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Task fetchedTask = document.toObject(Task.class);
                                if (!fetchedTask.isCompleted()) {
                                    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                                    if (alarmManager != null) {
                                        try {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                                                for (long reminderTime : fetchedTask.getReminderTimes()) {
                                                    scheduleExactAlarm(context, alarmManager, reminderTime, fetchedTask.getName());
                                                }
                                            } else {
                                                for (long reminderTime : fetchedTask.getReminderTimes()) {
                                                    scheduleLegacyAlarm(context, alarmManager, reminderTime, fetchedTask.getName());
                                                }
                                            }
                                        } catch (SecurityException e) {
                                            Toast.makeText(context, "Unable to schedule exact alarms: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            Log.e("BootReceiver", "Unable to schedule exact alarms", e);
                                        }
                                    }
                                }
                            }
                        } else {
                            Log.e("BootReceiver", "Error fetching tasks from Firestore", task.getException());
                        }
                    });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void scheduleExactAlarm(Context context, AlarmManager alarmManager, long reminderTime, String taskName) {
        if (reminderTime > System.currentTimeMillis()) {
            Intent intent = new Intent(context, TaskReminderReceiver.class);
            intent.putExtra("task_name", taskName);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) reminderTime, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
            Log.d("BootReceiver", "Exact alarm set for: " + new Date(reminderTime).toString());
        }
    }

    private void scheduleLegacyAlarm(Context context, AlarmManager alarmManager, long reminderTime, String taskName) {
        if (reminderTime > System.currentTimeMillis()) {
            Intent intent = new Intent(context, TaskReminderReceiver.class);
            intent.putExtra("task_name", taskName);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) reminderTime, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
            Log.d("BootReceiver", "Legacy alarm set for: " + new Date(reminderTime).toString());
        }
    }
}
