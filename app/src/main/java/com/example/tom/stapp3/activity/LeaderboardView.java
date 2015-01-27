package com.example.tom.stapp3.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.tom.stapp3.persistency.DatabaseHelper;
import com.example.tom.stapp3.Function;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.leaderboard_view);
        if(savedInstanceState == null) {
            savedInstanceState = new Bundle();
        }
        savedInstanceState.putInt("ListIndex", LEADERBOARD);
        super.onCreate(savedInstanceState);
        leaderboardList = (ListView) findViewById(R.id.leaderboard_list);
        ServerHelper.getInstance(this).getLeaderboardById(DatabaseHelper.getInstance(this).getSetting(DatabaseHelper.OWNER), new Function<ArrayList<Profile>>() {
            @Override
            public void call(ArrayList<Profile> param) {
                list = param;
                adapter = new LeaderboardListAdapter(getBaseContext(), R.layout.leaderboard_list_item, list);
                View header = getLayoutInflater().inflate(R.layout.leaderboard_head_foot, leaderboardList, false);
                TextView head = (TextView) header.findViewById(R.id.head_foot_text);
                head.setText("Load 10 higher ranks");
                leaderboardList.addHeaderView(header);
                View footer = getLayoutInflater().inflate(R.layout.leaderboard_head_foot, leaderboardList, false);
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
                            ServerHelper.getInstance(getApplicationContext()).getLeaderboardByRank(startRank - 2, new Function<ArrayList<Profile>>() {
                                @Override
                                public void call(ArrayList<Profile> param) {
                                    list.addAll(0, param);
                                    ((ArrayAdapter) ((HeaderViewListAdapter) leaderboardList.getAdapter()).getWrappedAdapter()).notifyDataSetChanged();
                                }
                            }, false);
                        } else if (position == list.size() + 1 && (endRank = list.get(list.size() - 1).getRank()) % 10 == 0) {
                            ServerHelper.getInstance(getApplicationContext()).getLeaderboardByRank(endRank + 1, new Function<ArrayList<Profile>>() {
                                @Override
                                public void call(ArrayList<Profile> param) {
                                    list.addAll(param);
                                    ((ArrayAdapter) ((HeaderViewListAdapter) leaderboardList.getAdapter()).getWrappedAdapter()).notifyDataSetChanged();
                                }
                            }, false);
                        } else  if(position > 0 && position <= list.size()) {
                            int destId = list.get(position - 1).getId();
                            Intent intent;
                            if(destId == DatabaseHelper.getInstance(getApplicationContext()).getSetting(DatabaseHelper.OWNER)) {
                                intent = new Intent(getBaseContext(), ProfileView.class);
                            } else {
                                intent = new Intent(getBaseContext(), StrangerView.class);
                                intent.putExtra("strangerId", destId);
                            }
                            startActivity(intent);
                            overridePendingTransition(R.anim.enter_right, R.anim.leave_left);
                        }
                    }
                });
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

                if(rank != null) {rank.setText(Integer.toString(p.getRank()));}
                if(avatar != null) {avatar.setImageResource(avatarID);}
                if(username != null) {username.setText(p.getUsername());}
                if(experience != null) {experience.setText(Integer.toString(p.getExperience()));}
            }
            return convertView;
        }
    }
}
