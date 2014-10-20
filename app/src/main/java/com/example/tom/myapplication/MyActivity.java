package com.example.tom.myapplication;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MyActivity extends Activity {

    DatabaseHelper dbh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        dbh = new DatabaseHelper(this);
        Seocnd detectconnection = new Seocnd(getApplicationContext());
        if (detectconnection.InternetConnecting()) {
            Log.e("INTERNET", "Internet available");
        } else {
            Log.e("INTERNET", "No internet available");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void submitProfile(View v) {
        Log.d("submitProfile", "storing profile");
        EditText firstName = (EditText) findViewById(R.id.firstName);
        EditText lastName = (EditText) findViewById(R.id.lastName);
        EditText username = (EditText) findViewById(R.id.username);
        EditText email = (EditText) findViewById(R.id.email);
        Profile profile = new Profile(/*Integer.parseInt(id.getText().toString())*/ 0, firstName.getText().toString(), lastName.getText().toString(), username.getText().toString(), email.getText().toString());
        dbh.storeProfile(profile);

        EditText password = (EditText) findViewById(R.id.password);
        /*String data = "";
        try {
            data = URLEncoder.encode("firstname", "UTF-8") + "=" + URLEncoder.encode(firstName.getText().toString(), "UTF-8");
            data += "&" +  URLEncoder.encode("lastname", "UTF-8") + "=" + URLEncoder.encode(lastName.getText().toString(), "UTF-8");
            data += "&" + URLEncoder.encode("username", "UTF-8") + "=" + URLEncoder.encode(username.getText().toString(), "UTF-8");
            data += "&" + URLEncoder.encode("email", "UTF-8") + "=" + URLEncoder.encode(email.getText().toString(), "UTF-8");
            data += "&" + URLEncoder.encode("password", "UTF-8") + "=" + URLEncoder.encode(password.getText().toString(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Log.d("POST", "Encoding failed");
        }
        String text = "";
        BufferedReader reader = null;

        try {
            URL url = new URL("http://eng.studev.groept.be/web2.0/a14_web02/Tom/post.php");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            text = sb.toString();
        } catch (Exception ex) {
            Log.d("POST", "Exception occured");
        } finally {
            try {
                reader.close();
            } catch (Exception ex) {
                Log.d("POST", "Reader couldn't close");
            }
        }
        Log.d("POST", "OUTPUT: " + text);*/

        new MyAsyncTask().execute(firstName.getText().toString(), lastName.getText().toString(), username.getText().toString(), email.getText().toString(), password.getText().toString());

        DBLog log = new DBLog(0, "storing profile", new Date());
        dbh.addLog(log);
    }

    private class MyAsyncTask extends AsyncTask<String, Integer, Double> {
        @Override
        protected Double doInBackground(String... params) {
            Log.d("ASYNC", "Started");
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://eng.studev.groept.be/web2.0/a14_web02/Tom/post.php");
            try {
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(5);
                nameValuePairs.add(new BasicNameValuePair("firstname", params[0]));
                nameValuePairs.add(new BasicNameValuePair("lastname", params[1]));
                nameValuePairs.add(new BasicNameValuePair("username", params[2]));
                nameValuePairs.add(new BasicNameValuePair("email", params[3]));
                nameValuePairs.add(new BasicNameValuePair("password", params[4]));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                HttpResponse response = httpclient.execute(httppost);
            } catch (ClientProtocolException e) {
                Log.e("POST", "POST failed");
            } catch (IOException e) {
                Log.e("POST", "POST failed");
            }
            return null;
        }
    }

}
