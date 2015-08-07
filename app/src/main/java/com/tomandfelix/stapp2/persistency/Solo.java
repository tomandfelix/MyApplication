package com.tomandfelix.stapp2.persistency;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.tomandfelix.stapp2.application.StApp;

/**
 * Created by Flixse on 27/01/2015.
 */
public class Solo extends Quest{
    public enum Difficulty {EASY, MEDIUM, HARD}

    private int xp;
    private int xpNeeded;
    private int duration;
    private Difficulty difficulty;
    private double progress;
    private Object data;
    private Processor processor;
    private Handler handler;

    public Solo(int id, String name, String description, int xp,int xpNeeded, int duration, Difficulty difficulty, Processor processor){
        super(id, name, description, Type.SOLO);
        this.xp = xp;
        this.xpNeeded = xpNeeded;
        this.duration = duration;
        this.difficulty = difficulty;
        this.progress = 0;
        this.processor = processor;
    }

    @Override
    public String getDescription() {
        return super.getDescription().replace("<duration>", Integer.toString(duration));
    }

    public int getxp() {
        return xp;
    }

    public int getXpNeeded() {
        return xpNeeded;
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
        Log.d("solo", "Progress=" + progress);
        this.progress = progress;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
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

    public void won() {
        StApp.makeToast("Quest complete, you have won!");
        data = "Quest complete, you have won!";
        Log.d("Solo", "Quest complete, you have won!");
        ServerHelper.getInstance().updateMoneyAndExperience(0, DatabaseHelper.getInstance().getOwner().getExperience() + xp, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

            }
        });
        stop();
    }

    public void lost() {
        StApp.makeToast("Quest complete, you have lost!");
        data = "Quest complete, you have lost!";
        Log.d("Solo", "Quest complete, you have lost!");
        stop();
    }

    private void stop() {
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
