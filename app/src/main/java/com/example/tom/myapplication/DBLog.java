package com.example.tom.myapplication;

import java.util.Date;

/**
 * Created by Tom on 6/10/2014.
 * The class that corresponds to the logs in the database
 */
public class DBLog {
    private int id;
    private String action;
    private Date datetime;

    public DBLog(int id, String action, Date datetime) {
        this.id = id;
        this.action = action;
        this.datetime = datetime;
    }

    public int getId() {

        return id;
    }

    public void setId(int id) {
        this.id = id;
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
}
