package com.tomandfelix.stapp2.activity;

import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
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
import com.tomandfelix.stapp2.persistency.ChallengeList;
import com.tomandfelix.stapp2.persistency.Quest;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Flixse on 19/03/2015.
 */
public class ListChallengesFragment extends ListFragment {
    private List<Challenge> list;
    public ListChallengesFragment(){
        super();
        list = ChallengeList.challenges;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = new ListView(getActivity());
        container.addView(view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        ChallengeListAdapter challengeAdapter = new ChallengeListAdapter(getActivity(), R.layout.list_item_challenge, list);
        this.setListAdapter(challengeAdapter);
        this.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i("ChallengesList", list.get(position).toString());
                Intent intent = new Intent(getActivity(), ChallengeLeaderboard.class);
                intent.putExtra("challengeID", position);
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.enter_right, R.anim.leave_left);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("DO NOT CRASH", "OK");
    }

    private class ChallengeListAdapter extends ArrayAdapter<Challenge> {
        private List<Challenge> data;
        private int itemLayoutId;

        public ChallengeListAdapter(Context context, int itemLayoutId, List<Challenge> data) {
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

            Challenge c = data.get(position);

            if(c != null) {
                TextView name = (TextView) convertView.findViewById(R.id.challenge_list_name);
                TextView people = (TextView) convertView.findViewById(R.id.challenge_list_people);

                name.setText(c.getName());
                people.setText(Integer.toString(c.getMaxAmount()));
            }
            return convertView;
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            if(observer != null) {
                super.unregisterDataSetObserver(observer);
            }
        }
    }
}
