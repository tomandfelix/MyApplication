package com.example.tom.stapp3.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import com.androidplot.Plot;
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

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class Graph extends DrawerActivity {
    private XYPlot plot;
    private final int grey = Color.parseColor("#40000000");

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

    private Number[] getScores(ArrayList<DBLog> logs) {
        Number[] scores = new Number[logs.size()];
        for(int i = 0; i < logs.size(); i++) {
            DBLog l = logs.get(i);
            if(l.getAction().equals(DatabaseHelper.LOG_CONNECT)) {
                if(i == 0) {
                    scores[i] = 0;
                } else {
                    scores[i] = scores[i - 1];
                }
            } else if(l.getAction().equals(DatabaseHelper.LOG_DISCONNECT)) {
                DBLog status = logs.get(getStatusIndex(logs, i));
                if(status.getAction().equals(DatabaseHelper.LOG_SIT) || status.getAction().equals(DatabaseHelper.LOG_STAND)) {
                    scores[i] = Logging.getIncreasingScore(l.getDatetime().getTime() - logs.get(i - 1).getDatetime().getTime(), scores[i - 1].doubleValue());
                } else {
                    scores[i] = Logging.getDecreasingScore(getSittingTimeBefore(logs, i), logs.get(getLastSitIndexBefore(logs, i)).getData());
                }
            } else { // LOG_SIT, LOG_STAND, LOG_OVERTIME or LOG_ACHIEVED_SCORE
                scores[i] = l.getData();
            }
        }
        return scores;
    }

    private Number[] getTimeValues(ArrayList<DBLog> logs) {
        Number[] timeValues = new Number[logs.size()];
        for(int i = 0; i < logs.size(); i++) {
            timeValues[i] = getGraphTime(logs.get(i));
        }
        return timeValues;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_graph);
        index = GRAPHS;
        super.onCreate(savedInstanceState);

        plot = (XYPlot) findViewById(R.id.chart);
        ArrayList<DBLog> logs = DatabaseHelper.getInstance(this).getTodaysLogs();
        if(logs.get(logs.size() - 1).getAction().equals(DatabaseHelper.LOG_DISCONNECT) && logs.get(logs.size() - 2).getAction().equals(DatabaseHelper.LOG_ACH_SCORE)) {
            logs.remove(logs.size() - 1);
        }
        Number[] scores = getScores(logs);
        Number[] timeValues = getTimeValues(logs);

        ArrayList<DBLog> maxLogs = DatabaseHelper.getInstance(this).getTodaysConnectionLogs();
        maxLogs.add(1, logs.get(1));
        for(DBLog l : maxLogs) {
            Log.i("TEST", l.toString());
        }
        Number[] maxScores = getScores(maxLogs);
        Number[] maxTimes = getTimeValues(maxLogs);

        final double maxScore = maxScores[maxScores.length - 1].doubleValue();

        Format graphTimeFormat = new Format() {
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

        Format graphScoreFormat = new Format() {
            @Override
            public StringBuffer format(Object object, StringBuffer buffer, FieldPosition field) {
                double percentage = ((Number) object).doubleValue() / maxScore * 100;
                return buffer.append(Double.toString(percentage)).append(" %");
            }

            @Override
            public Object parseObject(String string, ParsePosition position) {
                return null;
            }
        };

        XYSeries dataSeries = new SimpleXYSeries(Arrays.asList(timeValues), Arrays.asList(scores), "data");
        XYSeries maxSeries = new SimpleXYSeries(Arrays.asList(maxTimes), Arrays.asList(maxScores), "max");
//        plot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 300);
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
        plot.getGraphWidget().setMargins(PixelUtils.dpToPix(30), PixelUtils.dpToPix(10), PixelUtils.dpToPix(10), PixelUtils.dpToPix(10));

        // removes background colors from the graph, from outermost to inner most layer
        plot.setBackgroundPaint(null);
        plot.getGraphWidget().setBackgroundPaint(null);
        plot.getGraphWidget().setGridBackgroundPaint(null);
        plot.setBorderPaint(null);

        // removes horizontal and vertical grid lines from the graph
        plot.getGraphWidget().setRangeGridLinePaint(null);
        plot.getGraphWidget().setDomainGridLinePaint(null);

        // Sets the color for the axes and their labels
        plot.getGraphWidget().getRangeLabelPaint().setColor(grey);
        plot.getGraphWidget().getDomainLabelPaint().setColor(grey);
        plot.getGraphWidget().getRangeOriginLinePaint().setColor(grey);
        plot.getGraphWidget().getDomainOriginLinePaint().setColor(grey);

        // Scalable constants for the axes and label size
        final float labelSize = PixelUtils.spToPix(15);
        final float axesWidth = PixelUtils.dpToPix(2.5f);

        // Apply the above settings
        plot.getGraphWidget().getRangeLabelPaint().setTextSize(labelSize);
        plot.getGraphWidget().getDomainLabelPaint().setTextSize(labelSize);
        plot.getGraphWidget().getDomainOriginLinePaint().setStrokeWidth(axesWidth);
        plot.getGraphWidget().getRangeOriginLinePaint().setStrokeWidth(axesWidth);
    }
}
