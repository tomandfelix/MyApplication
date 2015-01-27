package com.example.tom.stapp3.activity;

import com.example.tom.stapp3.service.ShimmerService.LocalBinder;

import android.app.Activity;
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
import android.widget.Toast;

import com.example.tom.stapp3.R;
import com.example.tom.stapp3.service.ShimmerService;
import com.example.tom.stapp3.persistency.DatabaseHelper;
import com.example.tom.stapp3.tools.Logging;

import java.util.Set;

public class ConnectionView extends DrawerActivity {

    private static Context context;
    private final static int REQUEST_ENABLE_BT = 1; //this id will be returned after the activity for enabling bluetooth finishes
    private BluetoothAdapter mBluetoothAdapter = null;
    private static String mBluetoothAddress = null;

    private static View progress;
    private static View stopBtn;
    private static boolean progressVisible = false;
    private String[] deviceNames;

    private static Handler loggingMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case Logging.STATUS_STAND:
                    Log.i("handleMessage", "stand");
                    break;
                case Logging.STATUS_SIT:
                    Log.i("handleMessage", "sit");
                    break;
                case Logging.STATUS_OVERTIME:
                    Log.i("handleMessage", "sit overtime");
                    break;
            }
        }
    };

    private static Handler serviceMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case ShimmerService.SENSOR_CONNECTED:
                    break;
                case ShimmerService.SENSOR_STREAMING:
                    if (progressVisible) {
                        progressVisible = false;
                        progress.setVisibility(View.INVISIBLE);
                        stopBtn.setVisibility(View.VISIBLE);
                    }
                    break;
                case ShimmerService.SENSOR_DISCONNECTED:
                    Log.i("ConnectionHandler", "SERVICE_SENSOR_DISCONNECTED");
                    break;
            }
        }
    };

    private static Context getContext(){
        return ConnectionView.context;
    }

    private ServiceConnection mTestServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocalBinder binder = (LocalBinder) service;
            Log.d("SERVICE", "service connected");
            mService = binder.getService();
            mServiceBind = true;
            mService.setSecondaryHandler(serviceMessageHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("SERVICE", "service disconnected");
            mServiceBind = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_connection);
        if(savedInstanceState == null) {
            savedInstanceState = new Bundle();
        }
        savedInstanceState.putInt("ListIndex", CONNECTION);
        super.onCreate(savedInstanceState);

        Logging.setHandler(loggingMessageHandler);

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

        progress = findViewById(R.id.connection_progress);
        stopBtn = findViewById(R.id.stop_day);

        //Populate the listView
        ListView deviceList = (ListView) findViewById(R.id.paired);
        deviceList.setAdapter(new ArrayAdapter<>(this, R.layout.list_item_devices, deviceNames));

        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mBluetoothAddress = deviceNames[position];
                //DatabaseHelper.getInstance(getApplicationContext()).setAddress(mBluetoothAddress);
                mService.connectShimmer(mBluetoothAddress, "Device");
                findViewById(R.id.paired).setVisibility(View.INVISIBLE);
                progress.setVisibility(View.VISIBLE);
                progressVisible = true;
            }
        });
        if(DatabaseHelper.getInstance(getContext()).dayStarted() == null) {
            findViewById(R.id.start_day).setVisibility(View.VISIBLE);
        } else if(mBluetoothAddress == null) {
            findViewById(R.id.paired).setVisibility(View.VISIBLE);
        } else if(mService.DeviceIsStreaming(mBluetoothAddress)) {
            findViewById(R.id.stop_day).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.connection_progress).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroy() {
        Logging.setHandler(null);
    }

    public void startDay(View v) {
        v.setVisibility(View.INVISIBLE);
        DatabaseHelper.getInstance(this).startDay();
        findViewById(R.id.paired).setVisibility(View.VISIBLE);
    }

    public void stopDay(View v) {
        mBluetoothAddress = null;
        mService.removeAddress();
        mService.stopStreamingAllDevices();
        findViewById(R.id.stop_day).setVisibility(View.INVISIBLE);
        findViewById(R.id.start_day).setVisibility(View.VISIBLE);
        Logging.getInstance(getContext()).logAchievedScore();
    }

    @Override
    public void onStart() {
        super.onStart();
        //Prompts to enable bluetooth if disabled
        if(!mBluetoothAdapter.isEnabled()) {
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ConnectionView.context = getApplicationContext();
        Intent intent = new Intent(this, ShimmerService.class);
        getApplicationContext().bindService(intent, mTestServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
        case REQUEST_ENABLE_BT:
            if(resultCode != Activity.RESULT_OK) {
                Toast.makeText(this, "Bluetooth is now enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bluetooth not enabled\nExiting...", Toast.LENGTH_SHORT).show();
                finish();
            }
            break;
        }
    }
}
