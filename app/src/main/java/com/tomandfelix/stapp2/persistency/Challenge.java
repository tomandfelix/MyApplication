package com.tomandfelix.stapp2.persistency;

import com.tomandfelix.stapp2.activity.OpenChallenge;
import com.tomandfelix.stapp2.gcm.GCMMessageHandler;
import com.tomandfelix.stapp2.persistency.ChallengeStatus.Status;

/**
 * Created by Flixse on 27/01/2015.
 */
public class Challenge extends Quest {
    private int minAmount;
    private int maxAmount;
    private int xp;
    private int duration;
    private double progress;
    private Processor processor;

    public Challenge(int id, String name, String description, int minAmount, int maxAmount,int xp, int duration, Processor validator){
        super(id, name, description);
        this.progress = 0;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.xp = xp;
        this.duration = duration;
        this.processor = validator;
    }

    public int getxp() {
        return xp;
    }

    public void setxp(int xp) {
        this.xp = xp;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public int getMinAmount() {
        return minAmount;
    }

    public int getMaxAmount() {
        return maxAmount;
    }

    public int getDuration() {
        return duration;
    }

    public Processor getProcessor() {
        return processor;
    }

    @Override
    public String toString() {
        String info = "";
        info += "id:" + id;
        info += (name == null ? "":" name:" + name);
        info += (description == null ? "":" description:" + description);
        info += " people:" + minAmount + "-" + maxAmount;
        return info;
    }

    public static abstract class Processor {
        abstract void start(LiveChallenge challenge);
        void handleCommunicationMessage(LiveChallenge challenge, GCMMessage msg) {}
        abstract void onEverybodyDone(LiveChallenge challenge);
    }
}
