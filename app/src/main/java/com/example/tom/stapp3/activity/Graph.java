package com.example.tom.stapp3.activity;

import android.os.Bundle;

import com.example.tom.stapp3.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Arrays;

public class Graph extends DrawerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_graph);
        index = GRAPHS;
        super.onCreate(savedInstanceState);
        LineChart chart = (LineChart) findViewById(R.id.chart);
        chart.setDescription("Daily score");

        ArrayList<String> xlabels = new ArrayList<>(Arrays.asList("Q1", "Q2", "Q3", "Q4"));
        ArrayList<Entry> series1 = new ArrayList<>(Arrays.asList(new Entry(1, 0), new Entry(2, 1), new Entry(3, 2), new Entry(4, 3)));
        ArrayList<Entry> series2 = new ArrayList<>(Arrays.asList(new Entry(4, 0), new Entry(3, 1), new Entry(2, 2), new Entry(1, 3)));
        LineDataSet set1 = new LineDataSet(series1, "company 1");
        LineDataSet set2 = new LineDataSet(series2, "company 2");
        ArrayList<LineDataSet> y = new ArrayList<>(Arrays.asList(set1, set2));

        chart.setData(new LineData(xlabels, y));
    }
}
