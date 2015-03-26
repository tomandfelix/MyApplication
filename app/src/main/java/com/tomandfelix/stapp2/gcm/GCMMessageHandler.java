package com.tomandfelix.stapp2.gcm;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.tomandfelix.stapp2.persistency.Challenge;
import com.tomandfelix.stapp2.persistency.DatabaseHelper;
import com.tomandfelix.stapp2.persistency.GCMMessage;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GCMMessageHandler extends IntentService {
    public static final int MSG_RECEIVED = 0;
    public static final int MSG_SENT = 1;
    public static final List<Challenge> challenges = Collections.synchronizedList(new ArrayList<Challenge>());
    public static Handler handler = new MessageHandler();

    public GCMMessageHandler() {
        super("GcmMessageHandler");
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        final Bundle extras = intent.getExtras();

        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        int[] recIds = null;
        try {
            JSONArray recIdsJSON = new JSONArray(extras.getString("receiver_ids"));
            recIds = new int[recIdsJSON.length()];
            for(int i = 0; i < recIdsJSON.length(); i++) {
                recIds[i] = recIdsJSON.getInt(i);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        GCMMessage message = new GCMMessage(recIds,
                Integer.parseInt(extras.getString("challenge_id")),
                Integer.parseInt(extras.getString("message_type")),
                Integer.parseInt(extras.getString("sender_id")),
                extras.getString("message"));
        Log.d("GCM", "Received : " + message.toString());

        if(message.getMessageType() == GCMMessage.TEST_TOAST) {
            handler.post(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(),extras.getString("message") , Toast.LENGTH_LONG).show();
                }
            });
        } else {
            handler.obtainMessage(MSG_RECEIVED, message).sendToTarget();
        }
        GCMBroadCastReceiver.completeWakefulIntent(intent);
    }

    private static class MessageHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            if(msg.obj instanceof GCMMessage) {
                GCMMessage message = (GCMMessage) msg.obj;
                if (msg.what == MSG_RECEIVED) {
                    if (message.getMessageType() == GCMMessage.REQUEST) {
                        int[] receivers = message.getReceivers();
                        int[] opponents = new int[receivers.length];
                        int owner = DatabaseHelper.getInstance().getOwnerId();
                        for (int i = 0; i < receivers.length; i++) {
                            if (receivers[i] == owner) {
                                opponents[i] = message.getSenderId();
                            } else {
                                opponents[i] = receivers[i];
                            }
                        }
                        challenges.add(new Challenge(message.getChallengeId(), opponents));
                        challenges.get(challenges.size() - 1).setState(Challenge.REQ_REC);
                    } else if (message.getMessageType() == GCMMessage.ACCEPTED) {
                        synchronized (challenges) {
                            for (Challenge c : challenges) {
                                if (message.getChallengeId() == c.getId()) {
                                    c.setState(Challenge.ACCEPTED);
                                }
                            }
                        }
                    } else if (message.getMessageType() == GCMMessage.DECLINED) {
                        synchronized (challenges) {
                            for(Challenge c : challenges) {
                                if(message.getChallengeId() == c.getId()) {
                                    challenges.remove(c);
                                }
                            }
                        }
                    } else if (message.getMessageType() == GCMMessage.RESULT) {
                        synchronized (challenges) {
                            for (Challenge c : challenges) {
                                if (message.getChallengeId() == c.getId()) {
                                    c.addResult(message);
                                    if(c.getState() == Challenge.DONE) {
                                        handler.post(c.getValidator());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}