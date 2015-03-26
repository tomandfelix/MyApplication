package com.tomandfelix.stapp2.gcm;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.tomandfelix.stapp2.R;
import com.tomandfelix.stapp2.activity.OpenChallenge;
import com.tomandfelix.stapp2.activity.OpenChallengesFragment;
import com.tomandfelix.stapp2.persistency.Challenge;
import com.tomandfelix.stapp2.persistency.DatabaseHelper;
import com.tomandfelix.stapp2.persistency.GCMMessage;
import com.tomandfelix.stapp2.persistency.Profile;
import com.tomandfelix.stapp2.persistency.ServerHelper;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GCMMessageHandler extends IntentService {
    public static final int MSG_RECEIVED = 0;
    public static final List<Challenge> challenges = Collections.synchronizedList(new ArrayList<Challenge>());
    public static Handler handler = null;
    private static NotificationCompat.Builder mBuilder = null;
    private static NotificationManager notificationManager = null;

    public GCMMessageHandler() {
        super("GcmMessageHandler");
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        if(handler == null) {
            handler = new MessageHandler();
        }
        if(mBuilder == null) {
            mBuilder = new NotificationCompat.Builder(GCMMessageHandler.this);
            mBuilder.setSmallIcon(R.drawable.icon_notification);
            mBuilder.setContentTitle("Stapp 2");
            mBuilder.setAutoCancel(true);
        }
        if(notificationManager == null) {
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
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

    private class MessageHandler extends Handler{
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
                        if(OpenChallengesFragment.hasAdapter()) {
                            OpenChallengesFragment.getAdapter().notifyDataSetChanged();
                        } else {
                            PendingIntent pendingIntent;
                            mBuilder.setContentText("You have a new challenge");
                            Intent intent = new Intent(GCMMessageHandler.this, OpenChallenge.class);
                            intent.putExtra("challenge_index", challenges.size() - 1);
                            if(Build.VERSION.SDK_INT >= 16) {
                                TaskStackBuilder stackBuilder = TaskStackBuilder.create(GCMMessageHandler.this);
                                stackBuilder.addParentStack(OpenChallenge.class);
                                stackBuilder.addNextIntent(intent);
                                pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                            } else {
                                pendingIntent = PendingIntent.getActivity(GCMMessageHandler.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                            }
                            mBuilder.setContentIntent(pendingIntent);
                            notificationManager.notify(1, mBuilder.build());
                        }
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
                        ServerHelper.getInstance().getOtherProfile(message.getSenderId(), new ServerHelper.ResponseFunc<Profile>() {
                            @Override
                            public void onResponse(Profile response) {
                                mBuilder.setContentText(response.getUsername() + " declined one of your challenges");
                                PendingIntent pendingIntent = PendingIntent.getActivity(GCMMessageHandler.this, 0, new Intent(), 0);
                                mBuilder.setContentIntent(pendingIntent);
                                notificationManager.notify(1, mBuilder.build());
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError volleyError) {
                                Log.e("GCMMessageHandler", volleyError.getMessage());
                            }
                        }, false);
                    } else if (message.getMessageType() == GCMMessage.RESULT) {
                        synchronized (challenges) {
                            for (Challenge c : challenges) {
                                if (message.getChallengeId() == c.getId()) {
                                    c.addResult(message);
                                    if(c.getState() == Challenge.WAITING) {
                                        handler.post(c.getValidator());
                                    }
                                }
                            }
                        }
                    }
                }
                if(OpenChallenge.getHandler() != null) {
                    OpenChallenge.getHandler().obtainMessage(OpenChallenge.MSG_REFRESH).sendToTarget();
                }
            }
        }
    }
}