package com.example.tom.stapp3.activity;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

import com.example.tom.stapp3.animation.ExpandCollapse;
import com.example.tom.stapp3.animation.FadeInOut;
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
    private ExpandCollapse editToggle;
    private ExpandCollapse avatarGridToggle;
    private FadeInOut infoToggle;
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
            mProfile = DatabaseHelper.getInstance(this).getProfile(DatabaseHelper.getInstance(this).getSetting(DatabaseHelper.OWNER));
            updateVisual();
            ServerHelper.getInstance(this).getOtherProfile(DatabaseHelper.getInstance(this).getSetting(DatabaseHelper.OWNER), new Function<Profile>() {
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
                if(!(newAvatar = getResources().getStringArray(R.array.avatar_names)[position]).equals(mProfile.getAvatar())) {
                    avatarChanged = true;
                    mProfile.setAvatar(newAvatar);
                    updateAvatarImage();
                }
                findViewById(R.id.profile_avatar).callOnClick();
            }
        });
        editToggle = new ExpandCollapse(findViewById(R.id.profile_edit));
        avatarGridToggle = new ExpandCollapse(findViewById(R.id.edit_avatar_grid));
        infoToggle = new FadeInOut(findViewById(R.id.profile_info));
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
                if(editToggle.isOpen()) {
                    saveEditedInfo();
                }
                final Animation editToggleAnim = editToggle.getToggleAnimation();
                editToggleAnim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        if(editToggle.isOpen()) {
                            findViewById(R.id.profile_avatar).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    avatarGridToggle.toggle();
                                }
                            });
                        } else {
                            findViewById(R.id.profile_avatar).setOnClickListener(null);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                final Animation infoToggleAnim = infoToggle.getToggleAnimation();
                infoToggleAnim.setDuration(editToggleAnim.getDuration());
                findViewById(R.id.profile_info).startAnimation(infoToggleAnim);
                findViewById(R.id.profile_edit).startAnimation(editToggleAnim);
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

        if(newUsername == null || newUsername.equals("") || newUsername.equals(mProfile.getUsername()))
            newUsername = null;
        if(newFirstName == null || newFirstName.equals("") || newFirstName.equals(mProfile.getFirstName()))
            newFirstName = null;
        if(newLastName == null || newLastName.equals("") || newLastName.equals(mProfile.getLastName()))
            newLastName = null;
        if(newEmail == null || newEmail.equals("") || newEmail.equals(mProfile.getEmail()))
            newEmail = null;
        if(newPassword == null || newPassword.equals(""))
            newPassword = null;
        String newAvatar = avatarChanged ? mProfile.getAvatar() : null;

        if(newUsername == null && newFirstName == null && newLastName == null && newEmail == null && newPassword == null) {
            return;
        } else {

        }
    }
}