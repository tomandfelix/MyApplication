package com.example.tom.myapplication;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by Tom on 20/10/2014.
 * Contains the asynchronous helper methods for communicating with the server
 *
 * Code to print full reply from server
 */

/*String output = "";
while ((line = in.readLine()) != null) {
    output += line;
}
Log.e("OUTPUT", output);*/

public class ServerHelper {
    private DatabaseHelper dbh;

    public ServerHelper(DatabaseHelper dbh) {
        this.dbh = dbh;
    }

    public void createProfile(String firstName, String lastName, String username, String email, String password) {
        new CreateProfile().execute(firstName, lastName, username, email, password);
    }

    public void getProfile(String username, String password) {
        new GetProfile().execute(username, password);
    }

    public void getOtherProfile(int id) {
        new GetOtherProfile().execute(id);
    }

    public void getLeaderboard(int id) {
        new GetLeaderboard().execute(id);
    }

    public void deleteProfile(int id, String password) {
        new DeleteProfile().execute("" + id, password);
    }

    public void getRank(int id) {
        new GetRank().execute(id);
    }

    private class CreateProfile extends AsyncTask<String, Void, Profile> {
        @Override
        protected Profile doInBackground(String... params) {
            Log.d("ServerHelper", "AsyncTask CreateProfile started");
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://eng.studev.groept.be/thesis/a14_stapp2/createProfile.php");
            JSONObject query = new JSONObject();
            Profile prof = null;
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
                prof = new Profile(id , params[0], params[1], params[2], params[3], 0, 0);
            } catch (ClientProtocolException e) {
                Log.e("CreateProfile", "Error: ClientProtocolException");
            } catch (IOException e) {
                Log.e("CreateProfile", "Error: IOException");
            } catch (JSONException e) {
                Log.e("CreateProfile", "JSON error");
            }
            return prof;
        }

        @Override
        protected void onPostExecute(Profile profile) {
            Log.d("CreateProfile", profile.toString());
            dbh.storeProfile(profile);
        }
    }

    private class GetProfile extends AsyncTask<String, Void, Profile> {
        @Override
        protected Profile doInBackground(String... params) {
            Log.d("ServerHelper", "AsyncTask GetProfile started");
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://eng.studev.groept.be/thesis/a14_stapp2/getProfile.php");
            JSONObject query = new JSONObject();
            Profile prof = null;
            try {
                query.put("username", params[0]);
                query.put("password", params[1]);
                post.setEntity(new StringEntity(query.toString()));
                HttpResponse response = client.execute(post);
                BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String line;
                JSONObject result = new JSONObject();
                while ((line = in.readLine()) != null) {
                    result = new JSONObject(line);
                }
                in.close();
                prof = new Profile(result.getInt("id") , result.getString("firstname"), result.getString("lastname"), params[0], result.getString("email"), result.getInt("money"), result.getInt("experience"));
            } catch (ClientProtocolException e) {
                Log.e("GetProfile", "Error: ClientProtocolException");
            } catch (IOException e) {
                Log.e("GetProfile", "Error: IOException");
            } catch (JSONException e) {
                Log.e("GetProfile", "JSON error");
            }
            return prof;
        }

        @Override
        protected void onPostExecute(Profile profile) {
            Log.d("GetProfile", profile.toString());
            dbh.storeProfile(profile);
        }
    }

    private class GetOtherProfile extends AsyncTask<Integer, Void, Profile> {
        @Override
        protected Profile doInBackground(Integer... params) {
            Log.d("ServerHelper", "AsyncTask GetOtherProfile started");
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://eng.studev.groept.be/thesis/a14_stapp2/getOtherProfile.php");
            JSONObject query = new JSONObject();
            Profile prof = null;
            try {
                query.put("id", params[0].toString());
                post.setEntity(new StringEntity(query.toString()));
                HttpResponse response = client.execute(post);
                BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String line;
                JSONObject result = new JSONObject();
                if ((line = in.readLine()) != null) {
                    result = new JSONObject(line);
                }
                in.close();
                prof = new Profile(params[0] , null, null, result.getString("username"), null, result.getInt("money"), result.getInt("experience"));
            } catch (ClientProtocolException e) {
                Log.e("GetOtherProfile", "Error: ClientProtocolException");
            } catch (IOException e) {
                Log.e("GetOtherProfile", "Error: IOException");
            } catch (JSONException e) {
                Log.e("GetOtherProfile", "JSON error");
            }
            return prof;
        }

        @Override
        protected void onPostExecute(Profile profile) {
            Log.d("GetOtherProfile", profile.toString());
        }
    }

    private class GetLeaderboard extends AsyncTask<Integer, Void, ArrayList<RankedProfile>> {
        @Override
        protected ArrayList<RankedProfile> doInBackground(Integer... params) {
            Log.d("ServerHelper", "AsyncTask GetLeaderboard started");
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://eng.studev.groept.be/thesis/a14_stapp2/getLeaderboard.php");
            JSONObject query = new JSONObject();
            ArrayList<RankedProfile> prof = new ArrayList<RankedProfile>();
            try {
                query.put("id", params[0].toString());
                post.setEntity(new StringEntity(query.toString()));
                HttpResponse response = client.execute(post);
                BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String line;
                JSONArray result = new JSONArray();
                if ((line = in.readLine()) != null) {
                    result = new JSONArray(line);
                }
                in.close();
                for(int i = 0; i < result.length(); i++) {
                    JSONObject temp = result.getJSONObject(i);
                    prof.add(new RankedProfile(temp.getInt("id"), null, null, temp.getString("username"), null, temp.getInt("money"), temp.getInt("experience"), temp.getInt("rank")));
                }
            } catch (ClientProtocolException e) {
                Log.e("GetLeaderboard", "Error: ClientProtocolException");
            } catch (IOException e) {
                Log.e("GetLeaderboard", "Error: IOException");
            } catch (JSONException e) {
                Log.e("GetLeaderboard", "JSON error");
            }
            return prof;
        }

        @Override
        public void onPostExecute(ArrayList<RankedProfile> profiles) {
            for(RankedProfile rProf : profiles) {
                dbh.storeProfile(rProf);
                Log.d("GetLeaderboard", rProf.toString());
            }
        }
    }

    private class DeleteProfile extends AsyncTask<String, Void, Void> {
        @Override
        public Void doInBackground(String... params) {
            Log.d("ServerHelper", "AsyncTask DeleteProfile started");
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://eng.studev.groept.be/thesis/a14_stapp2/deleteProfile.php");
            JSONObject query = new JSONObject();
            try {
                query.put("id", params[0]);
                query.put("password", params[1]);
                post.setEntity(new StringEntity(query.toString()));
                client.execute(post);
            } catch (ClientProtocolException e) {
                Log.e("DeleteProfile", "Error: ClientProtocolException");
            } catch (IOException e) {
                Log.e("DeleteProfile", "Error: IOException");
            } catch (JSONException e) {
                Log.e("DeleteProfile", "JSON error");
            }
            return null;
        }
    }

    private class GetRank extends AsyncTask<Integer, Void, RankedProfile> {
        @Override
        public RankedProfile doInBackground(Integer...params) {
            Log.d("ServerHelper", "AsyncTask GetRank started");
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://eng.studev.groept.be/thesis/a14_stapp2/getRank.php");
            JSONObject query = new JSONObject();
            RankedProfile prof = null;
            try {
                query.put("id", params[0].toString());
                post.setEntity(new StringEntity(query.toString()));
                HttpResponse response = client.execute(post);
                BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String line;
                JSONObject result = new JSONObject();
                if ((line = in.readLine()) != null) {
                    result = new JSONObject(line);
                }
                in.close();
                prof = new RankedProfile(params[0] , null, null, result.getString("username"), null, result.getInt("money"), result.getInt("experience"), result.getInt("rank"));
            } catch (ClientProtocolException e) {
                Log.e("GetRank", "Error: ClientProtocolException");
            } catch (IOException e) {
                Log.e("GetRank", "Error: IOException");
            } catch (JSONException e) {
                Log.e("GetRank", "JSON error");
            }
            return prof;
        }

        @Override
        protected void onPostExecute(RankedProfile profile) {
            Log.d("GetRank", profile.toString());
        }
    }
}
