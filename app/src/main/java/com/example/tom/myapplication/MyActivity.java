package com.example.tom.myapplication;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

public class MyActivity extends Activity {

    private DatabaseHelper dbh;
    private ServerHelper sh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        dbh = new DatabaseHelper(this);
        sh = new ServerHelper(dbh);
        sh.deleteProfile(1, "secret");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return (id == R.id.action_settings || super.onOptionsItemSelected(item));
    }

    public void submitProfile(View v) {
        Log.d("submitProfile", "storing profile");
        EditText firstName = (EditText) findViewById(R.id.firstName);
        EditText lastName = (EditText) findViewById(R.id.lastName);
        EditText username = (EditText) findViewById(R.id.username);
        EditText email = (EditText) findViewById(R.id.email);
        EditText password = (EditText) findViewById(R.id.password);
        sh.createProfile(firstName.getText().toString(), lastName.getText().toString(), username.getText().toString(), email.getText().toString(), password.getText().toString());
    }

    public void getProfile (View v) {
        Log.d("getProfile", "getting Profile");
        EditText username = (EditText) findViewById(R.id.username);
        EditText password = (EditText) findViewById(R.id.password);
        sh.getProfile(username.getText().toString(), password.getText().toString());
    }

    public void getOtherProfile (View v) {
        EditText id = (EditText) findViewById(R.id.id);
        Log.d("getOtherProfile", "getting profile " + Integer.parseInt(id.getText().toString()));
        sh.getOtherProfile(Integer.parseInt(id.getText().toString()));
    }

    public void getLeaderboard (View v) {
        EditText id = (EditText) findViewById(R.id.id);
        Log.d("getLeaderboard", "getting leaderboard for id " + Integer.parseInt(id.getText().toString()));
        sh.getLeaderboard(Integer.parseInt(id.getText().toString()));
    }
}