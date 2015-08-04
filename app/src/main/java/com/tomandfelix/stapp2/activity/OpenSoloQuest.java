package com.tomandfelix.stapp2.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.TextView;

import com.gc.materialdesign.views.ButtonRectangle;
import com.gc.materialdesign.views.ProgressBarDeterminate;
import com.tomandfelix.stapp2.R;
import com.tomandfelix.stapp2.application.StApp;
import com.tomandfelix.stapp2.persistency.Solo;
import com.tomandfelix.stapp2.persistency.SoloList;
import com.tomandfelix.stapp2.service.ShimmerService;
import com.tomandfelix.stapp2.tools.Logging;

import java.lang.ref.WeakReference;
import java.util.Arrays;

public class OpenSoloQuest extends ServiceActivity {
    private static OpenSoloHandler handler = new OpenSoloHandler();
    private Solo solo;
    public static final int MSG_REFRESH = 1;
    private ButtonRectangle button;
    private ProgressBarDeterminate progress;
    private TextView result;
    private int state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_open_solo_quest);
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        handler.setInstance(this);
        TextView name = (TextView) findViewById(R.id.solo_quest_name);
        final TextView description = (TextView) findViewById(R.id.solo_quest_description);
        int id = getIntent().getIntExtra("position", -1);
        if(id == -1)
            finish();
        solo = SoloList.getSolo(id);
        name.setText(solo.getName());
        description.setText(solo.getDescription());
        button = (ButtonRectangle) findViewById(R.id.solo_quest_button);
        progress = (ProgressBarDeterminate) findViewById(R.id.open_solo_quest_progress);
        result = (TextView) findViewById(R.id.solo_quest_result);
        updateViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.setInstance(this);
        if(StApp.getHandler() != handler) {
            StApp.setHandler(handler);
        }
        updateViews();
        app.commandService(ShimmerService.REQUEST_STATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(StApp.getHandler() == handler) {
            StApp.setHandler(null);
        }
    }

    public void onButton(View v) {
        if(solo.getData() == null) {
            solo.start();
        } else {
            solo.clear();
            updateViews();
        }
    }

    public static Handler getHandler() {
        return handler;
    }

    public void updateViews() {
        if(solo.getHandler() == null) {
            if(solo.getData() == null) {
                if(Arrays.asList(Logging.STATE_CONNECTED, Logging.STATE_SIT, Logging.STATE_STAND, Logging.STATE_OVERTIME).contains(state)) {
                    button.setText("Start");
                    button.setEnabled(true);
                } else {
                    button.setText("Please connect a sensor first");
                    button.setEnabled(false);
                }
                button.setVisibility(View.VISIBLE);
                progress.setVisibility(View.INVISIBLE);
                result.setVisibility(View.INVISIBLE);
            } else {
                button.setText("Dismiss");
                button.setVisibility(View.VISIBLE);
                progress.setVisibility(View.INVISIBLE);
                if(solo.getData() instanceof String) {
                    result.setText((String) solo.getData());
                    result.setVisibility(View.VISIBLE);
                } else {
                    result.setVisibility(View.INVISIBLE);
                }
            }
        } else {
            button.setVisibility(View.INVISIBLE);
            progress.setProgress((int) solo.getProgress());
            progress.setVisibility(View.VISIBLE);
            if(solo.getData() instanceof String) {
                result.setText((String) solo.getData());
                result.setVisibility(View.VISIBLE);
            } else {
                result.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void updateState(int state) {
        this.state = state;
        updateViews();
    }

    private static class OpenSoloHandler extends Handler {
        private WeakReference<OpenSoloQuest> osq;

        public void setInstance(OpenSoloQuest osq) {
            this.osq = new WeakReference<>(osq);
        }
        @Override
        public void handleMessage(Message msg) {
            if(osq.get() != null) {
                if (msg.what == MSG_REFRESH) {
                    osq.get().updateViews();
                } else {
                    osq.get().updateState(msg.what);
                }
            }
        }
    }
}
