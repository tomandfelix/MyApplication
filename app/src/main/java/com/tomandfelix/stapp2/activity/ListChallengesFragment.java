package com.tomandfelix.stapp2.activity;

import android.app.Fragment;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.tomandfelix.stapp2.R;
import com.tomandfelix.stapp2.persistency.Challenge;
import com.tomandfelix.stapp2.persistency.Quest;
import com.tomandfelix.stapp2.tools.Algorithms;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Flixse on 19/03/2015.
 */
public class ListChallengesFragment extends ListFragment {
    private ArrayList<Quest> list;
    public ListChallengesFragment(){
        super();
        list = new ArrayList<>();
        list.add(new Challenge(1, "testChallenge1", "you have 30 seconds time to stand more than your oponent", 2,30,new Runnable(){
            @Override
            public void run(){
                long now = System.currentTimeMillis();
                Date start = new Date(now - 30 * 1000);
                Date end = new Date(now);
                Log.i("TestChallenge", Long.toString(Algorithms.millisecondsStood(getActivity(), start, end)));
            }
        }));
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("ListChallengesFragment", "onCreateView");
        View view = new ListView(getActivity());
        container.addView(view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        Log.d("ListChallengesFragment", "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
        ChallengeListAdapter challengeAdapter = new ChallengeListAdapter(getActivity(), R.layout.list_item_challenge, list);
        this.setListAdapter(challengeAdapter);
        this.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i("QuestList", list.get(position).toString());
                Intent intent = new Intent(getActivity(), ChallengeLeaderboard.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
            }
        });
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
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
