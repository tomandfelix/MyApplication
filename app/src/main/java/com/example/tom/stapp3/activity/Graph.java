package com.example.tom.stapp3.activity;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.androidplot.util.PixelUtils;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYSeriesFormatter;
import com.androidplot.xy.XYStepMode;
import com.example.tom.stapp3.R;
import com.example.tom.stapp3.persistency.DBLog;
import com.example.tom.stapp3.persistency.DatabaseHelper;
import com.example.tom.stapp3.tools.Logging;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Graph extends DrawerActivity {
    private XYSeries dataSeries = null;
    private XYSeries maxSeries = null;
    private Format graphScoreFormat;
    private Format graphTimeFormat;
    private double totalGood;
    private double totalBad;
    private double totalScore;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        handleData();
        fillPlot();
        showExtraDataIfNeeded();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_graph);
        fillPlot();
        showExtraDataIfNeeded();
        makeDrawer();
    }

    private Number getGraphTime(DBLog log) {
        return log.getDatetime().getTime() / 1000;
    }

    private int getLastSitIndexBefore(ArrayList<DBLog> logs, int index) {
        for(int i = index; i >= 0; i--) {
            if(logs.get(i).getAction().equals(DatabaseHelper.LOG_SIT)) {
                return i;
            }
        }
        return -1;
    }

    private long getSittingTimeBefore(ArrayList<DBLog> logs, int index) {
        long result = logs.get(index).getDatetime().getTime();
        index--;
        while(!logs.get(index).getAction().equals(DatabaseHelper.LOG_SIT)) {
            if(logs.get(index).getAction().equals(DatabaseHelper.LOG_CONNECT)) {
                result -= logs.get(index).getDatetime().getTime();
            } else if(logs.get(index).getAction().equals(DatabaseHelper.LOG_DISCONNECT)) {
                result += logs.get(index).getDatetime().getTime();
            }
            index--;
        }
        result -= logs.get(index).getDatetime().getTime();
        return result;
    }

    private int getStatusIndex(ArrayList<DBLog> logs, int index) {
        for(int i = index; i >= 0; i--) {
            if(logs.get(i).getAction().equals(DatabaseHelper.LOG_SIT) || logs.get(i).getAction().equals(DatabaseHelper.LOG_STAND) || logs.get(i).getAction().equals(DatabaseHelper.LOG_OVERTIME)) {
                return i;
            }
        }
        return -1;
    }

    private SeriesData getSeries(ArrayList<DBLog> logs) {
        SeriesData s = new SeriesData(logs.size());
        for(int i = 0; i < logs.size(); i++) {
            DBLog l = logs.get(i);
            s.timeValues[i] = getGraphTime(l);
            if(l.getAction().equals(DatabaseHelper.LOG_CONNECT)) {
                if(i == 0) {
                    s.scores[i] = 0;
                } else {
                    s.scores[i] = s.scores[i - 1];
                }
            } else if(l.getAction().equals(DatabaseHelper.LOG_DISCONNECT)) {
                DBLog status = logs.get(getStatusIndex(logs, i));
                if(status.getAction().equals(DatabaseHelper.LOG_SIT) || status.getAction().equals(DatabaseHelper.LOG_STAND)) {
                    s.scores[i] = Logging.getIncreasingScore(l.getDatetime().getTime() - logs.get(i - 1).getDatetime().getTime(), s.scores[i - 1].doubleValue());
                } else {
                    s.scores[i] = Logging.getDecreasingScore(getSittingTimeBefore(logs, i), logs.get(getLastSitIndexBefore(logs, i)).getData());
                }
            } else { // LOG_SIT, LOG_STAND, LOG_OVERTIME or LOG_ACHIEVED_SCORE
                s.scores[i] = l.getData();
            }
        }
        return s;
    }

    private void handleData() {
        ArrayList<DBLog> logs = DatabaseHelper.getInstance(this).getTodaysLogs();
        ArrayList<DBLog> maxLogs = DatabaseHelper.getInstance(this).getTodaysConnectionLogs();
        if(logs != null && logs.size() > 0 && maxLogs !=null && maxLogs.size() > 0) {
            if(!logs.get(logs.size() - 1).getAction().equals(DatabaseHelper.LOG_DISCONNECT)) {
                DBLog now = new DBLog(DatabaseHelper.LOG_DISCONNECT, new Date(), 0);
                logs.add(now);
                maxLogs.add(now);
            }
            maxLogs.add(1, logs.get(1));

            SeriesData scoresData = getSeries(logs);
            SeriesData maxData = getSeries(logs);

            final double maxScore = maxData.scores[maxData.scores.length - 1].doubleValue();

            totalGood = 0;
            totalBad = 0;
            totalScore = 0;
            for(int i = 1; i < scoresData.scores.length; i++) {
                if(scoresData.scores[i].doubleValue() > scoresData.scores[i - 1].doubleValue()) {
                    totalGood += scoresData.timeValues[i].doubleValue() - scoresData.timeValues[i - 1].doubleValue();
                } else if(scoresData.scores[i].doubleValue() < scoresData.scores[i - 1].doubleValue()) {
                    totalBad += scoresData.timeValues[i].doubleValue() - scoresData.timeValues[i - 1].doubleValue();
                }
            }
            totalScore = scoresData.scores[scoresData.scores.length - 1].doubleValue() / maxScore * 100;

            dataSeries = new SimpleXYSeries(Arrays.asList(scoresData.timeValues), Arrays.asList(scoresData.scores), "data");
            maxSeries = new SimpleXYSeries(Arrays.asList(maxData.timeValues), Arrays.asList(maxData.scores), "max");

            graphTimeFormat = new Format() {
                private SimpleDateFormat df = new SimpleDateFormat("HH:mm");

                @Override
                public StringBuffer format(Object object, StringBuffer buffer, FieldPosition field) {
                    Date date = new Date(((Number) object).longValue() * 1000);
                    return df.format(date, buffer, field);
                }

                @Override
                public Object parseObject(String string, ParsePosition position) {
                    return null;
                }
            };

            graphScoreFormat = new Format() {
                @Override
                public StringBuffer format(Object object, StringBuffer buffer, FieldPosition field) {
                    double percentage = ((Number) object).doubleValue() / maxScore * 100;
                    return buffer.append(Long.toString(Math.round(percentage))).append(" %");
                }

                @Override
                public Object parseObject(String string, ParsePosition position) {
                    return null;
                }
            };
        }
    }

    private void fillPlot() {
        XYPlot plot = (XYPlot) findViewById(R.id.chart);
        plot.setRangeStep(XYStepMode.SUBDIVIDE, 5);
        plot.setRangeValueFormat(graphScoreFormat);

        plot.setDomainStep(XYStepMode.SUBDIVIDE, 6);
        plot.setDomainValueFormat(graphTimeFormat);

        XYSeriesFormatter dataFormatter = new CustomFormatter(Color.GREEN, Color.YELLOW, Color.RED);
        XYSeriesFormatter maxFormatter = new LineAndPointFormatter(Color.GRAY, null, null, null);

        plot.addSeries(maxSeries, maxFormatter);
        plot.addSeries(dataSeries, dataFormatter);

        // Removes the unnecessary elements from the graph & it's layout manager
        plot.getLayoutManager().remove(plot.getTitleWidget());
        plot.getLayoutManager().remove(plot.getLegendWidget());
        plot.getLayoutManager().remove(plot.getDomainLabelWidget());
        plot.getLayoutManager().remove(plot.getRangeLabelWidget());

        plot.setTitleWidget(null);
        plot.setLegendWidget(null);
        plot.setDomainLabelWidget(null);
        plot.setRangeLabelWidget(null);

        //Sets the right margins for the graph
        plot.getGraphWidget().setMargins(PixelUtils.dpToPix(5), PixelUtils.dpToPix(10), PixelUtils.dpToPix(15), PixelUtils.dpToPix(5));

        // removes background colors from the graph, from outermost to inner most layer
        plot.setBackgroundPaint(null);
        plot.getGraphWidget().setBackgroundPaint(null);
        plot.getGraphWidget().setGridBackgroundPaint(null);
        plot.setBorderPaint(null);

        // removes horizontal and vertical grid lines from the graph
        plot.getGraphWidget().setRangeGridLinePaint(null);
        plot.getGraphWidget().setDomainGridLinePaint(null);

        // Sets the color for the axes and their labels
        plot.getGraphWidget().getRangeLabelPaint().setColor(Color.GRAY);
        plot.getGraphWidget().getDomainLabelPaint().setColor(Color.GRAY);
        plot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.GRAY);
        plot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.GRAY);

        // Scalable constants for the axes and label size
        final float labelSize = PixelUtils.spToPix(12);
        final float axesWidth = PixelUtils.dpToPix(2.5f);

        // Apply the above settings
        plot.getGraphWidget().getRangeLabelPaint().setTextSize(labelSize);
        plot.getGraphWidget().getDomainLabelPaint().setTextSize(labelSize);
        plot.getGraphWidget().getDomainOriginLinePaint().setStrokeWidth(axesWidth);
        plot.getGraphWidget().getRangeOriginLinePaint().setStrokeWidth(axesWidth);
    }

    private void showExtraDataIfNeeded() {
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            long hGood = Math.round(Math.floor(totalGood / 3600));
            long minGood = Math.round(Math.floor((totalGood % 3600) / 60));
            long secGood = Math.round(Math.floor(totalGood % 60));

            long hBad = Math.round(Math.floor(totalBad/3600));
            long minBad = Math.round(Math.floor((totalBad % 3600) / 60));
            long secBad = Math.round(Math.floor(totalBad % 60));

            ((TextView) findViewById(R.id.graph_good_time)).setText(hGood + ":" + minGood + ":" + secGood);
            ((TextView) findViewById(R.id.graph_bad_time)).setText(hBad + ":" + minBad + ":" + secBad);
            TextView score = (TextView) findViewById(R.id.graph_ach_score);
            score.setText(String.format("%.1f", totalScore) + " %");
            if(totalScore > 80) {score.setTextColor(getResources().getColor(R.color.green));}
            else if (totalScore < 65) {score.setTextColor(getResources().getColor(R.color.red));}
            else {score.setTextColor(getResources().getColor(R.color.orange));}
        }
    }

    private class SeriesData {
        public Number[] scores;
        public Number[] timeValues;

        public SeriesData(int size) {
            scores = new Number[size];
            timeValues = new Number[size];
        }
    }
}