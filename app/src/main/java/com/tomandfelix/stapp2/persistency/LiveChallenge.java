package com.tomandfelix.stapp2.persistency;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.common.primitives.Ints;
import com.tomandfelix.stapp2.activity.OpenChallenge;
import com.tomandfelix.stapp2.application.StApp;
import com.tomandfelix.stapp2.gcm.GCMMessageHandler;
import com.tomandfelix.stapp2.persistency.ChallengeStatus.Status;
import com.tomandfelix.stapp2.persistency.GCMMessage.MessageType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Tom on 30/06/2015.
 */
public class LiveChallenge extends Handler {
    private static final String TAG = LiveChallenge.class.getSimpleName();
    private String uniqueId;
    private Challenge challenge;
    private ChallengeStatus myStatus;
    private Map<Integer, ChallengeStatus> opponentStatus;

    public LiveChallenge(int id, int[] opponents) {
        this(UUID.randomUUID().toString(), id, opponents);
    }

    public LiveChallenge(String uniqueId, int id, int[] opponents) {
        super(Looper.getMainLooper());
        this.uniqueId = uniqueId;
        challenge = ChallengeList.getChallenge(id);
        myStatus = new ChallengeStatus(Status.NOT_ACCEPTED, null);
        opponentStatus = new HashMap<>(opponents.length);
        for(int opp : opponents) {
            opponentStatus.put(opp, new ChallengeStatus(Status.NOT_ACCEPTED, null));
        }
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public Challenge getChallenge() {
        return challenge;
    }

    public int[] getOpponents() {
        return Ints.toArray(opponentStatus.keySet());
    }

    public ChallengeStatus getMyStatus() {
        return myStatus;
    }

    public ChallengeStatus getStatusById(int id) {
        return opponentStatus.get(id);
    }

    public Collection<ChallengeStatus> getStatusses() {
        return opponentStatus.values();
    }

    public boolean hasEveryoneAccepted() {
        if(myStatus.getStatus() == Status.NOT_ACCEPTED) {
            return false;
        }
        for(ChallengeStatus s : opponentStatus.values()) {
            if(s.getStatus() == Status.NOT_ACCEPTED) {
                return false;
            }
        }
        return true;
    }

    public boolean isEverybodyDone() {
        if(myStatus.getStatus() != Status.DONE) {
            return false;
        }
        for(ChallengeStatus s : opponentStatus.values()) {
            if(s.getStatus() != Status.DONE) {
                return false;
            }
        }
        return true;
    }

    public void request() {
        post(new Runnable() {
            @Override
            public void run() {
                myStatus.setStatus(Status.ACCEPTED);
                sendMessage(MessageType.REQUEST, Integer.toString(challenge.getId()));
            }
        });
    }

    public void accept() {
        post(new Runnable() {
            @Override
            public void run() {
                myStatus.setStatus(Status.ACCEPTED);
                sendMessage(MessageType.ACCEPT, null);
                if (OpenChallenge.getHandler() != null) {
                    OpenChallenge.getHandler().obtainMessage(OpenChallenge.MSG_REFRESH).sendToTarget();
                }
            }
        });
    }

    public void decline() {
        post(new Runnable() {
            @Override
            public void run() {
                sendMessage(MessageType.DECLINE, null);
                StApp.challenges.remove(uniqueId);
                removeCallbacksAndMessages(null);
            }
        });
    }

    public void start() {
        post(new Runnable() {
            @Override
            public void run() {
                challenge.getProcessor().start(LiveChallenge.this);
            }
        });
    }

    public void postGCMmessage(final GCMMessage msg) {
        post(new Runnable() {
            @Override
            public void run() {
                switch (msg.getMessageType()) {
                    case DECLINE:
                        StApp.challenges.remove(uniqueId);
                        removeCallbacksAndMessages(null);
                        break;
                    case ACCEPT:
                        getStatusById(msg.getSenderId()).setStatus(Status.ACCEPTED);
                        break;
                    case COMMUNICATION:
                        challenge.getProcessor().handleCommunicationMessage(LiveChallenge.this, msg);
                        break;
                    case RESULT:
                        getStatusById(msg.getSenderId()).setStatus(Status.DONE);
                        getStatusById(msg.getSenderId()).setData(msg.getMessage());
                        if (isEverybodyDone()) {
                            challenge.getProcessor().onEverybodyDone(LiveChallenge.this);
                        }
                        break;
                }
                if (OpenChallenge.getHandler() != null) {
                    OpenChallenge.getHandler().obtainMessage(OpenChallenge.MSG_REFRESH).sendToTarget();
                }
            }
        });
    }

    public void sendMessage(MessageType messageType, String message) {
        final GCMMessage msg = new GCMMessage(getOpponents(), uniqueId, messageType, -1, message);
        ServerHelper.getInstance().sendMessage(msg, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                if(volleyError.getMessage().equals("none")) {
                    Log.d(TAG, "Sent: " + msg.toString());
                } else {
                    Log.e(TAG, volleyError.getMessage());
                }
            }
        });
    }
}
