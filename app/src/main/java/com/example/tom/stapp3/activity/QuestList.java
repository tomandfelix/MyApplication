package com.example.tom.stapp3.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.tom.stapp3.R;
import com.example.tom.stapp3.persistency.Challenge;
import com.example.tom.stapp3.persistency.Quest;
import com.example.tom.stapp3.persistency.Solo;
import com.example.tom.stapp3.tools.Algorithms;

import java.util.ArrayList;
import java.util.Date;

public class QuestList extends DrawerActivity {
    //private CoOperativeListAdapter coOperativeAdapter;
    private ArrayList<Quest> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quest_list);
        ListView questList = (ListView) findViewById(R.id.quest_list);
        switch(index) {
            case SOLO_QUEST:
                list = new ArrayList<>();
                list.add(new Solo(1, "testQuest1", "Stand for more than 10 seconds within 30 seconds", 1, 10, 30, Solo.EASY, new Runnable() {
                    @Override
                    public void run() {
                        long now = System.currentTimeMillis();
                        Date start = new Date(now - 30 * 1000);
                        Date end = new Date(now);
                        Log.i("TestQuest", Long.toString(Algorithms.millisecondsStood(QuestList.this, start, end)));
                    }
                }));
                //list.add(new Solo(2, "testQuest2", "This Quest is for testing purposes only. Do not try this at home!", 2, 20, 30, Solo.MEDIUM));
                //list.add(new Solo(3, "testQuest3", "This Quest is for testing purposes only. Do not try this at home!", 3, 30, 30, Solo.HARD));
                SoloQuestListAdapter soloAdapter = new SoloQuestListAdapter(this, R.layout.list_item_solo_quest, list);
                questList.setAdapter(soloAdapter);
                questList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Log.i("QuestList", list.get(position).toString());
                        Intent intent = new Intent(QuestList.this, SoloQuestDescription.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivity(intent);
                    }
                });
                break;
            case CHALLENGE:
                list = new ArrayList<>();
                list.add(new Challenge(1, "testChallenge1", "This Challenge is for testing purposes only. Do not try this at home!", 2));
                list.add(new Challenge(2, "testChallenge2", "This Challenge is for testing purposes only. Do not try this at home!", 2));
                list.add(new Challenge(3, "testChallenge3", "This Challenge is for testing purposes only. Do not try this at home!", 5));
                ChallengeListAdapter challengeAdapter = new ChallengeListAdapter(this, R.layout.list_item_challenge, list);
                questList.setAdapter(challengeAdapter);
                questList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Log.i("QuestList", list.get(position).toString());
                    }
                });
                break;
        }

    }

    private class SoloQuestListAdapter extends ArrayAdapter<Quest> {
        private ArrayList<Quest> data;
        private int itemLayoutId;

        public SoloQuestListAdapter(Context context, int itemLayoutId, ArrayList<Quest> data) {
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

            Solo s = (Solo) data.get(position);

            if(s != null) {
                ImageView difficulty = (ImageView) convertView.findViewById(R.id.solo_list_difficulty);
                TextView name = (TextView) convertView.findViewById(R.id.solo_list_name);
                TextView money = (TextView) convertView.findViewById(R.id.solo_list_money);
                TextView xp = (TextView) convertView.findViewById(R.id.solo_list_xp);

                switch(s.getDifficulty()) {
                    case Solo.EASY:
                        difficulty.setImageResource(R.drawable.circle_green);
                        break;
                    case Solo.MEDIUM:
                        difficulty.setImageResource(R.drawable.circle_orange);
                        break;
                    case Solo.HARD:
                        difficulty.setImageResource(R.drawable.circle_red);
                        break;
                }
                name.setText(s.getName());
                money.setText(Integer.toString(s.getMoney()));
                xp.setText(Integer.toString(s.getxp()));
            }
            return convertView;
        }
    }

    private class ChallengeListAdapter extends ArrayAdapter<Quest> {
        private ArrayList<Quest> data;
        private int itemLayoutId;

        public ChallengeListAdapter(Context context, int itemLayoutId, ArrayList<Quest> data) {
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

            Challenge c = (Challenge) data.get(position);

            if(c != null) {
                TextView name = (TextView) convertView.findViewById(R.id.challenge_list_name);
                TextView people = (TextView) convertView.findViewById(R.id.challenge_list_people);

                name.setText(c.getName());
                people.setText(Integer.toString(c.getPeopleAmount()));
            }
            return convertView;
        }
    }
}