package com.tomandfelix.stapp2.application;

import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.tomandfelix.stapp2.gcm.GCMMessageHandler;
import com.tomandfelix.stapp2.persistency.ChallengeList;
import com.tomandfelix.stapp2.persistency.DatabaseHelper;
import com.tomandfelix.stapp2.persistency.Profile;
import com.tomandfelix.stapp2.persistency.ServerHelper;
import com.tomandfelix.stapp2.persistency.VolleyQueue;
import com.tomandfelix.stapp2.service.ShimmerService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;

/**
 * Created by Tom on 11/03/2015.
 */
public class StApp  extends Application {
    private Profile mProfile;
    private static StApp singleton;
    private Messenger toService = null;
    private String PROJECT_NUMBER = "413268601960";
    private String gcmRegistrationId = null;
    private int playServicesResultCode;
    private static Handler gcmHandler = new Handler();
    private static Handler handler = null;

    private static class ServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if(handler != null) {
                Message copy = Message.obtain();
                copy.copyFrom(msg);
                handler.sendMessage(copy);
            }
        }
    }
    private final Messenger fromService = new Messenger(new ServiceHandler());
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d("StApp", "registering messenger");
            toService = new Messenger(service);
            Message msg = Message.obtain(null, ShimmerService.REGISTER_MESSENGER);
            msg.replyTo = fromService;
            sendToService(msg);
        }

        public void onServiceDisconnected(ComponentName arg0) {
            toService = null;
        }
    };

    public static Handler getHandler() {
        return handler;
    }

    public static void setHandler(Handler newHandler) {
        handler = newHandler;
        if(newHandler == null) {
            Log.i("StApp", "handler unset");
        } else {
            Log.i("StApp", "handler set");
        }
    }

    public void commandService(int command) {
        sendToService(Message.obtain(null, command));
    }

    public void commandServiceConnect(String address) {
        Message msg = Message.obtain(null, ShimmerService.CONNECT);
        Bundle bundle = new Bundle();
        bundle.putString("address", address);
        msg.setData(bundle);
        sendToService(msg);
    }

    private void sendToService(Message msg) {
        if(toService != null) {
            try {
                toService.send(msg);
            } catch (RemoteException e) {
                toService = null;
            }
        }
    }

    public static void makeToast(String text) {
        Toast.makeText(singleton, text, Toast.LENGTH_LONG).show();
    }

    public Profile getProfile() {
        return mProfile;
    }

    public void setProfile(Profile mProfile) {
        this.mProfile = mProfile;
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

    public String getGcmRegistrationId() {
        return gcmRegistrationId;
    }

    public int getPlayServicesResultCode() {
        return playServicesResultCode;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
        DatabaseHelper.init(this);
        DatabaseHelper.getInstance().openDB();
        ServerHelper.init(this);
        VolleyQueue.init(this);
        ChallengeList.init();
        new GCMMessageHandler();
        if(!isServiceRunning()) {
            startService(new Intent(this, ShimmerService.class));
        }
        bindService(new Intent(this, ShimmerService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
        handleGCM();
    }

    private void handleGCM() {
        playServicesResultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(playServicesResultCode == ConnectionResult.SUCCESS) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(StApp.this);
                        gcmRegistrationId = gcm.register(PROJECT_NUMBER);
                        Log.i("GCM", "Device registered, registration ID=" + gcmRegistrationId);
                        if(DatabaseHelper.getInstance().getToken() != null && !DatabaseHelper.getInstance().getToken().equals("")) {
                            ServerHelper.getInstance().updateProfileSettings(null, null, null, null, null, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError volleyError) {
                                    if(!volleyError.getMessage().equals("none")) {
                                        Log.e("handleGCM", volleyError.getMessage());
                                    }
                                }
                            });
                        }
                    } catch (IOException ex) {
                        Log.e("GCM :", ex.getMessage());
                        gcmHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                handleGCM();
                            }
                        }, 5000);
                    }
                    return null;
                }
            }.execute(null, null, null);
        }
    }
}
