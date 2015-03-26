package com.tomandfelix.stapp2.persistency;

import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.tomandfelix.stapp2.gcm.GCMMessageHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by Flixse on 27/01/2015.
 */
public class Challenge extends Quest {
    private int accepted;
    private int minAmount;
    private int maxAmount;
    private int duration;
    private Validator validator;
    private int state;
    private int[] opponents;
    private List<GCMMessage> results;
    private Date startTime;
    private String stateMessage;
    public static final int REQ_SENT = 0;
    public static final int REQ_REC = 1;
    public static final int ACCEPTED = 2;
    public static final int STARTED = 3;
    public static final int WAITING = 4;
    public static final int DONE = 5;

    public Challenge(int id, String name, String description, int minAmount, int maxAmount, int duration, Validator validator){
        super(id, name, description);
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.duration = duration;
        this.validator = validator;
        validator.setChallenge(this);
    }

    public Challenge(int id, int[] opponents) {
        super(id);
        synchronized (ChallengeList.challenges) {
            for (Challenge c : ChallengeList.challenges) {
                if (c.getId() == id) {
                    this.name = c.getName();
                    this.description = c.getDescription();
                    this.minAmount = c.getMinAmount();
                    this.maxAmount = c.getMaxAmount();
                    this.duration = c.getDuration();
                    this.validator = c.getValidator();
                    validator.setChallenge(this);
                }
            }
        }
        this.accepted = 0;
        this.state = -1;
        this.opponents = opponents;
        this.results = Collections.synchronizedList(new ArrayList<GCMMessage>());
    }

    public int getAccepted() {
        return accepted;
    }

    public void incrementAccepted() {
        accepted++;
    }

    public synchronized int getMinAmount() {
        return minAmount;
    }

    public synchronized int getMaxAmount() {
        return maxAmount;
    }

    public synchronized int getDuration() {
        return duration;
    }

    public synchronized Validator getValidator() {
        return validator;
    }

    public synchronized int getState() {
        return state;
    }

    public synchronized void setState(int newState) {
        this.state = newState;
        Log.e("Challenge", "STATE=" + newState);
    }

    public synchronized int[] getOpponents() {
        return opponents;
    }

    public synchronized void addResult(GCMMessage newResult) {
        results.add(newResult);
    }

    public synchronized List<GCMMessage> getResults() {
        return results;
    }

    public synchronized Date getStartTime() {return startTime;}

    public synchronized String getStateMessage() {
        return stateMessage;
    }

    public synchronized void setStateMessage(String newStateMessage) {
        this.stateMessage = newStateMessage;
    }

    public synchronized void startChallenge() {
        this.startTime = new Date();
        GCMMessageHandler.handler.postDelayed(getValidator(), getDuration() * 1000);
        setState(STARTED);
    }

    public GCMMessage sendMessage(int messageType, String message) {
        final GCMMessage msg = new GCMMessage(opponents, id, messageType, -1, message);
        ServerHelper.getInstance().sendMessage(msg, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                if(volleyError.getMessage().equals("none")) {
                    Log.d("Challenge", "Sent: " + msg.toString());
                } else {
                    Log.e("Challenge", volleyError.getMessage());
                }
            }
        });
        switch(messageType) {
            case GCMMessage.REQUEST:
                setState(Challenge.REQ_SENT);
                break;
            case GCMMessage.ACCEPTED:
                incrementAccepted();
                if(getAccepted() == getOpponents().length) {
                    setState(ACCEPTED);
                } else {
                    setState(REQ_SENT);
                }
                break;
            case GCMMessage.DECLINED:
                GCMMessageHandler.challenges.remove(this);
        }
        return msg;
    }

    @Override
    public synchronized String toString() {
        String info = "";
        info += "id:" + id;
        info += (name == null ? "":" name:" + name);
        info += (description == null ? "":" description:" + description);
        info += " people:" + minAmount + "-" + maxAmount;
        return info;
    }

    public static abstract class Validator implements Runnable {
        public Challenge challenge;

        public void setChallenge(Challenge challenge) {
            this.challenge = challenge;
        }
    }
}
