package com.example.tom.stapp3.activity;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.tom.stapp3.R;

/**
 * Created by Tom on 3/11/2014.
 * This class will create the proper fragment for the viewpager in FragmentViewer
 */
public class FragmentProvider extends Fragment{
    int position;

    static FragmentProvider init(int position) {
        FragmentProvider prov = new FragmentProvider();
        Bundle args = new Bundle();
        args.putInt("pos", position);
        prov.setArguments(args);
        return prov;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        position = getArguments() != null ? getArguments().getInt("pos") : 1;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layoutView;
        switch(position) {
            case 0:
                layoutView = inflater.inflate(R.layout.login_fragment, container, false);
                break;
            case 2:
                layoutView = inflater.inflate(R.layout.register_fragment, container, false);
                break;
            case 1:default :
                layoutView = inflater.inflate(R.layout.start_fragment, container, false);
                break;
        }
        return layoutView;
    }
}