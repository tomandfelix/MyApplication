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
    private static final long _30mn_to_ms = 30 * 60 * 1000;
    private static final long _24h_to_ms = 24 * 60 * 60 * 1000;
    private boolean mFirstWrite = true;

    private Context context;

    private String[] mSensorNames;
    private String[] mSensorFormats;
    private String[] mSensorUnits;

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

    private static final double ppmax = 100.0;
    private static final double k = 1.5;

    private double S[] = { 219.2, 98.4, 98.6, 1089.4, 1951.5 };
    private double gmeans[][] = { { 77.2, -113.9, -85.7, 823.4, 3917.8 },
            { -17.1, -382.8, 14.4, 4134.7, 5058.4 } };
    private static Handler handler = null;
    public static final int STATUS_SIT = 0;
    public static final int STATUS_STAND = 1;
    public static final int STATUS_OVERTIME = 2;
    private Date lastUpdate = new Date();
    private DBLog last = null;
    private DBLog secondLast = null;
    private DBLog lastSitStandOverBeforeDiscon = null;
    private long overTimeBoundary = -1;

    private Logging(Context context){
        this.context = context;
    }

    public static Logging getInstance(Context context) {
        if (uniqueInstance == null) {
            uniqueInstance = new Logging(context.getApplicationContext());
        }
        return uniqueInstance;
    }

    public static Handler getHandler() {
        return handler;
    }

    public static void setHandler(Handler newHandler) {
        handler = newHandler;
        if(newHandler == null) {
            Log.i("Logging", "handler unset");
        } else {
            Log.i("Logging", "handler set");
        }
    }

    private void sendToHandler(int msg) {
        if(handler != null) {
            handler.obtainMessage(msg).sendToTarget();
        }
    }

    public static double getIncreasingScore(long delta_t, double scoreBefore) {
        return scoreBefore + ppmax * delta_t / _30mn_to_ms;
    }

    public static double getDecreasingScore(long millisecondsSitting, double scoreStartedSitting) {
        return Math.pow((_24h_to_ms / millisecondsSitting) - 1, k) * (100 / Math.pow(47.0, k)) + scoreStartedSitting;
    }

    public long getConnectedTimeSinceSitStandOver(DBLog lastLog, Date now) {
        ArrayList<DBLog> logs = new ArrayList<>();
        long result = 0;
        if(lastLog.getAction().equals(DatabaseHelper.LOG_CONNECT)) {
            logs.add(lastLog);
            while(logs.get(logs.size() - 1).getAction().equals(DatabaseHelper.LOG_DISCONNECT) || logs.get(logs.size() - 1).getAction().equals(DatabaseHelper.LOG_CONNECT)) {
                logs.add(DatabaseHelper.getInstance(context).getLastLogBefore(logs.get(logs.size() - 1).getDatetime()));
            }
            result += now.getTime() - logs.get(0).getDatetime().getTime();
            for(int i = 1; i < logs.size(); i += 2) {
                result += logs.get(i).getDatetime().getTime() - logs.get(i + 1).getDatetime().getTime();
            }
        }
        return result;
    }

    private void clearCache() {
        last = null;
        secondLast = null;
        lastSitStandOverBeforeDiscon = null;
        overTimeBoundary = -1;
    }

    public static long getConnectionTime(DBLog firstRecordOfDay, ArrayList<DBLog> connectionLogs, Date stopTime) {
        long connectionTime = -1;
        if(firstRecordOfDay != null) {
            if(connectionLogs.get(connectionLogs.size() - 1).getAction().equals(DatabaseHelper.LOG_DISCONNECT)) {
                stopTime = connectionLogs.get(connectionLogs.size() - 1).getDatetime();
            }
            connectionTime = stopTime.getTime() - firstRecordOfDay.getDatetime().getTime();
            for(int i = 2; i < connectionLogs.size(); i+=2) {
                connectionTime -= connectionLogs.get(i).getDatetime().getTime() - connectionLogs.get(i - 1).getDatetime().getTime();
            }
        }
        return connectionTime;
    }

    public void logAchievedScore() {
        Date now = new Date();
        Date stopTime;
        DBLog last = DatabaseHelper.getInstance(context).getLastLog();
        DBLog absoluteLast = DatabaseHelper.getInstance(context).getLastLogBefore(now);
        if(absoluteLast.getAction().equals(DatabaseHelper.LOG_DISCONNECT)) {
            stopTime = absoluteLast.getDatetime();
        } else if(absoluteLast.getAction().equals(DatabaseHelper.LOG_CONNECT)) {
            DBLog discon = DatabaseHelper.getInstance(context).getLastLogBefore(absoluteLast.getDatetime());
            stopTime = new Date(now.getTime() - absoluteLast.getDatetime().getTime() + discon.getDatetime().getTime());
        } else {
            stopTime = now;
        }
        double achievedScore = 0;
        if(last != null) {
            if(last.getAction().equals(DatabaseHelper.LOG_START_DAY)) {
                achievedScore = 0;
            } else if(last.getAction().equals(DatabaseHelper.LOG_OVERTIME)) {
                DBLog lastSitStand = DatabaseHelper.getInstance(context).getLastSitStand();
                if(lastSitStand != null) {
                    achievedScore = getDecreasingScore(stopTime.getTime() - lastSitStand.getDatetime().getTime(), lastSitStand.getData());
                }
            } else {
                achievedScore = getIncreasingScore(stopTime.getTime() - last.getDatetime().getTime(), last.getData());
            }
        }
        double achievedScorePercentage;
        DBLog first = DatabaseHelper.getInstance(context).getFirstRecordOfDay();
        if(first != null) {
            long connectionTime = getConnectionTime(first, DatabaseHelper.getInstance(context).getTodaysConnectionLogs(), stopTime);
            double maxScoreToBeAchieved = getIncreasingScore(connectionTime, 0);
            achievedScorePercentage = Math.round(achievedScore * 10000 / maxScoreToBeAchieved) / 100.0;
            DatabaseHelper.getInstance(context).endDay(now, achievedScore, achievedScorePercentage, connectionTime);
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
                boolean isStanding = Math.abs(meanY - 41500) > 4500;

                Date now = new Date();
                if(last == null || now.getTime() - lastUpdate.getTime() > 500) {
                    last = DatabaseHelper.getInstance(context).getLastLog();
                }
                lastUpdate = now;
                if(last.getAction().equals(DatabaseHelper.LOG_SIT)) {
                    if(isStanding) {
                        DatabaseHelper.getInstance(context).addSitStand(now, true, getIncreasingScore(now.getTime() - last.getDatetime().getTime(), last.getData()));
                        sendToHandler(STATUS_STAND);
                        clearCache();
                    } else if(now.getTime() - last.getDatetime().getTime() >= _30mn_to_ms) {
                        DatabaseHelper.getInstance(context).addSitOvertime(now, getIncreasingScore(now.getTime() - last.getDatetime().getTime(), last.getData()));
                        sendToHandler(STATUS_OVERTIME);
                        clearCache();
                    }
                } else if(last.getAction().equals(DatabaseHelper.LOG_STAND)) {
                    if(!isStanding) {
                        DatabaseHelper.getInstance(context).addSitStand(now, false, getIncreasingScore(now.getTime() - last.getDatetime().getTime(), last.getData()));
                        sendToHandler(STATUS_SIT);
                        clearCache();
                    }
                } else if(last.getAction().equals(DatabaseHelper.LOG_OVERTIME)) {
                    if(isStanding) {
                        DBLog startedSitting = DatabaseHelper.getInstance(context).getLastSitStand();
                        DatabaseHelper.getInstance(context).addSitStand(now, true, getDecreasingScore(now.getTime() - startedSitting.getDatetime().getTime(), startedSitting.getData()));
                        sendToHandler(STATUS_STAND);
                        clearCache();
                    }
                } else if(last.getAction().equals(DatabaseHelper.LOG_CONNECT)) {
                    if(secondLast == null) {
                        secondLast = DatabaseHelper.getInstance(context).getLastLogBefore(last.getDatetime());
                    }
                    if(secondLast.getAction().equals(DatabaseHelper.LOG_START_DAY)) {
                        DatabaseHelper.getInstance(context).addSitStand(now, isStanding, 0);
                        sendToHandler(isStanding ? STATUS_STAND : STATUS_SIT);
                        clearCache();
                    } else {
                        if(lastSitStandOverBeforeDiscon == null) {
                            lastSitStandOverBeforeDiscon = DatabaseHelper.getInstance(context).getLastSitStandOver();
                        }
                        if (lastSitStandOverBeforeDiscon.getAction().equals(DatabaseHelper.LOG_OVERTIME)) {
                            if (isStanding) {
                                DBLog lastSit = DatabaseHelper.getInstance(context).getLastSitStand();
                                long sittingDuration = getConnectedTimeSinceSitStandOver(last, now) + lastSitStandOverBeforeDiscon.getDatetime().getTime() - lastSit.getDatetime().getTime();
                                DatabaseHelper.getInstance(context).addSitStand(now, true, getDecreasingScore(sittingDuration, lastSit.getData()));
                                sendToHandler(STATUS_STAND);
                                clearCache();
                            }
                        } else if (lastSitStandOverBeforeDiscon.getAction().equals(DatabaseHelper.LOG_SIT)) {
                            if (isStanding) {
                                long sittingTime = getConnectedTimeSinceSitStandOver(last, now);
                                DatabaseHelper.getInstance(context).addSitStand(now, true, getIncreasingScore(sittingTime, lastSitStandOverBeforeDiscon.getData()));
                                sendToHandler(STATUS_STAND);
                                clearCache();
                            } else {
                                if(overTimeBoundary == -1) {
                                    long sittingTime = getConnectedTimeSinceSitStandOver(last, now);
                                    overTimeBoundary = now.getTime() - sittingTime + _30mn_to_ms;
                                }
                                if(now.getTime() > overTimeBoundary) { // Only means sit_overtime if no discon-con happened in between
                                    long sittingTime = getConnectedTimeSinceSitStandOver(last, now);
                                    if (sittingTime >= _30mn_to_ms) {
                                        DatabaseHelper.getInstance(context).addSitOvertime(now, getIncreasingScore(sittingTime, lastSitStandOverBeforeDiscon.getData()));
                                        sendToHandler(STATUS_OVERTIME);
                                        clearCache();
                                    } else {
                                        overTimeBoundary = now.getTime() - sittingTime + _30mn_to_ms;
                                    }
                                }
                            }
                        } else if (lastSitStandOverBeforeDiscon.getAction().equals(DatabaseHelper.LOG_STAND)) {
                            if (!isStanding) {
                                long standingTime = getConnectedTimeSinceSitStandOver(last, now);
                                DatabaseHelper.getInstance(context).addSitStand(now, false, getIncreasingScore(standingTime, lastSitStandOverBeforeDiscon.getData()));
                                sendToHandler(STATUS_SIT);
                                clearCache();
                            }
                        }
                    }

                    /*DBLog secondLast = DatabaseHelper.getInstance(context).getLastLogBefore(last.getDatetime());
                    if(secondLast.getAction().equals(DatabaseHelper.LOG_START_DAY)) {
                        DatabaseHelper.getInstance(context).addSitStand(now, isStanding, 0);
                        sendToHandler(isStanding ? STATUS_STAND : STATUS_SIT);
                        last = null;
                    } else if(secondLast.getAction().equals(DatabaseHelper.LOG_DISCONNECT)) {
                        DBLog thirdLast = DatabaseHelper.getInstance(context).getLastLogBefore(secondLast.getDatetime());
                        long disconnectedDuration = last.getDatetime().getTime() - secondLast.getDatetime().getTime();
                        if(thirdLast.getAction().equals(DatabaseHelper.LOG_OVERTIME)) {
                            if(isStanding) {
                                DBLog startedSitting = DatabaseHelper.getInstance(context).getLastSitStand();
                                long sittingDuration = now.getTime() - startedSitting.getDatetime().getTime() - disconnectedDuration;
                                DatabaseHelper.getInstance(context).addSitStand(now, true, getDecreasingScore(sittingDuration, startedSitting.getData()));
                                sendToHandler(STATUS_STAND);
                                last = null;

                            }
                        } else if(thirdLast.getAction().equals(DatabaseHelper.LOG_SIT)) {
                            long sittingTime = now.getTime() - thirdLast.getDatetime().getTime() - disconnectedDuration;
                            if(isStanding) {
                                DatabaseHelper.getInstance(context).addSitStand(now, true, getIncreasingScore(sittingTime, thirdLast.getData()));
                                sendToHandler(STATUS_STAND);
                                last = null;
                            } else if(sittingTime >= _30mn_to_ms) {
                                DatabaseHelper.getInstance(context).addSitOvertime(now, getIncreasingScore(sittingTime, thirdLast.getData()));
                                sendToHandler(STATUS_OVERTIME);
                                last = null;
                            }
                        } else if(thirdLast.getAction().equals(DatabaseHelper.LOG_STAND)) {
                            if(!isStanding) {
                                long standingTime = now.getTime() - thirdLast.getDatetime().getTime() - disconnectedDuration;
                                DatabaseHelper.getInstance(context).addSitStand(now, false, getIncreasingScore(standingTime, thirdLast.getData()));
                                sendToHandler(STATUS_SIT);
                                last = null;
                            }
                        }
                    }*/
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


