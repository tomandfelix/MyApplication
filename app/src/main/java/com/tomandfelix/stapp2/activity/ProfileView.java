package com.tomandfelix.stapp2.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
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

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.gc.materialdesign.views.ButtonRectangle;
import com.tomandfelix.stapp2.R;
import com.tomandfelix.stapp2.persistency.DatabaseHelper;
import com.tomandfelix.stapp2.persistency.Profile;
import com.tomandfelix.stapp2.persistency.ServerHelper;
import com.tomandfelix.stapp2.tools.Logging;

import java.lang.ref.WeakReference;

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
    private static final String PAUSE = "Pause";
    private static final String RESUME = "Resume";
    private static final String START = "Start";
    private static final String STOP = "Stop";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i("onCreate", "ProfileView");
        setContentView(R.layout.activity_profile);
        super.onCreate(savedInstanceState);

        username = (TextView) findViewById(R.id.profile_username);
        rank = (TextView) findViewById(R.id.profile_rank);
        avatar = (ImageView) findViewById(R.id.profile_avatar);
        pauseButton = (ButtonRectangle) findViewById(R.id.profile_pause_button);
        startStopButton = (ButtonRectangle) findViewById(R.id.profile_start_stop_button);

        Profile profile = getIntent().getParcelableExtra("profile");
        if (profile != null) {
            mProfile = profile;
            updateVisual();
        } else {
            mProfile = DatabaseHelper.getInstance(this).getOwner();
            updateVisual();
            ServerHelper.getInstance(this).getProfile(new ServerHelper.ResponseFunc<Profile>() {
                @Override
                public void onResponse(Profile response) {
                    if (response != null) {
                        mProfile = response;
                        updateVisual();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    final AlertDialog.Builder alertDialog = new AlertDialog.Builder(ProfileView.this);
                    alertDialog.setMessage("It seems like an error occured, please logout and try again");
                    alertDialog.setPositiveButton("Dismiss", null);
                    alertDialog.show();
                }
            }, true);
        }

        statusIcon = (ImageView) findViewById(R.id.profile_status_icon);
        updateState(Logging.getInstance(this).getState());

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

    @Override
    protected void onResume() {
        super.onResume();
        if(Logging.getHandler() != loggingMessageHandler) {
            Logging.setHandler(loggingMessageHandler);
        }
        updateState(Logging.getInstance(this).getState());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(Logging.getHandler() == loggingMessageHandler) {
            Logging.setHandler(null);
        }
    }

    private void updateVisual() {
        getSupportActionBar().setTitle(mProfile.getFirstName() + " " + mProfile.getLastName());
        rank.setText(Integer.toString(mProfile.getRank()));
        username.setText(mProfile.getUsername());
        int avatarID = getResources().getIdentifier("avatar_" + mProfile.getAvatar() + "_512", "drawable", getPackageName());
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

    private void updateState(int state) {
        switch(state) {
            case Logging.STATE_DAY_STARTED:
            case Logging.STATE_CONNECTING:
                statusIcon.setImageResource(R.drawable.icon_disconnected);
                pauseButton.setVisibility(View.VISIBLE);
                pauseButton.setText(PAUSE);
                startStopButton.setText(STOP);
                break;
            case Logging.STATE_DISCONNECTED:
                statusIcon.setImageResource(R.drawable.icon_disconnected);
                pauseButton.setVisibility(View.VISIBLE);
                pauseButton.setText(RESUME);
                startStopButton.setText(STOP);
                break;
            case Logging.STATE_DAY_STOPPED:
                statusIcon.setImageResource(R.drawable.icon_no_day_started);
                pauseButton.setVisibility(View.GONE);
                startStopButton.setText(START);
                break;
            case Logging.STATE_CONNECTED:
                statusIcon.setImageResource(R.drawable.icon_sit_green);
                pauseButton.setVisibility(View.VISIBLE);
                pauseButton.setText(PAUSE);
                startStopButton.setText(STOP);
                break;
            case Logging.STATE_SIT:
                statusIcon.setImageResource(R.drawable.icon_sit_green);
                pauseButton.setVisibility(View.VISIBLE);
                pauseButton.setText(PAUSE);
                startStopButton.setText(STOP);
                break;
            case Logging.STATE_STAND:
                statusIcon.setImageResource(R.drawable.icon_stand);
                pauseButton.setVisibility(View.VISIBLE);
                pauseButton.setText(PAUSE);
                startStopButton.setText(STOP);
                break;
            case Logging.STATE_OVERTIME:
                statusIcon.setImageResource(R.drawable.icon_sit_red);
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
}