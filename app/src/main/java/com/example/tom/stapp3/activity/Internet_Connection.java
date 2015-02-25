package com.example.tom.stapp3.activity;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.net.*;
import java.io.*;

import com.example.tom.stapp3.R;

public class Internet_Connection extends Activity  {

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_internet__connection);
        new GetUDPData().execute();
        final Button button = (Button) findViewById(R.id.send_button);
        button.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {
                try {
                    
                    String messageStr = "Het werkt :)";
                    String host = "10.166.162.88";
                    int server_port = 4567;
                    DatagramSocket s = new DatagramSocket();
                    InetAddress address = InetAddress.getByName(host);
                    int msg_length = messageStr.length();
                    byte[] message = messageStr.getBytes();
                    DatagramPacket p = new DatagramPacket(message, msg_length, address, server_port);
                    s.send(p);
                } catch (IOException iOe) {
                    System.out.println("error");
                }
            }
        });
    }
   private class GetUDPData extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... strings) {

            try {
                String text;
                int server_port = 4567;
                byte[] message = new byte[1500];
                DatagramPacket p = new DatagramPacket(message,message.length);
                DatagramSocket s = new DatagramSocket(server_port);
                s.receive(p);
                text = new String(message, 0, p.getLength());
                s.close();
                return text;

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

            return null;
        }
       @Override
       protected void onPostExecute(String result){
            super.onPostExecute(result);
           ((TextView) findViewById(R.id.received_data)).setText(result);
       }
       
    }
    public void sendButton(final View v) {

}