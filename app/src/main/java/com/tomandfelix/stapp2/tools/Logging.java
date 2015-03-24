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


package com.tomandfelix.stapp2.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.tomandfelix.stapp2.persistency.DBLog;
import com.tomandfelix.stapp2.persistency.DatabaseHelper;
import com.tomandfelix.stapp2.driver.FormatCluster;
import com.tomandfelix.stapp2.driver.ObjectCluster;


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
    public static final int STATE_DAY_STARTED = 0;
    public static final int STATE_DAY_STOPPED = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    public static final int STATE_DISCONNECTED = 4;
    public static final int STATE_SIT = 5;
    public static final int STATE_STAND = 6;
    public static final int STATE_OVERTIME = 7;
    private DBLog currentActivity = null;   // always sit or stand after first assignment
    private DBLog last = null;              // always the last log
    private long connTimeSinceCurr = 0;
    private boolean connected = false;
    private boolean connecting = false;
    private boolean dayStarted = false;
    private boolean overtimeLogged = false;

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

    private void sendUpdate() {
        if(handler != null) {
            handler.obtainMessage(getState()).sendToTarget();
        }
    }

    public int getState() {
        if(!dayStarted) {
            return STATE_DAY_STOPPED;
        } else if(connecting) {
            return STATE_CONNECTING;
        } else if(!connected) {
            return STATE_DISCONNECTED;
        } else if(currentActivity == null) {
            return STATE_CONNECTED;
        } else if(overtimeLogged) {
            return STATE_OVERTIME;
        } else if(currentActivity.getAction().equals(DatabaseHelper.LOG_SIT)) {
            return STATE_SIT;
        } else if(currentActivity.getAction().equals(DatabaseHelper.LOG_STAND)) {
            return STATE_STAND;
        } else {
            return -1;
        }
    }

    public static double getIncreasingScore(long delta_t, double scoreBefore) {
        return scoreBefore + ppmax * (double) delta_t / (double) _30mn_to_ms;
    }

    public static double getDecreasingScore(long millisecondsSitting, double scoreStartedSitting) {
        double temp = Math.pow(((double) _24h_to_ms / (double) millisecondsSitting) - 1, k) * (100 / Math.pow(47.0, k)) + scoreStartedSitting;
        Log.d("GetDecreasingMetadata", temp + "=Math.pow((" + _24h_to_ms + " / " + millisecondsSitting + ") - 1, " + k + ") * (100 / Math.pow(47.0, " + k + ")) + " + scoreStartedSitting);
        return temp;
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

    public void logStartDay() {
        if(DatabaseHelper.getInstance().dayStarted() == null) {
            last = new DBLog(DatabaseHelper.LOG_START_DAY, new Date(), -1);
            DatabaseHelper.getInstance().addLog(last);
            connTimeSinceCurr = 0;
            dayStarted = true;
            sendUpdate();
        }
    }

    public void logConnecting() {
        if(!connecting) {
            connecting = true;
            sendUpdate();
        }
    }

    public void logConnect() {
        if(!DatabaseHelper.getInstance().isConnected()) {
            last = new DBLog(DatabaseHelper.LOG_CONNECT, new Date(), -1);
            DatabaseHelper.getInstance().addLog(last);
            connecting = false;
            connected = true;
            sendUpdate();
        }
    }

    public void logDisconnect() {
        if(DatabaseHelper.getInstance().isConnected()) {
            Date now = new Date();
            connTimeSinceCurr += now.getTime() - last.getDatetime().getTime();
            last = new DBLog(DatabaseHelper.LOG_DISCONNECT, now, -1);
            DatabaseHelper.getInstance().addLog(last);
            connected = false;
            sendUpdate();
        } else if(getState() == STATE_CONNECTING) {
            sendUpdate();
        }
    }

    public void logAchievedScore() {
        if(dayStarted) {
            Date now = new Date();
            Date stopTime = connected ? now : last.getDatetime();
            if (connected) {
                connTimeSinceCurr += now.getTime() - last.getDatetime().getTime();
            }
            double achievedScore;
            if (last.getAction().equals(DatabaseHelper.LOG_START_DAY)) {
                achievedScore = 0;
            } else if (overtimeLogged) {
                achievedScore = getDecreasingScore(connTimeSinceCurr, currentActivity.getData());
            } else {
                achievedScore = getIncreasingScore(connTimeSinceCurr, currentActivity.getData());
            }

            double achievedScorePercentage;
            DBLog first = DatabaseHelper.getInstance().getFirstRecordOfDay();
            if (first != null) {
                long connectionTime = getConnectionTime(first, DatabaseHelper.getInstance().getTodaysConnectionLogs(), stopTime);
                double maxScoreToBeAchieved = getIncreasingScore(connectionTime, 0);
                achievedScorePercentage = Math.round(achievedScore * 10000 / maxScoreToBeAchieved) / 100.0;
                DatabaseHelper.getInstance().addLog(new DBLog(DatabaseHelper.LOG_ACH_SCORE, now, achievedScore));
                DatabaseHelper.getInstance().addLog(new DBLog(DatabaseHelper.LOG_ACH_SCORE_PERC, now, achievedScorePercentage));
                last = new DBLog(DatabaseHelper.LOG_STOP_DAY, now, connectionTime);
                DatabaseHelper.getInstance().addLog(last);
                currentActivity = null;
            }
            dayStarted = false;
            sendUpdate();
        }
    }

    public void logData(ObjectCluster objectCluster) {
        try {
//            if (mFirstWrite) {
//                // First retrieve all the unique keys from the objectClusterLog
//                Multimap<String, FormatCluster> m = objectCluster.mPropertyCluster;
//
//                mSensorNames = new String[m.size()];
//                mSensorFormats = new String[m.size()];
//                mSensorUnits = new String[m.size()];
//                int i = 0;
//                int p = 0;
//                for (String key : m.keys()) {
//                    Log.d("PROPERTY", "KEY=" + key);
//                    for(FormatCluster format : m.get(key)) {
//                        Log.d("PROPERTY", "FORMAT=" + format.mFormat + " UNITS=" + format.mUnits + " DATA=" + format.mData);
//                    }
//                    // first check that there are no repeat entries
//                    if (compareStringArray(mSensorNames, key)) {
//                        for (FormatCluster formatCluster : m.get(key)) {
//                            mSensorFormats[p] = formatCluster.mFormat;
//                            mSensorUnits[p] = formatCluster.mUnits;
//                            p++;
//                        }
//                    }
//                    mSensorNames[i] = key;
//                    i++;
//                }
//
//                Log.d("Shimmer", "Data Written");
//                mFirstWrite = false;
//            }
//            lengthXYZ = 0.0;
//            Log.d("MEASUREMENT", "------------------------------");
//            for (int r = 0; r < mSensorNames.length; r++) {
//
//                Collection<FormatCluster> dataFormats = objectCluster.mPropertyCluster.get(mSensorNames[r]);
//                FormatCluster formatCluster = returnFormatCluster(dataFormats, mSensorFormats[r], mSensorUnits[r]); // retrieve the calibrated data
//                if (mSensorNames[r].equals("Accelerometer X")
//                        || mSensorNames[r].equals("Accelerometer Y")
//                        || mSensorNames[r].equals("Accelerometer Z")) {
//
//                    // maybe to add a condition of having value larger than 60
//
//                    if (mSensorNames[r].equals("Accelerometer Y")) {
//                        accYList.add(formatCluster.mData);
//                        Log.i("MEASUREMENT", "Y=" + formatCluster.mData);
//                    } else if (mSensorNames[r].equals("Accelerometer Z")) {
//                        accZList.add(formatCluster.mData);
//                        Log.i("MEASUREMENT", "Z=" + formatCluster.mData);
//                    } else if (mSensorNames[r].equals("Accelerometer X")) {
//                        accXList.add(formatCluster.mData);
//                        Log.i("MEASUREMENT", "X=" + formatCluster.mData);
//                    }
//                    lengthXYZ += Math.pow(formatCluster.mData, 2);
//                }
//            }
            lengthXYZ = 0.0;
//            Log.d("MEASUREMENT", "-------------------------------------------");
            for (FormatCluster data : objectCluster.mPropertyCluster.get("Accelerometer X")) {
                if(data.mFormat.equals("CAL")) {
                    accXList.add(data.mData);
//                    Log.i("MEASUREMENT", "X=" + data.mData);
                    lengthXYZ += Math.pow(data.mData, 2);
                }
            }
            for (FormatCluster data : objectCluster.mPropertyCluster.get("Accelerometer Y")) {
                if(data.mFormat.equals("CAL")) {
                    accYList.add(data.mData);
//                    Log.i("MEASUREMENT", "Y=" + data.mData);
                    lengthXYZ += Math.pow(data.mData, 2);
                }
            }
            for (FormatCluster data : objectCluster.mPropertyCluster.get("Accelerometer Z")) {
                if(data.mFormat.equals("CAL")) {
                    accZList.add(data.mData);
//                    Log.i("MEASUREMENT", "Z=" + data.mData);
                    lengthXYZ += Math.pow(data.mData, 2);
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
                tempList.add(-Math.abs(meanY));
                tempList.add(meanZ);
                tempList.add(rmsY);
                tempList.add(meanLength);
//                Log.i("MEASUREMENT", "X=" + meanX + "\tY=" + meanY + "\tZ=" + meanZ + "\tRMS=" + rmsY + "\tl=" + meanLength);
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
                boolean isStanding = !(D.get(0) > D.get(1));
//                Log.d("LOGDATA", isStanding ? "STAND" : "SIT");
//                boolean isStanding = Math.abs(meanY - 41000) > 4500;

                if(last == null) {
                    last = DatabaseHelper.getInstance().getLastLog();
                }
                if(dayStarted && connected) {
                    Date now = new Date();
                    if (currentActivity == null) { //first record of the day
                        last = new DBLog(isStanding ? DatabaseHelper.LOG_STAND : DatabaseHelper.LOG_SIT, now, 0);
                        currentActivity = last;
                        DatabaseHelper.getInstance().addLog(last);
                        connTimeSinceCurr = 0;
                        sendUpdate();
                    } else if (currentActivity.getAction().equals(DatabaseHelper.LOG_SIT)) {
                        if (isStanding) {
                            if (overtimeLogged) {
                                connTimeSinceCurr += now.getTime() - last.getDatetime().getTime();
                                last = new DBLog(DatabaseHelper.LOG_STAND, now, getDecreasingScore(connTimeSinceCurr, currentActivity.getData()));
                                currentActivity = last;
                                DatabaseHelper.getInstance().addLog(last);
                                connTimeSinceCurr = 0;
                                overtimeLogged = false;
                                sendUpdate();
                            } else {
                                connTimeSinceCurr += now.getTime() - last.getDatetime().getTime();
                                last = new DBLog(DatabaseHelper.LOG_STAND, now, getIncreasingScore(connTimeSinceCurr, currentActivity.getData()));
                                currentActivity = last;
                                DatabaseHelper.getInstance().addLog(last);
                                connTimeSinceCurr = 0;
                                sendUpdate();
                            }
                        } else if (!overtimeLogged && (now.getTime() - last.getDatetime().getTime() + connTimeSinceCurr) >= _30mn_to_ms) {
                            connTimeSinceCurr += now.getTime() - last.getDatetime().getTime();
                            last = new DBLog(DatabaseHelper.LOG_OVERTIME, now, getIncreasingScore(connTimeSinceCurr, currentActivity.getData()));
                            DatabaseHelper.getInstance().addLog(last);
                            overtimeLogged = true;
                            sendUpdate();
                        }
                    } else if (currentActivity.getAction().equals(DatabaseHelper.LOG_STAND)) {
                        if (!isStanding) {
                            connTimeSinceCurr += now.getTime() - last.getDatetime().getTime();
                            last = new DBLog(DatabaseHelper.LOG_SIT, now, getIncreasingScore(connTimeSinceCurr, currentActivity.getData()));
                            currentActivity = last;
                            DatabaseHelper.getInstance().addLog(last);
                            connTimeSinceCurr = 0;
                            sendUpdate();
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
            formatCluster= iFormatCluster.next();
            if (formatCluster.mFormat==format && formatCluster.mUnits==units){
                returnFormatCluster=formatCluster;
            }
        }
        return returnFormatCluster;
    }

}


