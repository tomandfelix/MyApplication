package com.tomandfelix.stapp2.gcm;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.tomandfelix.stapp2.activity.OpenChallengesFragment;
import com.tomandfelix.stapp2.application.StApp;
import com.tomandfelix.stapp2.persistency.GCMMessage;

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
        GCMMessage message = new GCMMessage(null,
                Integer.parseInt(extras.getString("challenge_id")),
                Integer.parseInt(extras.getString("message_type")),
                Integer.parseInt(extras.getString("sender_id")),
                extras.getString("message"));
        if(message.getChallengeId() == -1) {
            handler.post(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(),extras.getString("message") , Toast.LENGTH_LONG).show();
                }
            });
        } else if(message.getMessageType() == GCMMessage.REQUEST) {
            ((StApp) getApplication()).addRequest(message);
        } else if(message.getMessageType() == GCMMessage.RESULT) {
            if(((StApp) getApplication()).getResults().size() > 0) {
                long myMilliseconds = Long.parseLong(((StApp) getApplication()).getResults().get(0).getMessage());
                long otherMilliseconds = Long.parseLong(message.getMessage());
                if(myMilliseconds > otherMilliseconds) {
                    Toast.makeText(getApplicationContext(), "You won, big time!", Toast.LENGTH_LONG).show();
                } else if (myMilliseconds == otherMilliseconds) {
                    Toast.makeText(getApplicationContext(), "It's a Tie, how did you pull this off?", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "You had one thing to do, ONE! (you lost)", Toast.LENGTH_LONG).show();
                }
            } else {
                ((StApp) getApplication()).addResult(message);
            }
        } else if(message.getMessageType() == GCMMessage.ACCEPTED) {
            OpenChallengesFragment.handler.postDelayed(StApp.exampleChallenge.getValidator(), 30000);
        }

        GCMBroadCastReceiver.completeWakefulIntent(intent);

    }
}