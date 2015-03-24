package com.tomandfelix.stapp2.activity;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
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

import java.util.ArrayList;

public class OpenChallenge extends ActionBarActivity {
    private ListView openChallengeList;
    private OpenChallengeListAdapter adapter;
    private ArrayList<Profile> mProfileList = new ArrayList<>();
    private Challenge challenge;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_open_challenge);
        super.onCreate(savedInstanceState);
        openChallengeList = (ListView) findViewById(R.id.open_challenge_list_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        Intent intent = getIntent();

        int challengeId = intent.getIntExtra("challenge_id", 0);
        int[] opponentIds = intent.getIntArrayExtra("opponent_ids");
        for(int i = 0 ; i < ChallengeList.challenges.size();i++) {
            if(ChallengeList.challenges.get(i).getId() == challengeId){
                challenge = ChallengeList.challenges.get(i);
            }
        }

        ServerHelper.getInstance().getProfilesByIds(opponentIds, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e("error", "list of opponents");
            }
        }, new ServerHelper.VolleyCallback() {
            @Override
            public void getResult(float result) {

            }

            @Override
            public void getResultArray(ArrayList<Profile> result) {
                 mProfileList = result;
                updateVisual();
            }
        });
    }

    private void updateVisual() {
        adapter = new OpenChallengeListAdapter(OpenChallenge.this, R.layout.list_item_open_challenge, mProfileList);

        openChallengeList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            }
        });

        TextView challengeTitle = (TextView) findViewById(R.id.open_challenge_title);
        TextView challengeDescription = (TextView) findViewById(R.id.open_challenge_description); challengeTitle.setText(challenge.getName());
        challengeDescription.setText(challenge.getDescription() + "\n" + "duration : " + challenge.getDuration() + " seconds");
    }

    private class OpenChallengeListAdapter extends ArrayAdapter<Profile> {
        private int normalColor = getResources().getColor(R.color.secondaryText);
        private ArrayList<Profile> data;
        private int itemLayoutId;

        public OpenChallengeListAdapter(Context context, int itemLayoutId, ArrayList<Profile> data) {
            super(context, itemLayoutId, data);
            this.data = data;
            this.itemLayoutId = itemLayoutId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(itemLayoutId, parent, false);
            }
            Profile p = data.get(position);

            if(p != null) {
                TextView rank = (TextView) convertView.findViewById(R.id.open_challenge_rank);
                ImageView avatar = (ImageView) convertView.findViewById(R.id.open_challenge_avatar);
                TextView username = (TextView) convertView.findViewById(R.id.open_challenge_username);


                int avatarID = getResources().getIdentifier("avatar_" + p.getAvatar() +"_128", "drawable", getPackageName());

                rank.setTextColor(normalColor);
                username.setTextColor(normalColor);

                if(rank != null) {rank.setText(Integer.toString(p.getRank()));}
                if(avatar != null) {avatar.setImageResource(avatarID);}
                if(username != null) {username.setText(p.getUsername());}
            }
            return convertView;
        }
    }
}