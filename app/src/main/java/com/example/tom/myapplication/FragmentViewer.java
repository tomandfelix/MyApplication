package com.example.tom.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

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

    /*public void getOtherProfile (View v) {
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
        final EditText username = (EditText) findViewById(R.id.login_username);
        final EditText password = (EditText) findViewById(R.id.login_password);
        final TextView txtView = (TextView) findViewById(R.id.succes);
        sh.getProfile(username.getText().toString(), password.getText().toString(), new Function<Profile>() {
            @Override
            public void call(Profile param) {
                        if(param != null) {
                            txtView.setText("Profile is being loaded");
                            Intent intent = new Intent(getBaseContext(), ProfileView.class);
                            intent.putExtra("profile", param);
                            startActivity(intent);
                            overridePendingTransition(R.anim.enter_top, R.anim.leave_bottom);
                        }else {
                            txtView.setText("No such profile excist, please try again");
                            username.setText(null);
                            password.setText(null);
                        }
            }
        });
    }


    public void registerBtn(View v) {
        EditText firstName = (EditText) findViewById(R.id.new_firstname);
        EditText lastName = (EditText) findViewById(R.id.new_lastname);
        EditText username = (EditText) findViewById(R.id.new_username);
        EditText email = (EditText) findViewById(R.id.new_email);
        EditText password = (EditText) findViewById(R.id.new_password);
        sh.createProfile(firstName.getText().toString(), lastName.getText().toString(), username.getText().toString(), email.getText().toString(), password.getText().toString(), new Function<Profile>() {
            @Override
            public void call(Profile param) {
                Intent intent = new Intent(getBaseContext(), ProfileView.class);
                intent.putExtra("profile", param);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_top, R.anim.leave_bottom);
            }
        });
    }

    public void toLogin(View v) {
        mPager.setCurrentItem(0, true);
    }

    public void toNewProfile(View v) {
        mPager.setCurrentItem(2, true);
    }

    public void toStart(View v) {
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
