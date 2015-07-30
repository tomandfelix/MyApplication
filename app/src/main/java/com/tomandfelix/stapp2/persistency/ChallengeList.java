package com.tomandfelix.stapp2.persistency;

import android.util.Log;

import com.tomandfelix.stapp2.activity.OpenChallenge;
import com.tomandfelix.stapp2.application.StApp;
import com.tomandfelix.stapp2.persistency.ChallengeStatus.Status;
import com.tomandfelix.stapp2.tools.Algorithms;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Created by Tom on 23/03/2015.
 */
public class ChallengeList {
    private static final Profile mProfile = DatabaseHelper.getInstance().getOwner();
    public static final List<Challenge> challenges = createChallengesList();

    public static Challenge getChallenge(int id) {
        return challenges.get(id);
    }

    public static List<Challenge> getList() {
        return challenges;
    }

    private static List<Challenge> createChallengesList() {
        ArrayList<Challenge> list = new ArrayList<>();
        Challenge.Processor standMostIn30Min = new Challenge.Processor() {
            @Override
            public void start(final LiveChallenge challenge) {
                challenge.setMyStatus(Status.STARTED, DatabaseHelper.dateToString(new Date()));
                if(OpenChallenge.getHandler() != null) {
                    OpenChallenge.getHandler().obtainMessage(OpenChallenge.MSG_REFRESH).sendToTarget();
                }
                challenge.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("start", "running");
                        calculateResult(challenge);
                    }
                }, challenge.getChallenge().getDuration() * 60000);
            }

            private void calculateResult(LiveChallenge challenge) {
                if(challenge.getMyStatus() == Status.STARTED) {
                    long now = System.currentTimeMillis();
                    Date start = new Date(now - challenge.getChallenge().getDuration() * 60000);
                    Date end = new Date(now);
                    long result = Algorithms.millisecondsStood(start, end);
                    challenge.setMyStatus(Status.DONE, Long.toString(result));
                    challenge.sendMessage(GCMMessage.MessageType.RESULT, Long.toString(result));
                    if(challenge.isEverybodyDone()) {
                        challenge.getChallenge().getProcessor().onEverybodyDone(challenge);
                    }
                    if (OpenChallenge.getHandler() != null) {
                        OpenChallenge.getHandler().obtainMessage(OpenChallenge.MSG_REFRESH).sendToTarget();
                    }
                }
            }

            @Override
            public void onEverybodyDone(LiveChallenge challenge) {
                long mine = Long.parseLong(challenge.getMyStatusData());
                long maxOthers = 0;
                for(ChallengeStatus cs : challenge.getOpponentStatus().values()) {
                    maxOthers = Math.max(maxOthers, Long.parseLong(cs.getData()));
                }
                if(mine > maxOthers) {
                    StApp.makeToast("You won, big time!");
                    challenge.setMyStatus(Status.SCORED, challenge.getMyStatusData() + "|You won, big time!");
                } else if(mine == maxOthers) {
                    StApp.makeToast("It's a Tie, how did you pull this off?");
                    challenge.setMyStatus(Status.SCORED, challenge.getMyStatusData() + "|It's a Tie, how did you pull this off?");
                } else {
                    StApp.makeToast("You had one thing to do, ONE! (you lost)");
                    challenge.setMyStatus(Status.SCORED, challenge.getMyStatusData() + "|You had one thing to do, ONE! (you lost)");
                }
                if(OpenChallenge.getHandler() != null) {
                    OpenChallenge.getHandler().obtainMessage(OpenChallenge.MSG_REFRESH).sendToTarget();
                }
            }
        };

        Challenge.Processor followTheLeader = new Challenge.Processor() {
            @Override
            void start(final LiveChallenge challenge) {
                challenge.setMyStatus(Status.STARTED, DatabaseHelper.dateToString(new Date()));
                if(OpenChallenge.getHandler() != null) {
                    OpenChallenge.getHandler().obtainMessage(OpenChallenge.MSG_REFRESH).sendToTarget();
                }
                Log.d("start", "running");
                Random random = new Random();
                int count = 3000;
                int previousTime = 0;
                int randomInt;
                boolean sitStand = false; /*sitting*/
                StApp.makeToast("You get 30 seconds to sit for the challenge! The next instructions come later.");
                while (true) {
                    randomInt = random.nextInt(300);


                    final int runPreviousTime = previousTime;
                    final boolean runSitStand = sitStand;
                    challenge.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            checkProgress(challenge, runPreviousTime, runSitStand);
                            vibrate();
                        }
                    }, count );
                    if(challenge.getChallenge().getDuration() * 60000 <= count + randomInt * 1000){
                        break;
                    }
                    count += randomInt + 30000;
                    previousTime = randomInt;
                    if(!sitStand){
                        sitStand = true;
                    }else{
                        sitStand = false;
                    }
                }
                if(challenge.getChallenge().getDuration() - count <= 30000){
                    StApp.makeToast("challenge complete, you did your job!");
                    challenge.setMyStatus(Status.SCORED, challenge.getMyStatusData() + "Challenge complete, you did your job!");
                }else{
                    checkProgress(challenge,(challenge.getChallenge().getDuration() - count), sitStand);
                }
                onEverybodyDone(challenge);
            }
            public void vibrate(){
                StApp.vibrate(2000);
            }

            public void checkProgress(LiveChallenge challenge,int time ,boolean sitStand){
                Date end = new Date();
                Date start = new Date(end.getTime() - time * 1000);
                long result = Algorithms.millisecondsStood(start, end);
                if(sitStand) {
                    if (result <= 0.98 * time * 1000){
                        challenge.setMyStatus(Status.LOST, "You lost the challenge. Your teammates wont be happy!");
                        for(int i = 0; i < challenge.getOpponents().length; i ++ ) {
                            challenge.setStatusById(challenge.getOpponents()[i] , Status.LOST, mProfile.getUsername() + " lost you the challenge! Better luck next time");
                        }


                    }
                }else {
                    if(result > 0.02 * time * 1000){
                        challenge.setMyStatus(Status.LOST, "You lost the challenge. Your teammates wont be happy!");
                        for(int i = 0; i < challenge.getOpponents().length; i ++ ) {
                            challenge.setStatusById(challenge.getOpponents()[i] , Status.LOST, mProfile.getUsername() + " lost you the challenge! Better luck next time");
                        }
                    }
                }
                OpenChallenge.getHandler().obtainMessage(OpenChallenge.MSG_REFRESH).sendToTarget();
            }
            @Override
            void onEverybodyDone(LiveChallenge challenge) {

            }
        };

        Challenge.Processor standInGroup = new Challenge.Processor(){
            @Override
            void start(LiveChallenge challenge) {

            }

            @Override
            void onEverybodyDone(LiveChallenge challenge) {

            }
        };
        list.add(0, new Challenge(0, "Quick test", "Stand longer than your opponent", 2, 2, 500,  30, standMostIn30Min));
        list.add(1, new Challenge(1, "Group challenge", "Stand longer than your opponent", 3, 5, 750, 30, standMostIn30Min));
        list.add(2, new Challenge(2, "Follow the leader", "Is the leader standing? Than you need to stand. Is the leader sitting? Than you need to sit. Simple as that. How do you know when to change position? When your phone is vibrating! If one isn't able to follow the leader, the game stops!", 2, 5, 500, 30, followTheLeader));
        list.add(3, new Challenge(3, "Stand in group", "You try to get in group as much of seconds stood as possible.", 2, 5, 500, 60, standInGroup));
        return list;
    }
}
