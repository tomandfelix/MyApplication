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

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.tomandfelix.stapp2.R;
import com.tomandfelix.stapp2.gcm.GCMMessageHandler;
import com.tomandfelix.stapp2.persistency.Challenge;
import com.tomandfelix.stapp2.persistency.Profile;
import com.tomandfelix.stapp2.persistency.ServerHelper;

import java.util.List;

/**
 * Created by Flixse on 19/03/2015.
 */
public class OpenChallengesFragment extends ListFragment {
    private static RequestAdapter requestAdapter;

    public static ArrayAdapter getAdapter() {
        return requestAdapter;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        requestAdapter = null;
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
        requestAdapter = new RequestAdapter(getActivity(), R.layout.list_item_request, GCMMessageHandler.challenges);

        this.setListAdapter(requestAdapter);
        this.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getActivity(), OpenChallenge.class);
                intent.putExtra("challenge_index", position);
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.enter_right, R.anim.leave_left);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("DO NOT CRASH", "MKAY");
    }

    private class RequestAdapter extends ArrayAdapter<Challenge> {
        private List<Challenge> data;
        private int itemLayoutId;

        public RequestAdapter(Context context, int itemLayoutId, List<Challenge> data) {
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

            Challenge solution = data.get(position);

            if(solution != null) {
                TextView title = (TextView) convertView.findViewById(R.id.request_title);
                final TextView challenger = (TextView) convertView.findViewById(R.id.request_challenger);

                title.setText(solution.getName());
                ServerHelper.getInstance().getOtherProfile(solution.getOpponents()[0],
                        new ServerHelper.ResponseFunc<Profile>() {
                            @Override
                            public void onResponse(Profile response) {
                                challenger.setText(response.getUsername());
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError volleyError) {
                                Log.e("OpenChallengesFragment", volleyError.getMessage());
                            }
                        }, false);
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
