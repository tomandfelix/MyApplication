package com.example.tom.stapp3.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.tom.stapp3.persistency.DatabaseHelper;
import com.example.tom.stapp3.Function;
import com.example.tom.stapp3.persistency.Profile;
import com.example.tom.stapp3.R;
import com.example.tom.stapp3.persistency.ServerHelper;
import com.example.tom.stapp3.tools.Logging;

/**
 * Created by Tom on 17/11/2014.
 * The activity that shows you your own profile
 */
public class ProfileView extends DrawerActivity {
    private Profile mProfile;
    private boolean avatarChanged;
    private ImageView statusIcon;
    protected Handler loggingMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            status = msg.what;
            switch(msg.what) {
                case Logging.STATUS_STAND:
                    statusIcon.setImageResource(R.drawable.standing);
                    break;
                case Logging.STATUS_SIT:
                    statusIcon.setImageResource(R.drawable.sitting);
                    break;
                case Logging.STATUS_OVERTIME:
                    statusIcon.setImageResource(R.drawable.sitting);
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_profile);
        if (savedInstanceState == null) {
            savedInstanceState = new Bundle();
        }
        savedInstanceState.putInt("ListIndex", PROFILE);

        super.onCreate(savedInstanceState);

        Profile profile = getIntent().getParcelableExtra("profile");
        if (profile != null) {
            mProfile = profile;
            updateVisual();
        } else {
            mProfile = DatabaseHelper.getInstance(this).getProfile(DatabaseHelper.getInstance(this).getIntSetting(DatabaseHelper.OWNER));
            updateVisual();
            ServerHelper.getInstance(this).getOtherProfile(DatabaseHelper.getInstance(this).getIntSetting(DatabaseHelper.OWNER), new Function<Profile>() {
                @Override
                public void call(Profile profile) {
                    if (profile != null) {
                        mProfile = profile;
                        updateVisual();
                    }
                }
            }, false);
        }

        statusIcon = (ImageView) findViewById(R.id.profile_status_icon);

        avatarChanged = false;
        TypedArray avatars = getResources().obtainTypedArray(R.array.avatars);
        GridView avatarGridView = (GridView) findViewById(R.id.edit_avatar_grid);
        AvatarGridAdapter avatarGridAdapter = new AvatarGridAdapter(this, R.layout.grid_item_avatar, avatars);
        avatarGridView.setAdapter(avatarGridAdapter);
        avatarGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String newAvatar;
                findViewById(R.id.profile_avatar).callOnClick();
                if(!(newAvatar = getResources().getStringArray(R.array.avatar_names)[position]).equals(mProfile.getAvatar())) {
                    avatarChanged = true;
                    mProfile.setAvatar(newAvatar);
                    updateAvatarImage();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.profile_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit:
                if(findViewById(R.id.profile_edit).getVisibility() == View.GONE) {
                    findViewById(R.id.profile_edit).setVisibility(View.VISIBLE);
                    findViewById(R.id.profile_info).setVisibility(View.INVISIBLE);
                    findViewById(R.id.profile_status).setVisibility(View.INVISIBLE);
                    findViewById(R.id.profile_avatar).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(findViewById(R.id.edit_avatar_grid).getVisibility() == View.GONE) {
                                findViewById(R.id.edit_avatar_grid).setVisibility(View.VISIBLE);
                            } else {
                                findViewById(R.id.edit_avatar_grid).setVisibility(View.GONE);
                            }
                        }
                    });
                } else {
                    findViewById(R.id.profile_edit).setVisibility(View.GONE);
                    findViewById(R.id.profile_info).setVisibility(View.VISIBLE);
                    findViewById(R.id.profile_status).setVisibility(View.VISIBLE);
                    saveEditedInfo();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(Logging.getHandler() != loggingMessageHandler) {
            Logging.setHandler(loggingMessageHandler);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(Logging.getHandler() == loggingMessageHandler) {
            Logging.setHandler(null);
        }
    }

    private void updateVisual() {
        ((TextView) findViewById(R.id.profile_rank)).setText("Rank: " + mProfile.getRank());
        ((TextView) findViewById(R.id.profile_username)).setText(mProfile.getUsername());
        ((TextView) findViewById(R.id.profile_money)).setText("Money: " + mProfile.getMoney());
        ((TextView) findViewById(R.id.profile_experience)).setText("Experience: " + mProfile.getExperience());

        updateAvatarImage();

        ((EditText) findViewById(R.id.edit_username)).setText(mProfile.getUsername());
        ((EditText) findViewById(R.id.edit_firstname)).setText(mProfile.getFirstName());
        ((EditText) findViewById(R.id.edit_lastname)).setText(mProfile.getLastName());
        ((EditText) findViewById(R.id.edit_email)).setText(mProfile.getEmail());
    }

    private void updateAvatarImage() {
        ImageView avatar = (ImageView) findViewById(R.id.profile_avatar);
        int avatarID = getResources().getIdentifier("avatar_" + mProfile.getAvatar() + "_512", "drawable", getPackageName());
        avatar.setImageResource(avatarID);
    }

    public void saveEditedInfo() {
        String newUsername = ((EditText) findViewById(R.id.edit_username)).getText().toString();
        String newFirstName =  ((EditText) findViewById(R.id.edit_firstname)).getText().toString();
        String newLastName =  ((EditText) findViewById(R.id.edit_lastname)).getText().toString();
        String newEmail =  ((EditText) findViewById(R.id.edit_email)).getText().toString();
        String newPassword = ((EditText) findViewById(R.id.edit_password)).getText().toString();

        if(newUsername == null || newUsername.equals("") || newUsername.equals(mProfile.getUsername())) {
            newUsername = null;
        } else {
            mProfile.setUsername(newUsername);
        }
        if(newFirstName == null || newFirstName.equals("") || newFirstName.equals(mProfile.getFirstName())) {
            newFirstName = null;
        } else {
            mProfile.setFirstName(newFirstName);
        }
        if(newLastName == null || newLastName.equals("") || newLastName.equals(mProfile.getLastName())) {
            newLastName = null;
        } else {
            mProfile.setLastName(newLastName);
        }
        if(newEmail == null || newEmail.equals("") || newEmail.equals(mProfile.getEmail())) {
            newEmail = null;
        } else {
            mProfile.setEmail(newEmail);
        }
        if(newPassword == null || newPassword.equals(""))
            newPassword = null;
        final String newAvatar = avatarChanged ? mProfile.getAvatar() : null;

        if(newUsername != null || newFirstName != null || newLastName != null || newEmail != null || newPassword != null) {
            if(newPassword != null) {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setMessage("Please enter your current password").setTitle("Password");
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                input.setTransformationMethod(PasswordTransformationMethod.getInstance());
                alert.setView(input);
                final String finalNewFirstName = newFirstName;
                final String finalNewLastName = newLastName;
                final String finalNewUsername = newUsername;
                final String finalNewEmail = newEmail;
                final String finalNewPassword = newPassword;
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String oldPassword = input.getText().toString();
                        ServerHelper.getInstance(getApplicationContext()).updateProfileSettings(finalNewFirstName, finalNewLastName, finalNewUsername, finalNewEmail, newAvatar, oldPassword, finalNewPassword);
                    }
                });
                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((EditText) findViewById(R.id.edit_password)).setText(null);
                    }
                });
                alert.show();
            } else {
                ServerHelper.getInstance(this).updateProfileSettings(newFirstName, newLastName, newUsername, newEmail, newAvatar);
            }
        }
    }
}