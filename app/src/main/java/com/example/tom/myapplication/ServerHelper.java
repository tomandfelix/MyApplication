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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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
                int id = 0;
                String line;
                if((line = in.readLine()) != null) {
                    id = Integer.parseInt(line);
                }
                in.close();
                Log.d("POST", "reply= " + id);
                dbh.storeProfile(new Profile(id , params[0], params[1], params[2], params[3], 0, 0));
            } catch (ClientProtocolException e) {
                Log.e("POST", "POST failed");
            } catch (IOException e) {
                Log.e("POST", "POST failed");
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
            try {
                /*JSONObject query = new JSONObject();
                obj.put("username"), */
                List<NameValuePair> values = new ArrayList<NameValuePair>(5);
                values.add(new BasicNameValuePair("username", params[0]));
                values.add(new BasicNameValuePair("password", params[1]));
                post.setEntity(new UrlEncodedFormEntity(values));
                HttpResponse response = client.execute(post);
                BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String line;
                JSONObject json = new JSONObject();
                if((line = in.readLine()) != null) {
                    json = new JSONObject(line);
                }
                in.close();
                Log.d("GetProfile", json.getString("id"));
                Log.d("GetProfile", json.getString("firstname"));
                Log.d("GetProfile", json.getString("lastname"));
                Log.d("GetProfile", json.getString("email"));
                Log.d("GetProfile", json.getString("money"));
                Log.d("GetProfile", json.getString("experience"));
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
