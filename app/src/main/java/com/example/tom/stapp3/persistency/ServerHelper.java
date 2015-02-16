package com.example.tom.stapp3.persistency;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.example.tom.stapp3.Function;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

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
in.close();
Log.e("OUTPUT", output);*/

public class ServerHelper {
    private static ServerHelper uniqueInstance;
    private Context context;

    private ServerHelper(Context context) {
        this.context = context;
    }

    public static ServerHelper getInstance(Context context) {
        if(uniqueInstance == null) {
            uniqueInstance = new ServerHelper(context);
        }
        return uniqueInstance;
    }

    private int minutesAgo(Date input) {
        return Math.round(TimeUnit.MILLISECONDS.toMinutes(new Date().getTime() - input.getTime()));
    }

    public void createProfile(String firstName, String lastName, String username, String email, String avatar, String password, Function<Profile> callback) {
        CreateProfile temp = new CreateProfile(callback);
        temp.execute(firstName, lastName, username, email, avatar, password);
    }

    public void getProfile(String username, String password, Function<Profile> callback, boolean forceUpdate) {
        Profile tempProf;
        if(! forceUpdate && (tempProf = DatabaseHelper.getInstance(context).getProfile(username)) != null && minutesAgo(tempProf.getLastUpdate()) < 10) {
            callback.call(tempProf);
        } else {
            GetProfile temp = new GetProfile(callback);
            temp.execute(username, password);
        }
    }

    public void getOtherProfile(int id, Function<Profile> callback, boolean forceupdate) {
        Profile tempProf;
        if(! forceupdate && (tempProf = DatabaseHelper.getInstance(context).getProfile(id)) != null && minutesAgo(tempProf.getLastUpdate()) < 10) {
            callback.call(tempProf);
        } else {
            GetOtherProfile temp = new GetOtherProfile(callback);
            temp.execute(id);
        }
    }

    public void getLeaderboardById(int id, Function<ArrayList<Profile>> callback, boolean forceupdate) {
        Profile fromId = null;
        ArrayList<Profile> tempProf = null;
        boolean update = false;
        if(! forceupdate && ( fromId = DatabaseHelper.getInstance(context).getProfile(id)) != null && minutesAgo( fromId.getLastUpdate()) < 10 && (tempProf = DatabaseHelper.getInstance(context).getLeaderboardByRank(fromId.getRank())) != null) {
            for(Profile p : tempProf) {
                update = update || minutesAgo(p.getLastUpdate()) >= 10;
            }
        } else {
            update = true;
        }
        if(! update) {
            callback.call(tempProf);
        } else {
            GetLeaderboard temp = new GetLeaderboard(callback, "id");
            temp.execute(id);
        }
    }

    public void getLeaderboardByRank(int rank, Function<ArrayList<Profile>> callback, boolean forceUpdate) {
        ArrayList<Profile> tempProf = null;
        boolean update = false;
        if(!forceUpdate && (tempProf = DatabaseHelper.getInstance(context).getLeaderboardByRank(rank)) != null) {
            for( Profile p : tempProf) {
                update = update || minutesAgo(p.getLastUpdate()) >= 10;
            }
        } else {
            update = true;
        }
        if(! update) {
            callback.call(tempProf);
        } else {
            GetLeaderboard temp = new GetLeaderboard(callback, "rank");
            temp.execute(rank);
        }
    }

    public void deleteProfile(int id, String password) {
        new DeleteProfile().execute(Integer.toString(id), password);
    }

    public void updateMoneyAndExperience(int id, int money, int experience) {
        new UpdateMoneyAndExperience().execute(id, money, experience);
    }

    public void updateProfileSettings(int id, String password, String firstname, String lastname, String username, String email, String avatar, String new_password) {
        new UpdateProfileSettings().execute(Integer.toString(id), password, firstname, lastname, username, email, avatar, new_password);
        DatabaseHelper.getInstance(context).updateProfile(new Profile(id, firstname, lastname, username, email, -1, -1, avatar, -1, null));
    }

    private class CreateProfile extends AsyncTask<String, Void, Profile> {
        private Function<Profile> callback;

        public CreateProfile(Function<Profile> callback) {
            super();
            this.callback = callback;
        }

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
                query.put("avatar", params[4]);
                query.put("password", params[5]);
                post.setEntity(new StringEntity(query.toString()));
                HttpResponse response = client.execute(post);
                String output = EntityUtils.toString(response.getEntity());
                JSONObject result = new JSONObject(output);
                if(! result.toString().equals("{}")) {
                    prof = new Profile(result.getInt("id") , params[0], params[1], params[2], params[3], 0, 0, params[4], result.getInt("rank"), new Date());
                    DatabaseHelper.getInstance(context).setSetting(DatabaseHelper.TOKEN, result.getString("token"));
                }
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
            DatabaseHelper.getInstance(context).storeProfile(profile);
            DatabaseHelper.getInstance(context).setSetting(DatabaseHelper.OWNER, profile.getId());
            callback.call(profile);
        }
    }

    private class GetProfile extends AsyncTask<String, Void, Profile> {
        private Function<Profile> callback;

        public GetProfile(Function<Profile> callback) {
            super();
            this.callback = callback;
        }

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
                String output = EntityUtils.toString(response.getEntity());
                JSONObject result = new JSONObject(output);
                if(! result.toString().equals("{}")) {
                    prof = new Profile(result.getInt("id"), result.getString("firstname"), result.getString("lastname"), params[0], result.getString("email"),result.getInt("money"), result.getInt("experience"), result.getString("avatar"), result.getInt("rank"), new Date());
                    DatabaseHelper.getInstance(context).setSetting(DatabaseHelper.TOKEN, result.getString("token"));
                }
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
            if( profile != null) {
                Log.d("GetProfile", profile.toString());
                DatabaseHelper.getInstance(context).storeProfile(profile);
                DatabaseHelper.getInstance(context).setSetting(DatabaseHelper.OWNER, profile.getId());

            }
            callback.call(profile);
        }
    }

    private class GetOtherProfile extends AsyncTask<Integer, Void, Profile> {
        private Function<Profile> callback;

        public GetOtherProfile(Function<Profile> callback) {
            super();
            this.callback = callback;
        }

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
                String output = EntityUtils.toString(response.getEntity());
                JSONObject result = new JSONObject(output);
                if(! result.toString().equals("{}")) {
                    prof = new Profile(params[0], null, null, result.getString("username"), null, result.getInt("money"), result.getInt("experience"), result.getString("avatar"), result.getInt("rank"), new Date());
                }
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
            if(profile != null) {
                Log.d("GetOtherProfile", profile.toString());
                DatabaseHelper.getInstance(context).storeProfile(profile);
            }
            callback.call(profile);
        }
    }

    private class GetLeaderboard extends AsyncTask<Integer, Void, ArrayList<Profile>> {
        private Function<ArrayList<Profile>> callback;
        private String inputType;

        public GetLeaderboard(Function<ArrayList<Profile>> callback, String inputType) {
            super();
            this.callback = callback;
            this.inputType = inputType;
        }
        @Override
        protected ArrayList<Profile> doInBackground(Integer... params) {
            Log.d("ServerHelper", "AsyncTask GetLeaderboard started");
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://eng.studev.groept.be/thesis/a14_stapp2/getLeaderboard.php");
            JSONObject query = new JSONObject();
            ArrayList<Profile> prof = new ArrayList<Profile>();
            try {
                if(inputType.equals("id")) {
                    query.put("id", params[0].toString());
                } else if(inputType.equals("rank")) {
                    query.put("rank", params[0].toString());
                }
                post.setEntity(new StringEntity(query.toString()));
                HttpResponse response = client.execute(post);
                String output = EntityUtils.toString(response.getEntity());
                JSONArray result = new JSONArray(output);
                if(result.toString().equals("{}")) {
                    prof = null;
                } else {
                    for (int i = 0; i < result.length(); i++) {
                        JSONObject temp = result.getJSONObject(i);
                        prof.add(new Profile(temp.getInt("id"), null, null, temp.getString("username"), null, temp.getInt("money"), temp.getInt("experience"), temp.getString("avatar"), temp.getInt("rank"), new Date()));
                    }
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
        public void onPostExecute(ArrayList<Profile> profiles) {
            if(profiles != null) {
                for(Profile prof : profiles) {
                    DatabaseHelper.getInstance(context).storeProfile(prof);
                    Log.d("GetLeaderboard", prof.toString());
                }
            }
            callback.call(profiles);
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

    private class UpdateMoneyAndExperience extends AsyncTask<Integer, Void, Profile> {
        @Override
        public Profile doInBackground(Integer... params) {
            Log.d("ServerHelper", "AsyncTask UpdateMoneyAndExperience started");
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://eng.studev.groept.be/thesis/a14_stapp2/updateMoneyAndExperience.php");
            JSONObject query = new JSONObject();
            Profile prof = null;
            try {
                query.put("id", params[0]);
                query.put("money", params[1]);
                query.put("experience", params[2]);
                query.put("token", DatabaseHelper.getInstance(context).getStringSetting(DatabaseHelper.TOKEN));
                post.setEntity(new StringEntity(query.toString()));
                HttpResponse response = client.execute(post);
                String output = EntityUtils.toString(response.getEntity());
                JSONObject result = new JSONObject(output);
                if(! result.toString().equals("{}") && result.getInt("rank") != -1) {
                    prof = new Profile(params[0], null, null, null, null, params[1], params[2], null, result.getInt("rank"), new Date());
                }
            } catch (ClientProtocolException e) {
                Log.e("UpdateMoneyAndExperience", "Error: ClientProtocolException");
            } catch (IOException e) {
                Log.e("UpdateMoneyAndExperience", "Error: IOException");
            } catch (JSONException e) {
                Log.e("UpdateMoneyAndExperience", "JSON error");
            }
            return prof;
        }

        @Override
        public void onPostExecute(Profile param) {
            DatabaseHelper.getInstance(context).updateProfile(param);
        }
    }

    private class UpdateProfileSettings extends AsyncTask<String, Void, Void> {
        @Override
        public Void doInBackground(String...params) {
            Log.d("ServerHelper", "AsyncTask UpdateProfileSettings started");
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://eng.studev.groept.be/thesis/a14_stapp2/updateProfileSettings.php");
            JSONObject query = new JSONObject();
            try {
                query.put("id", params[0]);
                query.put("password", params[1]);
                query.put("firstname", params[2]);
                query.put("lastname", params[3]);
                query.put("username", params[4]);
                query.put("email", params[5]);
                query.put("avatar", params[6]);
                query.put("new_password", params[7]);
                post.setEntity(new StringEntity(query.toString()));
                HttpResponse response = client.execute(post);
                String output = EntityUtils.toString(response.getEntity());
                Log.e("OUTPUT", output);
            } catch (ClientProtocolException e) {
                Log.e("UpdateProfileSettings", "Error: ClientProtocolException");
            } catch (IOException e) {
                Log.e("UpdateProfileSettings", "Error: IOException");
            } catch (JSONException e) {
                Log.e("UpdateProfileSettings", "JSON error");
            }
            return null;
        }
    }
}