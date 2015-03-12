package com.example.tom.stapp3.activity;

/**
 * Created by Tom on 18/12/2014.
 * Every activity after the login screen will extend this activity, it takes care of the connection with the service
 */

import com.example.tom.stapp3.application.StApp;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public abstract class ServiceActivity extends ActionBarActivity {

    protected StApp app;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.app = (StApp) getApplication();
    }
}
