package com.example.tom.stapp3.persistency;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by Tom on 17/02/2015.
 * This class will encapsulate the Volley queue
 */
public class VolleyQueue {
    private static VolleyQueue mVolleyQueue;
    private RequestQueue mQueue;
    private static Context mContext;

    private VolleyQueue(Context context) {
        mContext = context;
        mQueue = getRequestQueue();
    }

    public static synchronized VolleyQueue getInstance(Context context) {
        if (mVolleyQueue == null) {
            mVolleyQueue = new VolleyQueue(context);
        }
        return mVolleyQueue;
    }

    private RequestQueue getRequestQueue() {
        if (mQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mQueue = Volley.newRequestQueue(mContext.getApplicationContext());
        }
        return mQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        req.setRetryPolicy(new DefaultRetryPolicy(8000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        getRequestQueue().add(req);
    }
}
