package com.example.tom.stapp3.activity;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.tom.stapp3.animation.ExpandCollapse;
import com.example.tom.stapp3.persistency.DatabaseHelper;
import com.example.tom.stapp3.Function;
import com.example.tom.stapp3.persistency.Profile;
import com.example.tom.stapp3.R;
import com.example.tom.stapp3.persistency.ServerHelper;

/**
 * Created by Tom on 17/11/2014.
 * The activity that shows you your own profile
 */
public class ProfileView extends DrawerActivity {
    private Profile mProfile;
    private ExpandCollapse editToggle;
    private ExpandCollapse avatarGridToggle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.profile_view);
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
        TypedArray avatars = getResources().obtainTypedArray(R.array.avatars);
        GridView avatarGridView = (GridView) findViewById(R.id.edit_avatar_grid);
        AvatarGridAdapter avatarGridAdapter = new AvatarGridAdapter(this, R.layout.avatar_grid_item, avatars);
        avatarGridView.setAdapter(avatarGridAdapter);
        avatarGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mProfile.setAvatar(getResources().getStringArray(R.array.avatar_names)[position]);
                updateVisual();
                findViewById(R.id.profile_avatar).callOnClick();
            }
        });
        editToggle = new ExpandCollapse(findViewById(R.id.profile_edit));
        avatarGridToggle = new ExpandCollapse(findViewById(R.id.edit_avatar_grid));
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
                Log.i("onOptionsItemSelected", "action_edit");
                editToggle.toggle();
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
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateVisual() {
        TextView rank = (TextView) findViewById(R.id.profile_rank);
        TextView username = (TextView) findViewById(R.id.profile_username);
        TextView money = (TextView) findViewById(R.id.profile_money);
        TextView experience = (TextView) findViewById(R.id.profile_experience);
        ImageView avatar = (ImageView) findViewById(R.id.profile_avatar);

        int avatarID = getResources().getIdentifier("avatar_" + mProfile.getAvatar() + "_512", "drawable", getPackageName());

        rank.setText("Rank: " + mProfile.getRank());
        username.setText(mProfile.getUsername());
        money.setText("Money: " + mProfile.getMoney());
        experience.setText("Experience: " + mProfile.getExperience());
        avatar.setImageResource(avatarID);
    }
}