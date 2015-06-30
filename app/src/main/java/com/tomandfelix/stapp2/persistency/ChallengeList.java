package com.tomandfelix.stapp2.persistency;

import android.util.Log;

import com.tomandfelix.stapp2.activity.OpenChallenge;
import com.tomandfelix.stapp2.application.StApp;
import com.tomandfelix.stapp2.persistency.ChallengeStatus.Status;
import com.tomandfelix.stapp2.tools.Algorithms;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Tom on 23/03/2015.
 */
public class ChallengeList {
    public static final Map<Integer, Challenge> challenges = createChallengesList();

    public static Challenge getChallenge(int id) {
        return challenges.get(id);
    }

    private static Map<Integer, Challenge> createChallengesList() {
        Map<Integer, Challenge> map = new HashMap<>();
        Challenge.Processor standMostIn30secs = new Challenge.Processor() {
            @Override
            public void start(final LiveChallenge challenge) {
                challenge.getMyStatus().setStatus(Status.STARTED);
                challenge.getMyStatus().setData(DatabaseHelper.dateToString(new Date()));
                if(OpenChallenge.getHandler() != null) {
                    OpenChallenge.getHandler().obtainMessage(OpenChallenge.MSG_REFRESH).sendToTarget();
                }
                challenge.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("start", "running");
                        calculateResult(challenge);
                    }
                }, challenge.getChallenge().getDuration() * 1000);
            }

            private void calculateResult(LiveChallenge challenge) {
                if(challenge.getMyStatus().getStatus() == Status.STARTED) {
                    long now = System.currentTimeMillis();
                    Date start = new Date(now - 30 * 1000);
                    Date end = new Date(now);
                    long result = Algorithms.millisecondsStood(start, end);
                    challenge.getMyStatus().setStatus(Status.DONE);
                    challenge.getMyStatus().setData(Long.toString(result));
                    challenge.sendMessage(GCMMessage.MessageType.RESULT, Long.toString(result));
                    if (OpenChallenge.getHandler() != null) {
                        OpenChallenge.getHandler().obtainMessage(OpenChallenge.MSG_REFRESH).sendToTarget();
                    }
                }
            }

            @Override
            public void onEverybodyDone(LiveChallenge challenge) {
                long mine = Long.parseLong(challenge.getMyStatus().getData());
                long maxOthers = 0;
                for(ChallengeStatus cs : challenge.getStatusses()) {
                    maxOthers = Math.max(maxOthers, Long.parseLong(cs.getData()));
                }
                challenge.getMyStatus().setStatus(Status.SCORED);
                if(mine > maxOthers) {
                    StApp.makeToast("You won, big time!");
                    challenge.getMyStatus().setData(challenge.getMyStatus().getData() + "|You won, big time!");
                } else if(mine == maxOthers) {
                    StApp.makeToast("It's a Tie, how did you pull this off?");
                    challenge.getMyStatus().setData(challenge.getMyStatus().getData() + "|It's a Tie, how did you pull this off?");
                } else {
                    StApp.makeToast("You had one thing to do, ONE! (you lost)");
                    challenge.getMyStatus().setData(challenge.getMyStatus().getData() + "|You had one thing to do, ONE! (you lost)");
                }
                if(OpenChallenge.getHandler() != null) {
                    OpenChallenge.getHandler().obtainMessage(OpenChallenge.MSG_REFRESH).sendToTarget();
                }
            }
        };
        map.put(0, new Challenge(0, "Quick test", "Stand longer than your opponent", 2, 2, 30, standMostIn30secs));
        map.put(1, new Challenge(1, "Group challenge", "Stand longer than your opponent", 3, 5, 30, standMostIn30secs));
        return map;
    }
}
