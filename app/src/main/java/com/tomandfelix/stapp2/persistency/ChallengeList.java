package com.tomandfelix.stapp2.persistency;

import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.tomandfelix.stapp2.application.StApp;
import com.tomandfelix.stapp2.gcm.GCMMessageHandler;
import com.tomandfelix.stapp2.tools.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by Tom on 23/03/2015.
 */
public class ChallengeList {
    public static final List<Challenge> challenges = Collections.synchronizedList(new ArrayList<Challenge>());
    public static void init() {
        if(challenges.size() == 0) {
            createChallengesList();
        }
    }

    private static void createChallengesList() {
        challenges.add(new Challenge(1, "testChallenge1", "you have 30 seconds time to stand more than your opponent", 2, 2,30,new Challenge.Validator(){
            @Override
            public void run() {
                if (challenge.getState() == Challenge.STARTED) {
                    long now = System.currentTimeMillis();
                    Date start = new Date(now - 30 * 1000);
                    Date end = new Date(now);
                    long result = Algorithms.millisecondsStood(start, end);
                    Log.i("TestChallenge", Long.toString(result));
                    challenge.getResults().add(challenge.sendMessage(GCMMessage.RESULT, Long.toString(result)));
                    challenge.setState(Challenge.DONE);
                    calcResult();
                } else if(challenge.getState() == Challenge.DONE) {
                    calcResult();
                }
            }

            private void calcResult() {
                if (challenge.getResults().size() == challenge.getOpponents().length + 1) {
                    long otherMilliseconds = 0;
                    long myMilliseconds = 0;
                    for(GCMMessage m : challenge.getResults()) {
                        if(m.getSenderId() == -1) {
                            myMilliseconds = Long.parseLong(m.getMessage());
                        } else {
                            otherMilliseconds = Long.parseLong(m.getMessage());
                        }
                    }
                    if (myMilliseconds > otherMilliseconds) {
                        StApp.makeToast("You won, big time!");
                        Log.d("StApp", "You won, big time!");
                    } else if (myMilliseconds == otherMilliseconds) {
                        StApp.makeToast("It's a Tie, how did you pull this off?");
                        Log.d("StApp", "It's a Tie, how did you pull this off?");
                    } else {
                        StApp.makeToast("You had one thing to do, ONE! (you lost)");
                        Log.d("StApp", "You had one thing to do, ONE! (you lost)");
                    }
                    GCMMessageHandler.challenges.remove(challenge);
                }
            }
        }));
    }
}
