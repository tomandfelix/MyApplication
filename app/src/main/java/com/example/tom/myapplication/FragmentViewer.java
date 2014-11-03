package com.example.tom.myapplication;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

/**
 * Created by Tom on 3/11/2014.
 */
public class FragmentViewer extends FragmentActivity {
    private static final int NUM_PAGES = 3;
    private ViewPager mPager;
    private PagerAdapter mAdapter;
    private DatabaseHelper dbh;
    private ServerHelper sh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pager);
        dbh = new DatabaseHelper(this);
        sh = new ServerHelper(dbh);
        mAdapter = new FragmentAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(1, false);

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

    /*    public void submitProfile(View v) {
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
    }*/

    public void loginBtn(View v) {
        //EditText username = (EditText) rootView.findViewById(R.layout.)
    }

    public void toLogin(View v) {
        mPager.setCurrentItem(0, true);
    }

    public void newBtn(View v) {
        mPager.setCurrentItem(2, true);
    }

    public void backBtn(View v) {
        mPager.setCurrentItem(1, true);
    }

    private class FragmentAdapter extends FragmentPagerAdapter {
        public FragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }

        @Override
        public Fragment getItem(int position) {
            return FragmentProvider.init(position);
        }
    }
}
