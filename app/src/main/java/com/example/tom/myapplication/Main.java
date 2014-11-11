package com.example.tom.myapplication;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class Main extends Activity {
    private ListView list;
    private String[] menuItems;
    private Fragment[] fragments;
    int currFragment;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DrawerLayout sideMenu = (DrawerLayout) findViewById(R.id.drawer);
        menuItems = getResources().getStringArray(R.array.sideMenu);
        fragments = new Fragment[menuItems.length];
        for(int i = 0; i < menuItems.length; i++) {
            fragments[i] = null;
        }
        setContentView(R.layout.activity_with_menu);
        Profile profile = getIntent().getParcelableExtra("profile");
        fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        currFragment = 0;
        fragments[0] = ProfileFragment.init(profile);
//        fragments[1] = new LeaderBoardFragment();
        fragmentTransaction.add(R.id.content_frame, fragments[0]);
        fragmentTransaction.commit();
        list = (ListView) findViewById(R.id.menulist);
        list.setAdapter(new ArrayAdapter<String>(this, R.layout.list_item, menuItems));
        list.setItemChecked(0, true);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("ListView", menuItems[position] + " selected");
                list.setItemChecked(position, true);
                if(position != currFragment) {
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
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
                    fragmentTransaction.replace(R.id.content_frame, fragments[position]);
                    fragmentTransaction.commit();
                    currFragment = position;
                }
                DrawerLayout sideMenu = (DrawerLayout) findViewById(R.id.drawer);
                sideMenu.closeDrawer(list);
            }
        });
    }
}