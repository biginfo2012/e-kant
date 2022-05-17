package com.business.ekant.data;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.business.ekant.MainActivity;
import com.business.ekant.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class SendPosService extends Service {
    String token, todayShiftId;
    public int counter = 0;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    int user_stime;
    private LocationManager mLocationManager;
    private RegisterFirebaseTokenTask mFirebaseTask = null;
    String locationProvider;
    Boolean success = false;
    String latitude = "", longitude ="", field_name = "";
    JSONArray staffAddress = null;
    JSONArray shiftAddress = null;

    public FusedLocationProviderClient mFusedLocationClient;
    private Location mLocation;
    final Handler someHandler = new Handler();
    private static Timer timer = new Timer();
    private Context ctx;

    public IBinder onBind(Intent arg0)
    {
        return null;
    }

    public void onCreate()
    {
        super.onCreate();
        ctx = this;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        LocationRequest locationRequest = new LocationRequest();

        locationRequest.setPriority(
// どれにするかはお好みで、ただしできない状況ではできないので
                LocationRequest.PRIORITY_HIGH_ACCURACY);
//                LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
//                LocationRequest.PRIORITY_LOW_POWER);
//                LocationRequest.PRIORITY_NO_POWER);
        startService();

    }

    private void startService()
    {
        timer.scheduleAtFixedRate(new mainTask(), 0, 60000);
    }

    private class mainTask extends TimerTask
    {
        public void run()
        {
            SharedPreferences sp1 = getSharedPreferences(Constants.SHARE_PREF,  0);
            user_stime = sp1.getInt(Constants.USER_STIME, 0);
            todayShiftId = sp1.getString(Constants.SHIFT_ID, null);
            String USER_SendPos = sp1.getString(Constants.USER_SendPos, null);
//            Log.d("asd", "---------------- this is USER_SendPos : " + USER_SendPos);
//            Log.d("asd", "---------------- this is todayShiftId : " + todayShiftId);
//            Log.d("asd", "---------------- this is saved_user_stime : " + user_stime);
            if((USER_SendPos == null || !USER_SendPos.equals(todayShiftId)) && todayShiftId != null){
                if(user_stime != 0){
                    field_name = sp1.getString(Constants.FIELD_NAME, null);
                    try {
                        staffAddress = new JSONArray(sp1.getString(Constants.STAFF_ADDRESS, null));
                        if(sp1.getString(Constants.SHIFT_ADDRESS, null) != null){
                            shiftAddress = new JSONArray(sp1.getString(Constants.SHIFT_ADDRESS, null));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    setFieldInfo(field_name);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                    Calendar calendar = Calendar.getInstance();
                    String cu_time = dateFormat.format(calendar.getTime());
                    String[] spTime = cu_time.split(":");

                    int time = 60 * Integer.parseInt(spTime[0]) + Integer.parseInt(spTime[1]);
                    Log.d("asd", "---------------- called time : " + time);
//                    Log.d("asd", "---------------- called require_user_stime : " + user_stime);
                    if (user_stime <= time - 5) {
                        //Location location = getLastKnownLocation();
                        getLastLocation();
                        if (mLocation != null) {
                            SharedPreferences sp2 = getSharedPreferences(Constants.SHARE_PREF,  0);
                            SharedPreferences.Editor Ed2 = sp2.edit();
                            Ed2.putString(Constants.USER_SendPos, todayShiftId);
                            Ed2.apply();

                            latitude = String.valueOf(mLocation.getLatitude());
                            longitude = String.valueOf(mLocation.getLongitude());
                            Log.d("asd", "fused latitude : " + latitude);
                            Log.d("asd", "fused longitude : " + longitude);

                        }
                        token = sp1.getString(Constants.TOKEN, null);
                        mFirebaseTask = new RegisterFirebaseTokenTask(token);
                        mFirebaseTask.execute((Void) null);
                        Log.d("asd", "---------------- called todayShiftId : " + todayShiftId);
                    }
                }
            }

        }
    }

    public void onDestroy()
    {
        super.onDestroy();
        //Toast.makeText(this, "Service Stopped ...", Toast.LENGTH_SHORT).show();
    }

    private void setFieldInfo(String field_name){
        String fieldId = null;
        if(shiftAddress != null){
            for (int j = 0; j < shiftAddress.length(); j++){
                try {
                    JSONObject obj = shiftAddress.getJSONObject(j);
                    if (obj.has(todayShiftId) && !obj.isNull(todayShiftId)) {
                        fieldId = obj.getString(todayShiftId);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        if(staffAddress != null){
            for(int i = 0; i < staffAddress.length(); i++){
                try {
                    JSONObject explrObject = staffAddress.getJSONObject(i);
                    if(fieldId != null){
                        if (explrObject.has("address") && !explrObject.isNull("address")){
                            String fid = explrObject.getString("address");
                            if(fid.equals(fieldId)){
                                if (explrObject.has("field") && !explrObject.isNull("field")) {
                                    // Do something with object.
                                    JSONObject field = explrObject.getJSONObject("field");
                                    String fieldName = field.getString("name");

                                    if(fieldName.equals(field_name)){
                                        if (explrObject.has("required_time") && !explrObject.isNull("required_time")) {
                                            int required_time = explrObject.getInt("required_time");
                                            user_stime = user_stime - required_time;
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else{
                        if (explrObject.has("field") && !explrObject.isNull("field")) {
                            // Do something with object.
                            JSONObject field = explrObject.getJSONObject("field");
                            String fieldName = field.getString("name");

                            if(fieldName.equals(field_name)){
                                if (explrObject.has("required_time") && !explrObject.isNull("required_time")) {
                                    int required_time = explrObject.getInt("required_time");
                                    user_stime = user_stime - required_time;
                                    return;
                                }

                            }
                        }
                    }



                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

//    @Override
//    public IBinder onBind(Intent intent) {
//        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
//    }
//    public int onStartCommand(Intent intent, int flags, int startId) {
//
//        someHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
//                Calendar calendar = Calendar.getInstance();
//                String cu_time = dateFormat.format(calendar.getTime());
//                String[] spTime = cu_time.split(":");
//                SharedPreferences sp1 = getSharedPreferences(Constants.SHARE_PREF, 0);
//                user_stime = sp1.getInt(Constants.USER_STIME, -1);
//                if(user_stime != -1){
//                    field_name = sp1.getString(Constants.FIELD_NAME, null);
//                    try {
//                        staffAddress = new JSONArray(sp1.getString(Constants.STAFF_ADDRESS, null));
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//
//                    setFieldInfo(field_name);
//
//                    int time = 60 * Integer.parseInt(spTime[0]) + Integer.parseInt(spTime[1]);
//                    if (user_stime == time - 5 && success == true) {
//                        success = false;
//                        SharedPreferences sp = getSharedPreferences(Constants.SHARE_PREF, 0);
//                        SharedPreferences.Editor Ed = sp.edit();
//                        Ed.putInt(Constants.USER_STIME, -1);
//                        Ed.apply();
//                        token = sp1.getString(Constants.TOKEN, null);
//                        todayShiftId = sp1.getString(Constants.SHIFT_ID, null);
//
////            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
////            locationProvider = mLocationManager.getBestProvider(new Criteria(), true);
////            if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
////                locationProvider = LocationManager.GPS_PROVIDER;
////            }
//
//                        //checkLocationPermission();
//
//                        Location location = getLastKnownLocation();
//
//                        //mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mlocationListener);
//                        //String lati = "";
//
//                        if (location != null) {
//                            latitude = String.valueOf(location.getLatitude());
//                            longitude = String.valueOf(location.getLongitude());
//                        }
//
////            else {
////
////            }
////
////
////
////
//
//                        mFirebaseTask = new RegisterFirebaseTokenTask(token);
//                        mFirebaseTask.execute((Void) null);
////            try {
////                // TODO: handle loggedInUser authentication
////
////                RequestParams rp = new RequestParams();
////                rp.add("shift_id", todayShiftId);
////                rp.add("latitude", latitude);
////                rp.add("longitude", longitude);
////
////                HttpUtils.addToken(token);
////                HttpUtils.post(Constants.CONFIRM_START, rp, new JsonHttpResponseHandler() {
////                    @Override
////                    public void onStart() {
////                        // called before request is started
////                    }
////
////                    @Override
////                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
////                        // If the response is JSONObject instead of expected JSONArray
////
////                        Log.d("asd", "---------------- this is response : " + response);
////                        try {
////                            JSONObject serverResp = new JSONObject(response.toString());
////
////                            String rpMessage = serverResp.optString("message");
////                            if (rpMessage == "ERR_INVALID_TOKEN") {
////                                Toast.makeText(getApplicationContext(), "失敗しました。", Toast.LENGTH_SHORT).show();
////                                return;
////                            }
////                            success = true;
////                        } catch (JSONException e) {
////                            // TODO Auto-generated catch block
////                            e.printStackTrace();
////                        }
////                    }
////
////                    @Override
////                    public void onFailure(int statusCode, Header[] headers, Throwable e, JSONObject response) {
////                        // called when response HTTP status is "4XX" (eg. 401, 403, 404)
////
////                        Log.d("asd", "failure login" + response);
////                        success = false;
////                    }
////
////                    @Override
////                    public void onRetry(int retryNo) {
////                        // called when request is retried
////                    }
////
////                    @Override
////                    public void onSuccess(int statusCode, Header[] headers, JSONArray timeline) {
////                        // Pull out the first event on the public timeline
////
////                    }
////                });
////
////
////            } catch (Exception e) {
////                Log.e("REST_API", "GET method failed: " + e.getMessage());
////                e.printStackTrace();
////
////            }
//                    }
//                }
//
//            }
//        }, 600);
//        //onTaskRemoved(intent);
//
//
//
//
////        Toast.makeText(getApplicationContext(),String.valueOf(time),
////                Toast.LENGTH_SHORT).show();
//
//        return START_STICKY;
//    }

//    @Override
//    public void onTaskRemoved(Intent rootIntent) {
//        Intent restartServiceIntent = new Intent(getApplicationContext(),this.getClass());
//        restartServiceIntent.setPackage(getPackageName());
//        startService(restartServiceIntent);
//        super.onTaskRemoved(rootIntent);
//    }

    LocationListener mlocationListener = new LocationListener() {
        @SuppressLint("DefaultLocale")
        @Override
        public void onLocationChanged(Location location) {

        }

        @Override
        public void onStatusChanged(String provider, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    @SuppressWarnings("MissingPermission")
    private void getLastLocation() {
        mFusedLocationClient.getLastLocation()
                .addOnCompleteListener(
                        new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                if (task.isSuccessful() && task.getResult() != null) {
                                    mLocation = task.getResult();
                                }
                            }
                        });
    }

    private Location getLastKnownLocation() {
        LocationManager mnLocationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = mnLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            //checkLocationPermission();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return bestLocation;
            }
            Location l = mnLocationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    public boolean checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            int fine_location_granted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
            int coarse_location_granted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

            if (fine_location_granted != PackageManager.PERMISSION_GRANTED || coarse_location_granted != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this,
//                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
//                        MY_PERMISSIONS_REQUEST_LOCATION);
                return false;
            }
        } else {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
//                    MY_PERMISSIONS_REQUEST_LOCATION);
        }

        return true;
    }

    private class RegisterFirebaseTokenTask extends AsyncTask<Void, Void, Boolean> {

        private final String mToken;
        private String mErrorMsg;

        RegisterFirebaseTokenTask(String ftoken) {
            mToken = ftoken;
        }

        private boolean isConnected() {
            ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected())
                return true;
            else
                return false;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.
            if (isConnected()) {
                ContentValues postParams = new ContentValues();
                JSONObject result = null;
                try {

                    postParams.put("shift_id", todayShiftId);
                    postParams.put("latitude", latitude);
                    postParams.put("longitude", longitude);

                    HttpPostRequest httpPostRequest = new HttpPostRequest();
                    result = httpPostRequest.POST(Constants.CONFIRM_START, postParams, mToken);

                    if (result == null) return false;
                    if (!result.getString("status").equals("success")) {
                        mErrorMsg = result.getString("message");
                        return false;
                    }
                    //success = true;

                    JSONArray result_str = result.optJSONArray("result");


                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            } else {

                //if (!userPassword.equals(mPassword))
                return false;
            }
            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean succ) {
            mFirebaseTask = null;

            if (succ) {
                success =true;

            } else {

//                if (mErrorMsg == null) {
//                    Toast.makeText(getApplicationContext(), "失敗しました。", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//                switch (mErrorMsg) {
//                    case "ERR_INVALID_FCM_TOKEN":
//                        Toast.makeText(getApplicationContext(), "失敗しました。", Toast.LENGTH_SHORT).show();
//                        break;
//                    case "ERR_INVALID_TOKEN":
//                        Toast.makeText(getApplicationContext(), "失敗しました。", Toast.LENGTH_SHORT).show();
//                        ToLogin();
//                    default:
//                        Toast.makeText(getApplicationContext(), mErrorMsg, Toast.LENGTH_SHORT).show();
//
//                }
            }
        }

        @Override
        protected void onCancelled() {
            mFirebaseTask = null;
        }
    }
}