package com.tomandfelix.stapp2.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.tomandfelix.stapp2.R;
import com.tomandfelix.stapp2.persistency.Quest;
import com.tomandfelix.stapp2.persistency.Solo;
import com.tomandfelix.stapp2.tabs.SlidingTabLayout;
import com.tomandfelix.stapp2.tools.Algorithms;

import java.util.ArrayList;
import java.util.Date;

public class SoloQuestList extends DrawerActivity {
    //private CoOperativeListAdapter coOperativeAdapter;
    private ArrayList<Quest> list;
    private SlidingTabLayout tabLayout;
    private ViewPager viewPager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_quest_list);
        super.onCreate(savedInstanceState);
        ListView questList = (ListView) findViewById(R.id.quest_list);
        list = new ArrayList<>();
        list.add(new Solo(1, "testQuest1", "Stand for more than 10 seconds within 30 seconds", 1, 10, 30, Solo.EASY, new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                Date start = new Date(now - 30 * 1000);
                Date end = new Date(now);
                Log.i("TestQuest", Long.toString(Algorithms.millisecondsStood(start, end)));
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
                Intent intent = new Intent(SoloQuestList.this, SoloQuestDescription.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
            }
        });
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


}