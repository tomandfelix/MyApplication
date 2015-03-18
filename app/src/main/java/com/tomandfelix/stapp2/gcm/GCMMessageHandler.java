package com.tomandfelix.stapp2.gcm;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class GCMMessageHandler extends IntentService {
    private Handler handler;
    public GCMMessageHandler() {
        super("GcmMessageHandler");
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        handler = new Handler();
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        final Bundle extras = intent.getExtras();

        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        Log.i("GCM", "Received : (" + gcm.getMessageType(intent) +")  "+extras.getString("message"));

        if(Integer.parseInt(extras.getString("challenge_id")) == -1) {
            handler.post(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(),extras.getString("message") , Toast.LENGTH_LONG).show();
                }
            });
        }

        GCMBroadCastReceiver.completeWakefulIntent(intent);

    }
}