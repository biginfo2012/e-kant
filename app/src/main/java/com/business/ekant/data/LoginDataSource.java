package com.business.ekant.data;

import android.content.SharedPreferences;
import android.util.Log;

import com.business.ekant.data.model.LoggedInUser;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import cz.msebera.android.httpclient.Header;

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
public class LoginDataSource {
    public String userToken = null;
    public String userName;
    public String userPlace;
    public Boolean isGetResponse = false;

    public Result<LoggedInUser> login(String username, String password) {

        try {
            // TODO: handle loggedInUser authentication
            String url = "api/v1/client/login";
            RequestParams rp = new RequestParams();
            rp.add("email", username);
            rp.add("password", password);

            HttpUtils.post(url, rp, new JsonHttpResponseHandler() {
                @Override
                public void onStart() {
                    // called before request is started
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    // If the response is JSONObject instead of expected JSONArray
                    isGetResponse = true;
                    Log.d("asd", "---------------- this is response : " + response);
                    try {
                        JSONObject serverResp = new JSONObject(response.toString());

                        String rpMessage = serverResp.optString("status");
                        if(rpMessage.equals("error")){
                            userToken = "error";
                        }
                        else{
                            JSONObject rt = serverResp.getJSONObject("result");
                            userToken = rt.optString("token");
                            JSONObject si = rt.getJSONObject("staff_info");
                            userName = si.optString("name");
                            JSONObject sf = si.optJSONObject("staff_field");
                            if(sf != null && sf.length() != 0){
                                JSONObject field = sf.optJSONObject("field");
                                userPlace = field.getString("name");
                            }
                        }
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable e, JSONObject response) {
                    // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                    isGetResponse = true;
                    Log.d("asd", "failure login" + response);
                }

                @Override
                public void onRetry(int retryNo) {
                    // called when request is retried
                }
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONArray timeline) {
                    // Pull out the first event on the public timeline

                }
            });

            if(userToken == "error" || userToken == null){
                return new Result.Error(new IOException("Error logging in"));
            }
            LoggedInUser fakeUser =
                    new LoggedInUser(
                            userToken, userName, userPlace);

            return new Result.Success<>(fakeUser);
        } catch (Exception e) {
            Log.e("REST_API", "GET method failed: " + e.getMessage());
            e.printStackTrace();
            return new Result.Error(new IOException("Error logging in", e));
        }
    }

    public void logout() {
        // TODO: revoke authentication
    }


}

