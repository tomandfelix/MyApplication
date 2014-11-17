package com.example.tom.myapplication;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


public class StrangerView extends Activity {
    private int strangerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stranger_view);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        strangerId = getIntent().getIntExtra("strangerId", -1);

        if(DatabaseHelper.getInstance().idPresent(strangerId)) {
            Profile profile = DatabaseHelper.getInstance().getProfile(strangerId);
            updateVisual(profile);

        }

        ServerHelper.getInstance().getRank(strangerId, new Function<RankedProfile>() {
            @Override
            public void call(RankedProfile param) {
                updateVisual(param);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void updateVisual(Profile profile) {
        TextView rank = (TextView) findViewById(R.id.stranger_rank);
        TextView username = (TextView) findViewById(R.id.stranger_username);
        TextView money = (TextView) findViewById(R.id.stranger_money);
        TextView experience = (TextView) findViewById(R.id.stranger_experience);

        if(profile instanceof RankedProfile) {
            rank.setText("Rank: " + ((RankedProfile) profile).getRank());
        }
        username.setText(profile.getUsername());
        money.setText("Money: " + profile.getMoney());
        experience.setText("Experience: " + profile.getExperience());
    }
}
