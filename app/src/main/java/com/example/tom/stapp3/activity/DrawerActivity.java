package com.example.tom.stapp3.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import com.example.tom.stapp3.R;
import com.example.tom.stapp3.persistency.DatabaseHelper;
import com.example.tom.stapp3.persistency.Profile;
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
    protected Drawer drawerBuilder;
    protected Drawer.Result drawer;
    protected Profile mProfile;
    protected String[] menuItems;
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
    protected static int index = PROFILE;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        menuItems = getResources().getStringArray(R.array.sideMenu);
        mProfile = DatabaseHelper.getInstance(this).getOwner();
        makeDrawer();
    }

    public void makeDrawer() {
        int avatar_id = getResources().getIdentifier("avatar_" + mProfile.getAvatar() + "_128", "drawable", getPackageName());
        final int oldIndex = index;
        drawerBuilder = new Drawer();
        drawerBuilder.withActivity(this);
        drawerBuilder.addDrawerItems(
                new PrimaryDrawerItem().withName(menuItems[0]).withIcon(avatar_id),
                new DividerDrawerItem(),
                new SecondaryDrawerItem().withName(menuItems[1]),
                new SecondaryDrawerItem().withName(menuItems[2]),
                new SecondaryDrawerItem().withName(menuItems[3]),
                new SecondaryDrawerItem().withName(menuItems[4]),
                new SecondaryDrawerItem().withName(menuItems[5]),
                new DividerDrawerItem(),
                new SecondaryDrawerItem().withName(menuItems[6]),
                new SecondaryDrawerItem().withName(menuItems[7]),
                new SecondaryDrawerItem().withName(menuItems[8]),
                new DividerDrawerItem(),
                new SecondaryDrawerItem().withName(menuItems[9]));
        drawerBuilder.withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                if (position != index) {
                    index = position;
                    Intent intent;
                    switch (position) {
                        case LEADERBOARD:
                            loadActivity(LeaderboardView.class);
                            break;
                        case GRAPHS:
                            loadActivity(Graph.class);
                            break;
                        case SOLO_QUEST:
                        case CHALLENGE:
                        case CO_OPERATIVE:
                            loadActivity(QuestList.class);
                            break;
                        case CONNECTION:
                            loadActivity(ConnectionView.class);
                            break;
                        case INTERNET_CONNECTION:
                            loadActivity(Internet_Connection.class);
                            break;
                        case LOGOUT:
                            index = oldIndex;
                            Log.d("logout", "clicked");
                            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case DialogInterface.BUTTON_POSITIVE:
                                            DatabaseHelper.getInstance(getApplicationContext()).setToken("");
                                            Intent intent = new Intent(DrawerActivity.this, FragmentViewer.class);
                                            startActivity(intent);
                                            overridePendingTransition(R.anim.enter_bottom, R.anim.leave_top);
                                            finish();
                                            break;
                                        case DialogInterface.BUTTON_NEGATIVE:
                                            drawer.setSelection(oldIndex);
                                            break;
                                    }
                                }
                            };
                            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(DrawerActivity.this);
                            alertDialog.setMessage("Are you sure you want to log out?");
                            alertDialog.setPositiveButton("Logout", listener);
                            alertDialog.setNegativeButton("Cancel", listener);
                            alertDialog.show();
                            break;
                        case PROFILE:
                            loadActivity(ProfileView.class);
                            break;
                    }
                }
            }
        });
        drawer = drawerBuilder.build();
        drawer.setSelection(index);
    }

    private void loadActivity(Class destination) {
        Log.i("loadActivity", destination.getName());
        Intent intent = new Intent(DrawerActivity.this, destination);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        finish();
    }
}