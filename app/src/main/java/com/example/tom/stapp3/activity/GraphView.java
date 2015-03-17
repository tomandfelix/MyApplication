package com.example.tom.stapp3.activity;

import android.content.res.Configuration;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.androidplot.xy.XYPlot;
import com.example.tom.stapp3.R;
import com.example.tom.stapp3.persistency.DatabaseHelper;
import com.example.tom.stapp3.graphtools.GraphParser;

import java.util.Date;

public class GraphView extends DrawerActivity {
    private GraphParser.DailyGraphData dailydata;
    private GraphParser.LongTermGraphData longTermData;
    private XYPlot plot;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        plot = (XYPlot) findViewById(R.id.chart);

        dailydata = GraphParser.formatDailyData(DatabaseHelper.getInstance(this).getTodaysLogs(), DatabaseHelper.getInstance(this).getTodaysConnectionLogs());
        longTermData = GraphParser.formatLongTermData(DatabaseHelper.getInstance(this).get2WeekEndLogs(), new Date());
//        drawDailyPlot();
        drawLongTermPlot();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_graph);
        drawDailyPlot();
        makeDrawer();
    }

    private void drawDailyPlot() {
        boolean success = GraphParser.formatDailyGraph(plot, dailydata,
                getResources().getColor(R.color.green),
                getResources().getColor(R.color.orange),
                getResources().getColor(R.color.red));
        if(success) {
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                ((TextView) findViewById(R.id.graph_good_time)).setText(dailydata.getTotalGood());
                ((TextView) findViewById(R.id.graph_bad_time)).setText(dailydata.getTotalBad());
                TextView score = (TextView) findViewById(R.id.graph_ach_score);
                score.setText(String.format("%.1f", dailydata.getTotalScore()) + " %");
                if(dailydata.getTotalScore() > 90) {score.setTextColor(getResources().getColor(R.color.green));}
                else if (dailydata.getTotalScore() < 75) {score.setTextColor(getResources().getColor(R.color.red));}
                else {score.setTextColor(getResources().getColor(R.color.orange));}
            }
        } else {
            findViewById(R.id.chart).setVisibility(View.GONE);
            findViewById(R.id.graph_error).setVisibility(View.VISIBLE);
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                ((TextView) findViewById(R.id.graph_error)).setText(getResources().getString(R.string.graph_daily_error));
                ((TextView) findViewById(R.id.graph_good_time)).setText("00:00:00");
                ((TextView) findViewById(R.id.graph_bad_time)).setText("00:00:00");
                ((TextView) findViewById(R.id.graph_ach_score)).setText("N/A");
            }
        }
    }

    private void drawLongTermPlot() {
        boolean success = GraphParser.formatLongTermGraph(plot, longTermData,
                getResources().getColor(R.color.green),
                getResources().getColor(R.color.orange),
                getResources().getColor(R.color.red));
        if(success) {
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                // TODO add the data to the view
            }
        } else {
            findViewById(R.id.chart).setVisibility(View.GONE);
            findViewById(R.id.graph_error).setVisibility(View.VISIBLE);
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                ((TextView) findViewById(R.id.graph_error)).setText(getResources().getString(R.string.graph_2week_error));
                ((TextView) findViewById(R.id.graph_good_time)).setText("00:00:00");
                ((TextView) findViewById(R.id.graph_bad_time)).setText("00:00:00");
                ((TextView) findViewById(R.id.graph_ach_score)).setText("N/A");
            }
        }
    }
}