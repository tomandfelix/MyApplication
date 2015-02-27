package com.example.tom.stapp3.activity;


import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.net.*;
import java.io.*;
import java.util.Collections;
import java.util.List;

import com.example.tom.stapp3.R;

import org.apache.http.conn.util.InetAddressUtils;

public class Internet_Connection extends DrawerActivity {
    private DatagramReceiver myReceiver = null;

    @Override
    protected void onResume() {
        super.onResume();
        myReceiver = new DatagramReceiver();
        myReceiver.bKeepRunning = true;
        myReceiver.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        myReceiver.kill();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_internet__connection);
        index = INTERNET_CONNECTION;
        super.onCreate(savedInstanceState);
        final Button button = (Button) findViewById(R.id.send_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("in button click","click");
                EditText messageSend = (EditText) findViewById(R.id.send_data);
                DatagramSender mySender = new DatagramSender(messageSend.getText().toString());
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
                mySender.run();
            }
        });
        String iPAddress = getIPAddress(true);
        ((TextView) findViewById(R.id.ip_addr)).setText(iPAddress);
    }

    private Runnable updateTextMessage = new Runnable() {
        public void run() {
            ((TextView) findViewById(R.id.received_data)).setText(myReceiver.getMessage());
        }
    };

    private class DatagramReceiver extends Thread {
        private boolean bKeepRunning = true;
        private String lastMessage = "";

        @Override
        public void run() {
            String text;
            int server_port = 4567;
            byte[] message = new byte[1500];
            DatagramPacket p = new DatagramPacket(message, message.length);
            try {
                DatagramSocket s = new DatagramSocket(server_port);
                while (bKeepRunning) {
                    s.receive(p);
                    text = new String(message, 0, p.getLength());
                    lastMessage = text;
                    Log.d("Last message ", lastMessage);
                    runOnUiThread(updateTextMessage);
                }
                if (bKeepRunning) {
                    Log.d("bKeepRunning ", "true");
                } else {
                    Log.d("bKeepRunning", "false");
                }
                if (s != null) {
                    s.close();
                }
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SocketException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public String getMessage() {
            return lastMessage;
        }

        public void kill() {
            bKeepRunning = false;
        }
    }

    private class DatagramSender implements Runnable {
        public Handler mHandler;
        private String data;

        public DatagramSender(String data){
            this.data = data;
        }

        @Override
        public void run() {
            try{
                Log.d("in DatagramSender","run");
                String messageStr = data;
                String host = "10.0.2.3";
                int server_port = 4567;
                DatagramSocket s = new DatagramSocket();
                InetAddress address = InetAddress.getByName(host);
                int msg_length = messageStr.length();
                byte[] message = messageStr.getBytes();
                DatagramPacket p = new DatagramPacket(message, msg_length, address, server_port);
                s.send(p);

            } catch (IOException iOe) {
                Log.d("IOException", "error");
            }

        }
    }
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                                return delim<0 ? sAddr : sAddr.substring(0, delim);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.d("error","Exception");
        }
        return "";
    }
}

