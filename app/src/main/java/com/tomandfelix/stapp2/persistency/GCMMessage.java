package com.tomandfelix.stapp2.persistency;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Tom on 19/03/2015.
 */

public class GCMMessage {
    public static final int REQUEST = 0;
    public static final int ACCEPTED = 1;
    public static final int COMMUNICATION = 2;
    public static final int RESULT = 3;

    private int[] recievers;
    private int challengeId;
    private int messageType;
    private int senderId;
    private String message;

    public GCMMessage(int[] recievers, int challengeId, int messageType, int senderId, String message) {
        this.recievers = recievers;
        this.challengeId = challengeId;
        this.messageType = messageType;
        this.senderId = senderId;
        this.message = message;
    }

    public int[] getRecievers() {
        return recievers;
    }

    public int getChallengeId() {
        return challengeId;
    }

    public int getMessageType() {
        return messageType;
    }

    public int getSenderId() {
        return senderId;
    }

    public String getMessage() {
        return message;
    }
}
