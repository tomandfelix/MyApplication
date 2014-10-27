package com.example.tom.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


/**
 * Created by Tom on 6/10/2014.
 * Works the local database for the application
 */
public class DatabaseHelper extends SQLiteOpenHelper{
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "data.sqlite";
    private static final String TABLE_LOGS = "logs";
    private static final String TABLE_PROFILES = "profiles";
    private static final String KEY_ID = "id";
    private static final String KEY_ACTION = "action";
    private static final String KEY_DATETIME = "datetime";
    private static final String KEY_FIRSTNAME = "firstname";
    private static final String KEY_LASTNAME = "lastname";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_MONEY = "money";
    private static final String KEY_EXPERIENCE = "experience";



    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_LOGS + " (" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_ACTION + " TEXT, " + KEY_DATETIME + " DATETIME)");
        db.execSQL("CREATE TABLE " + TABLE_PROFILES + " (" + KEY_ID + " INTEGER PRIMARY KEY NOT NULL UNIQUE, " + KEY_FIRSTNAME + " TEXT, " + KEY_LASTNAME + " TEXT, " + KEY_USERNAME + " TEXT, " + KEY_EMAIL + " TEXT, " + KEY_MONEY + " INT, " + KEY_EXPERIENCE + " INT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROFILES);
        onCreate(db);
    }

    public void addLog(DBLog log) {
        Log.d("addLog", "adding a log");
        ContentValues input = new ContentValues();
        input.put(KEY_ACTION, log.getAction());
        input.put(KEY_DATETIME, log.getDateTimeString());
        SQLiteDatabase db = getWritableDatabase();
        db.insert(TABLE_LOGS, null, input);
    }

    public boolean idPresent(int id) {
        String query = "SELECT " + KEY_ID + " FROM " + TABLE_PROFILES + " WHERE " + KEY_ID + " = " + id;
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if(cursor.moveToFirst()) {
            return true;
        } else {
            return false;
        }
    }

    public void storeProfile(Profile input){
        if(idPresent(input.getId())) {
            updateProfile(input);
        } else {
            ContentValues value = new ContentValues();
            if (input.getId() != 0) {
                value.put(KEY_ID, input.getId());
            }
            value.put(KEY_FIRSTNAME, input.getFirstName());
            value.put(KEY_LASTNAME, input.getLastName());
            value.put(KEY_USERNAME, input.getUsername());
            value.put(KEY_EMAIL, input.getEmail());
            value.put(KEY_MONEY, input.getMoney());
            value.put(KEY_EXPERIENCE, input.getExperience());
            SQLiteDatabase db = getWritableDatabase();
            db.insert(TABLE_PROFILES, null, value);
        }
    }

    public void updateProfile(Profile input) {
        ContentValues value = new ContentValues();
        if(input.getFirstName() != null) {
            value.put(KEY_FIRSTNAME, input.getFirstName());
        }
        if(input.getLastName() != null) {
            value.put(KEY_LASTNAME, input.getLastName());
        }
        if(input.getUsername() != null) {
            value.put(KEY_USERNAME, input.getUsername());
        }
        if(input.getEmail() != null) {
            value.put(KEY_EMAIL, input.getEmail());
        }
        if(input.getMoney() != 0) {
            value.put(KEY_MONEY, input.getMoney());
        }
        if(input.getExperience() != 0) {
            value.put(KEY_EXPERIENCE, input.getExperience());
        }
        SQLiteDatabase db = getWritableDatabase();
        db.update(TABLE_PROFILES, value, KEY_ID + " = ?", new String[]{"" + input.getId()});
    }

}