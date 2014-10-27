package com.example.tom.myapplication;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Tom on 20/10/2014.
 * Contains the asynchronous helper methods for communicating with the server
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

    public void getProfile(String username, String password) {
        Log.d("ServerHelper", "getting profile");
        new GetProfile().execute(username, password);
    }

    private class CreateProfile extends AsyncTask<String, Integer, Double> {
        @Override
        protected Double doInBackground(String... params) {
            Log.d("ASYNC", "CreateProfile started");
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://eng.studev.groept.be/thesis/a14_stapp2/createProfile.php");
            JSONObject query = new JSONObject();
            try {
                query.put("firstname", params[0]);
                query.put("lastname", params[1]);
                query.put("username", params[2]);
                query.put("email", params[3]);
                query.put("password", params[4]);
                post.setEntity(new StringEntity(query.toString()));
                HttpResponse response = client.execute(post);
                BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String line;
                JSONObject result = new JSONObject();
                if((line = in.readLine()) != null) {
                    result = new JSONObject(line);
                }
                int id = result.getInt("id");
                in.close();
                Log.d("POST", "reply= " + id);
                dbh.storeProfile(new Profile(id , params[0], params[1], params[2], params[3], 0, 0));
            } catch (ClientProtocolException e) {
                Log.e("CreateProfile", "Error: ClientProtocolException");
            } catch (IOException e) {
                Log.e("CreateProfile", "Error: IOException");
            } catch (JSONException e) {
                Log.e("CreateProfile", "JSON error");
            }
            return null;
        }
    }

    private class GetProfile extends AsyncTask<String, Integer, Double> {
        @Override
        protected Double doInBackground(String... params) {
            Log.d("ASYNC", "GetProfile started");
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://eng.studev.groept.be/thesis/a14_stapp2/getProfile.php");
            JSONObject query = new JSONObject();
            try {
                query.put("username", params[0]);
                query.put("password", params[1]);
                post.setEntity(new StringEntity(query.toString()));
                HttpResponse response = client.execute(post);
                BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String line;
                JSONObject result = new JSONObject();
                if ((line = in.readLine()) != null) {
                    result = new JSONObject(line);
                }
                in.close();
                Log.d("GetProfile", result.toString());
                dbh.storeProfile(new Profile(result.getInt("id") , result.getString("firstname"), result.getString("lastname"), params[0], result.getString("email"), result.getInt("money"), result.getInt("experience")));
            } catch (ClientProtocolException e) {
                Log.e("GetProfile", "Error: ClientProtocolException");
            } catch (IOException e) {
                Log.e("GetProfile", "Error: IOException");
            } catch (JSONException e) {
                Log.e("GetProfile", "JSON error");
            }
            return null;
        }
    }
}
