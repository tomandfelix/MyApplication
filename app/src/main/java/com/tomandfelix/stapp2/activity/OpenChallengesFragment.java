package com.tomandfelix.stapp2.activity;

import android.app.Fragment;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import com.tomandfelix.stapp2.application.StApp;
import com.tomandfelix.stapp2.persistency.Challenge;
import com.tomandfelix.stapp2.persistency.GCMMessage;
import com.tomandfelix.stapp2.persistency.Profile;
import com.tomandfelix.stapp2.persistency.Quest;
import com.tomandfelix.stapp2.persistency.ServerHelper;
import com.tomandfelix.stapp2.tools.Algorithms;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Flixse on 19/03/2015.
 */
public class OpenChallengesFragment extends ListFragment {
    private ArrayList<GCMMessage> list;
    public static Handler handler = new Handler();
    public OpenChallengesFragment(){
        super();
        list = new ArrayList<>(((StApp) getActivity().getApplication()).getRequests());
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
        RequestAdapter requestAdapter = new RequestAdapter(getActivity(), R.layout.list_item_request, list);
        this.setListAdapter(requestAdapter);
        this.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                GCMMessage message = list.get(position);
                ServerHelper.getInstance(getActivity()).sendMessage(new GCMMessage(new int[]{message.getSenderId()}, message.getChallengeId(), GCMMessage.ACCEPTED, 0, ""),
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError volleyError) {
                                Log.e("OpenChallengesFragment", volleyError.getMessage());
                            }
                        });
                handler.postDelayed(StApp.exampleChallenge.getValidator(), 30000);
                list.remove(0);
            }
        });
    }

    private class RequestAdapter extends ArrayAdapter<GCMMessage> {
        private ArrayList<GCMMessage> data;
        private int itemLayoutId;

        public RequestAdapter(Context context, int itemLayoutId, ArrayList<GCMMessage> data) {
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

            final GCMMessage m = data.get(position);
            Challenge solution = StApp.exampleChallenge;

            if(m != null) {
                TextView title = (TextView) convertView.findViewById(R.id.request_title);
                final TextView challenger = (TextView) convertView.findViewById(R.id.request_challenger);

                title.setText(solution.getName());
                ServerHelper.getInstance(getActivity()).getOtherProfile(m.getSenderId(),
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
    }
}
