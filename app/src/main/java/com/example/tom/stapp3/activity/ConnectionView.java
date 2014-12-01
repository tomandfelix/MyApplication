package com.example.tom.stapp3.activity;

import com.example.tom.stapp3.driver.Shimmer;
import com.example.tom.stapp3.driver.ShimmerService.LocalBinder;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.tom.stapp3.R;
import com.example.tom.stapp3.driver.ShimmerService;
import com.example.tom.stapp3.persistency.DatabaseHelper;

import java.util.Set;

public class ConnectionView extends DrawerActivity {

    private BluetoothAdapter mBluetoothAdapter = null;
    public static String currentlyVisible;
    private static Context context;
    private static ShimmerService mService;
    private boolean mServiceBind = false;
    private boolean mServiceFirstTime = true;
    private static String mConnectedDeviceName = null;
    private static boolean mEnableLogging = false;
    private String[] deviceNames;
    private final static int REQUEST_ENABLE_BT = 1; //this id will be returned after the activity for enabling bluetooth finishes

    private static Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case Shimmer.MESSAGE_STATE_CHANGE:
                    switch(msg.arg1) {
                        case Shimmer.STATE_CONNECTED:
                            break;
                        case Shimmer.MSG_STATE_FULLY_INITIALIZED:
                            mService.setEnableLogging(true);
                            break;
                        case Shimmer.STATE_CONNECTING:
                            break;
                        case Shimmer.STATE_NONE:
                            //TODO check if sensor is connected yet and adapt ui accordingly
                            mConnectedDeviceName = null;
                            break;
                    }
                    break;
                case Shimmer.MESSAGE_READ:
                    break;
                case Shimmer.MESSAGE_ACK_RECEIVED:
                    break;
                case Shimmer.MESSAGE_DEVICE_NAME:
                    break;
                case Shimmer.MESSAGE_TOAST:
                    break;
            }
        }
    };

    private ServiceConnection mTestServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocalBinder binder = (LocalBinder) service;
            Log.d("SERVICE", "service connected");
            mService = binder.getService();
            mServiceBind = true;
            mService.setGraphHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("SERVICE", "service disconnected");
            mServiceBind = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.connection_view);
        if(savedInstanceState == null) {
            savedInstanceState = new Bundle();
        }
        savedInstanceState.putInt("ListIndex", CONNECTION);
        super.onCreate(savedInstanceState);

        currentlyVisible = ("onCreate");
        DatabaseHelper.getInstance(this).setAddress("");

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

        //Create BT adapter and checks its existence
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null) {
            finish();
        }

        ConnectionView.context = getApplicationContext();
        Intent intent = new Intent(this, ShimmerService.class);
        PendingIntent pIntent = PendingIntent.getService(this, 0, intent, 0);
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, 60000, 60000, pIntent);

        //Get paired devices and add their address to the list
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0) {
            deviceNames = new String[pairedDevices.size()];
            int i = 0;
            for(BluetoothDevice d : pairedDevices) {
                deviceNames[i] = d.getAddress();
                i++;
            }
        } else {
            deviceNames = new String[1];
            deviceNames[0] = "No devices Found";
        }

        //Populate the listView
        ListView deviceList = (ListView) findViewById(R.id.paired);
        deviceList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_item, deviceNames));

        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String address = deviceNames[position];
                mConnectedDeviceName = address;
                DatabaseHelper.getInstance(getApplicationContext()).setAddress(address);
                mService.connectShimmer(address, "Device");
                mService.setGraphHandler(mHandler);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        //Prompts to enable bluetooth if disabled
        if(!mBluetoothAdapter.isEnabled()) {
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
        }
        currentlyVisible = ("onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        ConnectionView.context = getApplicationContext();
        Intent intent = new Intent(this, ShimmerService.class);
        getApplicationContext().bindService(intent, mTestServiceConnection, Context.BIND_AUTO_CREATE);
        currentlyVisible = ("onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        currentlyVisible = ("onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        currentlyVisible = ("onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        currentlyVisible = ("onDestroy");
    }

    public boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for(RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if("com.shimmerresearch.service.ShimmerServiceCBBC".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_ENABLE_BT:
                if(resultCode != Activity.RESULT_OK) {
                    finish();
                }
                break;
            case 2: //CONNECT SHIMMER
                if(resultCode == Activity.RESULT_OK) {
                    mService.setEnabledSensors(data.getExtras().getInt("Done"), mConnectedDeviceName);
                }
                break;
            case 6: //LOG FILE SHIMMER
                if(resultCode == Activity.RESULT_OK) {
                    mEnableLogging = data.getExtras().getBoolean("LogFileEnableLogging");
                    if(mEnableLogging) {
                        mService.setEnableLogging(mEnableLogging);
                    }
                }
                break;
        }
    }
}
