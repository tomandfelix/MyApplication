package com.tomandfelix.stapp2.persistency;

import android.os.Message;
import android.util.Log;

import com.tomandfelix.stapp2.activity.OpenChallenge;
import com.tomandfelix.stapp2.application.StApp;
import com.tomandfelix.stapp2.persistency.ChallengeStatus.Status;
import com.tomandfelix.stapp2.tools.Algorithms;
import com.tomandfelix.stapp2.tools.Logging;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Created by Tom on 23/03/2015.
 */
public class ChallengeList {
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
                challenge.setStatusMessage(null);
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
                    challenge.setStatusMessage(LiveChallenge.WAIT_FOR_RESULT_MSG);
                    challenge.sendMessage(GCMMessage.MessageType.RESULT, Long.toString(result));
                    if(challenge.isEverybodyDone()) {
                        challenge.getChallenge().getProcessor().onEverybodyDone(challenge);
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
                    challenge.won("You won, big time!");
                } else if(mine == maxOthers) {
                    challenge.lost("It's a Tie, better luck next time!");
                } else {
                    challenge.lost("You lost, keep trying!");
                }
            }
        };
        Challenge.Processor followTheLeader = new Challenge.Processor() {
            private final String startStanding = "Start standing in the next 30 seconds";
            private final String startSitting = "Start sitting in the next 30 seconds";
            private final String keepStanding = "Keep standing";
            private final String keepSitting = "Keep sitting";

            @Override
            void start(final LiveChallenge challenge) {
                Log.d("FollowTheLeader", "start running");
                if(challenge.getMyStatusData() == null) {
                    Random random = new Random();
                    String sequence = "";
                    int sum = 30;
                    // The sequence will be approximately 20 minutes long, but it doesn't matter how long it will be, as it will be repeated if necessary
                    while(sum < 1200) {
                        int randomInt = random.nextInt(120);
                        if(sequence.equals(""))
                            sequence += randomInt;
                        else
                            sequence += "|" + randomInt;
                        sum += randomInt + 30;
                    }
                    challenge.setMyStatus(Status.STARTED, DatabaseHelper.dateToString(new Date()) + "|" + sequence);
                    challenge.sendMessage(GCMMessage.MessageType.COMMUNICATION, sequence);
                } else {
                    challenge.setMyStatus(Status.STARTED, DatabaseHelper.dateToString(new Date()) + "|" + challenge.getMyStatusData());
                }
                challenge.setStatusMessage(startStanding);
                challenge.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        firstCheck(challenge);
                    }
                }, 30000);
            }

            private int[] getSequence(LiveChallenge challenge) {
                if(challenge.getMyStatusData() == null)
                    return null;
                else {
                    String[] parts = challenge.getMyStatusData().split("\\|");
                    if (parts.length == 1) {
                        return null;
                    } else {
                        int[] result = new int[parts.length - 1];
                        for (int i = 1; i < parts.length; i++) {
                            result[i - 1] = Integer.parseInt(parts[i]);
                        }
                        return result;
                    }
                }
            }

            private void alert(final LiveChallenge challenge, final int index) {
                StApp.vibrate(2000);
                final int[] sequence = getSequence(challenge);
                if(sequence != null) {
                    Date now = new Date();
                    ArrayList<DBLog> logs = DatabaseHelper.getInstance().getSitStandBetween(new Date(now.getTime() - sequence[index % sequence.length] * 1000), now);
                    if(logs != null) {
                        Date start = DatabaseHelper.stringToDate(challenge.getMyStatusData().split("\\|")[0]);
                        Date end = logs.get(0).getDatetime();
                        if(start != null) {
                            long result = end.getTime() - start.getTime();
                            challenge.setMyStatus(Status.DONE, Long.toString(result));
                            challenge.setStatusMessage(LiveChallenge.WAIT_FOR_RESULT_MSG);
                            challenge.sendMessage(GCMMessage.MessageType.RESULT, Long.toString(result));
                            if(challenge.isEverybodyDone()) {
                                challenge.getChallenge().getProcessor().onEverybodyDone(challenge);
                            }
                        }
                    } else {
                        challenge.setStatusMessage((index % 2 == 0) ? startSitting : startStanding);
                        challenge.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                checkProgress(challenge, index +1);
                            }
                        }, 30000);
                    }
                }
            }

            private void firstCheck(final LiveChallenge challenge) {
                if(DatabaseHelper.getInstance().getLastSitStand().getAction().equals(DatabaseHelper.LOG_STAND)) {
                    challenge.setStatusMessage(keepStanding);
                    int[] sequence  = getSequence(challenge);
                    if(sequence != null) {
                        challenge.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                alert(challenge, 0);
                            }
                        }, sequence[0] * 1000);
                    }
                } else {
                    StApp.vibrate(2000);
                    challenge.sendMessage(GCMMessage.MessageType.RESULT, "0");
                    challenge.lost("You lost, you were too late!");
                }
            }

            public void checkProgress(final LiveChallenge challenge, final int index) {
                final int[] sequence = getSequence(challenge);
                if(sequence != null) {
                    Date now = new Date();
                    ArrayList<DBLog> logs = DatabaseHelper.getInstance().getSitStandBetween(new Date(now.getTime() - 30000), now);
                    if(logs != null && logs.size() == 1 && logs.get(0).getAction().equals((index % 2 == 0) ? DatabaseHelper.LOG_STAND : DatabaseHelper.LOG_SIT)) {
                        challenge.setStatusMessage((index % 2 == 0) ? keepStanding : keepSitting);
                        challenge.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                alert(challenge, index);
                            }
                        }, sequence[index % sequence.length] * 1000);
                    } else {
                        StApp.vibrate(2000);
                        Date start = DatabaseHelper.stringToDate(challenge.getMyStatusData().split("\\|")[0]);
                        if(start != null) {
                            long result = now.getTime() - start.getTime() - 30000;
                            challenge.setMyStatus(Status.DONE, Long.toString(result));
                            challenge.setStatusMessage(LiveChallenge.WAIT_FOR_RESULT_MSG);
                            challenge.sendMessage(GCMMessage.MessageType.RESULT, Long.toString(result));
                            if(challenge.isEverybodyDone()) {
                                challenge.getChallenge().getProcessor().onEverybodyDone(challenge);
                            }
                        }
                    }
                }
            }
            @Override
            void onEverybodyDone(LiveChallenge challenge) {
                long mine = Long.parseLong(challenge.getMyStatusData());
                long maxOthers = 0;
                for(ChallengeStatus cs : challenge.getOpponentStatus().values()) {
                    maxOthers = Math.max(maxOthers, Long.parseLong(cs.getData()));
                }
                if(mine > maxOthers) {
                    challenge.won("You won, big time!");
                } else if(mine == maxOthers) {
                    challenge.lost("It's a Tie, better luck next time!");
                } else {
                    challenge.lost("You lost, keep trying!");
                }
            }

            @Override
            void handleCommunicationMessage(LiveChallenge challenge, GCMMessage msg) {
                if(challenge.getMyStatus() == Status.ACCEPTED && challenge.getMyStatusData() == null) {
                    challenge.setMyStatus(null, msg.getMessage());
                }
            }
        };
        Challenge.Processor alternateStanding = new Challenge.Processor() {
            private Runnable end;

            @Override
            void start(final LiveChallenge challenge) {
                Log.d("AlternateStanding", "start running");
                challenge.setMyStatus(Status.STARTED, DatabaseHelper.dateToString(new Date()));
                StApp.subscribeChallenge(challenge.getUniqueId());
                check(challenge, isStanding(), true);
                end = new Runnable() {
                    @Override
                    public void run() {
                        StApp.unsubscribeChallenge(challenge.getUniqueId());
                        challenge.wonCustomXP("Congratulations, you have endured this assignment for 60 minutes. you won!", calculateScore(challenge));
                        challenge.setMyStatus(Status.SCORED, null);
                    }
                };
                challenge.postDelayed(end, 3600000);
            }

            private boolean isStanding() {
                DBLog last = DatabaseHelper.getInstance().getLastSitStand();
                return last != null && last.getAction().equals(DatabaseHelper.LOG_STAND);
            }

            private int getAmountOthersStanding(LiveChallenge challenge) {
                int result = 0;
                for(ChallengeStatus cs : challenge.getOpponentStatus().values()) {
                    String data = cs.getData();
                    if(data != null && Boolean.parseBoolean(data)) {
                        result++;
                    }
                }
                return result;
            }

            private void check(LiveChallenge challenge, boolean standing, boolean sendMessage) {
                if(sendMessage) {
                    challenge.sendMessage(GCMMessage.MessageType.COMMUNICATION, Boolean.toString(standing));
                }
                int othersStanding = getAmountOthersStanding(challenge);
                if(!standing && othersStanding == 0) {
                    StApp.unsubscribeChallenge(challenge.getUniqueId());
                    challenge.removeCallbacks(end);
                    int score = calculateScore(challenge);
                    challenge.wonCustomXP("Challenge finished because nobody was standing." + (score == 0 ? " Keep in mind that the person that starts first, has to be standing!" : ""), score);
                } else {
                    challenge.setStatusMessage(othersStanding + " out of " + challenge.getOpponentStatus().size() + " of the other players are standing.");
                }
            }

            private int calculateScore(LiveChallenge challenge) {
                Log.d("AlternateStanding", "calculateScore running");
                Date start = DatabaseHelper.stringToDate(challenge.getMyStatusData());
                Log.d("AlternateStanding", "start = " + DatabaseHelper.dateToString(start));
                if(start == null)
                    return 0;
                else {
                    long time = Math.min(new Date().getTime() - start.getTime(), 3600000);
                    Log.d("AlternateStanding", "time = " + time);
                    if(time < 2000) {
                        return 0;
                    } else {
                        double result = (105 * Math.exp(time / 1200000d) - 105) / (challenge.getOpponentStatus().size() + 1);
                        Log.d("AlternateStanding", "result = " + result);
                        return (int) result;
                    }
                }
            }

            @Override
            void onEverybodyDone(LiveChallenge challenge) {}

            @Override
            void handleCommunicationMessage(LiveChallenge challenge, GCMMessage msg) {
                if(challenge.getMyStatus().equals(Status.ACCEPTED)) {
                    challenge.setStatusById(msg.getSenderId(), Status.STARTED, msg.getMessage());
                    if(getAmountOthersStanding(challenge) == 0)
                        challenge.lost("The challenge is already over, you joined too late!");
                } else if(!challenge.getMyStatus().equals(Status.SCORED)) {
                    challenge.setStatusById(msg.getSenderId(), Status.STARTED, msg.getMessage());
                    check(challenge, isStanding(), false);
                }
            }

            @Override
            void handleLoggingMessage(LiveChallenge challenge, Message message) {
                switch(message.what) {
                    case Logging.STATE_CONNECTING:
                    case Logging.STATE_DISCONNECTED:
                    case Logging.STATE_DAY_STOPPED:
                    case Logging.STATE_CONNECTED:
                    case Logging.STATE_SIT:
                    case Logging.STATE_OVERTIME:
                        check(challenge, false, true);
                        break;
                    case Logging.STATE_STAND:
                        check(challenge, true, true);
                        break;
                }
            }
        };

        list.add(0, new Challenge(0, "1-on-1", "Stand longer than your opponent in a period of <duration> minutes", 2, 2, 500,  30, true, standMostIn30Min));
        list.add(1, new Challenge(1, "Group competition", "Stand longer than all your opponents in a period of <duration> minutes", 3, 5, 750, 30, true, standMostIn30Min));
        list.add(2, new Challenge(2, "Follow the leader", "Everybody will get the same leader. If the leader stands up, you have to stand up. If the leader sits, you have to sit. The person that is able to endure the longest wins. Your phone will vibrate if you have to change position.", 2, 5, 500, 0, false, followTheLeader));
        list.add(3, new Challenge(3, "Alternate standing", "At every moment, one of the participating players should always be standing. The longer this assignment is fulfilled (max 60 minutes), the more experience is gained. Your score will depend on how long you participated and the amount of people that participated. The person that starts first should start standing up", 2, 5, 1000, 60, false, alternateStanding));
        return list;
    }
}
