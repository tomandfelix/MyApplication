package com.tomandfelix.stapp2.persistency;

import android.os.Handler;
import android.os.Looper;

/**
 * Created by Flixse on 27/01/2015.
 */
public class Solo extends Quest{
    public enum Difficulty {EASY, MEDIUM, HARD}

    private int xp;
    private int duration;
    private Difficulty difficulty;
    private double progress;
    private String data;
    private Processor processor;
    private Handler handler;

    public Solo(int id, String name, String description, int xp, int duration, Difficulty difficulty, Processor processor){
        super(id, name, description);
        this.xp = xp;
        this.duration = duration;
        this.difficulty = difficulty;
        this.progress = 0;
        this.processor = processor;
    }
    public int getxp() {
        return xp;
    }

    public int getDuration() {
        return duration;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Handler getHandler() {
        return handler;
    }

    public void start() {
        handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                processor.start(Solo.this);
            }
        });
    }

    public void stop() {
        handler.removeCallbacksAndMessages(null);
        handler = null;
        progress = 0;
    }

    public void clear() {
        data = null;
    }

    @Override
    public String toString() {
        String info = "";
        info += "id:" + id;
        info += (name == null ? "":" name:" + name);
        info += (description == null ? "":" description:" + description);
        info += " experience:" + xp;
        info += " duration:" + duration;
        info += " difficulty:";
        switch(difficulty) {
            case EASY:
                info += "EASY";
                break;
            case MEDIUM:
                info += "MEDIUM";
                break;
            case HARD:
                info += "HARD";
                break;
        }
        return info;
    }

    public static abstract class Processor {
        public abstract void start(Solo solo);
    }
}
