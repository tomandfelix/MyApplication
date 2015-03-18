package com.tomandfelix.stapp2.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;

import com.tomandfelix.stapp2.R;
import com.tomandfelix.stapp2.tabs.SlidingTabLayout;

//public class GraphView extends DrawerActivity {
//    private GraphParser.DailyGraphData dailydata;
//    private GraphParser.LongTermGraphData longTermData;
//    private XYPlot plot;
//
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_graph);
//        plot = (XYPlot) findViewById(R.id.chart);
//
//        dailydata = GraphParser.formatDailyData(DatabaseHelper.getInstance(this).getTodaysLogs(), DatabaseHelper.getInstance(this).getTodaysConnectionLogs());
//        longTermData = GraphParser.formatLongTermData(DatabaseHelper.getInstance(this).get2WeekEndLogs(), new Date());
//        drawDailyPlot();
//        drawLongTermPlot();
//    }
//
//    @Override
//    public void onConfigurationChanged(Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
//        setContentView(R.layout.activity_graph);
//        drawDailyPlot();
//        drawLongTermPlot();
//        makeDrawer();
//    }
//
//    private void drawDailyPlot() {
//        boolean success = GraphParser.formatDailyGraph(plot, dailydata,
//                getResources().getColor(R.color.green),
//                getResources().getColor(R.color.orange),
//                getResources().getColor(R.color.red));
//        if(!success) {
//            findViewById(R.id.chart).setVisibility(View.GONE);
//            findViewById(R.id.graph_error).setVisibility(View.VISIBLE);
//            ((TextView) findViewById(R.id.graph_error)).setText(getResources().getString(R.string.graph_daily_error));
//        }
//        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
//            ((TextView) findViewById(R.id.graph_label_1)).setText(getString(R.string.graph_good_time));
//            ((TextView) findViewById(R.id.graph_label_2)).setText(getString(R.string.graph_bad_time));
//            ((TextView) findViewById(R.id.graph_label_3)).setText(getString(R.string.graph_score));
//            if(success) {
//                ((TextView) findViewById(R.id.graph_value_1)).setText(dailydata.getTotalGood());
//                ((TextView) findViewById(R.id.graph_value_2)).setText(dailydata.getTotalBad());
//                TextView score = (TextView) findViewById(R.id.graph_value_3);
//                score.setText(String.format("%.1f", dailydata.getTotalScore()) + " %");
//                if(dailydata.getTotalScore() > 90) {score.setTextColor(getResources().getColor(R.color.green));}
//                else if (dailydata.getTotalScore() < 75) {score.setTextColor(getResources().getColor(R.color.red));}
//                else {score.setTextColor(getResources().getColor(R.color.orange));}
//            } else {
//                ((TextView) findViewById(R.id.graph_value_1)).setText("00:00:00");
//                ((TextView) findViewById(R.id.graph_value_2)).setText("00:00:00");
//                ((TextView) findViewById(R.id.graph_value_3)).setText("N/A");
//            }
//        }
//    }
//
//    private void drawLongTermPlot() {
//        boolean success = GraphParser.formatLongTermGraph(plot, longTermData,
//                getResources().getColor(R.color.green),
//                getResources().getColor(R.color.orange),
//                getResources().getColor(R.color.red));
//        if(!success) {
//            findViewById(R.id.chart).setVisibility(View.GONE);
//            findViewById(R.id.graph_error).setVisibility(View.VISIBLE);
//            ((TextView) findViewById(R.id.graph_error)).setText(getResources().getString(R.string.graph_2week_error));
//        }
//        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
//            ((TextView) findViewById(R.id.graph_label_1)).setText(getString(R.string.graph_average_score));
//            ((TextView) findViewById(R.id.graph_label_2)).setText(getString(R.string.graph_average_connection_time));
//            ((TextView) findViewById(R.id.graph_label_2)).setText("");
//            if(success) {
//                TextView averageScore = (TextView) findViewById(R.id.graph_value_1);
//                averageScore.setText(String.format("%.1f", longTermData.getAverageScore()) + " %");
//                if(dailydata.getTotalScore() > 90) {averageScore.setTextColor(getResources().getColor(R.color.green));}
//                else if (dailydata.getTotalScore() < 75) {averageScore.setTextColor(getResources().getColor(R.color.red));}
//                else {averageScore.setTextColor(getResources().getColor(R.color.orange));}
//                ((TextView) findViewById(R.id.graph_value_2)).setText(longTermData.getAverageConnectionTime());
//                ((TextView) findViewById(R.id.graph_value_3)).setText("");
//            } else {
//                ((TextView) findViewById(R.id.graph_value_1)).setText("N/A");
//                ((TextView) findViewById(R.id.graph_value_2)).setText("00:00:00");
//                ((TextView) findViewById(R.id.graph_value_3)).setText("");
//            }
//        }
//    }
//}

public class GraphView extends DrawerActivity {
    private SlidingTabLayout tabLayout;
    private ViewPager viewPager;
    private GraphPagerAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_graph);
        super.onCreate(savedInstanceState);

        adapter = new GraphPagerAdapter(getFragmentManager());
        viewPager = (ViewPager) findViewById(R.id.graph_pager);
        viewPager.setAdapter(adapter);
        tabLayout = (SlidingTabLayout) findViewById(R.id.tabs_bar);
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

        @Override
        public CharSequence getPageTitle(int position) {
            return position == 0 ? "1 Day" : "2 Weeks";
        }
    }
}