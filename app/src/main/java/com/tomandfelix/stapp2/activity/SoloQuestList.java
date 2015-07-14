package com.tomandfelix.stapp2.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.tomandfelix.stapp2.R;
import com.tomandfelix.stapp2.persistency.Solo;
import com.tomandfelix.stapp2.persistency.SoloList;

import java.util.ArrayList;
import java.util.List;

public class SoloQuestList extends DrawerActivity {
    SoloQuestListAdapter soloAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_quest_list);
        super.onCreate(savedInstanceState);
        ListView questList = (ListView) findViewById(R.id.quest_list);
        soloAdapter = new SoloQuestListAdapter(this, R.layout.list_item_solo_quest, SoloList.getList());
        questList.setAdapter(soloAdapter);
        questList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(SoloQuestList.this, OpenSoloQuest.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                intent.putExtra("position", position);
                startActivity(intent);
            }
        });
    }

    private class SoloQuestListAdapter extends ArrayAdapter<Solo> {
        private List<Solo> data;
        private int itemLayoutId;

        public SoloQuestListAdapter(Context context, int itemLayoutId, List<Solo> data) {
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
            Solo s = data.get(position);
            if(s != null) {
                ImageView difficulty = (ImageView) convertView.findViewById(R.id.solo_list_difficulty);
                TextView name = (TextView) convertView.findViewById(R.id.solo_list_name);
                TextView xp = (TextView) convertView.findViewById(R.id.solo_list_xp);

                switch(s.getDifficulty()) {
                    case EASY:
                        difficulty.setImageResource(R.drawable.circle_green);
                        break;
                    case MEDIUM:
                        difficulty.setImageResource(R.drawable.circle_orange);
                        break;
                    case HARD:
                        difficulty.setImageResource(R.drawable.circle_red);
                        break;
                }
                name.setText(s.getName());
                xp.setText(Integer.toString(s.getxp()));
            }
            return convertView;
        }
    }
}