package com.example.tom.stapp3.activity;

/**
 * Created by Tom on 18/12/2014.
 * Every activity after the login screen will extend this activity, it takes care of the connection with the service
 */

import com.example.tom.stapp3.service.ShimmerService;
import com.example.tom.stapp3.service.ShimmerService.LocalBinder;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.util.Log;

public abstract class ServiceActivity extends Activity {
    protected static ShimmerService mService;
    protected static boolean mServiceBind=false;
    protected static boolean mServiceFirstTime=true;
    protected static int status;

    protected ServiceConnection mTestServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName arg0, IBinder service) {
            // TODO Auto-generated method stub
            Log.d("ShimmerService", "service connected");
            LocalBinder binder = (ShimmerService.LocalBinder) service;
            mService = binder.getService();
            mServiceBind = true;
        }

        public void onServiceDisconnected(ComponentName arg0) {
            // TODO Auto-generated method stub
            mServiceBind = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //start service if needed
        if(!isMyServiceRunning()) {
            Log.d("SERVICE", "Creating service");
            Intent intent = new Intent(this, ShimmerService.class);
            startService(intent);
            Log.d("SERVICE", "Attempted to start service");
            if(mServiceFirstTime) {
                Log.d("SERVICE", "Attempting to bind service");
                getApplicationContext().bindService(intent, mTestServiceConnection, Context.BIND_AUTO_CREATE);
                mServiceFirstTime = false;
            }
        }
    }

    protected boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (service.service.getClassName().equals("com.example.tom.stapp3.service.ShimmerService")) {
                return true;
            }
        }
        return false;
    }


}
