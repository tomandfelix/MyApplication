package com.example.tom.myapplication;

import android.os.AsyncTask;
import android.util.Log;

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
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tom on 20/10/2014.
 */
public class ServerHelper {
    private DatabaseHelper dbh;

    public ServerHelper(DatabaseHelper dbh) {
        this.dbh = dbh;
    }

    public void createProfile(String firstName, String lastName, String username, String email, String password) {
        Log.d("ServerHelper", "storing profile");
        new CreateProfile().execute(firstName, lastName, username, email, password);
    }

    private class CreateProfile extends AsyncTask<String, Integer, Double> {
        @Override
        protected Double doInBackground(String... params) {
            Log.d("ASYNC", "Started");
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://eng.studev.groept.be/web2.0/a14_web02/Tom/createProfile.php");
            try {
                List<NameValuePair> values = new ArrayList<NameValuePair>(5);
                values.add(new BasicNameValuePair("firstname", params[0]));
                values.add(new BasicNameValuePair("lastname", params[1]));
                values.add(new BasicNameValuePair("username", params[2]));
                values.add(new BasicNameValuePair("email", params[3]));
                values.add(new BasicNameValuePair("password", params[4]));
                post.setEntity(new UrlEncodedFormEntity(values));
                HttpResponse response = client.execute(post);
                BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                StringBuffer sb = new StringBuffer("");
                String line;
                if((line = in.readLine()) != null) {
                    sb.append(line);
                }
                in.close();
                dbh.storeProfile(new Profile(Integer.parseInt(sb.toString()), params[0], params[1], params[2], params[3]));
                Log.d("POST", "reply= " + sb.toString());
            } catch (ClientProtocolException e) {
                Log.e("POST", "POST failed");
            } catch (IOException e) {
                Log.e("POST", "POST failed");
            }
            return null;
        }
    }
}
