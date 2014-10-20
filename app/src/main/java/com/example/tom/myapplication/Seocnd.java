package com.example.tom.myapplication;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by Tom on 20/10/2014.
 */
public class Seocnd {
    private Context context;

    public Seocnd(Context c) {
        this.context = c;
    }

    public boolean InternetConnecting() {
        ConnectivityManager connect = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connect != null) {
            NetworkInfo[] information = connect.getAllNetworkInfo();
            if(information != null) {
                for(int i = 0; i < information.length; i++) {
                    if(information[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }
}
