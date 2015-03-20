package com.tomandfelix.stapp2.application;

import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.tomandfelix.stapp2.persistency.Challenge;
import com.tomandfelix.stapp2.persistency.GCMMessage;
import com.tomandfelix.stapp2.persistency.Profile;
import com.tomandfelix.stapp2.persistency.ServerHelper;
import com.tomandfelix.stapp2.persistency.Solo;
import com.tomandfelix.stapp2.service.ShimmerService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.tomandfelix.stapp2.tools.Algorithms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Tom on 11/03/2015.
 */
public class StApp  extends Application {
    public static Challenge exampleChallenge;
    private ArrayList<GCMMessage> requests;
    private ArrayList<GCMMessage> results;
    private Solo solo;
    private Challenge challenge;
    private Profile mProfile;
    private static StApp singleton;
    private ShimmerService mService;
    private boolean mBound;
    private String PROJECT_NUMBER = "413268601960";
    private String gcmRegistrationId = null;
    private boolean showPlayServicesError = false;
    private int playServicesResultCode;
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = ((ShimmerService.LocalBinder) service).getService();
            mBound = true;
        }

        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
            mBound = false;
        }
    };

    public Solo getSolo() {
        return solo;
    }

    public void setSolo(Solo solo) {
        this.solo = solo;
    }

    public Challenge getChallenge() {
        return challenge;
    }

    public void setChallenge(Challenge challenge) {
        this.challenge = challenge;
    }

    public Profile getProfile() {
        return mProfile;
    }

    public void setProfile(Profile mProfile) {
        this.mProfile = mProfile;
    }

    public ArrayList<GCMMessage> getRequests() {
        return requests;
    }

    public void addRequest(GCMMessage newRequest) {
        if(newRequest != null) {
            requests.add(newRequest);
        }
    }
    public ArrayList<GCMMessage> getResults() {
        return results;
    }

    public void addResult(GCMMessage newResult) {
        if(newResult != null) {
            results.add(newResult);
        }
    }


    public static StApp getInstance() {
        return singleton;
    }

    protected boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (service.service.getClassName().equals("com.example.tom.stapp3.service.ShimmerService")) {
                return true;
            }
        }
        return false;
    }

    public ShimmerService getService() {
        return mService;
    }

    public String getGcmRegistrationId() {
        return gcmRegistrationId;
    }

    public int getPlayServicesResultCode() {
        return playServicesResultCode;
    }

    @Override
    public void onCreate() {

        requests = new ArrayList<>();
        results = new ArrayList<>();
        exampleChallenge = new Challenge(1, "testChallenge1", "you have 30 seconds time to stand more than your oponent", 2,30,new Runnable(){
            @Override
            public void run(){
                long now = System.currentTimeMillis();
                Date start = new Date(now - 30 * 1000);
                Date end = new Date(now);
                long result = Algorithms.millisecondsStood(StApp.this, start, end);
                Log.i("TestChallenge", Long.toString(result));
                if(getResults().size() > 0) {
                    long otherMilliseconds = Integer.parseInt(getResults().get(0).getMessage());
                    if(result > otherMilliseconds) {
                        Toast.makeText(getApplicationContext(), "You won, big time!", Toast.LENGTH_LONG).show();
                    } else if (result == otherMilliseconds) {
                        Toast.makeText(getApplicationContext(), "It's a Tie, how did you pull this off?", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "You had one thing to do, ONE! (you lost)", Toast.LENGTH_LONG).show();
                    }
                }
                ServerHelper.getInstance(StApp.this).sendMessage(new GCMMessage(new int[]{requests.get(0).getSenderId()}, requests.get(0).getChallengeId(), GCMMessage.RESULT, 0, Long.toString(result)),
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError volleyError) {
                                Log.e("StApp", volleyError.getMessage());
                            }
                        });
            }
        });

        super.onCreate();
        singleton = this;
        if(!isServiceRunning()) {
            startService(new Intent(this, ShimmerService.class));
        }
        bindService(new Intent(this, ShimmerService.class), mServiceConnection, Context.BIND_AUTO_CREATE);

        playServicesResultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(playServicesResultCode == ConnectionResult.SUCCESS) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(StApp.this);
                        gcmRegistrationId = gcm.register(PROJECT_NUMBER);
                        Log.i("GCM", "Device registered, registration ID=" + gcmRegistrationId);
                    } catch (IOException ex) {
                        Log.e("GCM :", ex.getMessage());
                    }
                    return null;
                }
            }.execute(null, null, null);
        }
    }
}
