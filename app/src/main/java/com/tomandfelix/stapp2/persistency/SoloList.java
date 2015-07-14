package com.tomandfelix.stapp2.persistency;

import android.util.Log;

import com.tomandfelix.stapp2.application.StApp;
import com.tomandfelix.stapp2.tools.Algorithms;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by Flixse on 14/07/2015.
 */
public class SoloList {
    public static final List<Solo> solos = createSoloList();

    public static Solo getSolo(int id) {
        return solos.get(id);
    }

    public static List<Solo> getList() {
        return solos;
    }

    private static List<Solo> createSoloList() {
        List<Solo> map = new ArrayList<>();
        Solo.Processor standAmountIn30min = new Solo.Processor() {
            @Override
            public void start(Solo solo) {
                solo.setData(DatabaseHelper.dateToString(new Date()));
                schedule(solo);
            }

            public void checkProgress(Solo solo) {
                Date start = DatabaseHelper.stringToDate(solo.getData());
                if(start != null && new Date().getTime() - start.getTime() >= 1800000) {
                    StApp.makeToast("Quest complete, you have lost!");
                    solo.stop();
                } else {
                    double result = (double) Algorithms.millisecondsStood(start, new Date());
                    solo.setProgress(Math.min( (result * 100d / ((double) getNeeded(solo))), 100d));
                    Log.d("solo", "Progress=" + solo.getProgress());
                    if (solo.getProgress() == 100) {
                        StApp.makeToast("Quest complete, you have won!");
                        solo.stop();
                    } else {
                        schedule(solo);
                    }
                }
            }

            private long getNeeded(Solo solo) {
                switch(solo.getDifficulty()) {
                    case EASY:
                        return 600000;
                    case MEDIUM:
                        return 900000;
                    case HARD:
                        return 1200000;
                    default :
                        return 0;
                }
            }

            private void schedule(final Solo solo) {
                solo.getHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkProgress(solo);
                    }
                }, getNeeded(solo) / 100);
            }
        };
        Solo.Processor standRandom = new Solo.Processor() {
            @Override
            public void start(final Solo solo) {
                Random random = new Random();
                for(int i = 0; i < solo.getDuration() / 10; i++) {
                    int randomInt = random.nextInt(510) * 1000;
                    Log.d("SoloList", "random " + i + ": " + randomInt);
                    solo.getHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            vibrate();
                        }
                    }, i * 600000 + randomInt);
                    solo.getHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            checkProgress(solo);
                        }
                    }, i * 600000 + 90000 + randomInt);
                }
            }

            private void vibrate() {
                StApp.vibrate(2000);
                Log.d("Solo", "Vibrating");
            }

            private void checkProgress(Solo solo) {
                Date end = new Date();
                Date start = new Date(end.getTime() - 90000);
                long result = Algorithms.millisecondsStood(start, end);
                if(result < 60000) {
                    StApp.makeToast("Quest complete, you have lost!");
                    Log.d("Solo", "Quest complete, you have lost!");
                    solo.stop();
                } else {
                    solo.setProgress(Math.min(solo.getProgress() + 1000d / ((double) solo.getDuration()), 100d));
                    Log.d("solo", "Progress=" + solo.getProgress());
                    if(solo.getProgress() == 100) {
                        StApp.makeToast("Quest complete, you have won!");
                        Log.d("Solo", "Quest complete, you have won!");
                        solo.stop();
                    }
                }
            }
        };
        map.add(0, new Solo(0, "Stand to win", "Stand for more than 10 minutes within 30 minutes", 150, 30, Solo.Difficulty.EASY, standAmountIn30min));
        map.add(1, new Solo(1, "Stand to win", "Stand for more than 15 minutes within 30 minutes", 200, 30, Solo.Difficulty.MEDIUM, standAmountIn30min));
        map.add(2, new Solo(2, "Stand to win", "Stand for more than 20 minutes within 30 minutes", 250, 30, Solo.Difficulty.HARD, standAmountIn30min));
        map.add(3, new Solo(3, "random stand up", "Within a period of 30 minutes you will get the task to stand for 1 minute every random period of time. When do you stand up? When you feel your phone vibrating!", 250, 30, Solo.Difficulty.EASY, standRandom));
        map.add(4, new Solo(4, "random stand up", "Within a period of 60 minutes you will get the task to stand for 1 minute every random period of time. When do you stand up? When you feel your phone vibrating!", 250, 60, Solo.Difficulty.MEDIUM, standRandom));
        map.add(5, new Solo(5, "random stand up", "Within a period of 90 minutes you will get the task to stand for 1 minute every random period of time. When do you stand up? When you feel your phone vibrating!", 250, 90, Solo.Difficulty.HARD, standRandom));
        return map;
    }
}
