package com.example.tom.stapp3.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.tom.stapp3.R;

/**
 * Created by Tom on 18/11/2014.
 * Base class for every activity that needs a side menu, this class takes care of this menu and the event handling for it
 */
public abstract class DrawerActivity extends ServiceActivity {
    protected String[] menuItems;
    protected ActionBarDrawerToggle toggle;
    protected int index;
    protected final static int PROFILE = 0;
    protected final static int CONNECTION = 1;
    protected final static int LEADERBOARD = 2;
    protected final static int SOLO_QUEST = 3;
    protected final static int CHALLENGE = 4;
    protected final static int CO_OPERATIVE = 5;
    protected final static int SETTINGS = 6;
    protected final static int LOGOUT = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        index = savedInstanceState.getInt("ListIndex");

        menuItems = getResources().getStringArray(R.array.sideMenu);
        if(getActionBar() != null) {
            getActionBar().setTitle(menuItems[index]);
        }
        ListView listView = (ListView) findViewById(R.id.menulist);
        if(listView == null) Log.e("ListView", "Listview is null");
        listView.setAdapter(new ArrayAdapter<String>(this, R.layout.list_item_drawer, menuItems));
        listView.setItemChecked(index, true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                ListView tempListView = (ListView) findViewById(R.id.menulist);
                tempListView.setItemChecked(position, true);
                DrawerLayout tempDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);
                tempDrawerLayout.closeDrawer(tempListView);
                if(position != index) {
                    Intent intent;
                    switch(position) {
                        case CONNECTION:
                            intent = new Intent(getBaseContext(), ConnectionView.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivity(intent);
                            break;
                        case LEADERBOARD:
                            intent = new Intent(getBaseContext(), LeaderboardView.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivity(intent);
                            break;
                        case SOLO_QUEST:
                        case CHALLENGE:
                        case CO_OPERATIVE:
                            intent = new Intent(getBaseContext(), QuestList.class);
                            intent.putExtra("Position", position);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivity(intent);
                            break;
                        case LOGOUT:
                            intent = new Intent(getBaseContext(), FragmentViewer.class);
                            startActivity(intent);
                            overridePendingTransition(R.anim.enter_bottom, R.anim.leave_top);
                            break;
                        case PROFILE:
                        default:
                            intent = new Intent(getBaseContext(), ProfileView.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivity(intent);
                            break;
                    }
                }
            }
        });

        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.app_name, R.string.app_name) {
            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getActionBar().setTitle(menuItems[index]);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getActionBar().setTitle("Menu");
            }
        };
        drawerLayout.setDrawerListener(toggle);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        toggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        toggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }


}