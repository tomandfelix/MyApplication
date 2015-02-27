package com.example.tom.stapp3.persistency;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.tom.stapp3.BuildConfig;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;


/**
 * Created by Tom on 6/10/2014.
 * Works the local database for the application
 */
public class DatabaseHelper extends SQLiteOpenHelper{
    private static DatabaseHelper uniqueInstance = null;
    private static final int DATABASE_VERSION = 1;
    private static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    private static final String DATABASE_NAME = "data.db";
    private static final String TABLE_LOGS = "logs";
    private static final String TABLE_PROFILES = "profiles";
    private static final String TABLE_SETTINGS = "settings";
    private static final String TABLE_SENSORS = "sensors";
    private static final String KEY_ID = "id";
    private static final String KEY_ACTION = "action";
    private static final String KEY_DATETIME = "datetime";
    private static final String KEY_METADATA = "metadata";
    private static final String KEY_FIRSTNAME = "firstname";
    private static final String KEY_LASTNAME = "lastname";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_MONEY = "money";
    private static final String KEY_EXPERIENCE = "experience";
    private static final String KEY_AVATAR = "avatar";
    private static final String KEY_RANK = "rank";
    private static final String KEY_UPDATED = "lastUpdated";
    private static final String KEY_SETTING = "name";
    private static final String KEY_VALUE_INT = "intValue";
    private static final String KEY_VALUE_STRING = "stringValue";
    private static final String KEY_MAC = "mac_address";
    private static final String KEY_FRIENDLY_NAME = "friendly_name";
    public static final String OWNER = "owner";
    public static final String NOTIF = "notification";
    public static final String TOKEN = "token";
    public static final String LAST_ENTERED_USERNAME  = "LEU";
    public static final String LOG_SIT = "sit";
    public static final String LOG_OVERTIME = "sit_overtime";
    public static final String LOG_STAND = "stand";
    public static final String LOG_START_DAY = "begin_day";
    public static final String LOG_STOP_DAY = "end_day";
    public static final String LOG_CONNECT = "sensor_connect";
    public static final String LOG_DISCONNECT = "sensor_disconnect";
    public static final String LOG_ACH_SCORE = "achieved_score";
    public static final String LOG_ACH_SCORE_PERC = "achieved_score_percent";

    public static DatabaseHelper getInstance(Context context) {
        if(uniqueInstance == null) {
            uniqueInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return uniqueInstance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_LOGS + " (" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_ACTION + " TEXT, " + KEY_DATETIME + " DATETIME, " + KEY_METADATA + " TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_PROFILES + " (" + KEY_ID + " INTEGER PRIMARY KEY NOT NULL UNIQUE, " + KEY_FIRSTNAME + " TEXT, " + KEY_LASTNAME + " TEXT, " + KEY_USERNAME + " TEXT, " + KEY_EMAIL + " TEXT, " + KEY_MONEY + " INT, " + KEY_EXPERIENCE + " INT, " + KEY_AVATAR + " TEXT, " + KEY_RANK + " INT, " + KEY_UPDATED + " DATETIME)");
        db.execSQL("CREATE TABLE " + TABLE_SETTINGS + " (" + KEY_SETTING + " TEXT PRIMARY KEY NOT NULL UNIQUE, " + KEY_VALUE_INT + " INTEGER, " + KEY_VALUE_STRING + " TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_SENSORS + " (" + KEY_MAC + " TEXT PRIMARY KEY NOT NULL UNIQUE, " + KEY_FRIENDLY_NAME + " TEXT)");
        ContentValues values = new ContentValues(2);
        values.put(KEY_SETTING, OWNER);
        values.put(KEY_VALUE_INT, -1);
        db.insert(TABLE_SETTINGS, null, values);
        values.clear();
        values.put(KEY_SETTING, NOTIF);
        values.put(KEY_VALUE_INT, 0);
        db.insert(TABLE_SETTINGS, null, values);
        values.clear();
        values.put(KEY_SETTING, TOKEN);
        values.put(KEY_VALUE_STRING, "");
        db.insert(TABLE_SETTINGS, null, values);
        values.clear();
        values.put(KEY_SETTING, LAST_ENTERED_USERNAME);
        values.put(KEY_VALUE_STRING, "");
        db.insert(TABLE_SETTINGS, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROFILES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SENSORS);
        onCreate(db);
    }

    private int secondsAgo(Date input) {
        return Math.round(TimeUnit.MILLISECONDS.toSeconds(new Date().getTime() - input.getTime()));
    }

    private String dateToString(Date date) {
        return df.format(date);
    }

    private Date stringToDate(String dateString) {
        try {
            return df.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void truncateLogs() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_LOGS + "; VACUUM;");
        db.close();
    }

    //--------------------------------------------------------LOGS------------------------------------------------------------------------------

    private void addLog(String action, Date datetime, String metadata) {
        ContentValues input = new ContentValues(2);
        input.put(KEY_ACTION, action);
        input.put(KEY_DATETIME, dateToString(datetime));
        input.put(KEY_METADATA, metadata == null ? "" : metadata);
        SQLiteDatabase db = getWritableDatabase();
        db.insert(TABLE_LOGS, null, input);
        db.close();
    }

    public DBLog getLastLog() {
        Log.i("getLastLog", "getting last log");
        String query = "SELECT " + KEY_ACTION + ", " + KEY_DATETIME + ", " + KEY_METADATA +  " FROM " + TABLE_LOGS + " WHERE " + KEY_ACTION + " IN('" + LOG_SIT + "', '" + LOG_STAND + "', '" + LOG_CONNECT + "', '" + LOG_OVERTIME + "') ORDER BY " + KEY_ID + " DESC LIMIT 1";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        DBLog result = null;
        if(cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            result = new DBLog(cursor.getString(0), stringToDate(cursor.getString(1)), cursor.getString(2));
        }
        db.close();
        return result;
    }
    public ArrayList<DBLog> getTodaysConnectionLogs() {
        Log.i("getTodaysConnectionLogs", "getting today's connect & disconnect logs");
        String query = "SELECT " + KEY_ACTION + ", " + KEY_DATETIME + ", " + KEY_METADATA + " FROM " + TABLE_LOGS + " WHERE " + KEY_ACTION + " IN('" + LOG_CONNECT + "', '" + LOG_DISCONNECT + "') AND " + KEY_ID + " > (SELECT " + KEY_ID + " FROM " + TABLE_LOGS + " WHERE " + KEY_ACTION + " = '" + LOG_START_DAY + "' ORDER BY " + KEY_ID + " DESC LIMIT 1) ORDER BY " + KEY_ID + " ASC";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        ArrayList<DBLog> result = null;
        if(cursor != null && cursor.getCount() > 0) {
            result = new ArrayList<>();
            while(cursor.moveToNext()) {
                result.add(new DBLog(cursor.getString(0), stringToDate(cursor.getString(1)), cursor.getString(2)));
            }
        }
        db.close();
        return result;
    }

    public ArrayList<DBLog> getLogsBetween(Date start, Date end) {
        Log.i("getLogsBetween", "getting logs between " + dateToString(start) + " and " + dateToString(end));
        String query = "SELECT " + KEY_ACTION + ", " + KEY_DATETIME + ", " + KEY_METADATA +  " FROM " + TABLE_LOGS + " WHERE " + KEY_DATETIME + " > '" + dateToString(start) + "' AND " + KEY_DATETIME + " < '" + dateToString(end) + "' ORDER BY " + KEY_ID + " ASC";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        ArrayList<DBLog> result = null;
        if(cursor != null && cursor.getCount() > 0) {
            result = new ArrayList<>();
            while(cursor.moveToNext()) {
                result.add(new DBLog(cursor.getString(0), stringToDate(cursor.getString(1)), cursor.getString(2)));
            }
        }
        db.close();
        return result;
    }

    public DBLog getLastSitStand() {
        Log.i("dayStarted", "checking");
        String query = "SELECT " + KEY_ACTION + ", " + KEY_DATETIME + ", " + KEY_METADATA + " FROM " + TABLE_LOGS + " WHERE " + KEY_ACTION + " IN('" + LOG_SIT + "', '" + LOG_STAND + "') ORDER BY " + KEY_ID + " DESC LIMIT 1";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        DBLog result = null;
        if(cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            result = new DBLog(cursor.getString(0), stringToDate(cursor.getString(1)), cursor.getString(2));
        }
        db.close();
        return result;
    }

    public DBLog getLastLogBefore(Date dateTime) {
        String query = "SELECT " + KEY_ACTION + ", " + KEY_DATETIME + ", " + KEY_METADATA + " FROM " + TABLE_LOGS + " WHERE " + KEY_DATETIME + " < '" + dateToString(dateTime) + "' ORDER BY " + KEY_ID + " DESC LIMIT 1";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        DBLog result = null;
        if(cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            result =  new DBLog(cursor.getString(0), stringToDate(cursor.getString(1)), cursor.getString(2));
        }
        db.close();
        return result;
    }

    public DBLog getFirstRecordOfDay() {
        String query = "SELECT " + KEY_ACTION + ", " + KEY_DATETIME + ", " + KEY_METADATA + " FROM " + TABLE_LOGS + " WHERE " + KEY_ACTION + " IN('" + LOG_SIT + "', '" + LOG_STAND + "') AND " + KEY_ID + " > (SELECT " + KEY_ID + " FROM " + TABLE_LOGS + " WHERE " + KEY_ACTION + " = '" + LOG_START_DAY + "' ORDER BY " + KEY_ID + " DESC LIMIT 1) ORDER BY " + KEY_ID + " ASC LIMIT 1";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        DBLog result = null;
        if(cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            result = new DBLog(cursor.getString(0), stringToDate(cursor.getString(1)), cursor.getString(2));
        }
        db.close();
        return result;
    }

    public Date dayStarted() {
        Log.i("dayStarted", "checking");
        String query = "SELECT " + KEY_ACTION + ", " + KEY_DATETIME + " FROM " + TABLE_LOGS + " WHERE " + KEY_ACTION + " IN('" + LOG_START_DAY + "', '" + LOG_STOP_DAY + "') ORDER BY " + KEY_ID + " DESC LIMIT 1";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        Date result = null;
        if(cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            result = cursor.getString(0).equals(LOG_START_DAY) ? stringToDate(cursor.getString(1)) : null;
        }
        db.close();
        return result;
    }

    public void startDay() {
        Log.i("startDay", "starting day");
        addLog(LOG_START_DAY, new Date(), null);
    }

    public boolean tryEndDay() {
        Log.i("tryEndDay", "trying to end day");
        if(dayStarted() != null && secondsAgo(dayStarted()) >= 36000) {
            addLog(LOG_STOP_DAY, new Date(), null);
            return true;
        }
        return false;
    }

    public void endDay(Date stopTime, double achieved, double percent, double connectionTime) {
        Log.i("endDay", "ending day");
        addLog(LOG_ACH_SCORE, stopTime, Double.toString(achieved));
        addLog(LOG_ACH_SCORE_PERC, stopTime, Double.toString(percent));
        addLog(LOG_STOP_DAY, stopTime, Double.toString(connectionTime));
    }

    public void addConnectionStatus(boolean connected) {
        Log.i("addConnectionStatus", connected ? "connected" : "disconnected");
        Date time = new Date();
        DBLog before = getLastLogBefore(time);
        if(!before.getAction().equals(connected ? LOG_CONNECT : LOG_DISCONNECT)) {
            addLog(connected ? LOG_CONNECT : LOG_DISCONNECT, time, null);
        }
    }

    public void addSitStand(Date datetime, boolean standing, String metadata) {
        Log.i("addSitStand", standing ? "stand" : "sit");
        addLog(standing ? LOG_STAND : LOG_SIT, datetime, metadata);
    }

    public void addSitOvertime(Date datetime, String metadata) {
        Log.i("addSitOvertime", metadata);
        addLog(LOG_OVERTIME, datetime, metadata);
    }

    //--------------------------------------------------------PROFILES--------------------------------------------------------------------------

    public void storeProfile(Profile input){
        if(getProfile(input.getId()) != null) {
            updateProfile(input);
        } else {
            ContentValues value = new ContentValues(7);
            if (input.getId() != 0) {
                value.put(KEY_ID, input.getId());
            }
            value.put(KEY_FIRSTNAME, input.getFirstName());
            value.put(KEY_LASTNAME, input.getLastName());
            value.put(KEY_USERNAME, input.getUsername());
            value.put(KEY_EMAIL, input.getEmail());
            value.put(KEY_MONEY, input.getMoney());
            value.put(KEY_EXPERIENCE, input.getExperience());
            value.put(KEY_AVATAR, input.getAvatar());
            value.put(KEY_RANK, input.getRank());
            value.put(KEY_UPDATED, dateToString(input.getLastUpdate()));
            SQLiteDatabase db = getWritableDatabase();
            db.insert(TABLE_PROFILES, null, value);
            db.close();
        }
    }

    /**
     * updates the local profiles database with the given input profile where the id from the database matches the id in the given profile
     * when an update of a specific field is not needed, insert null for a string or -1 for an int.
     * if profile.id is -1, nothing happens
     * @param input the input profile
     */
    public void updateProfile(Profile input) {
        if(input != null && input.getId() != -1) {
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
            if(input.getMoney() != -1) {
                value.put(KEY_MONEY, input.getMoney());
            }
            if(input.getExperience() != -1) {
                value.put(KEY_EXPERIENCE, input.getExperience());
            }
            if(input.getAvatar() != null) {
                value.put(KEY_AVATAR, input.getAvatar());
            }
            if(input.getRank() != -1) {
                value.put(KEY_RANK, input.getRank());
            }
            if(input.getLastUpdate() != null) {
                value.put(KEY_UPDATED, dateToString(input.getLastUpdate()));
            }
            if(value.size() == 0)
                return;
            SQLiteDatabase db = getWritableDatabase();
            db.update(TABLE_PROFILES, value, KEY_ID + " = ?", new String[]{Integer.toString(input.getId())});
            db.close();
        }
    }

    public Profile getProfile(int id) {
        String query = "SELECT " + KEY_FIRSTNAME + ", " + KEY_LASTNAME + ", " + KEY_USERNAME + ", " + KEY_EMAIL + ", " + KEY_MONEY + ", " + KEY_EXPERIENCE + ", " + KEY_AVATAR + ", " + KEY_RANK + ", " + KEY_UPDATED + " FROM " + TABLE_PROFILES + " WHERE " + KEY_ID + " = ?";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[] {Integer.toString(id)});
        Profile result = null;
        if(cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            result = new Profile(id, cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getInt(4), cursor.getInt(5), cursor.getString(6), cursor.getInt(7), stringToDate(cursor.getString(8)));
        }
        db.close();
        return result;
    }

    public Profile getProfile(String username) {
        String query = "SELECT " + KEY_ID + ", " + KEY_FIRSTNAME + ", " + KEY_LASTNAME + ", " + KEY_EMAIL + ", " + KEY_MONEY + ", " + KEY_EXPERIENCE + ", " + KEY_AVATAR + ", " + KEY_RANK + ", " + KEY_UPDATED + " FROM " + TABLE_PROFILES + " WHERE " + KEY_USERNAME + " = ?";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[] {username});
        Profile result = null;
        if(cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            result = new Profile(cursor.getInt(0), cursor.getString(1), cursor.getString(2), username, cursor.getString(3), cursor.getInt(4), cursor.getInt(5), cursor.getString(6), cursor.getInt(7), stringToDate(cursor.getString(8)));
        }
        db.close();
        return result;
    }

    public ArrayList<Profile> getLeaderboardByRank(int rank) {
        ArrayList<Profile> prof= null;
        rank -= (rank % 10) == 0 ? 10 : rank % 10;
        String query = "SELECT " + KEY_ID + ", " + KEY_FIRSTNAME + ", " + KEY_LASTNAME + ", " + KEY_USERNAME + ", " + KEY_EMAIL + ", " + KEY_MONEY + ", " + KEY_EXPERIENCE + ", " + KEY_AVATAR + ", " + KEY_RANK + ", " + KEY_UPDATED + " FROM " + TABLE_PROFILES + " ORDER BY " + KEY_RANK + " ASC LIMIT ?, 10";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{Integer.toString(rank)});
        if(cursor != null && cursor.getCount() == 10) {
            prof = new ArrayList<>(10);
            while(cursor.moveToNext()) {
                prof.add(new Profile(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getInt(5), cursor.getInt(6), cursor.getString(7), cursor.getInt(8), stringToDate(cursor.getString(9))));
            }
        }
        db.close();
        return prof == null ? null : prof.size() == 10 ? prof : null;
    }

    //--------------------------------------------------------SETTINGS--------------------------------------------------------------------------

    public int getIntSetting(String name) {
        String query = "SELECT " + KEY_VALUE_INT + " FROM " + TABLE_SETTINGS + " WHERE " + KEY_SETTING + " = ?";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{name});
        int result = -1;
        if(cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            result = cursor.getInt(0);
        }
        db.close();
        return result;
    }

    public String getStringSetting(String name) {
        String query = "SELECT " + KEY_VALUE_STRING + " FROM " + TABLE_SETTINGS + " WHERE " + KEY_SETTING + " = ?";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{name});
        String result = null;
        if(cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            result = cursor.getString(0);
        }
        db.close();
        return result;
    }

    public void setSetting(String name, int value) {
        ContentValues input = new ContentValues(1);
        input.put(KEY_VALUE_INT, value);
        SQLiteDatabase db = getWritableDatabase();
        db.update(TABLE_SETTINGS, input, KEY_SETTING + " = ?", new String[]{name});
        db.close();
    }

    public void setSetting(String name, String value) {
        ContentValues input = new ContentValues(1);
        input.put(KEY_VALUE_STRING, value);
        SQLiteDatabase db = getWritableDatabase();
        db.update(TABLE_SETTINGS, input, KEY_SETTING + " = ?", new String[]{name});
        db.close();
    }

    //--------------------------------------------------------SENSORS---------------------------------------------------------------------------

    public void setFriendlyName(String macAddress, String friendlyName) {
        ContentValues input = new ContentValues(2);
        input.put(KEY_MAC, macAddress);
        input.put(KEY_FRIENDLY_NAME, friendlyName);
        SQLiteDatabase db = getWritableDatabase();
        db.insert(TABLE_SENSORS, null, input);
        db.close();
    }

    public void updateFriendlyName(String macAddress, String newFriendlyName) {
        ContentValues input = new ContentValues(1);
        input.put(KEY_FRIENDLY_NAME, newFriendlyName);
        SQLiteDatabase db = getWritableDatabase();
        db.update(TABLE_SENSORS, input, KEY_MAC + " = ?", new String[]{macAddress});
        db.close();
    }

    public String getFriendlyName(String macAddress) {
        String query = "SELECT " + KEY_FRIENDLY_NAME + " FROM " + TABLE_SENSORS + " WHERE " + KEY_MAC + " = ?";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{macAddress});
        String result = null;
        if(cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            result = cursor.getString(0);
        }
        db.close();
        return result;
    }
}