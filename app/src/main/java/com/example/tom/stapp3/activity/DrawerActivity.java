package com.example.tom.stapp3.activity;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.support.v7.widget.Toolbar;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.tom.stapp3.R;
import com.example.tom.stapp3.persistency.DatabaseHelper;
import com.example.tom.stapp3.persistency.Profile;
import com.example.tom.stapp3.persistency.ServerHelper;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

/**
 * Created by Tom on 18/11/2014.
 * Base class for every activity that needs a side menu, this class takes care of this menu and the event handling for it
 */
public abstract class DrawerActivity extends ServiceActivity {
    protected Drawer.Result result;
    protected Profile mProfile;
    protected String[] menuItems;
    private static int index;
    protected ActionBarDrawerToggle toggle;
    protected final static int PROFILE = 0;
    protected final static int LEADERBOARD = 2;
    protected final static int GRAPHS = 3;
    protected final static int SOLO_QUEST = 4;
    protected final static int CHALLENGE = 5;
    protected final static int CO_OPERATIVE = 6;
    protected final static int CONNECTION = 8;
    protected final static int INTERNET_CONNECTION = 9;
    protected final static int SETTINGS = 10;
    protected final static int LOGOUT = 12;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        menuItems = getResources().getStringArray(R.array.sideMenu);
        mProfile = DatabaseHelper.getInstance(this).getProfile(DatabaseHelper.getInstance(this).getIntSetting(DatabaseHelper.OWNER));
        int avatar_id = getResources().getIdentifier("avatar_" + mProfile.getAvatar() + "_512", "drawable", getPackageName());
    result = new Drawer()
                .withActivity(this)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(menuItems[0]).withIcon(avatar_id),
                        new DividerDrawerItem(),
                        new SecondaryDrawerItem().withName(menuItems[2]),
                        new SecondaryDrawerItem().withName(menuItems[3]),
                        new SecondaryDrawerItem().withName(menuItems[4]),
                        new SecondaryDrawerItem().withName(menuItems[5]),
                        new SecondaryDrawerItem().withName(menuItems[6]),
                        new DividerDrawerItem(),
                        new SecondaryDrawerItem().withName(menuItems[1]),
                        new SecondaryDrawerItem().withName(menuItems[9]),
                        new SecondaryDrawerItem().withName(menuItems[7]),
                        new DividerDrawerItem(),
                        new SecondaryDrawerItem().withName(menuItems[8])
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                        ListView tempListView = (ListView) findViewById(R.id.menulist);
                        tempListView.setItemChecked(position, true);
                        DrawerLayout tempDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);
                        tempDrawerLayout.closeDrawer(tempListView);
                        if (position != index) {
                            index = position;
                            Intent intent;
                            switch (position) {
                                case LEADERBOARD:
                                    Log.d("leaderboard","clicked");
                                    intent = new Intent(DrawerActivity.this, LeaderboardView.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                    startActivity(intent);
                                    break;
                                case GRAPHS:
                                    Log.d("Graphs","clicked");
                                    intent = new Intent(DrawerActivity.this, Graph.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                    startActivity(intent);
                                    break;
                                case SOLO_QUEST:
                                case CHALLENGE:
                                case CO_OPERATIVE:
                                    Log.d("Quest","clicked");
                                    intent = new Intent(DrawerActivity.this, QuestList.class);
                                    intent.putExtra("Position", position);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                    startActivity(intent);
                                    break;
                                case CONNECTION:
                                    Log.d("Connection","clicked");
                                    intent = new Intent(DrawerActivity.this, ConnectionView.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                    startActivity(intent);
                                    break;
                                case INTERNET_CONNECTION:
                                    Log.d("internet connection","clicked");
                                    intent = new Intent(getBaseContext(), Internet_Connection.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                    startActivity(intent);
                                    break;
                                case LOGOUT:
                                    Log.d("logout","clicked");
//                            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    switch(which) {
//                                        case DialogInterface.BUTTON_POSITIVE:
//
//                                    }
//                                }
//                            }
                                    DatabaseHelper.getInstance(getApplicationContext()).setSetting(DatabaseHelper.TOKEN, "");
                                    intent = new Intent(DrawerActivity.this, FragmentViewer.class);
                                    startActivity(intent);
                                    overridePendingTransition(R.anim.enter_bottom, R.anim.leave_top);
                                    break;

                                case PROFILE:
                                    Log.d("Profile","clicked");
                                    intent = new Intent(DrawerActivity.this, ProfileView.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                    startActivity(intent);
                                    break;
                            }
                            finish();
                        }
                    }
                })
                .build();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        toggle.onConfigurationChanged(newConfig);
    }




}