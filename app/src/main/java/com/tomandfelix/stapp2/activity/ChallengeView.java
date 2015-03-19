package com.tomandfelix.stapp2.activity;

/**
 * Created by Flixse on 19/03/2015.
 */

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.tomandfelix.stapp2.R;
import com.tomandfelix.stapp2.tabs.SlidingTabLayout;

public class ChallengeView extends DrawerActivity {
    private SlidingTabLayout tabLayout;
    private ViewPager viewPager;
    private GraphPagerAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
            setContentView(R.layout.activity_challenge_list);
            super.onCreate(savedInstanceState);

            adapter = new GraphPagerAdapter(getFragmentManager());
            viewPager = (ViewPager) findViewById(R.id.challenge_pager);
            viewPager.setAdapter(adapter);
            tabLayout = (SlidingTabLayout) findViewById(R.id.tabs_challenge_bar);
            tabLayout.setViewPager(viewPager);
            tabLayout.setSelectedIndicatorColors(R.color.primaryColor);
            }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
            super.onConfigurationChanged(newConfig);
            tabLayout.setLabelWidth();
            if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
            findViewById(R.id.tabs_bar).setVisibility(View.VISIBLE);
            else
            findViewById(R.id.tabs_bar).setVisibility(View.GONE);
            }

    private class GraphPagerAdapter extends FragmentPagerAdapter {
        public GraphPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int index) {
            switch (index) {
                case 0:
                    return new ListChallengesFragment();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return position == 0 ? "1 Day" : "2 Weeks";
        }
    }
}