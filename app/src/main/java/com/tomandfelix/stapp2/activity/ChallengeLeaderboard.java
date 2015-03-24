package com.tomandfelix.stapp2.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.tomandfelix.stapp2.R;
import com.tomandfelix.stapp2.persistency.DatabaseHelper;
import com.tomandfelix.stapp2.persistency.Profile;
import com.tomandfelix.stapp2.persistency.ServerHelper;

import java.util.ArrayList;

public class ChallengeLeaderboard extends ServiceActivity {
    private ListView leaderboardList;
    private ChallengeLeaderboardAdapter adapter;
    private ArrayList<Profile> list;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("onCreate", "LeaderBoardView");
        setContentView(R.layout.activity_challenge_leaderboard);
        super.onCreate(savedInstanceState);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        leaderboardList = (ListView) findViewById(R.id.leaderboard_list);
        ServerHelper.getInstance().getLeaderboardById(DatabaseHelper.getInstance().getOwnerId(),
                new ServerHelper.ResponseFunc<ArrayList<Profile>>() {
                    @Override
                    public void onResponse(ArrayList<Profile> response) {
                        list = response;
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
    }

    private void setupList() {
        adapter = new ChallengeLeaderboardAdapter(ChallengeLeaderboard.this, R.layout.list_item_leaderboard, list);
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
                int startRank;
                int endRank;
                if(position == 0 && (startRank = list.get(0).getRank()) != 1) {
                    ServerHelper.getInstance().getLeaderboardByRank(startRank - 2,
                            new ServerHelper.ResponseFunc<ArrayList<Profile>>() {
                                @Override
                                public void onResponse(ArrayList<Profile> response) {
                                    list.addAll(0, response);
                                    ((ArrayAdapter) ((HeaderViewListAdapter) leaderboardList.getAdapter()).getWrappedAdapter()).notifyDataSetChanged();
                                }
                            }, null, false);
                } else if (position == list.size() + 1 && (endRank = list.get(list.size() - 1).getRank()) % 10 == 0) {
                    ServerHelper.getInstance().getLeaderboardByRank(endRank + 1,
                            new ServerHelper.ResponseFunc<ArrayList<Profile>>() {
                                @Override
                                public void onResponse(ArrayList<Profile> response) {
                                    list.addAll(response);
                                    ((ArrayAdapter) ((HeaderViewListAdapter) leaderboardList.getAdapter()).getWrappedAdapter()).notifyDataSetChanged();
                                }
                            }, null, false);
                } else  if(position > 0 && position <= list.size()) {
                    int destId = list.get(position - 1).getId();
                    if(destId != DatabaseHelper.getInstance().getOwnerId()) {
                        Intent intent = new Intent(ChallengeLeaderboard.this, ChallengeStranger.class);
                        intent.putExtra("strangerId", destId);
                        startActivity(intent);
                        overridePendingTransition(R.anim.enter_right, R.anim.leave_left);
                    }
                }
            }
        });
    }

    private class ChallengeLeaderboardAdapter extends ArrayAdapter<Profile> {
        private int normalColor = getResources().getColor(R.color.secondaryText);
        private int accentColor = getResources().getColor(R.color.accentColor);
        private ArrayList<Profile> data;
        private int itemLayoutId;

        public ChallengeLeaderboardAdapter(Context context, int itemLayoutId, ArrayList<Profile> data) {
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
                TextView rank = (TextView) convertView.findViewById(R.id.leaderboard_rank);
                ImageView avatar = (ImageView) convertView.findViewById(R.id.leaderboard_avatar);
                TextView username = (TextView) convertView.findViewById(R.id.leaderboard_username);
                TextView experience = (TextView) convertView.findViewById(R.id.leaderboard_experience);


                int avatarID = getResources().getIdentifier("avatar_" + p.getAvatar() +"_128", "drawable", getPackageName());
                if(p.getId() == app.getProfile().getId()){
                    rank.setTextColor(accentColor);
                    username.setTextColor(accentColor);
                    experience.setTextColor(accentColor);
                } else {
                    rank.setTextColor(normalColor);
                    username.setTextColor(normalColor);
                    experience.setTextColor(normalColor);
                }
                if(rank != null) {rank.setText(Integer.toString(p.getRank()));}
                if(avatar != null) {avatar.setImageResource(avatarID);}
                if(username != null) {username.setText(p.getUsername());}
                if(experience != null) {experience.setText(Integer.toString(p.getExperience()));}
            }
            return convertView;
        }
    }
}

