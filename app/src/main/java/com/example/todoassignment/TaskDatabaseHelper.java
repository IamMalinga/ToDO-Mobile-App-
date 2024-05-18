package com.example.todoassignment;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;

public class TaskDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "tasks.db";
    private static final int DATABASE_VERSION = 5;
    private static final String TABLE_NAME = "tasks";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_DUE_DATE = "due_date";
    private static final String COLUMN_COMPLETED = "completed";
    private static final String COLUMN_PRIORITY = "priority";
    private static final String COLUMN_REMINDERS = "reminders";

    public TaskDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " TEXT PRIMARY KEY, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_DESCRIPTION + " TEXT, " +
                COLUMN_DUE_DATE + " INTEGER, " +
                COLUMN_COMPLETED + " INTEGER, " +
                COLUMN_PRIORITY + " INTEGER, " +
                COLUMN_REMINDERS + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_REMINDERS + " TEXT DEFAULT ''");
        }
    }

    public boolean addTask(Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, task.getId());
        values.put(COLUMN_NAME, task.getName());
        values.put(COLUMN_DESCRIPTION, task.getDescription());
        values.put(COLUMN_DUE_DATE, task.getDueDate());
        values.put(COLUMN_COMPLETED, task.isCompleted() ? 1 : 0);
        values.put(COLUMN_PRIORITY, task.getPriority());
        values.put(COLUMN_REMINDERS, new JSONArray(task.getReminderTimes()).toString());
        long result = db.insert(TABLE_NAME, null, values);
        db.close();
        return result != -1;
    }

    public void updateTask(Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, task.getName());
        values.put(COLUMN_DESCRIPTION, task.getDescription());
        values.put(COLUMN_DUE_DATE, task.getDueDate());
        values.put(COLUMN_COMPLETED, task.isCompleted() ? 1 : 0);
        values.put(COLUMN_PRIORITY, task.getPriority());
        values.put(COLUMN_REMINDERS, new JSONArray(task.getReminderTimes()).toString());
        db.update(TABLE_NAME, values, COLUMN_ID + "=?", new String[]{task.getId()});
        db.close();
    }

    public ArrayList<Task> getAllTasks() {
        ArrayList<Task> taskList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);

        if (cursor.moveToFirst()) {
            do {
                Task task = new Task(
                        cursor.getString(cursor.getColumnIndex(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_NAME)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION)),
                        cursor.getLong(cursor.getColumnIndex(COLUMN_DUE_DATE)),
                        cursor.getInt(cursor.getColumnIndex(COLUMN_COMPLETED)) == 1,
                        cursor.getInt(cursor.getColumnIndex(COLUMN_PRIORITY)),
                        convertJsonToReminderTimes(cursor.getString(cursor.getColumnIndex(COLUMN_REMINDERS)))
                );
                taskList.add(task);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return taskList;
    }

    private List<Long> convertJsonToReminderTimes(String json) {
        try {
            JSONArray jsonArray = new JSONArray(json);
            List<Long> reminders = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                reminders.add(jsonArray.getLong(i));
            }
            return reminders;
        } catch (JSONException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void deleteTask(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_ID + "=?", new String[]{id});
        db.close();
    }
}
