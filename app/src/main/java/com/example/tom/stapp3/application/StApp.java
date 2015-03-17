package com.example.tom.stapp3.application;

import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.example.tom.stapp3.service.ShimmerService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;

/**
 * Created by Tom on 11/03/2015.
 */
public class StApp  extends Application {
    private static StApp singleton;
    private ShimmerService mService;
    private boolean mBound;
    private String PROJECT_NUMBER = "760585394380";
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

    public boolean isShowPlayServicesError() {
        return showPlayServicesError;
    }

    public int getPlayServicesResultCode() {
        return playServicesResultCode;
    }

    @Override
    public void onCreate() {
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
