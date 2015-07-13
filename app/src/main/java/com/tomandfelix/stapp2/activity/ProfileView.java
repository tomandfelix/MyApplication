package com.tomandfelix.stapp2.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.gc.materialdesign.views.ButtonRectangle;
import com.gc.materialdesign.views.ProgressBarCircularIndeterminate;
import com.tomandfelix.stapp2.R;
import com.tomandfelix.stapp2.application.StApp;
import com.tomandfelix.stapp2.persistency.DatabaseHelper;
import com.tomandfelix.stapp2.persistency.Profile;
import com.tomandfelix.stapp2.persistency.ServerHelper;
import com.tomandfelix.stapp2.service.ShimmerService;
import com.tomandfelix.stapp2.tools.Logging;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Set;

/**
 * Created by Tom on 17/11/2014.
 * The activity that shows you your own profile
 */
public class ProfileView extends DrawerActivity {
//    private boolean avatarChanged;
    private static ImageView statusIcon;
    private Handler loggingMessageHandler = new ProfileHandler(this);
    private TextView username;
    private TextView rank;
    private ImageView avatar;
    private ButtonRectangle pauseButton;
    private ButtonRectangle startStopButton;
    private ProgressBarCircularIndeterminate connecting;
    private static final String PAUSE = "Pause";
    private static final String RESUME = "Resume";
    private static final String START = "Start";
    private static final String STOP = "Stop";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("onCreate", "ProfileView");
        setContentView(R.layout.activity_profile);
        super.onCreate(savedInstanceState);

        username = (TextView) findViewById(R.id.profile_username);
        rank = (TextView) findViewById(R.id.profile_rank);
        avatar = (ImageView) findViewById(R.id.profile_avatar);
        pauseButton = (ButtonRectangle) findViewById(R.id.profile_pause_button);
        startStopButton = (ButtonRectangle) findViewById(R.id.profile_start_stop_button);
        connecting = (ProgressBarCircularIndeterminate) findViewById(R.id.profile_progress);

        Profile profile = getIntent().getParcelableExtra("profile");
        if (profile != null) {
            app.setProfile(profile);
            updateVisual();
        } else {
            app.setProfile(DatabaseHelper.getInstance().getOwner());
            updateVisual();
            ServerHelper.getInstance().getProfile(new ServerHelper.ResponseFunc<Profile>() {
                @Override
                public void onResponse(Profile response) {
                    if (response != null) {
                        app.setProfile(response);
                        updateVisual();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    askForPassword();
                }
            }, true);
        }

        statusIcon = (ImageView) findViewById(R.id.profile_status_icon);
        app.commandService(ShimmerService.REQUEST_STATE);
    }

    private void askForPassword() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage("It seems like an error occured, Please enter your password again").setTitle("Password");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setTransformationMethod(PasswordTransformationMethod.getInstance());
        alert.setView(input);
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch(which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        String password = input.getText().toString();
                        ServerHelper.getInstance().login(DatabaseHelper.getInstance().getOwner().getUsername(), password,
                                new ServerHelper.ResponseFunc<Profile>() {
                                    @Override
                                    public void onResponse(Profile response) {
                                        app.setProfile(response);
                                        updateVisual();
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError volleyError) {
                                        if(volleyError.getMessage().equals("wrong")) {
                                            askForPassword();
                                        }
                                    }
                                });
                        break;
                }
            }
        };
        alert.setPositiveButton("CONFIRM", listener);
        alert.setNegativeButton("CANCEL", listener);
        alert.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(StApp.getHandler() != loggingMessageHandler) {
            StApp.setHandler(loggingMessageHandler);
        }
        app.commandService(ShimmerService.REQUEST_STATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(StApp.getHandler() == loggingMessageHandler) {
            StApp.setHandler(null);
        }
    }

    private void updateVisual() {
        getSupportActionBar().setTitle(app.getProfile().getFirstName() + " " + app.getProfile().getLastName());
        rank.setText(Integer.toString(app.getProfile().getRank()));
        username.setText(app.getProfile().getUsername());
        int avatarID = getResources().getIdentifier("avatar_" + app.getProfile().getAvatar() + "_512", "drawable", getPackageName());
        avatar.setImageResource(avatarID);
    }

    public void onPauseResume(View view) {
        if(pauseButton.getText().equals(PAUSE)) {
            app.commandService(ShimmerService.PAUSE);
        } else {
            String sensor = DatabaseHelper.getInstance().getSensor();
            if (sensor != null && !sensor.equals("")) {
                app.commandServiceConnect(sensor);
            }
        }
    }

    public void onStartStop(View view) {
        if(startStopButton.getText().equals(START)) {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBTIntent, 1);
            } else if(DatabaseHelper.getInstance().getSensor() == null || DatabaseHelper.getInstance().getSensor().equals("")) {
                createSensorDialog().show();
            } else {
                app.commandService(ShimmerService.LOG_START_DAY);
                String sensor = DatabaseHelper.getInstance().getSensor();
                app.commandServiceConnect(sensor);
            }
        } else {
            app.commandService(ShimmerService.DISCONNECT);
            app.commandService(ShimmerService.LOG_ACHIEVED_SCORE);
        }
    }

    private void updateState(int state) {
        switch(state) {
            case Logging.STATE_CONNECTING:
                statusIcon.setVisibility(View.GONE);
                connecting.setVisibility(View.VISIBLE);
                pauseButton.setVisibility(View.VISIBLE);
                pauseButton.setText(PAUSE);
                startStopButton.setText(STOP);
                break;
            case Logging.STATE_DISCONNECTED:
                statusIcon.setVisibility(View.VISIBLE);
                statusIcon.setImageResource(R.drawable.icon_disconnected);
                connecting.setVisibility(View.GONE);
                pauseButton.setVisibility(View.VISIBLE);
                pauseButton.setText(RESUME);
                startStopButton.setText(STOP);
                break;
            case Logging.STATE_DAY_STOPPED:
                statusIcon.setVisibility(View.VISIBLE);
                statusIcon.setImageResource(R.drawable.icon_no_day_started);
                connecting.setVisibility(View.GONE);
                pauseButton.setVisibility(View.GONE);
                startStopButton.setText(START);
                break;
            case Logging.STATE_CONNECTED:
            case Logging.STATE_SIT:
                statusIcon.setVisibility(View.VISIBLE);
                statusIcon.setImageResource(R.drawable.icon_sit_green);
                connecting.setVisibility(View.GONE);
                pauseButton.setVisibility(View.VISIBLE);
                pauseButton.setText(PAUSE);
                startStopButton.setText(STOP);
                break;
            case Logging.STATE_STAND:
                statusIcon.setVisibility(View.VISIBLE);
                statusIcon.setImageResource(R.drawable.icon_stand);
                connecting.setVisibility(View.GONE);
                pauseButton.setVisibility(View.VISIBLE);
                pauseButton.setText(PAUSE);
                startStopButton.setText(STOP);
                break;
            case Logging.STATE_OVERTIME:
                statusIcon.setVisibility(View.VISIBLE);
                statusIcon.setImageResource(R.drawable.icon_sit_red);
                connecting.setVisibility(View.GONE);
                pauseButton.setVisibility(View.VISIBLE);
                pauseButton.setText(PAUSE);
                startStopButton.setText(STOP);
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                onStartStop(null);
            } else {
                Toast.makeText(this, "Bluetooth not enabled\nExiting...", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private Dialog createSensorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Please Select a sensor, you can change this later in the settings").setTitle("Select a sensor");
        ListView listView = new ListView(this);
        builder.setView(listView);

        final ArrayList<String> deviceNames = new ArrayList<>();
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter != null) {
            //Get paired devices and add their address to the list
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice d : pairedDevices) {
                    if (d.getName().contains("RN42")) {
                        deviceNames.add(d.getAddress());
                    }
                }
            } else {
                deviceNames.add("No devices Found");
            }
        }

        //Populate the listView
        ArrayAdapter deviceNamesAdapter = new ArrayAdapter<>(this, R.layout.list_item_devices, deviceNames);
        listView.setAdapter(deviceNamesAdapter);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        final Dialog result =  builder.create();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                DatabaseHelper.getInstance().setSensor(deviceNames.get(position));
                result.dismiss();
                onStartStop(null);
            }
        });
        return result;
    }

    private static class ProfileHandler extends Handler {
        private final WeakReference<ProfileView> mProfileView;

        public ProfileHandler(ProfileView aProfileView) {
            mProfileView = new WeakReference<>(aProfileView);
        }

        @Override
        public void handleMessage(Message msg) {
            if(mProfileView.get() != null) {
                mProfileView.get().updateState(msg.what);
            }
        }
    }
}