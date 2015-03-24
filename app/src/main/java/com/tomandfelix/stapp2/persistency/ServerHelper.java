package com.tomandfelix.stapp2.persistency;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.tomandfelix.stapp2.application.StApp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
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
    private static Context context;

    private ServerHelper(Context context) {
        ServerHelper.context = context;
    }

    public static void init(Context context) {
        if(uniqueInstance == null) {
            uniqueInstance = new ServerHelper(context.getApplicationContext());
            ServerHelper.context = context;
        }
    }

    public static ServerHelper getInstance() {
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
            request.put("gcm_reg_id", ((StApp) context).getGcmRegistrationId());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest createProfile = new JsonObjectRequest(Request.Method.POST, "http://eng.studev.groept.be/thesis/a14_stapp2/createProfile.php", request, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if(!response.has("error")) {
                    Profile result = null;
                    try {
                        DatabaseHelper.getInstance().setToken(response.getString("token"));
                        result = new Profile(response.getInt("id"), firstName, lastName, username, email, 0, 0, avatar, response.getInt("rank"), new Date());
                    } catch(JSONException e) {
                        e.printStackTrace();
                    }
                    if(result != null) {
                        DatabaseHelper.getInstance().storeProfile(result);
                        DatabaseHelper.getInstance().setOwnerId(result.getId());
                        DatabaseHelper.getInstance().setLastEnteredUsername(result.getUsername());
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
        VolleyQueue.getInstance().addToRequestQueue(createProfile);
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
            request.put("gcm_reg_id", ((StApp) context).getGcmRegistrationId());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest login = new JsonObjectRequest(Request.Method.POST, "http://eng.studev.groept.be/thesis/a14_stapp2/login.php", request, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if(!response.has("error")) {
                    Profile result = null;
                    try {
                        DatabaseHelper.getInstance().setToken(response.getString("token"));
                        result = extractProfile(response);
                        result.setUsername(username);
                    } catch(JSONException e) {
                        e.printStackTrace();
                    }
                    if(result != null) {
                        DatabaseHelper.getInstance().storeProfile(result);
                        DatabaseHelper.getInstance().setOwnerId(result.getId());
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
        VolleyQueue.getInstance().addToRequestQueue(login);
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
        if(DatabaseHelper.getInstance().getOwnerId() <= 0) {
            errorListener.onErrorResponse(new VolleyError("owner"));
            return;
        }
        Profile stored = DatabaseHelper.getInstance().getProfile(DatabaseHelper.getInstance().getOwnerId());
        if(!forceUpdate && minutesAgo(stored.getLastUpdate()) < 10) {
            responseListener.onResponse(stored);
        } else {
            JSONObject request = new JSONObject();
            try {
                request.put("id", stored.getId());
                request.put("token", DatabaseHelper.getInstance().getToken());
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
                            DatabaseHelper.getInstance().storeProfile(result);
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
            VolleyQueue.getInstance().addToRequestQueue(getProfile);
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
        Profile stored = DatabaseHelper.getInstance().getProfile(id);
        if(!forceUpdate && stored != null && minutesAgo(stored.getLastUpdate()) < 10) {
            responseListener.onResponse(stored);
        } else {
            JSONObject request = new JSONObject();
            try {
                request.put("id", id);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.d("GetOtherProfile", request.toString());
            JsonObjectRequest getOtherProfile = new JsonObjectRequest(Request.Method.POST, "http://eng.studev.groept.be/thesis/a14_stapp2/getOtherProfile.php", request, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    if(!response.has("error")) {
                        Profile result;
                        result = extractProfile(response);
                        result.setId(id);
                        if(result != null) {
                            DatabaseHelper.getInstance().storeProfile(result);
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
            VolleyQueue.getInstance().addToRequestQueue(getOtherProfile);
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
        Profile fromStored = DatabaseHelper.getInstance().getProfile(id);
        ArrayList<Profile> stored = DatabaseHelper.getInstance().getLeaderboardByRank(fromStored.getRank());
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
                            DatabaseHelper.getInstance().storeProfile(p);
                        responseListener.onResponse(result);
                    }
                }
            }, errorListener);
            VolleyQueue.getInstance().addToRequestQueue(getLeaderBoardById);
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
        ArrayList<Profile> stored = DatabaseHelper.getInstance().getLeaderboardByRank(rank);
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
                            DatabaseHelper.getInstance().storeProfile(p);
                        responseListener.onResponse(result);
                    }
                }
            }, errorListener);
            VolleyQueue.getInstance().addToRequestQueue(getLeaderBoardByRank);
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
            request.put("id", DatabaseHelper.getInstance().getOwnerId());
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
        VolleyQueue.getInstance().addToRequestQueue(deleteProfile);
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
            request.put("id", DatabaseHelper.getInstance().getOwnerId());
            request.put("money", money);
            request.put("experience", experience);
            request.put("token", DatabaseHelper.getInstance().getToken());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest updateMXP = new JsonObjectRequest(Request.Method.POST, "http://eng.studev.groept.be/thesis/a14_stapp2/updateMoneyAndExperience.php", request, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if(!response.has("error")) {
                    Profile result = null;
                    try {
                        result = new Profile(DatabaseHelper.getInstance().getOwnerId(), null, null, null, null, money, experience, null, response.getInt("rank"), new Date());
                    } catch(JSONException e) {
                        e.printStackTrace();
                    }
                    if(result != null) {
                        DatabaseHelper.getInstance().updateProfile(result);
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
        VolleyQueue.getInstance().addToRequestQueue(updateMXP);
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
            request.put("id", DatabaseHelper.getInstance().getOwnerId());
            request.put("firstname", firstname);
            request.put("lastname", lastname);
            request.put("username", username);
            request.put("email", email);
            request.put("avatar", avatar);
            request.put("gcm_reg_id", ((StApp) context).getGcmRegistrationId());
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
                            DatabaseHelper.getInstance().updateProfile(new Profile(DatabaseHelper.getInstance().getOwnerId(), firstname, lastname, username, email, -1, -1, avatar, -1, null));
                        }
                        errorListener.onErrorResponse(new VolleyError(response.getString("error")));
                    } catch(JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, errorListener);
        VolleyQueue.getInstance().addToRequestQueue(updateProf);
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
            request.put("id", DatabaseHelper.getInstance().getOwnerId());
            request.put("firstname", firstname);
            request.put("lastname", lastname);
            request.put("username", username);
            request.put("email", email);
            request.put("avatar", avatar);
            request.put("gcm_reg_id", ((StApp) context).getGcmRegistrationId());
            request.put("token", DatabaseHelper.getInstance().getToken());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest updateProf = new JsonObjectRequest(Request.Method.POST, "http://eng.studev.groept.be/thesis/a14_stapp2/updateProfileSettings.php", request, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if(response.has("error")) {
                    try {
                        if(response.getString("error").equals("none")) {
                            DatabaseHelper.getInstance().updateProfile(new Profile(DatabaseHelper.getInstance().getOwnerId(), firstname, lastname, username, email, -1, -1, avatar, -1, null));
                        }
                        errorListener.onErrorResponse(new VolleyError(response.getString("error")));
                    } catch(JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, errorListener);
        VolleyQueue.getInstance().addToRequestQueue(updateProf);
    }

    public void uploadLogs(ArrayList<IdLog> logs, final ResponseFunc<Integer> responseListener, final Response.ErrorListener errorListener) {
        JSONObject request = new JSONObject();
        JSONArray requestArray = new JSONArray();
        try {
            request.put("id", DatabaseHelper.getInstance().getOwnerId());
            request.put("token", DatabaseHelper.getInstance().getToken());
            for(IdLog l : logs) {
                requestArray.put(l.toJSONObject());
            }
            request.put("logs", requestArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest uploadLogs = new JsonObjectRequest(Request.Method.POST, "http://eng.studev.groept.be/thesis/a14_stapp2/uploadLogs.php", request, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try{
                    if(response.has("lastId")) {
                        responseListener.onResponse(response.getInt("lastId"));
                    }else if(response.has("error")) {
                        errorListener.onErrorResponse(new VolleyError(response.getString("error")));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, errorListener);
        VolleyQueue.getInstance().addToRequestQueue(uploadLogs);
    }

    public void downloadLogs(int lastId, final Response.ErrorListener errorListener) {
        JSONObject request = new JSONObject();
        try {
            request.put("id", DatabaseHelper.getInstance().getOwnerId());
            request.put("token", DatabaseHelper.getInstance().getToken());
            request.put("lastId", lastId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonArrayRequest downloadLogs = new JsonArrayRequest(Request.Method.POST, "http://eng.studev.groept.be/thesis/a14_stapp2/downloadLogs.php", request, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                ArrayList<IdLog> result = null;
                try{
                    if(!response.getJSONObject(0).has("error")) {
                        result = new ArrayList<>();
                        for(int i = 0; i < response.length(); i++) {
                            result.add(new IdLog(response.getJSONObject(i)));
                        }
                    }else {
                        errorListener.onErrorResponse(new VolleyError(response.getJSONObject(0).getString("error")));
                    }
                } catch (JSONException | ParseException e) {
                    e.printStackTrace();
                }
                if(result != null) {
                    DatabaseHelper.getInstance().storeLogs(result);
                    errorListener.onErrorResponse(new VolleyError("none"));
                }

            }
        }, errorListener);
        VolleyQueue.getInstance().addToRequestQueue(downloadLogs);
    }

    public void sendMessage(GCMMessage message, final Response.ErrorListener errorListener) {
        JSONObject request = new JSONObject();
        JSONArray requestArray = new JSONArray();
        try {
            request.put("id", DatabaseHelper.getInstance().getOwnerId());
            request.put("token", DatabaseHelper.getInstance().getToken());
            for(int i : message.getReceivers()) {
                requestArray.put(i);
            }
            request.put("receiver_ids", requestArray);
            request.put("challenge_id", message.getChallengeId());
            request.put("message", message.getMessage());
            request.put("message_type", message.getMessageType());
            request.put("sender_id", DatabaseHelper.getInstance().getOwnerId());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest uploadLogs = new JsonObjectRequest(Request.Method.POST, "http://eng.studev.groept.be/thesis/a14_stapp2/sendGCMMessage.php", request, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try{
                    if(response.has("error")) {
                        errorListener.onErrorResponse(new VolleyError(response.getString("error")));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, errorListener);
        VolleyQueue.getInstance().addToRequestQueue(uploadLogs);
    }
    public void getProgressOfOther(int id, final Response.ErrorListener errorListener, final VolleyCallback callback) {

        JSONObject request = new JSONObject();
        try {
            request.put("id", id);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final JsonArrayRequest getProgressOfOther = new JsonArrayRequest(Request.Method.POST, "http://eng.studev.groept.be/thesis/a14_stapp2/getProgressOfOther.php", request, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    if(!response.isNull(0) && !response.getJSONObject(0).getString("action").equals("end_day")){
                        ArrayList<Integer> achieved_score = new ArrayList<>();
                        ArrayList<Integer> time = new ArrayList<>();
                        float divider = 0;
                        float upper = 0 ;
                        for(int i = 0; i < response.length();i++) {
                            Log.d("response",response.get(i).toString());
                            if(response.getJSONObject(i).getString("action").equals("achieved_score_percent")){
                                achieved_score.add(response.getJSONObject(i).getInt("DATA"));
                            }else{
                                time.add(response.getJSONObject(i).getInt("DATA"));
                            }
                        }
                        if(achieved_score.size() > 0 && time.size() > 0 ) {
                            for (int i = 0; i < achieved_score.size(); i++) {
                                upper += ((float) achieved_score.get(i)) * time.get(i);
                                divider += ((float) time.get(i));
                            }
                            float result = upper / divider ;
                            callback.getResult(result);
                            Log.d("end result", result + "");
                        }else{
                            Log.e("error", "one of the two arrays is empty");
                        }
                    }else{
                        callback.getResult(0);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
            , errorListener);
        VolleyQueue.getInstance().addToRequestQueue(getProgressOfOther);
    }
    public interface VolleyCallback{
        void getResult(float result);
    }
}