package com.tomandfelix.stapp2.activity;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.res.Configuration;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.TextView;

import com.androidplot.xy.XYPlot;
import com.tomandfelix.stapp2.R;
import com.tomandfelix.stapp2.pagerAdapter.GraphPagerAdapter;
import com.tomandfelix.stapp2.persistency.DatabaseHelper;
import com.tomandfelix.stapp2.graphtools.GraphParser;

import java.util.Date;

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

public class GraphView extends DrawerActivity implements ActionBar.TabListener{
    private ViewPager viewPager;
    private GraphPagerAdapter mAdapter;
    private ActionBar actionBar;
    private String[] tabs = {"Day", "2 Weeks"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_graph);
        super.onCreate(savedInstanceState);

        viewPager = (ViewPager) findViewById(R.id.pager);
        actionBar = getActionBar();
        mAdapter = new GraphPagerAdapter(getFragmentManager());

        viewPager.setAdapter(mAdapter);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        for(String s : tabs) {
            actionBar.addTab(actionBar.newTab().setText(s).setTabListener(this));
        }

        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
    }
}