package com.tomandfelix.stapp2.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.tomandfelix.stapp2.R;
import com.tomandfelix.stapp2.persistency.Solo;
import com.tomandfelix.stapp2.tools.Algorithms;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class SoloQuestDescription extends ServiceActivity {
    private Solo solo;
    final Handler myHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_solo_quest_description);
        super.onCreate(savedInstanceState);
        TextView name = (TextView) findViewById(R.id.solo_quest_name);
        final TextView description = (TextView) findViewById(R.id.solo_quest_description);
        solo = new Solo(1, "testQuest1", "Stand for more than 10 seconds within 30 seconds", 1, 10, 30, Solo.EASY, new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                Date start = new Date(now - 30 * 1000);
                Date end = new Date(now);
                long result = Algorithms.millisecondsStood(SoloQuestDescription.this, start, end);
                Log.i("TestQuest", Long.toString(result));
                if(result > 10000) {
                    description.setText("YOU WON!");
                } else {
                    description.setText("YOU LOSE!");
                }
            }
        });
        name.setText(solo.getName());
        description.setText(solo.getDescription());
    }

    public void backButton(View v) {
        Intent intent = new Intent(SoloQuestDescription.this, SoloQuestList.class);
        intent.putExtra("Position", 3);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    public void startButton(View v) {
        findViewById(R.id.solo_quest_buttons).setVisibility(View.GONE);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                updateGUI();
            }
        }, solo.getDuration() * 1000);
    }

    private void updateGUI() {
        myHandler.post(solo.getValidator());
    }
}
