package com.example.tom.stapp3.application;

import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.IBinder;
import android.util.Log;

import com.example.tom.stapp3.service.ShimmerService;

/**
 * Created by Tom on 11/03/2015.
 */
public class StApp  extends Application {
    private static StApp singleton;
    private ShimmerService mService;
    private boolean mBound;
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

    @Override
    public void onCreate() {
        Log.e("StApp", "onCreate");
        super.onCreate();
        singleton = this;
        if(!isServiceRunning()) {
            startService(new Intent(this, ShimmerService.class));
            bindService(new Intent(this, ShimmerService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }
}
