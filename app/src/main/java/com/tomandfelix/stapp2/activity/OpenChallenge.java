package com.tomandfelix.stapp2.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.gc.materialdesign.views.ButtonRectangle;
import com.gc.materialdesign.views.ProgressBarDeterminate;
import com.tomandfelix.stapp2.R;
import com.tomandfelix.stapp2.gcm.GCMMessageHandler;
import com.tomandfelix.stapp2.persistency.Challenge;
import com.tomandfelix.stapp2.persistency.GCMMessage;
import com.tomandfelix.stapp2.persistency.Profile;
import com.tomandfelix.stapp2.persistency.ServerHelper;

import java.util.ArrayList;
import java.util.Date;

public class OpenChallenge extends ServiceActivity {
    private static Handler handler;
    private ListView openChallengeList;
    private OpenChallengeListAdapter adapter;
    private ArrayList<Profile> mProfileList = new ArrayList<>();
    private Challenge challenge;
    private View buttons;
    private ButtonRectangle negativeButton;
    private ButtonRectangle positiveButton;
    private ProgressBarDeterminate progress;
    private TextView resultView;
    public static final int MSG_REFRESH = 1;
    private static final String START = "Start";
    private static final String WAITING = "Waiting";
    private static final String ACCEPT = "Accept";
    private static final String DECLINE = "Decline";
    private static final String DISMISS = "Dismiss";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_open_challenge);
        super.onCreate(savedInstanceState);

        openChallengeList = (ListView) findViewById(R.id.open_challenge_list_view);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        buttons = findViewById(R.id.open_challenge_buttons);
        negativeButton = (ButtonRectangle) findViewById(R.id.open_challenge_negative_btn);
        positiveButton = (ButtonRectangle) findViewById(R.id.open_challenge_positive_btn);
        progress = (ProgressBarDeterminate) findViewById(R.id.open_challenge_progress);
        resultView = (TextView) findViewById(R.id.open_challenge_result);

        Intent intent = getIntent();
        int challengeIndex = intent.getIntExtra("challenge_index", 0);
        challenge = GCMMessageHandler.challenges.get(challengeIndex);
        updateChallengeViews();

        ServerHelper.getInstance().getProfilesByIds(challenge.getOpponents(), new ServerHelper.ResponseFunc<ArrayList<Profile>>() {
            @Override
            public void onResponse(ArrayList<Profile> response) {
                mProfileList = response;
                updateProfileViews();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e("OpenChallenge", volleyError.getMessage());
            }
        }, false);
    }

    public static Handler getHandler() {
        return handler;
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler = new OpenHandler();
        updateChallengeViews();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
        handler = null;
    }

    private void updateChallengeViews() {
        switch(challenge.getState()) {
            case Challenge.REQ_SENT:
                buttons.setVisibility(View.VISIBLE);
                progress.setVisibility(View.INVISIBLE);
                resultView.setVisibility(View.INVISIBLE);
                negativeButton.setVisibility(View.GONE);
                positiveButton.setVisibility(View.VISIBLE);
                positiveButton.setText(WAITING);
                positiveButton.setEnabled(false);
                break;
            case Challenge.ACCEPTED:
                buttons.setVisibility(View.VISIBLE);
                progress.setVisibility(View.INVISIBLE);
                resultView.setVisibility(View.INVISIBLE);
                negativeButton.setVisibility(View.GONE);
                positiveButton.setVisibility(View.VISIBLE);
                positiveButton.setText(START);
                positiveButton.setEnabled(true);
                break;
            case Challenge.REQ_REC:
                buttons.setVisibility(View.VISIBLE);
                progress.setVisibility(View.INVISIBLE);
                resultView.setVisibility(View.INVISIBLE);
                negativeButton.setVisibility(View.VISIBLE);
                negativeButton.setText(DECLINE);
                positiveButton.setVisibility(View.VISIBLE);
                positiveButton.setText(ACCEPT);
                positiveButton.setEnabled(true);
                break;
            case Challenge.STARTED:
                buttons.setVisibility(View.INVISIBLE);
                progress.setVisibility(View.VISIBLE);
                resultView.setVisibility(View.INVISIBLE);
                progress.setMin(0);
                progress.setMax(challenge.getDuration());
                progress.setProgress((int) ((new Date().getTime() - challenge.getStartTime().getTime()) / 1000));
                startTimer();
                break;
            case Challenge.WAITING:
                buttons.setVisibility(View.INVISIBLE);
                progress.setVisibility(View.INVISIBLE);
                resultView.setVisibility(View.VISIBLE);
                resultView.setText("Waiting for results from other players");
                break;
            case Challenge.DONE:
                buttons.setVisibility(View.VISIBLE);
                progress.setVisibility(View.INVISIBLE);
                resultView.setVisibility(View.VISIBLE);
                resultView.setText(challenge.getStateMessage());
                negativeButton.setVisibility(View.GONE);
                positiveButton.setVisibility(View.VISIBLE);
                positiveButton.setText(DISMISS);
                positiveButton.setEnabled(true);
                break;
        }
    }

    private void startTimer() {
        progress.postDelayed(new Runnable() {
            @Override
            public void run() {
                progress.setProgress(progress.getProgress() + 1);
                if(progress.getProgress() != challenge.getDuration()) {
                    startTimer();
                }
            }
        }, 1000);
    }

    public void onPositiveButton(View v) {
        switch(challenge.getState()) {
            case Challenge.ACCEPTED:
                challenge.startChallenge();
                break;
            case Challenge.REQ_REC:
                challenge.sendMessage(GCMMessage.ACCEPTED, "");
                break;
            case Challenge.DONE:
                GCMMessageHandler.challenges.remove(challenge);
                finish();
                break;
        }
        updateChallengeViews();
    }

    public void onNegativeButton(View v) {
        switch(challenge.getState()) {
            case Challenge.REQ_REC:
                challenge.sendMessage(GCMMessage.DECLINED, "");
                finish();
                break;
        }
        updateChallengeViews();
    }

    private void updateProfileViews() {
        adapter = new OpenChallengeListAdapter(OpenChallenge.this, R.layout.list_item_open_challenge, mProfileList);
        openChallengeList.setAdapter(adapter);

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

                username.setTextColor(normalColor);

                if(rank != null) {rank.setText(Integer.toString(p.getRank()));}
                if(avatar != null) {avatar.setImageResource(avatarID);}
                if(username != null) {username.setText(p.getUsername());}
            }
            return convertView;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }
    }

    private class OpenHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == MSG_REFRESH) {
                updateChallengeViews();
            }
        }
    }
}