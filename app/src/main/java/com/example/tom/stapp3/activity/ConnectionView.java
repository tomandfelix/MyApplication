package com.example.tom.stapp3.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

    private final static int REQUEST_ENABLE_BT = 1; //this id will be returned after the activity for enabling bluetooth finishes
    private BluetoothAdapter mBluetoothAdapter = null;

    private static View progress;
    private static View stopBtn;
    private static boolean progressVisible = false;
    private String[] deviceNames;

    private static Handler serviceMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case ShimmerService.SENSOR_STREAMING:
                    if (progressVisible) {
                        progressVisible = false;
                        progress.setVisibility(View.INVISIBLE);
                        stopBtn.setVisibility(View.VISIBLE);
                    }
                    break;
                case ShimmerService.SENSOR_DISCONNECTED:
                    if(stopBtn.getVisibility() == View.VISIBLE) {
                        stopBtn.setVisibility(View.INVISIBLE);
                        progress.setVisibility(View.VISIBLE);
                    }
                    break;
            }
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
        mService.setSecondaryHandler(serviceMessageHandler);

        //Create BT adapter and checks its existence
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null) {
            finish();
        }

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

        View startBtn = findViewById(R.id.start_day);
        progress = findViewById(R.id.connection_progress);
        stopBtn = findViewById(R.id.stop_day);

        //Populate the listView
        ListView deviceList = (ListView) findViewById(R.id.paired);
        deviceList.setAdapter(new ArrayAdapter<>(this, R.layout.list_item_devices, deviceNames));

        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String bluetoothAddress = deviceNames[position];
                mService.connectShimmer(bluetoothAddress, "Device");
                findViewById(R.id.paired).setVisibility(View.INVISIBLE);
                progress.setVisibility(View.VISIBLE);
                progressVisible = true;
            }
        });

        if(DatabaseHelper.getInstance(this).dayStarted() == null) {
            startBtn.setVisibility(View.VISIBLE);
        } else if(mService.getAddress().equals("")) {
            deviceList.setVisibility(View.VISIBLE);
        } else if(mService != null && mService.DeviceIsStreaming(mService.getAddress())) {
            stopBtn.setVisibility(View.VISIBLE);
        } else {
            progress.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroy() {
        mService.setSecondaryHandler(null);
        super.onDestroy();
    }

    public void startDay(View v) {
        v.setVisibility(View.INVISIBLE);
        DatabaseHelper.getInstance(this).startDay();
        findViewById(R.id.paired).setVisibility(View.VISIBLE);
    }

    public void stopDay(View v) {
        mService.removeAddress();
        mService.stopStreamingAllDevices();
        findViewById(R.id.stop_day).setVisibility(View.INVISIBLE);
        findViewById(R.id.start_day).setVisibility(View.VISIBLE);
        Logging.getInstance(this).logAchievedScore();
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
    public void onPause() {
        super.onPause();
        mService.setSecondaryHandler(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        mService.setSecondaryHandler(serviceMessageHandler);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT) {
            if(resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Bluetooth is now enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bluetooth not enabled\nExiting...", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
