//v0.2 -  8 January 2013

/*
 * Copyright (c) 2010, Shimmer Research, Ltd.
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
 * @date   October, 2013
 */

//Future updates needed
//- the handler should be converted to static

package com.tomandfelix.stapp2.service;



import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.tomandfelix.stapp2.driver.*;
import com.tomandfelix.stapp2.persistency.DatabaseHelper;
import com.tomandfelix.stapp2.persistency.IdLog;
import com.tomandfelix.stapp2.persistency.ServerHelper;
import com.tomandfelix.stapp2.tools.Logging;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class ShimmerService extends Service {
    private static ShimmerService uniqueInstance;
    private static final String TAG = "MyService";
    private static final boolean mEnableLogging=true;
    public HashMap<String, Object> mMultiShimmer = new HashMap<>(7);
    private static String address = "";
    private int retryAmount = 0;
    private Handler uploadHandler;
    private Runnable uploadCheck;
    private int uploadFrequency;
    public static final int REGISTER_MESSENGER = 0;
    public static final int UPLOAD_DATA = 1;
    public static final int DOWNLOAD_DATA = 2;
    public static final int CONNECT = 3;
    public static final int DISCONNECT = 4;
    public static final int REQUEST_STATE = 5;
    public static final int LOG_START_DAY = 6;
    public static final int LOG_ACHIEVED_SCORE = 7;

    private static Messenger toApp;
    private static class AppHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case REGISTER_MESSENGER:
                    Log.d("ShimmerService", "received messenger");
                    toApp = msg.replyTo;
                    break;
                case UPLOAD_DATA:
                    uniqueInstance.uploadData();
                    break;
                case DOWNLOAD_DATA:
                    uniqueInstance.downloadData();
                    break;
                case CONNECT:
                    uniqueInstance.connectShimmer(msg.getData().getString("address"), "Device");
                    break;
                case DISCONNECT:
                    uniqueInstance.disconnectShimmer();
                    break;
                case REQUEST_STATE:
                    uniqueInstance.sendToApp(Logging.getInstance(uniqueInstance).getState());
                    break;
                case LOG_START_DAY:
                    Logging.getInstance(uniqueInstance).logStartDay();
                    break;
                case LOG_ACHIEVED_SCORE:
                    Logging.getInstance(uniqueInstance).logAchievedScore();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    private final Messenger mMessenger = new Messenger(new AppHandler());

    public void sendToApp(int state) {
        if(toApp != null) {
            try {
                toApp.send(Message.obtain(null, state));
            } catch (RemoteException e) {
                toApp = null;
            }
        }
    }

    public void tryReconnect() {
        if(address.equals("")) {
            disconnectAllDevices();
        } else if (retryAmount < 5) {
            Log.e("RETRY", Integer.toString(retryAmount));
            retryAmount++;
            connectShimmer(address, "Device");
        } else {
            Log.e("RETRY", "waiting 30 seconds");
            Handler handler = new Handler(Looper.getMainLooper());
            final Runnable r = new Runnable() {
                public void run() {
                    retryAmount = 0;
                    tryReconnect();
                }
            };
            handler.postDelayed(r, 30000);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        uniqueInstance = this;
        uploadFrequency = DatabaseHelper.getInstance().getUploadFrequency();
        uploadHandler = new Handler();
        uploadCheck = new Runnable() {
            @Override
            public void run() {
                uploadHandler.postDelayed(uploadCheck, uploadFrequency);
                uploadData();
            }
        };
        Log.d(TAG, "onCreate");
        if(DatabaseHelper.getInstance().dayStarted() == null) {
            address = "";
        }
    }

    private void uploadData() {
        NetworkInfo wifi = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifi.isConnected() || DatabaseHelper.getInstance().uploadOn3G()) {
            ArrayList<IdLog> logs = DatabaseHelper.getInstance().getLogsToUpload();
            if(logs != null) {
                Log.d("uploadData", "uploading records " + logs.get(0).getId() + "->" + logs.get(logs.size() - 1).getId());
                final boolean more = logs.size() == 1000;
                ServerHelper.getInstance().uploadLogs(logs, new ServerHelper.ResponseFunc<Integer>() {
                    @Override
                    public void onResponse(Integer response) {
                        DatabaseHelper.getInstance().confirmUpload(response);
                        if(more) {
                            uploadData();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        if(volleyError.getMessage().equals("token")) {
                            // TODO password prompt
                        } else {
                            Log.e("uploadData", volleyError.getMessage());
                        }
                    }
                });
            } else {
                int state = Logging.getInstance(this).getState();
                if(state == Logging.STATE_CONNECTING || state == Logging.STATE_DISCONNECTED || state == Logging.STATE_DAY_STOPPED) {
                    stopUploadTask();
                }
            }
        }
    }

    private void downloadData() {
        IdLog last = DatabaseHelper.getInstance().getLastLog();
        ServerHelper.getInstance().downloadLogs(last == null ? 0 : last.getId(), new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                if(volleyError.getMessage().equals("none")) {
                    Logging.getInstance(ShimmerService.this).checkDB();
                    uploadData();
                } else {
                    Log.e("downloadData", volleyError.getMessage());
                }
            }
        });
    }

    private void startUploadTask() {
        Log.d("uploadTask", "START");
        uploadHandler.removeCallbacks(uploadCheck);
        uploadCheck.run();
    }

    private void stopUploadTask() {
        Log.d("uploadTask", "STOP");
        uploadHandler.removeCallbacks(uploadCheck);
    }

    @Override
    public void onDestroy() {
//        Toast.makeText(this, "My Service Stopped", Toast.LENGTH_LONG).show();
        Log.d(TAG, "onDestroy");
        Collection<Object> colS=mMultiShimmer.values();
        for(Object o : colS) {
            ((Shimmer) o).stop();
        }

    }

    public void disconnectAllDevices(){
        Collection<Object> colS=mMultiShimmer.values();
        for(Object o : colS) {
            ((Shimmer) o).stop();
        }
        mMultiShimmer.clear();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("LocalService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        if(DatabaseHelper.getInstance().dayStarted() != null) {
            if(!address.equals("")) {
                if(!DevicesConnected(address)) {
                    connectShimmer(address, "Device");
                } else if(!DeviceIsStreaming(address)) {
                    startStreaming(address);
                }

            }
        }
        return START_STICKY;
    }

    @Override
    public void onStart(Intent intent, int startid) {
//        Toast.makeText(this, "My Service Started", Toast.LENGTH_LONG).show();

        Log.d(TAG, "onStart");

    }

    public void connectShimmer(String bluetoothAddress,String selectedDevice) {
        Log.d("Shimmer","net Connection");
        Logging.getInstance(this).logConnecting();
        address = bluetoothAddress;
        Shimmer shimmerDevice=new Shimmer(this, mHandler,selectedDevice,false);
        mMultiShimmer.remove(bluetoothAddress);
        if (mMultiShimmer.get(bluetoothAddress)==null){
            mMultiShimmer.put(bluetoothAddress,shimmerDevice);
            ((Shimmer) mMultiShimmer.get(bluetoothAddress)).connect(bluetoothAddress);
        }
    }

    public void disconnectShimmer() {
        address = "";
        Logging.getInstance(this).logDisconnect();
        stopStreamingAllDevices();
    }

    public void onStop(){
//        Toast.makeText(this, "My Service Stopped", Toast.LENGTH_LONG).show();
        Log.d(TAG, "onDestroy");
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            stemp.stop();
        }
    }

    public final Handler mHandler = new ShimmerHandler(this);

    private static class ShimmerHandler extends Handler {
        private WeakReference<ShimmerService> mShimmerService;

        public ShimmerHandler(ShimmerService aShimmerService) {
            mShimmerService = new WeakReference<>(aShimmerService);
        }

        @Override
        public void handleMessage(Message msg) {
            if(mShimmerService.get() != null) {
                switch (msg.what) { // handlers have a what identifier which is used to identify the type of msg
                    case Shimmer.MESSAGE_READ:
                        if ((msg.obj instanceof ObjectCluster)) {    // within each msg an object can be include, objectclusters are used to represent the data structure of the shimmer device
                            ObjectCluster objectCluster = (ObjectCluster) msg.obj;
                            if (mEnableLogging) {
                                Logging.getInstance(mShimmerService.get()).logData(objectCluster);
                            }
                        }
                        break;
                    case Shimmer.MESSAGE_TOAST:
                        Log.d("toast", msg.getData().getString(Shimmer.TOAST));
                        if (msg.getData().getString(Shimmer.TOAST).equals("Device connection was lost") ||
                                msg.getData().getString(Shimmer.TOAST).contains("stopped streaming") ||
                                msg.getData().getString(Shimmer.TOAST).contains("Killing Connection")) {
                            Logging.getInstance(mShimmerService.get()).logDisconnect();
                            mShimmerService.get().tryReconnect();
                        } else if (msg.getData().getString(Shimmer.TOAST).contains("is now Streaming")) {
                            Logging.getInstance(mShimmerService.get()).logConnect();
                            mShimmerService.get().startUploadTask();
                            mShimmerService.get().retryAmount = 0;
                        } else if (msg.getData().getString(Shimmer.TOAST).contains("is ready for Streaming")) {
                            mShimmerService.get().startStreamingAllDevices();
                        } else if (msg.getData().getString(Shimmer.TOAST).equals("Unable to connect device")) {
                            mShimmerService.get().tryReconnect();
                        }
                        break;
                    case Shimmer.MESSAGE_STATE_CHANGE:
                        Intent intent = new Intent("com.shimmerresearch.service.ShimmerService");
                        Log.d("ShimmerGraph", "Sending");
                        switch (msg.arg1) {
                            case Shimmer.STATE_CONNECTED:
                                Log.d("Shimmer", ((ObjectCluster) msg.obj).mBluetoothAddress + "  " + ((ObjectCluster) msg.obj).mMyName);

                                intent.putExtra("ShimmerBluetoothAddress", ((ObjectCluster) msg.obj).mBluetoothAddress);
                                intent.putExtra("ShimmerDeviceName", ((ObjectCluster) msg.obj).mMyName);
                                intent.putExtra("ShimmerState", Shimmer.STATE_CONNECTED);
                                mShimmerService.get().sendBroadcast(intent);
                                break;
                            case Shimmer.STATE_CONNECTING:
                                intent.putExtra("ShimmerBluetoothAddress", ((ObjectCluster) msg.obj).mBluetoothAddress);
                                intent.putExtra("ShimmerDeviceName", ((ObjectCluster) msg.obj).mMyName);
                                intent.putExtra("ShimmerState", Shimmer.STATE_CONNECTING);
                                break;
                            case Shimmer.STATE_NONE:
                                intent.putExtra("ShimmerBluetoothAddress", ((ObjectCluster) msg.obj).mBluetoothAddress);
                                intent.putExtra("ShimmerDeviceName", ((ObjectCluster) msg.obj).mMyName);
                                intent.putExtra("ShimmerState", Shimmer.STATE_NONE);
                                mShimmerService.get().sendBroadcast(intent);
                                break;
                        }
                        break;

                    case Shimmer.MESSAGE_STOP_STREAMING_COMPLETE:
                        break;
                }
            }
        }
    }


    private void stopStreamingAllDevices() {
        // TODO Auto-generated method stub
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;

            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                stemp.stopStreaming();

            }
        }
    }

    public void startStreamingAllDevices() {
        // TODO Auto-generated method stub
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                stemp.startStreaming();
            }
        }
    }

    public void setAllSampingRate(double samplingRate) {
        // TODO Auto-generated method stub
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                stemp.writeSamplingRate(samplingRate);
            }
        }
    }

    public void setAllAccelRange(int accelRange) {
        // TODO Auto-generated method stub
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                stemp.writeAccelRange(accelRange);
            }
        }
    }

    public void setAllGSRRange(int gsrRange) {
        // TODO Auto-generated method stub
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                stemp.writeGSRRange(gsrRange);
            }
        }
    }

    public void setAllEnabledSensors(int enabledSensors) {
        // TODO Auto-generated method stub
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                stemp.writeEnabledSensors(enabledSensors);
            }
        }
    }


    public void setEnabledSensors(int enabledSensors,String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.writeEnabledSensors(enabledSensors);
            }
        }
    }

    public void toggleLED(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.toggleLed();
            }
        }
    }

    public void writePMux(String bluetoothAddress,int setBit) {
        // TODO Auto-generated method stub
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.writePMux(setBit);
            }
        }
    }

    public void write5VReg(String bluetoothAddress,int setBit) {
        // TODO Auto-generated method stub
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.writeFiveVoltReg(setBit);
            }
        }
    }




    public int getEnabledSensors(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS=mMultiShimmer.values();
        int enabledSensors=0;
        for(Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState()==Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)){
                enabledSensors = stemp.getEnabledSensors();
            }
        }
        return enabledSensors;
    }


    public void writeSamplingRate(String bluetoothAddress,double samplingRate) {
        // TODO Auto-generated method stub
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.writeSamplingRate(samplingRate);
            }
        }
    }

    public void writeAccelRange(String bluetoothAddress,int accelRange) {
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.writeAccelRange(accelRange);
            }
        }
    }

    public void writeGyroRange(String bluetoothAddress,int range) {
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.writeGyroRange(range);
            }
        }
    }

    public void writePressureResolution(String bluetoothAddress,int resolution) {
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                //currently not supported
                stemp.writePressureResolution(resolution);
            }
        }
    }

    public void writeMagRange(String bluetoothAddress,int range) {
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.writeMagRange(range);
            }
        }
    }

    public void writeGSRRange(String bluetoothAddress,int gsrRange) {
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.writeGSRRange(gsrRange);
            }
        }
    }


    public double getSamplingRate(String bluetoothAddress) {
        // TODO Auto-generated method stub

        Collection<Object> colS=mMultiShimmer.values();
        double SRate=-1;
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState()==Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)){
                SRate= stemp.getSamplingRate();
            }
        }
        return SRate;
    }

    public int getAccelRange(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS=mMultiShimmer.values();
        int aRange=-1;
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState()==Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)){
                aRange = stemp.getAccelRange();
            }
        }
        return aRange;
    }

    public int getShimmerState(String bluetoothAddress){

        // TODO Auto-generated method stub
        Collection<Object> colS=mMultiShimmer.values();
        int status=-1;
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getBluetoothAddress().equals(bluetoothAddress)){
                status = stemp.getShimmerState();
                Log.d("ShimmerState",Integer.toString(status));
            }
        }
        return status;

    }

    public int getGSRRange(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS=mMultiShimmer.values();
        int gRange=-1;
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState()==Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)){
                gRange = stemp.getGSRRange();
            }
        }
        return gRange;
    }

    public int get5VReg(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS=mMultiShimmer.values();
        int fiveVReg=-1;
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState()==Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)){
                fiveVReg = stemp.get5VReg();
            }
        }
        return fiveVReg;
    }

    public boolean isLowPowerMagEnabled(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS=mMultiShimmer.values();
        boolean enabled=false;
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState()==Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)){
                enabled = stemp.isLowPowerMagEnabled();
            }
        }
        return enabled;
    }


    public int getpmux(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS=mMultiShimmer.values();
        int pmux=-1;
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState()==Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)){
                pmux = stemp.getPMux();
            }
        }
        return pmux;
    }


    public void startStreaming(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState()==Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)){
                stemp.startStreaming();
            }
        }
    }

    public int sensorConflictCheckandCorrection(String bluetoothAddress, int enabledSensors, int sensorToCheck) {
        // TODO Auto-generated method stub
        int newSensorBitmap = 0;
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState()==Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)){
                newSensorBitmap = stemp.sensorConflictCheckandCorrection(enabledSensors,sensorToCheck);
            }
        }
        return newSensorBitmap;
    }
    public List<String> getListofEnabledSensors(String bluetoothAddress) {
        // TODO Auto-generated method stub
        List<String> listofSensors = null;
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState()==Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)){
                listofSensors = stemp.getListofEnabledSensors();
            }
        }
        return listofSensors;
    }




    public void stopStreaming(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState()==Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)){
                stemp.stopStreaming();
            }
        }
    }

    public void setBlinkLEDCMD(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Shimmer stemp=(Shimmer) mMultiShimmer.get(bluetoothAddress);
        if (stemp!=null){
            if (stemp.getShimmerState()==Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)){
                if (stemp.getCurrentLEDStatus()==0){
                    stemp.writeLEDCommand(1);
                } else {
                    stemp.writeLEDCommand(0);
                }
            }
        }

    }

    public void enableLowPowerMag(String bluetoothAddress,boolean enable) {
        // TODO Auto-generated method stub
        Shimmer stemp=(Shimmer) mMultiShimmer.get(bluetoothAddress);
        if (stemp!=null){
            if (stemp.getShimmerState()==Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)){
                stemp.enableLowPowerMag(enable);
            }
        }
    }


    public void setBattLimitWarning(String bluetoothAddress, double limit) {
        // TODO Auto-generated method stub
        Shimmer stemp=(Shimmer) mMultiShimmer.get(bluetoothAddress);
        if (stemp!=null){
            if (stemp.getBluetoothAddress().equals(bluetoothAddress)){
                stemp.setBattLimitWarning(limit);
            }
        }

    }


    public double getBattLimitWarning(String bluetoothAddress) {
        // TODO Auto-generated method stub
        double limit=-1;
        Shimmer stemp=(Shimmer) mMultiShimmer.get(bluetoothAddress);
        if (stemp!=null){
            if (stemp.getBluetoothAddress().equals(bluetoothAddress)){
                limit=stemp.getBattLimitWarning();
            }
        }
        return limit;
    }


    public double getPacketReceptionRate(String bluetoothAddress) {
        // TODO Auto-generated method stub
        double rate=-1;
        Shimmer stemp=(Shimmer) mMultiShimmer.get(bluetoothAddress);
        if (stemp!=null){
            if (stemp.getBluetoothAddress().equals(bluetoothAddress)){
                rate=stemp.getPacketReceptionRate();
            }
        }
        return rate;
    }


    public void disconnectShimmer(String bluetoothAddress){
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState()==Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)){
                stemp.stop();
            }
        }
        mMultiShimmer.remove(bluetoothAddress);

    }

    public boolean DevicesConnected(String bluetoothAddress){
        boolean deviceConnected=false;
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState()==Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)){
                deviceConnected=true;
            }
        }
        return deviceConnected;
    }

    public boolean DeviceIsStreaming(String bluetoothAddress){
        boolean deviceStreaming=false;
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getStreamingStatus()  && stemp.getBluetoothAddress().equals(bluetoothAddress)){
                deviceStreaming=true;
            }
        }
        return deviceStreaming;
    }

    public boolean GetInstructionStatus(String bluetoothAddress){
        boolean instructionStatus=false;
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getBluetoothAddress().equals(bluetoothAddress)){
                instructionStatus=stemp.getInstructionStatus();
            }
        }
        return instructionStatus;
    }

    public String getFWVersion (String bluetoothAddress){
        String version="";
        Shimmer stemp=(Shimmer) mMultiShimmer.get(bluetoothAddress);
        if (stemp!=null){
            version=stemp.getFirmwareMajorVersion()+"."+stemp.getFirmwareMinorVersion();
        }
        return version;
    }

    public int getShimmerVersion (String bluetoothAddress){
        int version=0;
        Shimmer stemp=(Shimmer) mMultiShimmer.get(bluetoothAddress);
        if (stemp!=null){
            version=stemp.getShimmerVersion();
        }
        return version;
    }


    public Shimmer getShimmer(String bluetoothAddress){
        // TODO Auto-generated method stub
        Shimmer shimmer = null;
        Collection<Object> colS=mMultiShimmer.values();
        for (Object col : colS) {
            Shimmer stemp = (Shimmer) col;
            if (stemp.getShimmerState()==Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)){
                shimmer = stemp;
            }
        }
        return shimmer;
    }

    public void test(){
        Log.d("ShimmerTest","Test");
    }
}
