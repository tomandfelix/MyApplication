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
    private final long _30mn_to_ms = 30 * 1000;
    private final long _25mn_to_ms = 25 * 1000;
    private final long _24h_to_ms = 24 * 60 * 1000;
    private boolean mFirstWrite = true;
    private boolean resultFirstWrite = true;
    private boolean firstWriteSitOvertime = true;
    private boolean warningSound = true;

    private Context context;
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
//    private double achievedScore = 0.0;
//    private double delta_t = 0.0;
//    private double previous_sit_achievedScore = 0.0;

    private boolean isStanding = true;

    private double S[] = { 219.2, 98.4, 98.6, 1089.4, 1951.5 };
    private double gmeans[][] = { { 77.2, -113.9, -85.7, 823.4, 3917.8 },
            { -17.1, -382.8, 14.4, 4134.7, 5058.4 } };
    private static Handler handler = null;
    public static final int STATUS_SIT = 0;
    public static final int STATUS_STAND = 1;
    public static final int STATUS_OVERTIME = 2;
    private Date lastUpdate = new Date();
    private DBLog last = null;

    private Logging(Context context){
        this.context = context;
//        achievedScore = 0.0;
//        delta_t = 0;
//        previous_sit_achievedScore = 0.0;
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
        Log.i("Logging", "handler set");
    }

    private void sendToHandler(int msg) {
        if(handler != null) {
            handler.obtainMessage(msg).sendToTarget();
        }
    }

    public void logAchievedScore() {
        DBLog last = DatabaseHelper.getInstance(context).getLastLog();
        Date stopTime = new Date();
        double achievedScore = 0.0;
        if(last != null) {
            if(last.getAction().equals(DatabaseHelper.LOG_START_DAY)) {
                achievedScore = 0.0;
            } else if(last.getAction().equals(DatabaseHelper.LOG_OVERTIME)) {
                DBLog lastSitStand = DatabaseHelper.getInstance(context).getLastSitStand();
                if(lastSitStand != null) {
                    String endMetadata = getDecreasingMetadata(stopTime.getTime() - lastSitStand.getDatetime().getTime(), lastSitStand.getMetadata());
                    String[] metaData = endMetadata.split("\\|");
                    achievedScore = Double.parseDouble(metaData[1]);
                }
            } else {
                String endMetaData = getIncreasingMetadata(stopTime.getTime() - last.getDatetime().getTime(), last.getMetadata());
                String[] metaData = endMetaData.split("\\|");
                achievedScore = Double.parseDouble(metaData[1]);
                startTime = last.getDatetime();
            }
        }
        double achievedScorePercentage;
        DBLog first = DatabaseHelper.getInstance(context).getFirstRecordOfDay();
        if(first != null) {
            double connectionTime = stopTime.getTime() - first.getDatetime().getTime();
            ArrayList<DBLog> connectionLogs = DatabaseHelper.getInstance(context).getTodaysConnectionLogs();
            for(int i = 2; i < connectionLogs.size(); i+=2) {
                connectionTime -= connectionLogs.get(i).getDatetime().getTime() - connectionLogs.get(i - 1).getDatetime().getTime();
            }
            double maxScoreToBeAchieved = (ppmax * connectionTime) / _30mn_to_ms;
            achievedScorePercentage = Math.round(achievedScore * 10000 / maxScoreToBeAchieved) / 100.0;
            DatabaseHelper.getInstance(context).endDay(stopTime, achievedScore, achievedScorePercentage, connectionTime);
        }
    }

    private String getIncreasingMetadata(long delta_t, String metaDataBefore) {
        String[] metadata = metaDataBefore.split("\\|");
        double previousScore = Double.parseDouble(metadata[1]);
        double score = previousScore + ppmax * delta_t / _30mn_to_ms;
        return delta_t + "|" + score;
    }

    private String getDecreasingMetadata(long millisecondsSitting, String metaDataStartedSitting) {
        String[] metaDataLastSit = metaDataStartedSitting.split("\\|");
        double previousSitScore = Double.parseDouble(metaDataLastSit[1]);

        double score = Math.pow((_24h_to_ms / millisecondsSitting) - 1, k) * (100 / Math.pow(47.0, k)) + previousSitScore;
        return millisecondsSitting + "|" + score;
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

                Date now = new Date();
                if(last == null || now.getTime() - lastUpdate.getTime() > 500) {
                    last = DatabaseHelper.getInstance(context).getLastLog();
                }
                lastUpdate = now;
                if(last.getAction().equals(DatabaseHelper.LOG_SIT)) {
                    if(isStanding) {
                        DatabaseHelper.getInstance(context).addSitStand(now, true, getIncreasingMetadata(now.getTime() - last.getDatetime().getTime(), last.getMetadata()));
                        last = null;
                    } else if(now.getTime() - last.getDatetime().getTime() >= _30mn_to_ms) {
                        DatabaseHelper.getInstance(context).addSitOvertime(now, getIncreasingMetadata(now.getTime() - last.getDatetime().getTime(), last.getMetadata()));
                        last = null;
                    }
                } else if(last.getAction().equals(DatabaseHelper.LOG_STAND)) {
                    if(!isStanding) {
                        DatabaseHelper.getInstance(context).addSitStand(now, false, getIncreasingMetadata(now.getTime() - last.getDatetime().getTime(), last.getMetadata()));
                        last = null;
                    }
                } else if(last.getAction().equals(DatabaseHelper.LOG_OVERTIME)) {
                    if(isStanding) {
                        DBLog startedSitting = DatabaseHelper.getInstance(context).getLastSitStand();
                        Log.d("LogData", "stand after sit_overtime\n" + startedSitting.toString() + "\n" + last.toString() + "\n" + now.toString());
                        DatabaseHelper.getInstance(context).addSitStand(now, true, getDecreasingMetadata(now.getTime() - startedSitting.getDatetime().getTime(), startedSitting.getMetadata()));
                        last = null;
                    }
                } else if(last.getAction().equals(DatabaseHelper.LOG_CONNECT)) {
                    DBLog secondLast = DatabaseHelper.getInstance(context).getLastLogBefore(last.getDatetime());
                    if(secondLast.getAction().equals(DatabaseHelper.LOG_START_DAY)) {
                        DatabaseHelper.getInstance(context).addSitStand(now, isStanding, "0.0|0.0");
                        last = null;
                    } else if(secondLast.getAction().equals(DatabaseHelper.LOG_DISCONNECT)) {
                        DBLog thirdLast = DatabaseHelper.getInstance(context).getLastLogBefore(secondLast.getDatetime());
                        long disconnectedDuration = last.getDatetime().getTime() - secondLast.getDatetime().getTime();
                        if(thirdLast.getAction().equals(DatabaseHelper.LOG_OVERTIME)) {
                            if(isStanding) {
                                DBLog startedSitting = DatabaseHelper.getInstance(context).getLastSitStand();
                                long sittingDuration = now.getTime() - startedSitting.getDatetime().getTime() - disconnectedDuration;
                                DatabaseHelper.getInstance(context).addSitStand(now, true, getDecreasingMetadata(sittingDuration, startedSitting.getMetadata()));
                                last = null;

                            }
                        } else if(thirdLast.getAction().equals(DatabaseHelper.LOG_SIT)) {
                            long sittingTime = now.getTime() - thirdLast.getDatetime().getTime() - disconnectedDuration;
                            if(isStanding) {
                                DatabaseHelper.getInstance(context).addSitStand(now, true, getIncreasingMetadata(sittingTime, thirdLast.getMetadata()));
                                last = null;
                            } else if(sittingTime >= _30mn_to_ms) {
                                DatabaseHelper.getInstance(context).addSitOvertime(now, getIncreasingMetadata(sittingTime, thirdLast.getMetadata()));
                                last = null;
                            }
                        } else if(thirdLast.getAction().equals(DatabaseHelper.LOG_STAND)) {
                            if(!isStanding) {
                                long standingTime = now.getTime() - thirdLast.getDatetime().getTime() - disconnectedDuration;
                                DatabaseHelper.getInstance(context).addSitStand(now, false, getIncreasingMetadata(standingTime, thirdLast.getMetadata()));
                                last = null;
                            }
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


