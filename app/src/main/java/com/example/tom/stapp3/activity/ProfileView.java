package com.example.tom.stapp3.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

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
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.profile_view);
        if(savedInstanceState == null) {
            savedInstanceState = new Bundle();
        }
        savedInstanceState.putInt("ListIndex", PROFILE);
        super.onCreate(savedInstanceState);

        Profile profile = getIntent().getParcelableExtra("profile");
        if(profile != null) {
            updateVisual(profile);
        } else {
            updateVisual(DatabaseHelper.getInstance(this).getProfile(DatabaseHelper.getInstance(this).getSetting(DatabaseHelper.OWNER)));
            ServerHelper.getInstance(this).getOtherProfile(DatabaseHelper.getInstance(this).getSetting(DatabaseHelper.OWNER), new Function<Profile>() {
                @Override
                public void call(Profile profile) {
                    if(profile != null) {
                        updateVisual(profile);
                    }
                }
            }, false);
        }
    }

    private void updateVisual(Profile profile) {
        TextView username = (TextView) findViewById(R.id.profile_username);
        TextView money = (TextView) findViewById(R.id.profile_money);
        TextView experience = (TextView) findViewById(R.id.profile_experience);
        ImageView avatar = (ImageView) findViewById(R.id.profile_avatar);

        String mDrawableAvatar = "avatar_" + profile.getAvatar() + ".png";
        int resID = getResources().getIdentifier(mDrawableAvatar, "drawable", getPackageName());
        if(profile.getAvatar() != null) { avatar.setImageResource(resID);}
        if(profile.getUsername() != null) {username.setText( profile.getUsername());}
        if(profile.getMoney() != -1) {money.setText(profile.getMoney());}
        if(profile.getExperience() != -1) {experience.setText( profile.getExperience());}
    }


}
