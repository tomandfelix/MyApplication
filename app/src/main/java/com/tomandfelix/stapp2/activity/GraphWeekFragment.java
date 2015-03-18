package com.tomandfelix.stapp2.activity;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tomandfelix.stapp2.R;

/**
 * Created by Tom on 18/03/2015.
 */
public class GraphWeekFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_graph_week, container, false);
        return rootView;
    }
}
