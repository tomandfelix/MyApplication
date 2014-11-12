package com.example.tom.myapplication;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class Main extends Activity {
    private String[] menuItems;
    private Fragment[] fragments;
    int currFragment;
    private FragmentManager fragmentManager;
    private ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_with_menu);
        Profile profile = getIntent().getParcelableExtra("profile");

        menuItems = getResources().getStringArray(R.array.sideMenu);
        fragments = new Fragment[menuItems.length];
        fragments[0] = ProfileFragment.init(profile);
        currFragment = 0;

        fragmentManager = getFragmentManager();
        getActionBar().setTitle(menuItems[currFragment]);
        fragmentManager.beginTransaction().add(R.id.content_frame, fragments[0]).commit();

        ListView listView = (ListView) findViewById(R.id.menulist);
        listView.setAdapter(new ArrayAdapter<String>(this, R.layout.list_item, menuItems));
        listView.setItemChecked(0, true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                ListView tempListView = (ListView) findViewById(R.id.menulist);
                tempListView.setItemChecked(position, true);
                if(position != currFragment) {
                    if(fragments[position] == null) {
                        switch(position) {
                            case 0:
                                fragments[0] = new ProfileFragment();
                                break;
                            case 1:
                                fragments[1] = new LeaderBoardFragment();
                                break;
                            default:
                                return;
                        }
                    }
                    fragmentManager.beginTransaction().replace(R.id.content_frame, fragments[position]).commit();
                    currFragment = position;
                }
                DrawerLayout tempDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);
                tempDrawerLayout.closeDrawer(tempListView);
            }
        });

        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_launcher, R.string.app_name, R.string.app_name) {
            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getActionBar().setTitle(menuItems[currFragment]);
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