package com.example.tom.stapp3.activity;

import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.tom.stapp3.persistency.DatabaseHelper;
import com.example.tom.stapp3.Function;
import com.example.tom.stapp3.persistency.Profile;
import com.example.tom.stapp3.R;
import com.example.tom.stapp3.persistency.ServerHelper;

/**
 * Created by Tom on 3/11/2014.
 * This is the first activity, it is a viewpager with 3 fragments: login, start and register
 */
public class FragmentViewer extends FragmentActivity implements FragmentProvider.OnFragmentInteractionListener{
    private static final int NUM_PAGES = 3;
    private ViewPager mPager;
    private String avatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pager);
        PagerAdapter mAdapter = new FragmentAdapter(getFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(1, false);
        DatabaseHelper.getInstance(this).setReadable();
    }

    public void loginBtn(View v) {
        final EditText username = (EditText) findViewById(R.id.login_username);
        final EditText password = (EditText) findViewById(R.id.login_password);
        final TextView txtView = (TextView) findViewById(R.id.succes);
        ServerHelper.getInstance(this).getProfile(username.getText().toString(), password.getText().toString(), new Function<Profile>() {
            @Override
            public void call(Profile param) {
                        if(param != null) {
                            txtView.setText("Profile is being loaded");
                            Intent intent = new Intent(getBaseContext(), ProfileView.class);
                            intent.putExtra("profile", param);
                            startActivity(intent);
                            overridePendingTransition(R.anim.enter_top, R.anim.leave_bottom);
                        }else {
                            txtView.setText("No such profile exist, please try again");
                            username.setText(null);
                            password.setText(null);
                        }
            }
        }, true);
    }


    public void registerBtn(View v) {
        EditText firstName = (EditText) findViewById(R.id.new_firstname);
        EditText lastName = (EditText) findViewById(R.id.new_lastname);
        EditText username = (EditText) findViewById(R.id.new_username);
        EditText email = (EditText) findViewById(R.id.new_email);
        EditText password = (EditText) findViewById(R.id.new_password);

        ServerHelper.getInstance(this).createProfile(firstName.getText().toString(), lastName.getText().toString(), username.getText().toString(), email.getText().toString(), "unknown", password.getText().toString(), new Function<Profile>() {
            @Override
            public void call(Profile param) {
                Intent intent = new Intent(getBaseContext(), ProfileView.class);
                intent.putExtra("profile", param);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_top, R.anim.leave_bottom);
            }
        });
    }

    public void toLogin(View v) {
        mPager.setCurrentItem(0, true);
    }

    public void toNewProfile(View v) {
        mPager.setCurrentItem(2, true);
    }

    public void toStart(View v) {
        mPager.setCurrentItem(1, true);
    }

    public void toggleAvatarGrid(View v) {
        final GridView avatarGrid = (GridView) findViewById(R.id.avatar_grid);
        if(avatarGrid.getVisibility() == View.INVISIBLE) {
            avatarGrid.getLayoutParams().height = 0;
            avatarGrid.setVisibility(View.VISIBLE);
            Animation expand = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    avatarGrid.getLayoutParams().height = interpolatedTime == 1 ? RelativeLayout.LayoutParams.WRAP_CONTENT : (int) (300 * interpolatedTime);
                    avatarGrid.requestLayout();
                }

                @Override
                public boolean willChangeBounds() {
                    return true;
                }
            };
            expand.setDuration((int) (300 / avatarGrid.getContext().getResources().getDisplayMetrics().density));
            avatarGrid.startAnimation(expand);
        } else {
            Animation collapse = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    if(interpolatedTime == 1) {
                        avatarGrid.setVisibility(View.INVISIBLE);
                    } else {
                        avatarGrid.getLayoutParams().height = 300 - (int) (300 * interpolatedTime);
                        avatarGrid.requestLayout();
                    }
                }

                @Override
                public boolean willChangeBounds() {
                    return true;
                }
            };
            collapse.setDuration((int) (300 / avatarGrid.getContext().getResources().getDisplayMetrics().density));
            avatarGrid.startAnimation(collapse);
        }
    }

    @Override
    public void onFragmentInteraction(String message) {
        avatar = message;
        Log.i("Avatar", message);
        setAvatarImage(message);
        toggleAvatarGrid(null);
    }

    public void setAvatarImage(String avatar) {
        int avatarID = getResources().getIdentifier("avatar_" + avatar + "_512", "drawable", getPackageName());
        ImageView avatarView = (ImageView) findViewById(R.id.new_avatar);
        avatarView.setImageResource(avatarID);
        avatarView.requestLayout();
        findViewById(R.id.avatar_grid).requestLayout();
        findViewById(R.id.new_username).requestLayout();
    }

    private class FragmentAdapter extends FragmentPagerAdapter {
        public FragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }

        @Override
        public Fragment getItem(int position) {
            return FragmentProvider.init(position);
        }
    }
}
