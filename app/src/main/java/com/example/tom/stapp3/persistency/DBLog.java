package com.example.tom.stapp3.persistency;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Tom on 6/10/2014.
 * The class that corresponds to the logs in the database
 */
public class DBLog {
    private String action;
    private Date datetime;
    private String metadata;

    public DBLog(String action, Date datetime, String metadata) {
        this.action = action;
        this.datetime = datetime;
        this.metadata = metadata;
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

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
