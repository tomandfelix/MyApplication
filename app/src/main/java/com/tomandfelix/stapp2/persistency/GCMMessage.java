package com.tomandfelix.stapp2.persistency;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Tom on 19/03/2015.
 */

public class GCMMessage {
    public static final int TEST_TOAST = -1;
    public static final int REQUEST = 0;
    public static final int ACCEPTED = 1;
    public static final int DECLINED = 2;
    public static final int COMMUNICATION = 3;
    public static final int RESULT = 4;

    private int[] receivers;
    private int challengeId;
    private int messageType;
    private int senderId;
    private String message;

    public GCMMessage(int[] receivers, int challengeId, int messageType, int senderId, String message) {
        this.receivers = receivers;
        this.challengeId = challengeId;
        this.messageType = messageType;
        this.senderId = senderId;
        this.message = message;
    }

    public int[] getReceivers() {
        return receivers;
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

    public String toString() {
        String result = "challengeId: " + challengeId;
        result += " messageType: " + messageType;
        result += " message: '" + message + "'";
        result += " sender: " + senderId;
        result += " receiver_ids: ";
        for(int i = 0; i < receivers.length - 1; i++) {
            result += receivers[i] + ", ";
        }
        result += receivers[receivers.length - 1];
        return result;
    }
}
