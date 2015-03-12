package com.example.tom.stapp3.persistency;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

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

import java.io.IOException;
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
            uniqueInstance = new ServerHelper(context.getApplicationContext());
        }
        return uniqueInstance;
    }

    public interface ResponseFunc<INPUT> {
        /**
         *
         * @param response input of type INPUT to the call function
         */
        public void onResponse(INPUT response);
    }

    private int minutesAgo(Date input) {
        return Math.round(TimeUnit.MILLISECONDS.toMinutes(new Date().getTime() - input.getTime()));
    }

    /**
     * Extracts as much information from a json object as it can to build a profile
     * Not all data has to present
     * If there is no usable data, returns null
     * @param object The JSON object
     * @return The extracted profile
     */
    public Profile extractProfile(JSONObject object) {
        Profile result = null;
        try {
            if(object.has("id") || object.has("firstname") || object.has("lastname") || object.has("username") || object.has("email") || object.has("money") || object.has("experience") || object.has("avatar") || object.has("rank")) {
                result = new Profile(
                        object.has("id") ? object.getInt("id") : -1,
                        object.has("firstname") ? object.getString("firstname") : null,
                        object.has("lastname") ? object.getString("lastname") : null,
                        object.has("username") ? object.getString("username") : null,
                        object.has("email") ? object.getString("email") : null,
                        object.has("money") ? object.getInt("money") : -1,
                        object.has("experience") ? object.getInt("experience") : -1,
                        object.has("avatar") ? object.getString("avatar") : null,
                        object.has("rank") ? object.getInt("rank") : -1,
                        new Date()
                );
            }
        } catch(JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Creates a new profile in the online database, upon success, the login token and profile are added to the database, the profile is set as the owner and as last logged in person
     * @param firstName The first name for the new profile
     * @param lastName The surname for the new profile
     * @param username The username for the new profile
     * @param email The email for the new profile
     * @param avatar the avatar for the new profile
     * @param password The password for the new profile
     * @param responseListener The function that is called upon success, the argument for this function will be the created profile
     * @param errorListener The function that is called upon error, Possible errors:
     *                      * 'exists' The username is not unique
     *                      * 'input' There was some input missing
     *                      * 'database' Something went wrong with the database
     */
    public void createProfile(final String firstName, final String lastName, final String username, final String email, final String avatar, String password, final ResponseFunc<Profile> responseListener, final Response.ErrorListener errorListener) {
        JSONObject request = new JSONObject();
        try {
            request.put("firstname", firstName);
            request.put("lastname", lastName);
            request.put("username", username);
            request.put("email", email);
            request.put("avatar", avatar);
            request.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest createProfile = new JsonObjectRequest(Request.Method.POST, "http://eng.studev.groept.be/thesis/a14_stapp2/createProfile.php", request, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if(!response.has("error")) {
                    Profile result = null;
                    try {
                        DatabaseHelper.getInstance(context).setToken(response.getString("token"));
                        result = new Profile(response.getInt("id"), firstName, lastName, username, email, 0, 0, avatar, response.getInt("rank"), new Date());
                    } catch(JSONException e) {
                        e.printStackTrace();
                    }
                    if(result != null) {
                        DatabaseHelper.getInstance(context).storeProfile(result);
                        DatabaseHelper.getInstance(context).setOwnerId(result.getId());
                        DatabaseHelper.getInstance(context).setLastEnteredUsername(result.getUsername());
                        responseListener.onResponse(result);
                    }
                } else {
                    try {
                        errorListener.onErrorResponse(new VolleyError(response.getString("error")));
                    } catch(JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, errorListener);
        VolleyQueue.getInstance(context).addToRequestQueue(createProfile);
    }

    /**
     * If an existing set of credentials are used, the login token and profile are added to the database, the profile is set as the owner and as last logged in person
     * @param username The username of the profile
     * @param password The password of the profile
     * @param responseListener The function that is called upon success, the argument for this function will be the profile
     * @param errorListener The function that is called upon error, Possible errors:
     *                      * 'wrong' The given combination of username/password were wrong
     *                      * 'input' There was some input missing
     *                      * 'database' Something went wrong with the database
     */
    public void login(final String username, String password, final ResponseFunc<Profile> responseListener, final Response.ErrorListener errorListener ) {
        JSONObject request = new JSONObject();
        try {
            request.put("username", username);
            request.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest login = new JsonObjectRequest(Request.Method.POST, "http://eng.studev.groept.be/thesis/a14_stapp2/login.php", request, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if(!response.has("error")) {
                    Profile result = null;
                    try {
                        DatabaseHelper.getInstance(context).setToken(response.getString("token"));
                        result = extractProfile(response);
                        result.setUsername(username);
                    } catch(JSONException e) {
                        e.printStackTrace();
                    }
                    if(result != null) {
                        DatabaseHelper.getInstance(context).storeProfile(result);
                        DatabaseHelper.getInstance(context).setOwnerId(result.getId());
                        responseListener.onResponse(result);
                    }
                } else {
                    try {
                        errorListener.onErrorResponse(new VolleyError(response.getString("error")));
                    } catch(JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, errorListener);
        VolleyQueue.getInstance(context).addToRequestQueue(login);
    }

    /**
     * Gets the profile of the person that is logged in from the server and updates it in the local database, only when the token is still up to date
     * @param responseListener The function that is called upon success, the argument for this function will be the profile
     * @param errorListener The function that is called upon error, Possible errors:
     *                      * 'owner' no owner is set in the database
     *                      * 'token' The given token did not match with the one from the database, should log in again
     *                      * 'input' There was some input missing
     *                      * 'database' Something went wrong with the database
     * @param forceUpdate if true, a request will be sent to the server, if false, will only send a request if the local data is more then 10 minutes old
     */
    public void getProfile(final ResponseFunc<Profile> responseListener, final Response.ErrorListener errorListener, boolean forceUpdate) {
        if(DatabaseHelper.getInstance(context).getOwnerId() <= 0) {
            errorListener.onErrorResponse(new VolleyError("owner"));
            return;
        }
        Profile stored = DatabaseHelper.getInstance(context).getProfile(DatabaseHelper.getInstance(context).getOwnerId());
        if(!forceUpdate && minutesAgo(stored.getLastUpdate()) < 10) {
            responseListener.onResponse(stored);
        } else {
            JSONObject request = new JSONObject();
            try {
                request.put("id", stored.getId());
                request.put("token", DatabaseHelper.getInstance(context).getToken());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            JsonObjectRequest getProfile = new JsonObjectRequest(Request.Method.POST, "http://eng.studev.groept.be/thesis/a14_stapp2/getProfile.php", request, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    if(!response.has("error")) {
                        Profile result;
                        result = extractProfile(response);
                        if(result != null) {
                            DatabaseHelper.getInstance(context).storeProfile(result);
                            responseListener.onResponse(result);
                        }
                    } else {
                        try {
                            errorListener.onErrorResponse(new VolleyError(response.getString("error")));
                        } catch(JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, errorListener);
            VolleyQueue.getInstance(context).addToRequestQueue(getProfile);
        }
    }

    /**
     * Gets someone else's profile from the server and stores/updates it locally
     * @param id the id of the profile to get
     * @param responseListener The function that is called upon success, the argument for this function will be the profile
     * @param errorListener The function that is called upon error, Possible errors:
     *                      * 'id' The given id does not exist on the server
     *                      * 'input' There was some input missing
     *                      * 'database' Something went wrong with the database
     * @param forceUpdate if true, a request will be sent to the server, if false, will only send a request if the local data is more then 10 minutes old
     */
    public void getOtherProfile(final int id, final ResponseFunc<Profile> responseListener, final Response.ErrorListener errorListener, boolean forceUpdate) {
        Profile stored = DatabaseHelper.getInstance(context).getProfile(id);
        if(!forceUpdate && minutesAgo(stored.getLastUpdate()) < 10) {
            responseListener.onResponse(stored);
        } else {
            JSONObject request = new JSONObject();
            try {
                request.put("id", id);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            JsonObjectRequest getOtherProfile = new JsonObjectRequest(Request.Method.POST, "http://eng.studev.groept.be/thesis/a14_stapp2/getOtherProfile.php", request, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    if(!response.has("error")) {
                        Profile result;
                        result = extractProfile(response);
                        result.setId(id);
                        if(result != null) {
                            DatabaseHelper.getInstance(context).storeProfile(result);
                            responseListener.onResponse(result);
                        }
                    } else {
                        try {
                            errorListener.onErrorResponse(new VolleyError(response.getString("error")));
                        } catch(JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, errorListener);
            VolleyQueue.getInstance(context).addToRequestQueue(getOtherProfile);
        }
    }

    /**
     * Gets the leaderboard from the server, it will return at most 10 profiles
     * @param id The id that will be present in the 10 profiles
     * @param responseListener The function that is called upon success, the argument for this function will be the ArrayList of profiles
     * @param errorListener The function that is called upon error, Possible errors:
     *                      * 'rank' The given rank does not exist
     *                      * 'input' There was some input missing
     *                      * 'database' Something went wrong with the database
     * @param forceUpdate if true, a request will be sent to the server, if false, will only send a request if the local data is more then 10 minutes old
     */
    public void getLeaderboardById(final int id, final ResponseFunc<ArrayList<Profile>> responseListener, final Response.ErrorListener errorListener,  boolean forceUpdate) {
        Profile fromStored = DatabaseHelper.getInstance(context).getProfile(id);
        ArrayList<Profile> stored = DatabaseHelper.getInstance(context).getLeaderboardByRank(fromStored.getRank());
        boolean update = false;
        if(!forceUpdate && fromStored != null && minutesAgo(fromStored.getLastUpdate()) < 10 && stored != null) {
            for(Profile p : stored)
                update = update || minutesAgo(p.getLastUpdate()) >= 10;
        } else update = true;

        if(!update)
            responseListener.onResponse(stored);
        else {
            JSONObject request = new JSONObject();
            try {
                request.put("id", id);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            JsonArrayRequest getLeaderBoardById = new JsonArrayRequest(Request.Method.POST, "http://eng.studev.groept.be/thesis/a14_stapp2/getLeaderboard.php", request, new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {
                    ArrayList<Profile> result = null;
                    try {
                        if(!response.getJSONObject(0).has("error")) {
                            result = new ArrayList<>();
                            for(int i = 0; i < response.length(); i++) {
                                result.add(extractProfile(response.getJSONObject(i)));
                            }
                        } else {
                            errorListener.onErrorResponse(new VolleyError(response.getJSONObject(0).getString("error")));
                        }
                    } catch(JSONException e) {
                        e.printStackTrace();
                    }
                    if(result != null) {
                        for(Profile p:result)
                            DatabaseHelper.getInstance(context).storeProfile(p);
                        responseListener.onResponse(result);
                    }
                }
            }, errorListener);
            VolleyQueue.getInstance(context).addToRequestQueue(getLeaderBoardById);
        }
    }

    /**
     * Gets the leaderboard from the server, it will return at most 10 profiles
     * @param rank The rank that will be present in the 10 profiles
     * @param responseListener The function that is called upon success, the argument for this function will be the ArrayList of profiles
     * @param errorListener The function that is called upon error, Possible errors:
     *                      * 'rank' The given rank does not exist
     *                      * 'input' There was some input missing
     *                      * 'database' Something went wrong with the database
     * @param forceUpdate if true, a request will be sent to the server, if false, will only send a request if the local data is more then 10 minutes old
     */
    public void getLeaderboardByRank(int rank, final ResponseFunc<ArrayList<Profile>> responseListener, final Response.ErrorListener errorListener, boolean forceUpdate) {
        ArrayList<Profile> stored = DatabaseHelper.getInstance(context).getLeaderboardByRank(rank);
        boolean update = false;
        if(!forceUpdate && stored != null) {
            for(Profile p : stored)
                update = update || minutesAgo(p.getLastUpdate()) >= 10;
        } else update = true;

        if(!update)
            responseListener.onResponse(stored);
        else {
            JSONObject request = new JSONObject();
            try {
                request.put("rank", rank);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            JsonArrayRequest getLeaderBoardByRank = new JsonArrayRequest(Request.Method.POST, "http://eng.studev.groept.be/thesis/a14_stapp2/getLeaderboard.php", request, new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {
                    ArrayList<Profile> result = null;
                    try {
                        if(!response.getJSONObject(0).has("error")) {
                            result = new ArrayList<>();
                            for(int i = 0; i < response.length(); i++) {
                                result.add(extractProfile(response.getJSONObject(i)));
                            }
                        } else {
                            errorListener.onErrorResponse(new VolleyError(response.getJSONObject(0).getString("error")));
                        }
                    } catch(JSONException e) {
                        e.printStackTrace();
                    }
                    if(result != null) {
                        for(Profile p:result)
                            DatabaseHelper.getInstance(context).storeProfile(p);
                        responseListener.onResponse(result);
                    }
                }
            }, errorListener);
            VolleyQueue.getInstance(context).addToRequestQueue(getLeaderBoardByRank);
        }
    }

    /**
     * Deletes the owner's profile from the server and local database
     * @param password The password of the profile
     * @param errorListener The function that is called upon error AND completion, Possible errors:
     *                      * 'none' Acknowledgement of removal
     *                      * 'password' The password is wrong, or the profile is already gone
     *                      * 'input' There was some input missing
     *                      * 'database' Something went wrong with the database
     */
    public void deleteProfile(String password, final Response.ErrorListener errorListener) {
        JSONObject request = new JSONObject();
        try {
            request.put("id", DatabaseHelper.getInstance(context).getOwnerId());
            request.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest deleteProfile = new JsonObjectRequest(Request.Method.POST, "http://eng.studev.groept.be/thesis/a14_stapp2/deleteProfile.php", request, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if(response.has("error")) {
                    try {
                        errorListener.onErrorResponse(new VolleyError(response.getString("error")));
                    } catch(JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, errorListener);
        VolleyQueue.getInstance(context).addToRequestQueue(deleteProfile);
    }

    /**
     * Updates the money and the experience of the owner, as long as the token is up to date
     * @param money The new value for money
     * @param experience The new value for experience
     * @param errorListener The function that is called upon error AND completion, Possible errors:
     *                      * 'token' The token does not match the one from the server
     *                      * 'input' There was some input missing
     *                      * 'database' Something went wrong with the database
     */
    public void updateMoneyAndExperience(final int money, final int experience, final Response.ErrorListener errorListener) {
        JSONObject request = new JSONObject();
        try {
            request.put("id", DatabaseHelper.getInstance(context).getOwnerId());
            request.put("money", money);
            request.put("experience", experience);
            request.put("token", DatabaseHelper.getInstance(context).getToken());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest updateMXP = new JsonObjectRequest(Request.Method.POST, "http://eng.studev.groept.be/thesis/a14_stapp2/updateMoneyAndExperience.php", request, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if(!response.has("error")) {
                    Profile result = null;
                    try {
                        result = new Profile(DatabaseHelper.getInstance(context).getOwnerId(), null, null, null, null, money, experience, null, response.getInt("rank"), new Date());
                    } catch(JSONException e) {
                        e.printStackTrace();
                    }
                    if(result != null) {
                        DatabaseHelper.getInstance(context).updateProfile(result);
                        errorListener.onErrorResponse(new VolleyError("none"));
                    }
                } else {
                    try {
                        errorListener.onErrorResponse(new VolleyError(response.getString("error")));
                    } catch(JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, errorListener);
        VolleyQueue.getInstance(context).addToRequestQueue(updateMXP);
    }

    /**
     * Updates the profile parameters that updateMoneyAndExperience does not, requires the password to authenticate
     * @param firstname The new first name
     * @param lastname The new surname
     * @param username The new username, this must be unique online
     * @param email The new email
     * @param avatar The new avatar
     * @param password The old password
     * @param new_password The new password
     * @param errorListener The function that is called upon error AND completion, Possible errors:
     *                      * 'none' Acknowledgement of update
     *                      * 'password' The password is wrong, or the profile doesn't exist
     *                      * 'input' There was some input missing
     *                      * 'database' Something went wrong with the database
     */
    public void updateProfileSettings(final String firstname, final String lastname, final String username, final String email, final String avatar, String password, String new_password, final Response.ErrorListener errorListener) {
        JSONObject request = new JSONObject();
        try {
            request.put("id", DatabaseHelper.getInstance(context).getOwnerId());
            request.put("firstname", firstname);
            request.put("lastname", lastname);
            request.put("username", username);
            request.put("email", email);
            request.put("avatar", avatar);
            request.put("password", password);
            request.put("new_password", new_password);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest updateProf = new JsonObjectRequest(Request.Method.POST, "http://eng.studev.groept.be/thesis/a14_stapp2/updateProfileSettings.php", request, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if(response.has("error")) {
                    try {
                        if(response.getString("error").equals("none")) {
                            DatabaseHelper.getInstance(context).updateProfile(new Profile(DatabaseHelper.getInstance(context).getOwnerId(), firstname, lastname, username, email, -1, -1, avatar, -1, null));
                        }
                        errorListener.onErrorResponse(new VolleyError(response.getString("error")));
                    } catch(JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, errorListener);
        VolleyQueue.getInstance(context).addToRequestQueue(updateProf);
    }

    /**
     * Updates the profile parameters that updateMoneyAndExperience does not, requires the token to be up to date
     * @param firstname The new first name
     * @param lastname The new surname
     * @param username The new username, this must be unique online
     * @param email The email for the new profile
     * @param avatar The new avatar
     * @param errorListener The function that is called upon error AND completion, Possible errors:
     *                      * 'none' Acknowledgement of update
     *                      * 'token' The token does not match the one from the server
     *                      * 'input' There was some input missing
     *                      * 'database' Something went wrong with the database
     */
    public void updateProfileSettings(final String firstname, final String lastname, final String username, final String email, final String avatar, final Response.ErrorListener errorListener) {
        JSONObject request = new JSONObject();
        try {
            request.put("id", DatabaseHelper.getInstance(context).getOwnerId());
            request.put("firstname", firstname);
            request.put("lastname", lastname);
            request.put("username", username);
            request.put("email", email);
            request.put("avatar", avatar);
            request.put("token", DatabaseHelper.getInstance(context).getToken());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest updateProf = new JsonObjectRequest(Request.Method.POST, "http://eng.studev.groept.be/thesis/a14_stapp2/updateProfileSettings.php", request, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if(response.has("error")) {
                    try {
                        if(response.getString("error").equals("none")) {
                            DatabaseHelper.getInstance(context).updateProfile(new Profile(DatabaseHelper.getInstance(context).getOwnerId(), firstname, lastname, username, email, -1, -1, avatar, -1, null));
                        }
                        errorListener.onErrorResponse(new VolleyError(response.getString("error")));
                    } catch(JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, errorListener);
        VolleyQueue.getInstance(context).addToRequestQueue(updateProf);
    }

//    public void uploadLogs(ArrayList<DBLog> logs, final Response.ErrorListener errorListener) {
//        JSONObject request = new JSONObject();
//        try {
//            request.put("id", DatabaseHelper.getInstance(context).getOwnerId());
//            request.put("token", DatabaseHelper.getInstance(context).getToken());
//            request.put("logs", )
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }
}