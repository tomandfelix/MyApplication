package com.example.tom.stapp3.persistency;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Tom on 6/10/2014.
 * The class that corresponds to the logs in the database
 */
public class DBLog {
    protected String action;
    protected Date datetime;
    protected double data;

    public DBLog(String action, Date datetime, double data) {
        this.action = action;
        this.datetime = datetime;
        this.data = data;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Date getDatetime() {
        return datetime;
    }

    public void setDatetime(Date datetime) {
        this.datetime = datetime;
    }

    public double getData() {
        return data;
    }

    public void setData(double data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return action + " " + DatabaseHelper.df.format(datetime) + " " + data;
    }
}
