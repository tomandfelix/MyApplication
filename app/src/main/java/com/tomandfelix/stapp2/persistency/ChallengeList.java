package com.tomandfelix.stapp2.persistency;

import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.tomandfelix.stapp2.application.StApp;
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
            public void run(){
                long now = System.currentTimeMillis();
                Date start = new Date(now - 30 * 1000);
                Date end = new Date(now);
                long result = Algorithms.millisecondsStood(start, end);
                Log.i("TestChallenge", Long.toString(result));
                GCMMessage mes = new GCMMessage(challenge.getOpponents(), challenge.getId(), GCMMessage.RESULT, 0, Long.toString(result));
                if(challenge.getResults().size() > 0) {
                    long otherMilliseconds = Integer.parseInt(challenge.getResults().get(0).getMessage());
                    if(result > otherMilliseconds) {
                        StApp.makeToast("You won, big time!");
                        Log.d("StApp", "You won, big time!");
                    } else if (result == otherMilliseconds) {
                        StApp.makeToast("It's a Tie, how did you pull this off?");
                        Log.d("StApp", "It's a Tie, how did you pull this off?");
                    } else {
                        StApp.makeToast("You had one thing to do, ONE! (you lost)");
                        Log.d("StApp", "You had one thing to do, ONE! (you lost)");
                    }
                } else {
                    challenge.getResults().add(mes);
                }
                ServerHelper.getInstance().sendMessage(mes,
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError volleyError) {
                                Log.e("StApp", volleyError.getMessage());
                            }
                        });
            }
        }));
    }
}
