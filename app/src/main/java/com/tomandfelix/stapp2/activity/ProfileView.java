package com.tomandfelix.stapp2.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.gc.materialdesign.views.ButtonRectangle;
import com.gc.materialdesign.views.ProgressBarCircularIndeterminate;
import com.tomandfelix.stapp2.R;
import com.tomandfelix.stapp2.persistency.DatabaseHelper;
import com.tomandfelix.stapp2.persistency.Profile;
import com.tomandfelix.stapp2.persistency.ServerHelper;
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
        updateState(Logging.getInstance().getState());

//        avatarChanged = false;
//        TypedArray avatars = getResources().obtainTypedArray(R.array.avatars);
//        GridView avatarGridView = (GridView) findViewById(R.id.edit_avatar_grid);
//        AvatarGridAdapter avatarGridAdapter = new AvatarGridAdapter(this, R.layout.grid_item_avatar, avatars);
//        avatarGridView.setAdapter(avatarGridAdapter);
//        avatarGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                String newAvatar;
//                findViewById(R.id.profile_avatar).callOnClick();
//                if(!(newAvatar = getResources().getStringArray(R.array.avatar_names)[position]).equals(mProfile.getAvatar())) {
//                    avatarChanged = true;
//                    mProfile.setAvatar(newAvatar);
//                    updateAvatarImage();
//                }
//            }
//        });
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.profile_menu, menu);
//        return super.onCreateOptionsMenu(menu);
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.action_edit:
//                if(findViewById(R.id.profile_edit).getVisibility() == View.GONE) {
//                    findViewById(R.id.profile_edit).setVisibility(View.VISIBLE);
//                    findViewById(R.id.profile_info).setVisibility(View.INVISIBLE);
//                    findViewById(R.id.profile_status).setVisibility(View.INVISIBLE);
//                    findViewById(R.id.profile_avatar).setOnClickListener(new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//                            if(findViewById(R.id.edit_avatar_grid).getVisibility() == View.GONE) {
//                                findViewById(R.id.edit_avatar_grid).setVisibility(View.VISIBLE);
//                            } else {
//                                findViewById(R.id.edit_avatar_grid).setVisibility(View.GONE);
//                            }
//                        }
//                    });
//                } else {
//                    findViewById(R.id.profile_edit).setVisibility(View.GONE);
//                    findViewById(R.id.profile_info).setVisibility(View.VISIBLE);
//                    findViewById(R.id.profile_status).setVisibility(View.VISIBLE);
//                    saveEditedInfo();
//                }
//                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }

    private void askForPassword() {
        AlertDialog.Builder alert = new AlertDialog.Builder(ProfileView.this);
        alert.setMessage("It seems like an error occured, Please enter your password again").setTitle("Password");
        final EditText input = new EditText(ProfileView.this);
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
        if(Logging.getHandler() != loggingMessageHandler) {
            Logging.setHandler(loggingMessageHandler);
        }
        updateState(Logging.getInstance().getState());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(Logging.getHandler() == loggingMessageHandler) {
            Logging.setHandler(null);
        }
    }

    private void updateVisual() {
        getSupportActionBar().setTitle(app.getProfile().getFirstName() + " " + app.getProfile().getLastName());
        rank.setText(Integer.toString(app.getProfile().getRank()));
        username.setText(app.getProfile().getUsername());
        int avatarID = getResources().getIdentifier("avatar_" + app.getProfile().getAvatar() + "_512", "drawable", getPackageName());
        avatar.setImageResource(avatarID);
//        ((TextView) findViewById(R.id.profile_experience)).setText(mProfile.getExperience() + " XP");

//        ((EditText) findViewById(R.id.edit_username)).setText(mProfile.getUsername());
//        ((EditText) findViewById(R.id.edit_firstname)).setText(mProfile.getFirstName());
//        ((EditText) findViewById(R.id.edit_lastname)).setText(mProfile.getLastName());
//        ((EditText) findViewById(R.id.edit_email)).setText(mProfile.getEmail());
    }

    private String validateInput(String input, String old) {
        if(input == null || input.equals("") || input.equals(old)) {
            return null;
        } else {
            return input;
        }
    }

//    public void saveEditedInfo() {
//        final String newUsername = validateInput(((EditText) findViewById(R.id.edit_username)).getText().toString(), mProfile.getUsername());
//        final String newFirstName =  validateInput(((EditText) findViewById(R.id.edit_firstname)).getText().toString(), mProfile.getFirstName());
//        final String newLastName =  validateInput(((EditText) findViewById(R.id.edit_lastname)).getText().toString(), mProfile.getLastName());
//        final String newEmail =  validateInput(((EditText) findViewById(R.id.edit_email)).getText().toString(), mProfile.getEmail());
//        final String newPassword = validateInput(((EditText) findViewById(R.id.edit_password)).getText().toString(), "");
//        final String newAvatar = avatarChanged ? mProfile.getAvatar() : null;
//        int avatarID = getResources().getIdentifier("avatar_" + mProfile.getAvatar() + "_512", "drawable", getPackageName());
//        drawer.updateIcon(getResources().getDrawable(avatarID),0);
//
//
//        ((EditText) findViewById(R.id.edit_password)).setText("");
//
//        if(newUsername != null) mProfile.setUsername(newUsername);
//        if(newFirstName != null) mProfile.setFirstName(newFirstName);
//        if(newLastName != null) mProfile.setLastName(newLastName);
//        if(newEmail != null) mProfile.setEmail(newEmail);
//
//        if(newUsername != null || newFirstName != null || newLastName != null || newEmail != null || newPassword != null) {
//            if(newPassword != null) {
//                AlertDialog.Builder alert = new AlertDialog.Builder(this);
//                alert.setMessage("Please enter your current password").setTitle("Password");
//                final EditText input = new EditText(this);
//                input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
//                input.setTransformationMethod(PasswordTransformationMethod.getInstance());
//                alert.setView(input);
//                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        switch(which) {
//                            case DialogInterface.BUTTON_POSITIVE:
//                                String oldPassword = input.getText().toString();
//                                ServerHelper.getInstance(getApplicationContext()).updateProfileSettings(newFirstName, newLastName, newUsername, newEmail, newAvatar, oldPassword, newPassword, null); //TODO make this a meaningful errorListener
//                                break;
//                            case DialogInterface.BUTTON_NEGATIVE:
//                                ((EditText) findViewById(R.id.edit_password)).setText(null);
//                                break;
//                        }
//                    }
//                };
//                alert.setPositiveButton("CONFIRM", listener);
//                alert.setNegativeButton("CANCEL", listener);
//                alert.show();
//            } else {
//                ServerHelper.getInstance(this).updateProfileSettings(newFirstName, newLastName, newUsername, newEmail, newAvatar, null); //TODO make this a meaningful errorListener
//            }
//        }
//    }

    public void onPauseResume(View view) {
        if(pauseButton.getText().equals(PAUSE)) {
            Log.e("onPauseResume", pauseButton.getText());
            app.getService().disconnectShimmer();
        } else {
            String sensor = DatabaseHelper.getInstance().getSensor();
            if (sensor != null && !sensor.equals("")) {
                app.getService().connectShimmer(sensor, "Device");
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
                Logging.getInstance().logStartDay();
                String sensor = DatabaseHelper.getInstance().getSensor();
                app.getService().connectShimmer(sensor, "Device");
            }
        } else {
            app.getService().disconnectShimmer();
            Logging.getInstance().logAchievedScore();
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
}