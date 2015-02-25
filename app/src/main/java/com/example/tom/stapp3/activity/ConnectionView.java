package com.example.tom.stapp3.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.example.tom.stapp3.R;
import com.example.tom.stapp3.service.ShimmerService;
import com.example.tom.stapp3.persistency.DatabaseHelper;
import com.example.tom.stapp3.tools.Logging;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Set;

public class ConnectionView extends DrawerActivity {

    private final static int REQUEST_ENABLE_BT = 1; //this id will be returned after the activity for enabling bluetooth finishes
    private BluetoothAdapter mBluetoothAdapter = null;

    private static View progress;
    private static View stopBtn;
    private static boolean progressVisible = false;
    private ArrayList<String> deviceNames;
    private ArrayList<BluetoothDevice> shimmerDevices = null;
    private ArrayAdapter<String> deviceNamesAdapter;

    private final Handler serviceMessageHandler = new ConnectionHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_connection);
        index = CONNECTION;
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
            deviceNames = new ArrayList<>();
            shimmerDevices = new ArrayList<>();
            for(BluetoothDevice d:pairedDevices) {
                if(d.getName().contains("RN42")) {
                    shimmerDevices.add(d);
                    String friendlyName = DatabaseHelper.getInstance(this).getFriendlyName(d.getAddress());
                    if(friendlyName == null) {
                        deviceNames.add(d.getName());
                    } else {
                        deviceNames.add(friendlyName);
                    }
                }
            }
        } else {
            deviceNames = new ArrayList<>();
            deviceNames.add("No devices Found");
        }

        View startBtn = findViewById(R.id.start_day);
        progress = findViewById(R.id.connection_progress);
        stopBtn = findViewById(R.id.stop_day);

        //Populate the listView
        final ListView deviceList = (ListView) findViewById(R.id.paired);
        deviceNamesAdapter = new ArrayAdapter<>(this, R.layout.list_item_devices, deviceNames);
        deviceList.setAdapter(deviceNamesAdapter);

        if(shimmerDevices != null && shimmerDevices.size() > 0) {
            deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                    final String bluetoothAddress = shimmerDevices.get(position).getAddress();
                    mService.connectShimmer(bluetoothAddress, "Device");
                    findViewById(R.id.paired).setVisibility(View.INVISIBLE);
                    progress.setVisibility(View.VISIBLE);
                    progressVisible = true;

                    if(DatabaseHelper.getInstance(getApplicationContext()).getFriendlyName(shimmerDevices.get(position).getAddress()) == null) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(ConnectionView.this);
                        alert.setMessage("This is your first time connecting to this sensor, please provide a friendly name for this device").setTitle("Friendly Name");
                        final EditText input = new EditText(ConnectionView.this);
                        input.setSingleLine();
                        alert.setView(input);
                        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String friendlyName = input.getText().toString();
                                Log.d("onClick", "setting pos " + position + " to " + friendlyName);
                                deviceNames.set(position, friendlyName);
                                deviceNamesAdapter.notifyDataSetChanged();
                                DatabaseHelper.getInstance(getApplicationContext()).setFriendlyName(bluetoothAddress, friendlyName);
                            }
                        });
                        alert.show();
                    }
                }
            });
        }

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
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Bluetooth is now enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bluetooth not enabled\nExiting...", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private static class ConnectionHandler extends Handler{
        private final WeakReference<ConnectionView> mConnectionView;

        public ConnectionHandler(ConnectionView aConnectionView) {
            mConnectionView = new WeakReference<>(aConnectionView);
        }

        @Override
        public void handleMessage(Message msg) {
            if(mConnectionView.get() != null) {
                switch (msg.what) {
                    case ShimmerService.SENSOR_STREAMING:
                        if (progressVisible) {
                            progressVisible = false;
                            progress.setVisibility(View.INVISIBLE);
                            stopBtn.setVisibility(View.VISIBLE);
                        }
                        break;
                    case ShimmerService.SENSOR_DISCONNECTED:
                        if (stopBtn.getVisibility() == View.VISIBLE) {
                            stopBtn.setVisibility(View.INVISIBLE);
                            progress.setVisibility(View.VISIBLE);
                        }
                        break;
                }
            }
        }
    }
}
