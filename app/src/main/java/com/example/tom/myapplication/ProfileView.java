package com.example.tom.myapplication;

import android.os.Bundle;
import android.widget.TextView;

/**
 * Created by Tom on 17/11/2014.
 */
public class ProfileView extends DrawerActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.profile_view);
        if(savedInstanceState == null) {
            savedInstanceState = new Bundle();
        }
        savedInstanceState.putInt("ListIndex", 0);
        super.onCreate(savedInstanceState);

        Profile profile = getIntent().getParcelableExtra("profile");
        if(profile != null) {
            updateVisual(profile);
        } else {
            updateVisual(DatabaseHelper.getInstance().getProfile(DatabaseHelper.getInstance().getSetting(DatabaseHelper.OWNER)));
            ServerHelper.getInstance().getOtherProfile(DatabaseHelper.getInstance().getSetting(DatabaseHelper.OWNER), new Function<Profile>() {
                @Override
                public void call(Profile profile) {
                    if(profile != null) {
                        updateVisual(profile);
                    }
                }
            });
        }
    }

    private void updateVisual(Profile profile) {
        TextView id = (TextView) findViewById(R.id.id);
        TextView firstName = (TextView) findViewById(R.id.firstname);
        TextView lastName = (TextView) findViewById(R.id.lastname);
        TextView username = (TextView) findViewById(R.id.username);
        TextView email = (TextView) findViewById(R.id.email);
        TextView money = (TextView) findViewById(R.id.money);
        TextView experience = (TextView) findViewById(R.id.experience);

        if(profile.getId() != -1) {id.setText("ID: " + profile.getId());}
        if(profile.getFirstName() != null) {firstName.setText("First Name: " + profile.getFirstName());}
        if(profile.getLastName() != null) {lastName.setText("Last Name: " + profile.getLastName());}
        if(profile.getUsername() != null) {username.setText("Username: " + profile.getUsername());}
        if(profile.getEmail() != null) {email.setText("Email: " + profile.getEmail());}
        if(profile.getMoney() != -1) {money.setText("Money: " + profile.getMoney());}
        if(profile.getExperience() != -1) {experience.setText("Experience: " + profile.getExperience());}
    }
}
