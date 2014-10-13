package com.example.tom.myapplication;

import android.content.ContentValues;
import android.content.Context;
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



    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_LOGS + " (" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_ACTION + " TEXT, " + KEY_DATETIME + " DATETIME)");
        db.execSQL("CREATE TABLE " + TABLE_PROFILES + " (" + KEY_ID + " INTEGER PRIMARY KEY NOT NULL UNIQUE, " + KEY_FIRSTNAME + " TEXT, " + KEY_LASTNAME + " TEXT, " + KEY_USERNAME + " TEXT, " + KEY_EMAIL + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROFILES);
        onCreate(db);
    }

    /*public void addLog(DBLog log) {
        Log.d("addLog", "adding a log");
        ContentValues input = new ContentValues();
        input.put(KEY_ACTION, log.getAction());
        input.put(KEY_DATETIME, log.getDatetime());
        SQLiteDatabase db = getWritableDatabase();
        db.insert(TABLE_LOGS, null, input);
    }*/

    public void storeProfile(Profile input){
        ContentValues value = new ContentValues();
        if(input.getId() != 0) {
            value.put(KEY_ID, input.getId());
        }
        value.put(KEY_FIRSTNAME, input.getFirstName());
        value.put(KEY_LASTNAME, input.getLastName());
        value.put(KEY_USERNAME, input.getUsername());
        value.put(KEY_EMAIL, input.getEmail());
        SQLiteDatabase db = getWritableDatabase();
        db.insert(TABLE_PROFILES, null, value);
    }
}