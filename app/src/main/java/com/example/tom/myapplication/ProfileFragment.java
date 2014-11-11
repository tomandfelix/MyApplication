package com.example.tom.myapplication;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Tom on 11/11/2014.
 */
public class ProfileFragment extends Fragment {
    private Profile profile;

    static ProfileFragment init(Profile profile) {
        ProfileFragment frag = new ProfileFragment();
        Bundle args = new Bundle();
        args.putParcelable("profile", profile);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profile = getArguments() != null ? (Profile) getArguments().getParcelable("profile") : null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.profile_fragment, container, false);
        TextView id = (TextView) view.findViewById(R.id.id);
        TextView firstName = (TextView) view.findViewById(R.id.firstname);
        TextView lastName = (TextView) view.findViewById(R.id.lastname);
        TextView username = (TextView) view.findViewById(R.id.username);
        TextView email = (TextView) view.findViewById(R.id.email);
        TextView money = (TextView) view.findViewById(R.id.money);
        TextView experience = (TextView) view.findViewById(R.id.experience);
        id.setText(id.getText().toString() + profile.getId());
        firstName.setText(firstName.getText().toString() + profile.getFirstName());
        lastName.setText(lastName.getText().toString() + profile.getLastName());
        username.setText(username.getText().toString() + profile.getUsername());
        email.setText(email.getText().toString() + profile.getEmail());
        money.setText(money.getText().toString() + profile.getMoney());
        experience.setText(experience.getText().toString() + profile.getExperience());
        return view;
    }
}
