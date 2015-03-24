package com.tomandfelix.stapp2.activity;

import android.content.Intent;
import android.support.v4.app.NavUtils;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.tomandfelix.stapp2.R;
import com.tomandfelix.stapp2.gcm.GCMMessageHandler;
import com.tomandfelix.stapp2.persistency.Challenge;
import com.tomandfelix.stapp2.persistency.ChallengeList;
import com.tomandfelix.stapp2.persistency.GCMMessage;
import com.tomandfelix.stapp2.persistency.Profile;
import com.tomandfelix.stapp2.persistency.ServerHelper;

public class ChallengeStranger extends ServiceActivity {
    Profile mProfile;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_challenge_stranger);
        super.onCreate(savedInstanceState);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        int strangerId = getIntent().getIntExtra("strangerId", -1);
        ServerHelper.getInstance().getOtherProfile(strangerId,
                new ServerHelper.ResponseFunc<Profile>() {
                    @Override
                    public void onResponse(Profile response) {
                        mProfile = response;
                        updateVisual(response);
                    }
                }, null, false); //TODO make this a meaningful errorListener

    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if(item.getItemId() == android.R.id.home) {
//            NavUtils.navigateUpFromSameTask(this);
//            return true;
//        } else {
//            return super.onOptionsItemSelected(item);
//        }
//    }

    private void updateVisual(Profile profile) {
        Challenge challenge = ChallengeList.challenges.get(0);
        TextView rank = (TextView) findViewById(R.id.challenger_rank);
        TextView username = (TextView) findViewById(R.id.challenger_username);
        ImageView avatar = (ImageView) findViewById(R.id.challenger_avatar);
        TextView challengeTitle = (TextView) findViewById(R.id.challenge_title);
        TextView challengeDescription = (TextView) findViewById(R.id.challenge_description);
        int avatarID = getResources().getIdentifier("avatar_" + profile.getAvatar() + "_512", "drawable", getPackageName());

        rank.setText( profile.getRank() + "");
        username.setText(profile.getUsername());
        avatar.setImageResource(avatarID);
        challengeTitle.setText( challenge.getName());
        challengeDescription.setText(challenge.getDescription() + "\n" + "duration : " + challenge.getDuration() + " seconds");
        getSupportActionBar().setTitle("Challenge " + profile.getUsername());
    }
    public void toChallenge(View view){
        int[] ids =  new int[1];
        ids[0] = mProfile.getId();
        ServerHelper.getInstance().sendMessage(new GCMMessage(ids, ChallengeList.challenges.get(0).getId(), GCMMessage.REQUEST, 0, ""), new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                if(!volleyError.getMessage().equals("none")) {
                    Log.e("GCMTestActivity", volleyError.getMessage());
                }
            }
        });
        Intent intent = new Intent(this, ProfileView.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
