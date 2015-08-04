package com.tomandfelix.stapp2.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.gc.materialdesign.views.ButtonRectangle;
import com.tomandfelix.stapp2.R;
import com.tomandfelix.stapp2.application.StApp;
import com.tomandfelix.stapp2.persistency.ChallengeList;
import com.tomandfelix.stapp2.persistency.DatabaseHelper;
import com.tomandfelix.stapp2.persistency.LiveChallenge;
import com.tomandfelix.stapp2.persistency.Profile;
import com.tomandfelix.stapp2.persistency.ServerHelper;

import java.util.ArrayList;

public class ChallengeLeaderboard extends ServiceActivity {
    private Profile mProfile;
    private ListView leaderboardList;
    private ButtonRectangle confirmBtn;
    private ChallengeLeaderboardAdapter adapter;
    private ArrayList<Profile> list;
    private ArrayList<Boolean> checked;
    private int count = 0;
    private int challengeID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_challenge_leaderboard);
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mProfile = DatabaseHelper.getInstance().getOwner();
        challengeID = getIntent().getExtras().getInt("challengeID", 0);
        confirmBtn = (ButtonRectangle) findViewById(R.id.challenge_leaderboard_confirm);

        checked = new ArrayList<>();
        leaderboardList = (ListView) findViewById(R.id.leaderboard_list);
        if(ServerHelper.getInstance().checkInternetConnection()) {
            ServerHelper.getInstance().getLeaderboardById(DatabaseHelper.getInstance().getOwnerId(),
                    new ServerHelper.ResponseFunc<ArrayList<Profile>>() {
                        @Override
                        public void onResponse(ArrayList<Profile> response) {
                            list = response;
                            for (int i = 0; i < response.size(); i++)
                                checked.add(false);
                            setupList();
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            final AlertDialog alertDialog = new AlertDialog.Builder(ChallengeLeaderboard.this).create();
                            alertDialog.setMessage("It seems like an error occured, please logout and try again");
                            alertDialog.setButton("Dismiss", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    alertDialog.dismiss();
                                }
                            });
                            alertDialog.show();
                        }
                    }, false);
        }else{
            Toast.makeText(getApplicationContext(),"Unable to get leaderboard, no internet connection", Toast.LENGTH_SHORT).show();
        }
        checkCount();
    }

    private void setupList() {
        adapter = new ChallengeLeaderboardAdapter(ChallengeLeaderboard.this, R.layout.list_item_challenge_leaderboard, list);
        View header = getLayoutInflater().inflate(R.layout.list_head_foot_leaderboard, leaderboardList, false);
        TextView head = (TextView) header.findViewById(R.id.head_foot_text);
        head.setText("Load 10 higher ranks");
        leaderboardList.addHeaderView(header);
        View footer = getLayoutInflater().inflate(R.layout.list_head_foot_leaderboard, leaderboardList, false);
        TextView foot = (TextView) footer.findViewById(R.id.head_foot_text);
        foot.setText("Load 10 lower ranks");
        leaderboardList.addFooterView(footer);
        leaderboardList.setAdapter(adapter);
        leaderboardList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(ServerHelper.getInstance().checkInternetConnection()) {
                    int startRank;
                    int endRank;
                    if (position == 0 && (startRank = list.get(0).getRank()) != 1) {
                        ServerHelper.getInstance().getLeaderboardByRank(startRank - 2,
                                new ServerHelper.ResponseFunc<ArrayList<Profile>>() {
                                    @Override
                                    public void onResponse(ArrayList<Profile> response) {
                                        list.addAll(0, response);
                                        for (int i = 0; i < response.size(); i++)
                                            checked.add(0, false);
                                        ((ArrayAdapter) ((HeaderViewListAdapter) leaderboardList.getAdapter()).getWrappedAdapter()).notifyDataSetChanged();
                                    }
                                }, null, false);
                    } else if (position == list.size() + 1 && (endRank = list.get(list.size() - 1).getRank()) % 10 == 0) {
                        ServerHelper.getInstance().getLeaderboardByRank(endRank + 1,
                                new ServerHelper.ResponseFunc<ArrayList<Profile>>() {
                                    @Override
                                    public void onResponse(ArrayList<Profile> response) {
                                        list.addAll(response);
                                        for (int i = 0; i < response.size(); i++)
                                            checked.add(false);
                                        ((ArrayAdapter) ((HeaderViewListAdapter) leaderboardList.getAdapter()).getWrappedAdapter()).notifyDataSetChanged();
                                    }
                                }, null, false);
                    }
                }else{
                    Toast.makeText(getApplicationContext(),"Unable to get leaderboard, no internet connection", Toast.LENGTH_SHORT).show();
                }
            }


        });
    }

    public void onConfirm(View v) {
        int count = 0;
        for(boolean c : checked) {
            if(c) {
                count++;
            }
        }
        int[] ids = new int[count];
        count = 0;
        for(int i = 0; i < list.size(); i++) {
            if(checked.get(i)) {
                ids[count++] = list.get(i).getId();
            }
        }
        LiveChallenge challenge = new LiveChallenge(challengeID, ids);
        StApp.challenges.put(challenge.getUniqueId(), challenge);
        challenge.request();
        Intent intent = new Intent(this, OpenChallenge.class);
        intent.putExtra("challenge_unique_index", challenge.getUniqueId());
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        overridePendingTransition(R.anim.enter_right, R.anim.leave_left);
        finish();
    }

    public void checkCount() {
        int min = ChallengeList.challenges.get(challengeID).getMinAmount();
        int max = ChallengeList.challenges.get(challengeID).getMaxAmount();
        if((count + 1) < min) {
            confirmBtn.setText("Choose " + (min - (count + 1)) + " more " + (min - (count + 1) > 1 ? "opponents" : "opponent"));
            confirmBtn.setEnabled(false);
        } else if((count + 1) > max) {
            confirmBtn.setText("Remove " + ((count + 1) - max) + ((count + 1) - max > 1 ? " opponents" : " opponent"));
            confirmBtn.setEnabled(false);
        } else {
            confirmBtn.setText("Confirm");
            confirmBtn.setEnabled(true);
        }
    }

    private class ChallengeLeaderboardAdapter extends ArrayAdapter<Profile> {
        private int normalColor = getResources().getColor(R.color.secondaryText);
        private int accentColor = getResources().getColor(R.color.accentColor);
        private int itemLayoutId;

        public ChallengeLeaderboardAdapter(Context context, int itemLayoutId, ArrayList<Profile> data) {
            super(context, itemLayoutId, data);
            this.itemLayoutId = itemLayoutId;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = getLayoutInflater().inflate(itemLayoutId, parent, false);
            }
            Profile p = getItem(position);

            if(p != null) {
                TextView rank = (TextView) convertView.findViewById(R.id.leaderboard_rank);
                ImageView avatar = (ImageView) convertView.findViewById(R.id.leaderboard_avatar);
                TextView username = (TextView) convertView.findViewById(R.id.leaderboard_username);
                TextView experience = (TextView) convertView.findViewById(R.id.leaderboard_experience);
                CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.opponent_checkbox);


                int avatarID = getResources().getIdentifier("avatar_" + p.getAvatar() +"_128", "drawable", getPackageName());
                if(p.getId() == mProfile.getId()){
                    rank.setTextColor(accentColor);
                    username.setTextColor(accentColor);
                    experience.setTextColor(accentColor);
                    checkBox.setVisibility(View.INVISIBLE);
                } else {
                    rank.setTextColor(normalColor);
                    username.setTextColor(normalColor);
                    experience.setTextColor(normalColor);
                    checkBox.setVisibility(View.VISIBLE);
                }
                checkBox.setChecked(checked.get(position));
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        checked.set(position, isChecked);
                        if(isChecked) {
                            count++;
                            checkCount();
                        } else {
                            count--;
                            checkCount();
                        }
                    }
                });
                if(rank != null) {rank.setText(Integer.toString(p.getRank()));}
                if(avatar != null) {avatar.setImageResource(avatarID);}
                if(username != null) {username.setText(p.getUsername());}
                if(experience != null) {experience.setText(Integer.toString(p.getExperience()));}
            }
            return convertView;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }
    }
}

