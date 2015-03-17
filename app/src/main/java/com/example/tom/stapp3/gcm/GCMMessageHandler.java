package com.example.tom.stapp3.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * Created by Tom on 16/03/2015.
 */
public class GCMMessageHandler extends IntentService{
    private Handler handler;

    public GCMMessageHandler() {
        super("GCMMessageHandler");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);
        final int challengeId = Integer.parseInt(extras.getString("challenge_id"));
        final String message = extras.getString("message");
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(challengeId == -1) {
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                } else {
                    //TODO implementatie
                }
            }
        });
        GCMBroadCastReceiver.completeWakefulIntent(intent);
    }
}
