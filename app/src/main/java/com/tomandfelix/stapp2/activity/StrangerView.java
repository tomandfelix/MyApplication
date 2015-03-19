package com.tomandfelix.stapp2.activity;

import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.tomandfelix.stapp2.R;
import com.tomandfelix.stapp2.persistency.Profile;
import com.tomandfelix.stapp2.persistency.ServerHelper;


public class StrangerView extends ServiceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_stranger_profile);
        super.onCreate(savedInstanceState);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        int strangerId = getIntent().getIntExtra("strangerId", -1);
        ServerHelper.getInstance(this).getOtherProfile(strangerId,
                new ServerHelper.ResponseFunc<Profile>() {
                    @Override
                    public void onResponse(Profile response) {
                        updateVisual(response);
                    }
                }, null, false); //TODO make this a meaningful errorListener
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
        TextView experience = (TextView) findViewById(R.id.stranger_experience_text);
        ImageView avatar = (ImageView) findViewById(R.id.stranger_avatar);

        int avatarID = getResources().getIdentifier("avatar_" + profile.getAvatar() + "_512", "drawable", getPackageName());

        rank.setText( profile.getRank() + "");
        username.setText(profile.getUsername());
        experience.setText("" + profile.getExperience());
        avatar.setImageResource(avatarID);
    }
}
