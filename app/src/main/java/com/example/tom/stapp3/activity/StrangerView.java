package com.example.tom.stapp3.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.tom.stapp3.Function;
import com.example.tom.stapp3.R;
import com.example.tom.stapp3.persistency.Profile;
import com.example.tom.stapp3.persistency.ServerHelper;


public class StrangerView extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stranger_view);
        if(getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        int strangerId = getIntent().getIntExtra("strangerId", -1);
        ServerHelper.getInstance(this).getOtherProfile(strangerId, new Function<Profile>() {
            @Override
            public void call(Profile param) {
                updateVisual(param);
            }
        }, false);
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
        ImageView avatar = (ImageView) findViewById(R.id.stranger_avatar);

        int avatarID = getResources().getIdentifier("avatar_" + profile.getAvatar(), "drawable", getPackageName());

        rank.setText("Rank: " + profile.getRank());
        username.setText(profile.getUsername());
        money.setText("Money: " + profile.getMoney());
        experience.setText("Experience: " + profile.getExperience());
        avatar.setImageResource(avatarID);
    }
}
