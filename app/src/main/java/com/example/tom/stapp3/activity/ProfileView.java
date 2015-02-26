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
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.tom.stapp3.persistency.DatabaseHelper;
import com.example.tom.stapp3.persistency.Profile;
import com.example.tom.stapp3.R;
import com.example.tom.stapp3.persistency.ServerHelper;
import com.example.tom.stapp3.tools.Logging;

import java.lang.ref.WeakReference;

/**
 * Created by Tom on 17/11/2014.
 * The activity that shows you your own profile
 */
public class ProfileView extends DrawerActivity {
    private Profile mProfile;
    private boolean avatarChanged;
    private static ImageView statusIcon;
    private Handler loggingMessageHandler = new ProfileHandler(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_profile);
        index = PROFILE;
        super.onCreate(savedInstanceState);
        Profile profile = getIntent().getParcelableExtra("profile");
        if (profile != null) {
            mProfile = profile;
            updateVisual();
        } else {
            mProfile = DatabaseHelper.getInstance(this).getProfile(DatabaseHelper.getInstance(this).getIntSetting(DatabaseHelper.OWNER));
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
                    final AlertDialog alertDialog = new AlertDialog.Builder(ProfileView.this).create();
                    alertDialog.setMessage("It seems like an error occured, please logout and try again");
                    alertDialog.setButton("Dismiss", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            alertDialog.dismiss();
                        }
                    });
                    alertDialog.show();
                }
            }, true);
        }

        statusIcon = (ImageView) findViewById(R.id.profile_status_icon);
        //String lastAction = DatabaseHelper.getInstance(this).getLastLog().getAction();
        int result = Logging.STATUS_SIT;
        /*if(lastAction.equals(DatabaseHelper.LOG_STAND)) {
            result = Logging.STATUS_STAND;
        } else if(lastAction.equals(DatabaseHelper.LOG_OVERTIME)) {
            result = Logging.STATUS_OVERTIME;
        }*/
        loggingMessageHandler.obtainMessage(result).sendToTarget();

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

    private String validateInput(String input, String old) {
        if(input == null || input.equals("") || input.equals(old)) {
            return null;
        } else {
            return input;
        }
    }

    public void saveEditedInfo() {
        final String newUsername = validateInput(((EditText) findViewById(R.id.edit_username)).getText().toString(), mProfile.getUsername());
        final String newFirstName =  validateInput(((EditText) findViewById(R.id.edit_firstname)).getText().toString(), mProfile.getFirstName());
        final String newLastName =  validateInput(((EditText) findViewById(R.id.edit_lastname)).getText().toString(), mProfile.getLastName());
        final String newEmail =  validateInput(((EditText) findViewById(R.id.edit_email)).getText().toString(), mProfile.getEmail());
        final String newPassword = validateInput(((EditText) findViewById(R.id.edit_password)).getText().toString(), "");
        final String newAvatar = avatarChanged ? mProfile.getAvatar() : null;

        ((EditText) findViewById(R.id.edit_password)).setText("");

        if(newUsername != null) mProfile.setUsername(newUsername);
        if(newFirstName != null) mProfile.setFirstName(newFirstName);
        if(newLastName != null) mProfile.setLastName(newLastName);
        if(newEmail != null) mProfile.setEmail(newEmail);

        if(newUsername != null || newFirstName != null || newLastName != null || newEmail != null || newPassword != null) {
            if(newPassword != null) {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setMessage("Please enter your current password").setTitle("Password");
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                input.setTransformationMethod(PasswordTransformationMethod.getInstance());
                alert.setView(input);
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String oldPassword = input.getText().toString();
                        ServerHelper.getInstance(getApplicationContext()).updateProfileSettings(newFirstName, newLastName, newUsername, newEmail, newAvatar, oldPassword, newPassword, null); //TODO make this a meaningful errorListener
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
                ServerHelper.getInstance(this).updateProfileSettings(newFirstName, newLastName, newUsername, newEmail, newAvatar, null); //TODO make this a meaningful errorListener
            }
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
                status = msg.what;
                switch (msg.what) {
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
        }
    }
}