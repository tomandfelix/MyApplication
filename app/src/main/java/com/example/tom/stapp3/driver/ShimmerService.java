package com.example.tom.stapp3.driver;

//v0.2 -  8 January 2013

import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.example.tom.stapp3.activity.ConnectionView;
import com.example.tom.stapp3.persistency.DBLog;
import com.example.tom.stapp3.persistency.DatabaseHelper;
import com.google.common.collect.Multimap;

public class ShimmerService extends Service {
    private static final String TAG = "MyService";
    private static boolean mEnableLogging = false;
    private boolean mFirstWrite = true;
    private boolean resultFirstWrite = true;
    private boolean firstWriteSitOvertime = true;
    private boolean warningSound = true;
    private Date startTime;
    private String[] mSensorNames;
    private String[] mSensorFormats;
    private String[] mSensorUnits;
    private String previousResult = "";
    private final double logDetSigma = 58.2792;
    private final long _30mn_to_ms = 30 * 60 * 1000;
    private final long _25mn_to_ms = 25 * 60 * 1000;
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
    private final IBinder mBinder = new LocalBinder();
    private String isStanding = "stand";
    private double S[] = { 219.2, 98.4, 98.6, 1089.4, 1951.5 };
    private double gmeans[][] = { { 77.2, -113.9, -85.7, 823.4, 3917.8 },
            { -17.1, -382.8, 14.4, 4134.7, 5058.4 } };
    public HashMap<String, Object> mMultiShimmer = new HashMap<String, Object>(
            7);
    public Shimmer myShimmer = null;
    private Handler mHandlerGraph = null;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");

        if (ConnectionView.currentlyVisible == null) {
            String address = DatabaseHelper.getInstance(getApplicationContext()).getAddress();
            if (address != null) {
                if (mMultiShimmer.get(address) == null) {
                    Shimmer shimmerDevice = new Shimmer(
                            getApplicationContext(), mHandler, "Device", false);
                    mMultiShimmer.put(address, shimmerDevice);
                }
            }
        }
    }

    public class LocalBinder extends Binder {
        public ShimmerService getService() {
            Log.d(TAG, "ShimmerServiceBinder getService");
            // Return this instance of LocalService so clients can call public
            // methods
            return ShimmerService.this;
        }
    }

    @Override
    public void onDestroy() {
        // Toast.makeText(this, "My Service Stopped", Toast.LENGTH_LONG).show();
        Log.d(TAG, "onDestroy");
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            stemp.stop();
        }

    }

    public void disconnectAllDevices() {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            stemp.stop();
        }
        mMultiShimmer.clear();
        // mLogShimmer.clear();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("LocalService", "Received start id " + startId + ": " + intent);
        if (DatabaseHelper.getInstance(getApplicationContext()).dayStarted() != null) {
            if(DatabaseHelper.getInstance(getApplicationContext()).tryEndDay()) {
                disconnectAllDevices();
            }
        }

        setEnableLogging(true);
        String address = DatabaseHelper.getInstance(getApplicationContext()).getAddress();
        Log.d("ShimmerService", "address: '" + address + "'");

        if (!address.equals("")) {
            final String addressToConnect = address;
            myShimmer = (Shimmer) (mMultiShimmer.get(address));

            if (myShimmer == null) {
                System.out.println("entering myshimmer null");
                this.connectShimmer(address, "Device");
            } else {

                if ((myShimmer != null)
                        && (myShimmer.getShimmerState() == Shimmer.STATE_NONE)) {
                    new AsyncTask<URL, Integer, Long>() {
                        // This method is called when the thread runs
                        @Override
                        protected Long doInBackground(URL... params) {
                            Shimmer shimmerDevice = new Shimmer(
                                    getApplicationContext(), mHandler,
                                    "Device", false);
                            mMultiShimmer.remove(addressToConnect);
                            // shimmerLog1 = (Logging) mLogShimmer
                            // .get(addressToConnect);
                            // if (shimmerLog1 != null) {
                            // shimmerLog1.closeDatabase();
                            // mLogShimmer.clear();
                            // }

                            // Logging.getUniqueInstance(getApplicationContext())
                            // .closeDatabase();

                            if (mMultiShimmer.get(addressToConnect) == null) {
                                mMultiShimmer.put(addressToConnect,
                                        shimmerDevice);
                                ((Shimmer) mMultiShimmer.get(addressToConnect))
                                        .connect(addressToConnect, "default");
                            }
                            try {
                                Thread.sleep(15 * 1000);
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            System.out.println("Shimmer state_after: "
                                    + shimmerDevice.getShimmerState());
                            if (shimmerDevice.getShimmerState() == Shimmer.STATE_CONNECTED
                                    && shimmerDevice.getBluetoothAddress()
                                    .equals(addressToConnect)) {
                                shimmerDevice.startStreaming();
                            }
                            return null;
                        }

                    }.execute();

                } else if ((myShimmer != null)
                        && (myShimmer.getShimmerState() == Shimmer.STATE_CONNECTED)
                        && (!DeviceIsStreaming(addressToConnect))) {
                    new AsyncTask<URL, Integer, Long>() {
                        // This method is called when the thread runs
                        @Override
                        protected Long doInBackground(URL... params) {

                            if (myShimmer.getShimmerState() == Shimmer.STATE_CONNECTED
                                    && myShimmer.getBluetoothAddress().equals(
                                    addressToConnect)) {
                                myShimmer.startStreaming();
                            }

                            return null;
                        }
                    }.execute();
                }
            }

        }
        return START_STICKY;
    }

    @Override
    public void onStart(Intent intent, int startid) {
        Log.d(TAG, "onStart");
    }

    public void checkShimmerConnection() {
        String address = DatabaseHelper.getInstance(getApplicationContext()).getAddress();
        if (address != "") {
            connectShimmer(address, "Device");
            Shimmer stemp = (Shimmer) (mMultiShimmer.get(address));
            while (!(stemp.getShimmerState() == Shimmer.STATE_CONNECTED)) {
            }
            ;
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
                    && stemp.getBluetoothAddress().equals(address)) {
                stemp.startStreaming();
            }
        }
    }

    public void connectShimmer(String bluetoothAddress, String selectedDevice) {
        Log.d("Shimmer", "net Connection");
        Shimmer shimmerDevice = new Shimmer(this, mHandler, selectedDevice,
                false);
        mMultiShimmer.remove(bluetoothAddress);
        if (mMultiShimmer.get(bluetoothAddress) == null) {
            mMultiShimmer.put(bluetoothAddress, shimmerDevice);
            ((Shimmer) mMultiShimmer.get(bluetoothAddress)).connect(
                    bluetoothAddress, "default");
        }
    }

    public void onStop() {
        Log.d(TAG, "onDestroy");
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            stemp.stop();
        }
    }

    public void toggleAllLEDS() {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                stemp.toggleLed();
            }
        }
    }

    public final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) { // handlers have a what identifier which is used
                // to identify the type of msg
                case Shimmer.MESSAGE_READ:
                    if ((msg.obj instanceof ObjectCluster)) { // within each msg an object can be include, objectclusters are used to represent the data structure of the shimmer device
                        ObjectCluster objectCluster = (ObjectCluster) msg.obj;
                        if (mEnableLogging == true) {
                            logData(objectCluster);
                        }
                        if (mHandlerGraph != null) {
                            mHandlerGraph.obtainMessage(Shimmer.MESSAGE_READ,
                                    objectCluster).sendToTarget();
                        }
                        // }
                    }
                    break;
                case Shimmer.MESSAGE_TOAST:
                    Log.d("toast", msg.getData().getString(Shimmer.TOAST));
                    // Toast.makeText(getApplicationContext(),
                    // msg.getData().getString(Shimmer.TOAST),
                    // Toast.LENGTH_SHORT).show();
                    if (msg.getData().getString(Shimmer.TOAST)
                            .equals("Device connection was lost")) {
                        // String address = Logging.readAddressFile();

                        DatabaseHelper.getInstance(getApplicationContext()).addLog(new DBLog("sensor_disconnect", new Date(), null));

                        // Logging.getUniqueInstance(getApplicationContext())
                        // .closeDatabase();
                        //
                        // shimmerLog1 = (Logging) mLogShimmer.get(address);
                        // if (shimmerLog1 != null) {
                        // // achievedScore has to be called before
                        // // acheiveScorePercent and SensorDiscon so that the last
                        // // achievedScore is updated
                        // shimmerLog1.logAchievedScore();
                        // shimmerLog1.logAchivedScorePercent();
                        //
                        // shimmerLog1.logSDI();
                        //
                        // shimmerLog1.logSensorDiscon();
                        //
                        // shimmerLog1.closeDatabase();
                        // mLogShimmer.clear();
                        // }
                    } else if (msg.getData().getString(Shimmer.TOAST)
                            .contains("Unable to connect device")) {

                        // String address = Logging.readAddressFile();
                        // shimmerLog1 = (Logging) mLogShimmer.get(address);
                        // if (shimmerLog1 != null) {
                        // shimmerLog1.closeDatabase();
                        // mLogShimmer.clear();
                        // }

                        // Logging.getUniqueInstance(getApplicationContext())
                        // .closeDatabase();

                    } else if (msg.getData().getString(Shimmer.TOAST)
                            .contains("is now Streaming")) {
                        DatabaseHelper.getInstance(getApplicationContext()).addLog(new DBLog("sensor_connect", new Date(), null));

                    }
                    break;
                case Shimmer.MESSAGE_STATE_CHANGE:
                    Intent intent = new Intent(
                            "com.shimmerresearch.service.ShimmerService");
                    Log.d("ShimmerGraph", "Sending");
                    if (mHandlerGraph != null) {
                        mHandlerGraph.obtainMessage(Shimmer.MESSAGE_STATE_CHANGE,
                                msg.arg1, -1, msg.obj).sendToTarget();
                    }

                    switch (msg.arg1) {
                        case Shimmer.STATE_CONNECTED:
                            Log.d("Shimmer",
                                    ((ObjectCluster) msg.obj).mBluetoothAddress + "  "
                                            + ((ObjectCluster) msg.obj).mMyName);

                            intent.putExtra("ShimmerBluetoothAddress",
                                    ((ObjectCluster) msg.obj).mBluetoothAddress);
                            intent.putExtra("ShimmerDeviceName",
                                    ((ObjectCluster) msg.obj).mMyName);
                            intent.putExtra("ShimmerState", Shimmer.STATE_CONNECTED);
                            sendBroadcast(intent);

                            break;
                        case Shimmer.STATE_CONNECTING:
                            intent.putExtra("ShimmerBluetoothAddress",
                                    ((ObjectCluster) msg.obj).mBluetoothAddress);
                            intent.putExtra("ShimmerDeviceName",
                                    ((ObjectCluster) msg.obj).mMyName);
                            intent.putExtra("ShimmerState", Shimmer.STATE_CONNECTING);
                            break;
                        case Shimmer.STATE_NONE:
                            intent.putExtra("ShimmerBluetoothAddress",
                                    ((ObjectCluster) msg.obj).mBluetoothAddress);
                            intent.putExtra("ShimmerDeviceName",
                                    ((ObjectCluster) msg.obj).mMyName);
                            intent.putExtra("ShimmerState", Shimmer.STATE_NONE);
                            sendBroadcast(intent);
                            break;
                    }
                    break;

                case Shimmer.MESSAGE_STOP_STREAMING_COMPLETE:
                    String address = msg.getData().getString("Bluetooth Address");
                    boolean stop = msg.getData().getBoolean("Stop Streaming");
                    if (stop == true) {
                        closeAndRemoveFile(address);
                    }
                    break;
            }
        }
    };

    public void stopStreamingAllDevices() {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();

            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                stemp.stopStreaming();

            }
        }
    }

    public void startStreamingAllDevices() {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                stemp.startStreaming();
            }
        }
    }

    public void setEnableLogging(boolean enableLogging) {
        mEnableLogging = enableLogging;
        Log.d("Shimmer", "Logging :" + Boolean.toString(mEnableLogging));
    }

    public boolean getEnableLogging() {
        return mEnableLogging;
    }

    public void setAllSampingRate(double samplingRate) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                stemp.writeSamplingRate(samplingRate);
            }
        }
    }

    public void setAllAccelRange(int accelRange) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                stemp.writeAccelRange(accelRange);
            }
        }
    }

    public void setAllGSRRange(int gsrRange) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                stemp.writeGSRRange(gsrRange);
            }
        }
    }

    public void setAllEnabledSensors(int enabledSensors) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                stemp.writeEnabledSensors(enabledSensors);
            }
        }
    }

    public void setEnabledSensors(int enabledSensors, String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
                    && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.writeEnabledSensors(enabledSensors);
            }
        }
    }

    public void toggleLED(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
                    && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.toggleLed();
            }
        }
    }

    public void writePMux(String bluetoothAddress, int setBit) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
                    && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.writePMux(setBit);
            }
        }
    }

    public void write5VReg(String bluetoothAddress, int setBit) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
                    && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.writeFiveVoltReg(setBit);
            }
        }
    }

    public int getEnabledSensors(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        int enabledSensors = 0;
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
                    && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                enabledSensors = stemp.getEnabledSensors();
            }
        }
        return enabledSensors;
    }

    public void writeSamplingRate(String bluetoothAddress, double samplingRate) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
                    && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.writeSamplingRate(samplingRate);
            }
        }
    }

    public void writeAccelRange(String bluetoothAddress, int accelRange) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
                    && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.writeAccelRange(accelRange);
            }
        }
    }

    public void writeGSRRange(String bluetoothAddress, int gsrRange) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
                    && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.writeGSRRange(gsrRange);
            }
        }
    }

    public double getSamplingRate(String bluetoothAddress) {
        // TODO Auto-generated method stub

        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        double SRate = -1;
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
                    && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                SRate = stemp.getSamplingRate();
            }
        }
        return SRate;
    }

    public int getAccelRange(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        int aRange = -1;
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
                    && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                aRange = stemp.getAccelRange();
            }
        }
        return aRange;
    }

    public int getShimmerState(String bluetoothAddress) {

        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        int status = -1;
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                status = stemp.getShimmerState();
                Log.d("ShimmerState", Integer.toString(status));
            }
        }
        return status;

    }

    public int getGSRRange(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        int gRange = -1;
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
                    && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                gRange = stemp.getGSRRange();
            }
        }
        return gRange;
    }

    public int get5VReg(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        int fiveVReg = -1;
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
                    && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                fiveVReg = stemp.get5VReg();
            }
        }
        return fiveVReg;
    }

    public boolean isLowPowerMagEnabled(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        boolean enabled = false;
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
                    && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                enabled = stemp.isLowPowerMagEnabled();
            }
        }
        return enabled;
    }

    public int getpmux(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        int pmux = -1;
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
                    && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                pmux = stemp.getPMux();
            }
        }
        return pmux;
    }

    public void startStreaming(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
                    && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.startStreaming();
            }
        }
    }

    public void stopStreaming(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
                    && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.stopStreaming();
            }
        }
    }

    public void setBlinkLEDCMD(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Shimmer stemp = (Shimmer) mMultiShimmer.get(bluetoothAddress);
        if (stemp != null) {
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
                    && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                if (stemp.getCurrentLEDStatus() == 0) {
                    stemp.writeLEDCommand(1);
                } else {
                    stemp.writeLEDCommand(0);
                }
            }
        }

    }

    public void enableLowPowerMag(String bluetoothAddress, boolean enable) {
        // TODO Auto-generated method stub
        Shimmer stemp = (Shimmer) mMultiShimmer.get(bluetoothAddress);
        if (stemp != null) {
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
                    && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.enableLowPowerMag(enable);
            }
        }
    }

    public void setBattLimitWarning(String bluetoothAddress, double limit) {
        // TODO Auto-generated method stub
        Shimmer stemp = (Shimmer) mMultiShimmer.get(bluetoothAddress);
        if (stemp != null) {
            if (stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.setBattLimitWarning(limit);
            }
        }

    }

    public double getBattLimitWarning(String bluetoothAddress) {
        // TODO Auto-generated method stub
        double limit = -1;
        Shimmer stemp = (Shimmer) mMultiShimmer.get(bluetoothAddress);
        if (stemp != null) {
            if (stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                limit = stemp.getBattLimitWarning();
            }
        }
        return limit;
    }

    public double getPacketReceptionRate(String bluetoothAddress) {
        // TODO Auto-generated method stub
        double rate = -1;
        Shimmer stemp = (Shimmer) mMultiShimmer.get(bluetoothAddress);
        if (stemp != null) {
            if (stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                rate = stemp.getPacketReceptionRate();
            }
        }
        return rate;
    }

    public void disconnectShimmer(String bluetoothAddress) {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
                    && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.stop();

            }
        }
        // mLogShimmer.remove(bluetoothAddress);
        mMultiShimmer.remove(bluetoothAddress);

    }

    public void setGraphHandler(Handler handler) {
        mHandlerGraph = handler;
    }

    public boolean DevicesConnected(String bluetoothAddress) {
        boolean deviceConnected = false;
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED
                    && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                deviceConnected = true;
            }
        }
        return deviceConnected;
    }

    public boolean DeviceIsStreaming(String bluetoothAddress) {
        boolean deviceStreaming = false;
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getStreamingStatus() == true
                    && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                deviceStreaming = true;
            }
        }
        return deviceStreaming;
    }

    public boolean GetInstructionStatus(String bluetoothAddress) {
        boolean instructionStatus = false;
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                instructionStatus = stemp.getInstructionStatus();
            }
        }
        return instructionStatus;
    }

    public void closeAndRemoveFile(String bluetoothAddress) {
        // if (mEnableLogging == true && mLogShimmer.get(bluetoothAddress) !=
        // null) {
        // mLogShimmer.get(bluetoothAddress).closeDatabase();
        // mLogShimmer.remove(bluetoothAddress);
        // }
    }

    public double getFWVersion(String bluetoothAddress) {
        double version = 0;
        Shimmer stemp = (Shimmer) mMultiShimmer.get(bluetoothAddress);
        if (stemp != null) {
            version = stemp.getFirmwareVersion();
        }
        return version;
    }

    public void test() {
        Log.d("ShimmerTest", "Test");
    }

    public void logData(ObjectCluster objectCluster) {

        ObjectCluster objectClusterLog = objectCluster;
        try {
            if (mFirstWrite == true) {
                // First retrieve all the unique keys from the objectClusterLog
                Multimap<String, FormatCluster> m = objectClusterLog.mPropertyCluster;

                mSensorNames = new String[m.size()];
                mSensorFormats = new String[m.size()];
                mSensorUnits = new String[m.size()];
                int i = 0;
                int p = 0;
                for (String key : m.keys()) {
                    // first check that there are no repeat entries
                    if (compareStringArray(mSensorNames, key) == true) {
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

                Collection<FormatCluster> dataFormats = objectClusterLog.mPropertyCluster
                        .get(mSensorNames[r]);
                FormatCluster formatCluster = (FormatCluster) returnFormatCluster(dataFormats, mSensorFormats[r], mSensorUnits[r]); // retrieve the calibrated data
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
                // do the calculation to get label as standing or sitting

                ArrayList<Double> D = new ArrayList<Double>();
                D.clear();
                for (int i = 0; i < 2; i++) {
                    double sumResult = 0.0;
                    for (int j = 0; j < 5; j++) {
                        sumResult = sumResult
                                + Math.pow(
                                ((double) ((tempList.get(j) - gmeans[i][j]) / S[j])),
                                2.0);
                    }
                    D.add((-0.69314718) - 0.5 * (sumResult + logDetSigma));
                }
                if (D.get(0) > D.get(1)) {
                    isStanding = "sit";
                } else {
                    isStanding = "stand";
                }

                if (resultFirstWrite == true) {
                    DBLog log = DatabaseHelper.getInstance(getApplicationContext()).getLastLog();

                    if (log != null) {
                        // after changing something in the log_begin_newday, it
                        // should never come to this blog
                        if (log.getAction().equals("begin_day")) {

                            resultFirstWrite = false;
                            startTime = new Date();
                            previousResult = isStanding;
                            delta_t = 0;
                            achievedScore = 0.0;
                            previous_sit_achievedScore = 0.0;
                            DatabaseHelper.getInstance(getApplicationContext()).addLog(new DBLog(isStanding, new Date(), null));

                        } else if (log.getAction().equals("sit_overtime")) {

                            try {
                                resultFirstWrite = false;
                                firstWriteSitOvertime = false;
                                previousResult = "sit";
                                DBLog lastsitstandlog = DatabaseHelper.getInstance(getApplicationContext()).getLastSitStand();

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
                                previousResult = log.getAction();
                                startTime = log.getDatetime();
                                achievedScore = Double.parseDouble(metadata[1]);
                                previous_sit_achievedScore = 0.0;
                                long sit_overtime_ms_from70 = startTime.getTime()
                                        + _30mn_to_ms;
                                if (log.getAction().equals("sit")
                                        && System.currentTimeMillis() > sit_overtime_ms_from70) {
                                    Calendar calendar = Calendar.getInstance();
                                    calendar.setTimeInMillis(sit_overtime_ms_from70);
                                    firstWriteSitOvertime = false;
                                    delta_t = _30mn_to_ms;
                                    previous_sit_achievedScore = (ppmax * delta_t) / _30mn_to_ms;
                                    achievedScore += previous_sit_achievedScore;
                                    DatabaseHelper.getInstance(getApplicationContext()).addLog(new DBLog("sit_overtime", new Date(), delta_t + "|" + achievedScore));
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
                        DatabaseHelper.getInstance(getApplicationContext()).addLog(new DBLog(isStanding, new Date(), delta_t + "|" + achievedScore));
                    }

                } else { // resultFirstWrite = false

                    if (!previousResult.equals(isStanding)) {
                        System.out.println("isstanding:" + isStanding);
                        // set firstWroteSitOvertime back to true
                        firstWriteSitOvertime = true;
                        warningSound = true;
                        // if (isStanding.equals("sit")) {
                        // configurationAlert(isStanding,
                        // R.drawable.icon_sitting_green, "alert");
                        // } else {
                        // configurationAlert(isStanding,
                        // R.drawable.icon_standing_green, "pebbles");
                        // }
                        // calculate achieved score and log

                        delta_t = System.currentTimeMillis() - startTime.getTime();
                        if (isStanding.equals("sit")) {
                            achievedScore += (double) (ppmax * delta_t)
                                    / (_30mn_to_ms);
                        } else {
                            if (delta_t <= _30mn_to_ms) {
                                achievedScore += (double) ((ppmax * delta_t) / (_30mn_to_ms));
                            } else {
                                double temp_score = (double) (Math.pow(
                                        (1440.0 * 60000.0 / delta_t) - 1.0, k) * (100.0 / Math
                                        .pow(47.0, k)))
                                        - previous_sit_achievedScore;
                                achievedScore += temp_score;
                                previous_sit_achievedScore = 0.0;
                            }
                        }

                        // user change state
                        previousResult = isStanding;
                        startTime = new Date();
                        DatabaseHelper.getInstance(getApplicationContext()).addLog(new DBLog(isStanding, new Date(), delta_t + "|" + achievedScore));
                    }

                    if (firstWriteSitOvertime && isStanding.equals("sit")) {
                        if (System.currentTimeMillis() > startTime.getTime()
                                + _25mn_to_ms
                                && System.currentTimeMillis() < startTime.getTime()
                                + _30mn_to_ms) {
                            if (warningSound) {
                                /*configurationAlert(
                                        "U moet opstaan binnen 5 minuten",
                                        R.drawable.icon_sitting_orange,
                                        "pebbles");*/
                                warningSound = false;
                            }
                        } else if (System.currentTimeMillis() > startTime.getTime()
                                + _30mn_to_ms) {
                            firstWriteSitOvertime = false;
                            delta_t = System.currentTimeMillis() - startTime.getTime();
                            previous_sit_achievedScore = (double) ((ppmax * delta_t) / (_30mn_to_ms));
                            achievedScore += previous_sit_achievedScore;
                            DatabaseHelper.getInstance(getApplicationContext()).addLog(new DBLog("sit_overtime", new Date(), delta_t + "|" + achievedScore));
                            /*configurationAlert("Tijd om recht te staan!",
                                    R.drawable.icon_sitting_red, "alert");*/
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

    private boolean compareStringArray(String[] stringArray, String string) {
        boolean uniqueString = true;
        int size = stringArray.length;
        for (int i = 0; i < size; i++) {
            if (stringArray[i] == string) {
                uniqueString = false;
            }

        }
        return uniqueString;
    }

    private FormatCluster returnFormatCluster(
            Collection<FormatCluster> collectionFormatCluster, String format,
            String units) {
        Iterator<FormatCluster> iFormatCluster = collectionFormatCluster
                .iterator();
        FormatCluster formatCluster;
        FormatCluster returnFormatCluster = null;

        while (iFormatCluster.hasNext()) {
            formatCluster = (FormatCluster) iFormatCluster.next();
            if (formatCluster.mFormat == format
                    && formatCluster.mUnits == units) {
                returnFormatCluster = formatCluster;
            }
        }
        return returnFormatCluster;
    }
}