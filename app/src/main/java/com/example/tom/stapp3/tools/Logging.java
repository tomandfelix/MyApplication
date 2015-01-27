/*Rev 0.2
 * 
 *  Copyright (c) 2010, Shimmer Research, Ltd.
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:

 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of Shimmer Research, Ltd. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Jong Chern Lim
 * 
 * Changes since 0.1
 * - Added method to get outputFile
 * 
 */


package com.example.tom.stapp3.tools;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.example.tom.stapp3.persistency.DBLog;
import com.example.tom.stapp3.persistency.DatabaseHelper;
import com.google.common.collect.Multimap;
import com.example.tom.stapp3.driver.FormatCluster;
import com.example.tom.stapp3.driver.ObjectCluster;


public class Logging {
    private static Logging uniqueInstance;
    private final long _30mn_to_ms = 30 * 60 * 1000;
    private final long _25mn_to_ms = 25 * 60 * 1000;
    private boolean mFirstWrite = true;
    private boolean resultFirstWrite = true;
    private boolean firstWriteSitOvertime = true;
    private boolean warningSound = true;

    private static Context context;
    // private DatabaseManager db;

    private Date startTime = new Date();
    private String[] mSensorNames;
    private String[] mSensorFormats;
    private String[] mSensorUnits;

    private boolean previousResult;

    private final double logDetSigma = 58.2792;
    private double meanY = 0.0;
    private double meanX = 0.0;
    private double meanZ = 0.0;
    private double rmsY = 0.0;
    private double lengthXYZ = 0.0;
    private double meanLength = 0.0;
    private ArrayList<Double> tempList = new ArrayList<Double>();
    private ArrayList<Double> accYList = new ArrayList<Double>();
    private ArrayList<Double> accZList = new ArrayList<Double>();
    private ArrayList<Double> accXList = new ArrayList<Double>();
    private ArrayList<Double> lengthList = new ArrayList<Double>();

    private final double ppmax = 100.0;
    private final double k = 1.5;
    private double achievedScore = 0.0;
    private double delta_t = 0.0;
    private double previous_sit_achievedScore = 0.0;

    private boolean isStanding = true;

    private double S[] = { 219.2, 98.4, 98.6, 1089.4, 1951.5 };
    private double gmeans[][] = { { 77.2, -113.9, -85.7, 823.4, 3917.8 },
            { -17.1, -382.8, 14.4, 4134.7, 5058.4 } };
    private static Handler handler = null;
    public static final int STATUS_SIT = 0;
    public static final int STATUS_STAND = 1;
    public static final int STATUS_OVERTIME = 2;

    private Logging(Context context){
        this.context = context;
        achievedScore = 0.0;
        delta_t = 0;
        previous_sit_achievedScore = 0.0;
    }

    public static Logging getInstance(Context context) {
        if (uniqueInstance == null) {
            uniqueInstance = new Logging(context);
        }
        return uniqueInstance;
    }

    public static void setHandler(Handler newHandler) {
        handler = newHandler;
    }

    private void sendToHandler(int msg) {
        if(handler != null) {
            handler.obtainMessage(msg).sendToTarget();
        }
    }

    public void logAchievedScore() {
        DBLog last = DatabaseHelper.getInstance(context).getLastLog();
        Date stopTime = new Date();
        if(last != null) {
            if(last.getAction().equals(DatabaseHelper.LOG_START_DAY)) {
                achievedScore = 0.0;
            } else if(last.getAction().equals(DatabaseHelper.LOG_OVERTIME)) {
                DBLog lastSitStand = DatabaseHelper.getInstance(context).getLastSitStand();
                if(lastSitStand != null) {
                    previous_sit_achievedScore = Double.parseDouble(last.getMetadata().split("\\|")[1]) - Double.parseDouble(lastSitStand.getMetadata().split("\\|")[1]);
                    achievedScore = Double.parseDouble(last.getMetadata().split("\\|")[1]);
                    startTime = last.getDatetime();
                    double delta_t = stopTime.getTime() - startTime.getTime();
                    achievedScore += (Math.pow((1440.0 * 60000.0 / delta_t) - 1, k) * (100.0 / Math.pow(47.0, k))) - previous_sit_achievedScore;
                }
            } else {
                String[] metadata = last.getMetadata().split("\\|");
                achievedScore = Double.parseDouble(metadata[1]);
                startTime = last.getDatetime();
                // calculate the achievedscore from the last state until now
                delta_t = stopTime.getTime() - startTime.getTime();
                achievedScore += (ppmax * delta_t) / _30mn_to_ms;
            }
        }
        double achievedScorePercentage = 0.0;
        DBLog first = DatabaseHelper.getInstance(context).getFirstRecordOfDay();
        if(first != null) {
            double connectionTime = stopTime.getTime() - first.getDatetime().getTime();
            double maxScoreToBeAchieved = (ppmax * connectionTime) / _30mn_to_ms;
            achievedScorePercentage = achievedScore * 100 / maxScoreToBeAchieved;
            DatabaseHelper.getInstance(context).endDay(stopTime, achievedScore, achievedScorePercentage, connectionTime);
        }
    }

    public void logData(ObjectCluster objectCluster) {

        try {
            if (mFirstWrite) {
                // First retrieve all the unique keys from the objectClusterLog
                Multimap<String, FormatCluster> m = objectCluster.mPropertyCluster;

                mSensorNames = new String[m.size()];
                mSensorFormats = new String[m.size()];
                mSensorUnits = new String[m.size()];
                int i = 0;
                int p = 0;
                for (String key : m.keys()) {
                    // first check that there are no repeat entries
                    if (compareStringArray(mSensorNames, key)) {
                        for (FormatCluster formatCluster : m.get(key)) {
                            mSensorFormats[p] = formatCluster.mFormat;
                            mSensorUnits[p] = formatCluster.mUnits;
                            p++;
                        }
                    }
                    mSensorNames[i] = key;
                    i++;
                }

                Log.d("Shimmer", "Data Written");
                mFirstWrite = false;
            }
            lengthXYZ = 0.0;
            for (int r = 0; r < mSensorNames.length; r++) {

                Collection<FormatCluster> dataFormats = objectCluster.mPropertyCluster
                        .get(mSensorNames[r]);
                FormatCluster formatCluster = returnFormatCluster(dataFormats, mSensorFormats[r], mSensorUnits[r]); // retrieve the calibrated data
                if (mSensorNames[r].equals("Accelerometer X")
                        || mSensorNames[r].equals("Accelerometer Y")
                        || mSensorNames[r].equals("Accelerometer Z")) {

                    // maybe to add a condition of having value larger than 60

                    if (mSensorNames[r].equals("Accelerometer Y")) {
                        accYList.add(formatCluster.mData);
                    } else if (mSensorNames[r].equals("Accelerometer Z")) {
                        accZList.add(formatCluster.mData);
                    } else if (mSensorNames[r].equals("Accelerometer X")) {
                        accXList.add(formatCluster.mData);
                    }
                    lengthXYZ += Math.pow(formatCluster.mData, 2);
                }
            }

            lengthList.add(lengthXYZ);

            if (accYList.size() > 40) {
                while (accYList.size() > 40) {
                    accYList.remove(accYList.size() - 41);
                }
            }
            if (accZList.size() > 40) {
                while (accZList.size() > 40) {
                    accZList.remove(accZList.size() - 41);
                }
            }
            if (accXList.size() > 40) {
                while (accXList.size() > 40) {
                    accXList.remove(accXList.size() - 41);
                }
            }
            if (lengthList.size() > 40) {
                while (lengthList.size() > 40) {
                    lengthList.remove(lengthList.size() - 41);
                }
            }

            if (accYList.size() == 40 && accZList.size() == 40
                    && accXList.size() == 40) {
                meanX = 0.0;
                meanY = 0.0;
                meanZ = 0.0;
                rmsY = 0.0;

                meanLength = 0.0;

                // calculate features
                for (double temp : accXList) {
                    meanX += temp;
                }
                for (double temp : accYList) {
                    meanY += temp;
                    rmsY += Math.pow(temp, 2);
                }
                for (double temp : accZList) {
                    meanZ += temp;
                }
                for (double temp : lengthList) {
                    meanLength += temp;
                }

                tempList.clear();
                tempList.add(meanX);
                tempList.add(meanY);
                tempList.add(meanZ);
                tempList.add(rmsY);
                tempList.add(meanLength);
                /*Log.i("MEASUREMENT", "X=" + meanX + "\tY=" + meanY + "\tZ=" + meanZ + "\tRMS=" + rmsY + "\tl=" + meanLength);
                // do the calculation to get label as standing or sitting

                ArrayList<Double> D = new ArrayList<>();
                D.clear();
                for (int i = 0; i < 2; i++) {
                    double sumResult = 0.0;
                    for (int j = 0; j < 5; j++) {
                        sumResult = sumResult
                                + Math.pow(
                                (tempList.get(j) - gmeans[i][j]) / S[j],
                                2.0);
                    }
                    D.add((-0.69314718) - 0.5 * (sumResult + logDetSigma));
                }
                isStanding = !(D.get(0) > D.get(1));
                Log.d("LOGDATA", isStanding ? "STAND" : "SIT");*/
                isStanding = Math.abs(meanY - 41500) > 4500;

                if (resultFirstWrite) {
                    DBLog log = DatabaseHelper.getInstance(context).getLastLog();

                    if (log != null) {
                        // after changing something in the log_begin_newday, it
                        // should never come to this blog
                        if (log.getAction().equals(DatabaseHelper.LOG_START_DAY)) {

                            resultFirstWrite = false;
                            startTime = new Date();
                            previousResult = isStanding;
                            delta_t = 0;
                            achievedScore = 0.0;
                            previous_sit_achievedScore = 0.0;
                            DatabaseHelper.getInstance(context).addSitStand(isStanding, delta_t + "|" + achievedScore);
                            sendToHandler(isStanding ? STATUS_STAND : STATUS_SIT);
                        } else if (log.getAction().equals(DatabaseHelper.LOG_OVERTIME)) {

                            try {
                                resultFirstWrite = false;
                                firstWriteSitOvertime = false;
                                previousResult = false;
                                DBLog lastsitstandlog = DatabaseHelper.getInstance(context).getLastSitStand();

                                if (lastsitstandlog != null) {
                                    previous_sit_achievedScore = Double
                                            .parseDouble(log.getMetadata()
                                                    .split("\\|")[1])
                                            - Double.parseDouble(lastsitstandlog
                                            .getMetadata().split("\\|")[1]);

                                    achievedScore = Double.parseDouble(log
                                            .getMetadata().split("\\|")[1]);

                                    startTime = lastsitstandlog.getDatetime();

                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.d("Logging", "catch_sit_overtime");
                            }

                        } else { // last log is sit or stand
                            try {
                                resultFirstWrite = false;
                                String[] metadata = log.getMetadata().split(
                                        "\\|");
                                previousResult = log.getAction().equals(DatabaseHelper.LOG_STAND);
                                startTime = log.getDatetime();
                                achievedScore = Double.parseDouble(metadata[1]);
                                previous_sit_achievedScore = 0.0;
                                long sit_overtime_ms_from70 = startTime.getTime()
                                        + _30mn_to_ms;
                                if (log.getAction().equals(DatabaseHelper.LOG_SIT)
                                        && System.currentTimeMillis() > sit_overtime_ms_from70) {
                                    Calendar calendar = Calendar.getInstance();
                                    calendar.setTimeInMillis(sit_overtime_ms_from70);
                                    firstWriteSitOvertime = false;
                                    delta_t = _30mn_to_ms;
                                    previous_sit_achievedScore = (ppmax * delta_t) / _30mn_to_ms;
                                    achievedScore += previous_sit_achievedScore;
                                    DatabaseHelper.getInstance(context).addSitOvertime(delta_t + "|" + achievedScore);
                                    sendToHandler(STATUS_OVERTIME);
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.d("Logging", "catch sit_stand");
                            }
                            // no state is logged for now, but the next sample
                        }
                    } else { // the very first time with empty database
                        resultFirstWrite = false;
                        startTime = new Date();
                        previousResult = isStanding;
                        delta_t = 0;
                        achievedScore = 0.0;
                        previous_sit_achievedScore = 0.0;
                        DatabaseHelper.getInstance(context).addSitStand(isStanding, delta_t + "|" + achievedScore);
                        sendToHandler(isStanding ? STATUS_STAND : STATUS_SIT);
                    }

                } else { // resultFirstWrite = false

                    if (!previousResult == isStanding) {
                        System.out.println(Boolean.toString(isStanding));
                        // set firstWroteSitOvertime back to true
                        firstWriteSitOvertime = true;
                        warningSound = true;
                        // if (!isStanding) {
                        // configurationAlert(isStanding,
                        // R.drawable.icon_sitting_green, "alert");
                        // } else {
                        // configurationAlert(isStanding,
                        // R.drawable.icon_standing_green, "pebbles");
                        // }
                        // calculate achieved score and log

                        delta_t = System.currentTimeMillis() - startTime.getTime();
                        if (!isStanding) {
                            achievedScore += ppmax * delta_t
                                    / (_30mn_to_ms);
                        } else {
                            if (delta_t <= _30mn_to_ms) {
                                achievedScore += (ppmax * delta_t) / (_30mn_to_ms);
                            } else {
                                double temp_score = Math.pow(
                                        (1440.0 * 60000.0 / delta_t) - 1.0, k) * (100.0 / Math
                                        .pow(47.0, k))
                                        - previous_sit_achievedScore;
                                achievedScore += temp_score;
                                previous_sit_achievedScore = 0.0;
                            }
                        }

                        // user change state
                        previousResult = isStanding;
                        startTime = new Date();
                        DatabaseHelper.getInstance(context).addSitStand(isStanding, delta_t + "|" + achievedScore);
                        sendToHandler(isStanding ? STATUS_STAND : STATUS_SIT);
                    }

                    if (firstWriteSitOvertime && !isStanding) {
                        if (System.currentTimeMillis() > startTime.getTime()
                                + _25mn_to_ms
                                && System.currentTimeMillis() < startTime.getTime()
                                + _30mn_to_ms) {
                            if (warningSound) {

                                warningSound = false;
                            }
                        } else if (System.currentTimeMillis() > startTime.getTime()
                                + _30mn_to_ms) {
                            firstWriteSitOvertime = false;
                            delta_t = System.currentTimeMillis() - startTime.getTime();
                            previous_sit_achievedScore = (ppmax * delta_t) / (_30mn_to_ms);
                            achievedScore += previous_sit_achievedScore;
                            DatabaseHelper.getInstance(context).addSitOvertime(delta_t + "|" + achievedScore);
                            sendToHandler(STATUS_OVERTIME);
                        }
                    }

                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.d("Shimmer", "Error logging");
        }
    }

    private boolean compareStringArray(String[] stringArray, String string){
        boolean uniqueString=true;
        int size = stringArray.length;
        for (int i=0;i<size;i++){
            if (stringArray[i]==string){
                uniqueString=false;
            }

        }
        return uniqueString;
    }

    private FormatCluster returnFormatCluster(Collection<FormatCluster> collectionFormatCluster, String format, String units){
        Iterator<FormatCluster> iFormatCluster=collectionFormatCluster.iterator();
        FormatCluster formatCluster;
        FormatCluster returnFormatCluster = null;

        while(iFormatCluster.hasNext()){
            formatCluster=(FormatCluster)iFormatCluster.next();
            if (formatCluster.mFormat==format && formatCluster.mUnits==units){
                returnFormatCluster=formatCluster;
            }
        }
        return returnFormatCluster;
    }

}


