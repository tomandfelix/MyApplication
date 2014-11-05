package com.example.tom.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;


public class ProfileView extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_view);
        Intent intent = getIntent();
        Profile profile = intent.getParcelableExtra("profile");
        TextView id = (TextView) findViewById(R.id.id);
        TextView firstName = (TextView) findViewById(R.id.firstname);
        TextView lastName = (TextView) findViewById(R.id.lastname);
        TextView username = (TextView) findViewById(R.id.username);
        TextView email = (TextView) findViewById(R.id.email);
        TextView money = (TextView) findViewById(R.id.money);
        TextView experience = (TextView) findViewById(R.id.experience);
        id.setText(id.getText().toString() + profile.getId());
        firstName.setText(firstName.getText().toString() + profile.getFirstName());
        lastName.setText(lastName.getText().toString() + profile.getLastName());
        username.setText(username.getText().toString() + profile.getUsername());
        email.setText(email.getText().toString() + profile.getEmail());
        money.setText(money.getText().toString() + profile.getMoney());
        experience.setText(experience.getText().toString() + profile.getExperience());

    }
}