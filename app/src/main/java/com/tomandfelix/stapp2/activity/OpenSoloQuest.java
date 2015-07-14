package com.tomandfelix.stapp2.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import com.tomandfelix.stapp2.R;
import com.tomandfelix.stapp2.application.StApp;
import com.tomandfelix.stapp2.persistency.Solo;
import com.tomandfelix.stapp2.persistency.SoloList;

import java.util.Timer;
import java.util.TimerTask;

public class OpenSoloQuest extends ServiceActivity {
    private Solo solo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_solo_quest_description);
        super.onCreate(savedInstanceState);
        TextView name = (TextView) findViewById(R.id.solo_quest_name);
        final TextView description = (TextView) findViewById(R.id.solo_quest_description);
        int id = getIntent().getIntExtra("position", -1);
        if(id == -1)
            finish();
        solo = SoloList.getSolo(id);
        name.setText(solo.getName());
        description.setText(solo.getDescription());
        if(solo.getHandler() == null) {
            findViewById(R.id.solo_quest_buttons).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.solo_quest_buttons).setVisibility(View.GONE);
        }
    }

    public void backButton(View v) {
        Intent intent = new Intent(OpenSoloQuest.this, SoloQuestList.class);
        intent.putExtra("Position", 3);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    public void startButton(View v) {
        findViewById(R.id.solo_quest_buttons).setVisibility(View.GONE);
        solo.start();
    }
}
