package com.example.tom.stapp3.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.tom.stapp3.R;
import com.example.tom.stapp3.persistency.DatabaseHelper;
import com.example.tom.stapp3.persistency.Profile;
import com.example.tom.stapp3.persistency.ServerHelper;

/**
 * Created by Tom on 3/11/2014.
 * This is the first activity, it is a viewpager with 3 fragments: login, start and register
 */
public class FragmentViewer extends FragmentActivity implements FragmentProvider.OnFragmentInteractionListener{
    private static final int NUM_PAGES = 3;
    private ViewPager mPager;
    private String avatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String token = DatabaseHelper.getInstance(getApplicationContext()).getStringSetting(DatabaseHelper.TOKEN);
        if(token != null && !token.equals("")) {
            Log.d("start", "Token present");
            ServerHelper.getInstance(getApplicationContext()).getProfile(new ServerHelper.ResponseFunc<Profile>() {
                @Override
                public void onResponse(Profile response) {
                    if(response != null) {
                        Log.d("start", "Token accepted");
                        Intent intent = new Intent(getBaseContext(), ProfileView.class);
                        intent.putExtra("profile", response);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.d("start", "Token rejected");
                        loadStart();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    loadStart();
                }
            }, true);
        } else {
            Log.d("start", "Token absent");
            loadStart();
        }

        /*ArrayList<DBLog> logs = new ArrayList<>();
        logs.add(new DBLog("sit", new Date(2015, 1, 27, 18, 1, 0), ""));
        logs.add(new DBLog("stand", new Date(2015, 1, 27, 18, 1, 30), ""));
        logs.add(new DBLog("sensor_disconnect", new Date(2015, 1, 27, 18, 1, 45), ""));
        logs.add(new DBLog("sensor_connect", new Date(2015, 1, 27, 18, 2, 0), ""));
        logs.add(new DBLog("stand", new Date(2015, 1, 27, 18, 2, 30), ""));
        logs.add(new DBLog("sit", new Date(2015, 1, 27, 18, 3, 0), ""));
        logs.add(new DBLog("sensor_disconnect", new Date(2015, 1, 27, 18, 4, 0), ""));
        logs.add(new DBLog("sensor_connect", new Date(2015, 1, 27, 18, 5, 0), ""));
        logs.add(new DBLog("sit", new Date(2015, 1, 27, 18, 5, 30), ""));
        logs.add(new DBLog("stand", new Date(2015, 1, 27, 18, 5, 45), ""));
        Log.i("TIME_STOOD", Long.toString(Algorithms.millisecondsStood(logs)));
        Date start = new Date(2015, 1, 27, 18, 0, 45);
        DBLog logBefore = new DBLog ("stand", new Date(2015, 1, 27, 18, 0, 30), "");
        long result = Algorithms.millisecondsStood(logs);
        if(logBefore.getAction().equals(DatabaseHelper.LOG_STAND)) {
            result += logs.get(0).getDatetime().getTime() - start.getTime();
        }
        Log.i("TIME_STOOD SINCE", Long.toString(result));
        Date end = new Date(2015, 1, 27, 18, 6, 0);
        if(logs.get(logs.size() - 1).getAction().equals(DatabaseHelper.LOG_STAND)) {
            result += end.getTime() - logs.get(logs.size() - 1).getDatetime().getTime();
        }
        Log.i("TIME_STOOD SINCE, UNTIL", Long.toString(result));*/
    }

    private void loadStart() {
        setContentView(R.layout.activity_start);
        PagerAdapter mAdapter = new FragmentAdapter(getFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(1, false);
        DatabaseHelper.getInstance(this).setReadable();
        avatar = "manager";
    }

    public void loginBtn(final View v) {
        v.setEnabled(false);
        final EditText username = (EditText) findViewById(R.id.login_username);
        final EditText password = (EditText) findViewById(R.id.login_password);
        final TextView txtView = (TextView) findViewById(R.id.succes);
        DatabaseHelper.getInstance(getApplicationContext()).setSetting(DatabaseHelper.LAST_ENTERED_USERNAME, username.getText().toString());
        ServerHelper.getInstance(this).login(username.getText().toString(), password.getText().toString(), new ServerHelper.ResponseFunc<Profile>() {
            @Override
            public void onResponse(Profile response) {
                if(response != null) {
                    txtView.setText("profile is being loaded");
                    Intent intent = new Intent(getBaseContext(), ProfileView.class);
                    intent.putExtra("profile", response);
                    startActivity(intent);
                    overridePendingTransition(R.anim.enter_top, R.anim.leave_bottom);
                } else {
                    txtView.setText("No such profile exists, please try again");
                    password.setText(null);
                    v.setEnabled(true);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("loginBtn", error.getMessage());
            }
        });

    }


    public void registerBtn(View v) {
        EditText firstName = (EditText) findViewById(R.id.new_firstname);
        EditText lastName = (EditText) findViewById(R.id.new_lastname);
        EditText username = (EditText) findViewById(R.id.new_username);
        EditText email = (EditText) findViewById(R.id.new_email);
        EditText password = (EditText) findViewById(R.id.new_password);

        ServerHelper.getInstance(this).createProfile(firstName.getText().toString(), lastName.getText().toString(), username.getText().toString(), email.getText().toString(), avatar, password.getText().toString(), new ServerHelper.ResponseFunc<Profile>() {
            @Override
            public void onResponse(Profile response) {
                Intent intent = new Intent(getBaseContext(), ProfileView.class);
                intent.putExtra("profile", response);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_top, R.anim.leave_bottom);
                finish();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("registerBtn", error.getMessage());
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

    @Override
    public void onFragmentInteraction(String message) {
        Log.i("onFragmentInteraction", message);
        avatar = message;
        setAvatarImage(message);
    }

    public void setAvatarImage(String avatar) {
        int avatarID = getResources().getIdentifier("avatar_" + avatar + "_512", "drawable", getPackageName());
        ImageView avatarView = (ImageView) findViewById(R.id.new_avatar);
        avatarView.setImageResource(avatarID);
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
