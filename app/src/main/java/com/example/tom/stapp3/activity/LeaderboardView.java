package com.example.tom.stapp3.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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
import com.example.tom.stapp3.persistency.DatabaseHelper;
import com.example.tom.stapp3.persistency.Profile;
import com.example.tom.stapp3.R;
import com.example.tom.stapp3.persistency.ServerHelper;

import java.util.ArrayList;

/**
 * Created by Tom on 17/11/2014.
 * This is the class that will display the leaderboard
 */
public class LeaderboardView extends DrawerActivity {
    private ListView leaderboardList;
    private LeaderboardListAdapter adapter;
    private ArrayList<Profile> list;
    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("going in","leaderboard view");
        setContentView(R.layout.activity_leaderboard);
        index = LEADERBOARD;
        super.onCreate(savedInstanceState);
        leaderboardList = (ListView) findViewById(R.id.leaderboard_list);
        context = this;
        ServerHelper.getInstance(this).getLeaderboardById(DatabaseHelper.getInstance(this).getIntSetting(DatabaseHelper.OWNER),
                new ServerHelper.ResponseFunc<ArrayList<Profile>>() {
                    @Override
                    public void onResponse(ArrayList<Profile> response) {
                        list = response;
                        adapter = new LeaderboardListAdapter(LeaderboardView.this, R.layout.list_item_leaderboard, list);
                        View header = getLayoutInflater().inflate(R.layout.list_head_foot_leaderboard, leaderboardList, false);
                        TextView head = (TextView) header.findViewById(R.id.head_foot_text);
                        head.setText("Load 10 higher ranks");
                        head.setTextColor(getResources().getColor(R.color.material_drawer_secondary_text));
                        head.setBackgroundColor(getResources().getColor(R.color.material_drawer_primary_light));
                        leaderboardList.addHeaderView(header);
                        View footer = getLayoutInflater().inflate(R.layout.list_head_foot_leaderboard, leaderboardList, false);
                        TextView foot = (TextView) footer.findViewById(R.id.head_foot_text);
                        foot.setText("Load 10 lower ranks");
                        foot.setTextColor(getResources().getColor(R.color.material_drawer_secondary_text));
                        foot.setBackgroundColor(getResources().getColor(R.color.material_drawer_primary_light));
                        leaderboardList.addFooterView(footer);
                        leaderboardList.setAdapter(adapter);
                        leaderboardList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                int startRank;
                                int endRank;
                                if(position == 0 && (startRank = list.get(0).getRank()) != 1) {
                                    ServerHelper.getInstance(getApplicationContext()).getLeaderboardByRank(startRank - 2,
                                            new ServerHelper.ResponseFunc<ArrayList<Profile>>() {
                                                @Override
                                                public void onResponse(ArrayList<Profile> response) {
                                                    list.addAll(0, response);
                                                    ((ArrayAdapter) ((HeaderViewListAdapter) leaderboardList.getAdapter()).getWrappedAdapter()).notifyDataSetChanged();
                                                }
                                            }, null, false);
                                } else if (position == list.size() + 1 && (endRank = list.get(list.size() - 1).getRank()) % 10 == 0) {
                                    ServerHelper.getInstance(getApplicationContext()).getLeaderboardByRank(endRank + 1,
                                            new ServerHelper.ResponseFunc<ArrayList<Profile>>() {
                                                @Override
                                                public void onResponse(ArrayList<Profile> response) {
                                                    list.addAll(response);
                                                    ((ArrayAdapter) ((HeaderViewListAdapter) leaderboardList.getAdapter()).getWrappedAdapter()).notifyDataSetChanged();
                                                }
                                            }, null, false);
                                } else  if(position > 0 && position <= list.size()) {
                                    int destId = list.get(position - 1).getId();
                                    Intent intent;
                                    if(destId == DatabaseHelper.getInstance(getApplicationContext()).getIntSetting(DatabaseHelper.OWNER)) {
                                        intent = new Intent(LeaderboardView.this, ProfileView.class);
                                    } else {
                                        intent = new Intent(LeaderboardView.this, StrangerView.class);
                                        intent.putExtra("strangerId", destId);
                                    }
                                    startActivity(intent);
                                    overridePendingTransition(R.anim.enter_right, R.anim.leave_left);
                                }
                            }
                        });
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
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

    private class LeaderboardListAdapter extends ArrayAdapter<Profile> {
        private ArrayList<Profile> data;
        private int itemLayoutId;

        public LeaderboardListAdapter(Context context, int itemLayoutId, ArrayList<Profile> data) {
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
                if(p.getId() == mProfile.getId()){
                    convertView.setBackgroundColor(getResources().getColor(R.color.material_drawer_accent));
                } else {
                    convertView.setBackgroundColor(Color.TRANSPARENT);
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
