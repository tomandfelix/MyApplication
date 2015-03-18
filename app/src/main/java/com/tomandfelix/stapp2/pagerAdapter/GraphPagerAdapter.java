package com.tomandfelix.stapp2.pagerAdapter;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

import com.tomandfelix.stapp2.activity.GraphDayFragment;
import com.tomandfelix.stapp2.activity.GraphWeekFragment;

/**
 * Created by Tom on 18/03/2015.
 */
public class GraphPagerAdapter extends FragmentPagerAdapter{
    public GraphPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int index) {
        switch (index) {
            case 0:
                return new GraphDayFragment();
            case 1:
                return new GraphWeekFragment();
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 2;
    }
}
