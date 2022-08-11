package com.business.ekant.data;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
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
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.business.ekant.MainActivity;
import com.business.ekant.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class LocationService extends Service {
    public static final String BROADCAST_ACTION = "Hello World";
    private static final int TWO_MINUTES = 1000 * 60;
    private static final String CHANNEL_ID = "fcm_default_channel";
    public LocationManager locationManager;
    public MyLocationListener listener;
    public Location previousBestLocation = null;
    Intent intent;
    String token, todayShiftId;
    int user_stime;
    private LocationService.RegisterFirebaseTokenTask mFirebaseTask = null;
    Boolean success = false;
    String latitude = "", longitude = "", field_name = "";
    JSONArray staffAddress = null;
    JSONArray shiftAddress = null;

    @Override
    public void onCreate() {
        super.onCreate();
        intent = new Intent(BROADCAST_ACTION);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocationListener();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000, 0, (LocationListener) listener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000, 0, listener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input = intent.getStringExtra("inputExtra");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentText(input)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocationListener();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return START_NOT_STICKY;
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000, 0, (LocationListener) listener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000, 0, listener);
        //do heavy work on a background thread
        //stopSelf();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /**
     * Checks whether two providers are the same
     */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public static Thread performOnBackgroundThread(final Runnable runnable) {
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {
                }
            }
        };
        t.start();
        return t;
    }

    public class MyLocationListener implements LocationListener {
        public void onLocationChanged(final Location loc) {
            if (isBetterLocation(loc, previousBestLocation)) {
                loc.getLatitude();
                loc.getLongitude();
                intent.putExtra("Latitude", loc.getLatitude());
                intent.putExtra("Longitude", loc.getLongitude());
                intent.putExtra("Provider", loc.getProvider());
                sendBroadcast(intent);
                latitude = String.valueOf(loc.getLatitude());
                longitude = String.valueOf(loc.getLongitude());
                SharedPreferences sp1 = getSharedPreferences(Constants.SHARE_PREF, 0);
                user_stime = sp1.getInt(Constants.USER_STIME, 0);
                todayShiftId = sp1.getString(Constants.SHIFT_ID, null);
                String USER_SendPos = sp1.getString(Constants.USER_SendPos, null);
                Log.d("tag", "user_stime:" + user_stime);
                Log.d("tag", "todayShiftId:" + todayShiftId);
                Log.d("tag", "USER_SendPos:" + USER_SendPos);
                if ((USER_SendPos == null || !USER_SendPos.equals(todayShiftId)) && todayShiftId != null) {
                    if (user_stime != 0) {
                        field_name = sp1.getString(Constants.FIELD_NAME, null);
                        Log.d("tag", "field_name:" + field_name);
                        try {
                            staffAddress = new JSONArray(sp1.getString(Constants.STAFF_ADDRESS, null));
                            if (sp1.getString(Constants.SHIFT_ADDRESS, null) != null) {
                                shiftAddress = new JSONArray(sp1.getString(Constants.SHIFT_ADDRESS, null));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        setFieldInfo(field_name);
                        Log.d("tag", "user_stime:" + user_stime);
                        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                        Calendar calendar = Calendar.getInstance();
                        String cu_time = dateFormat.format(calendar.getTime());
                        String[] spTime = cu_time.split(":");
                        int time = 60 * Integer.parseInt(spTime[0]) + Integer.parseInt(spTime[1]);
                        //if (user_stime <= time) {
                            latitude = String.valueOf(loc.getLatitude());
                            longitude = String.valueOf(loc.getLongitude());
                            token = sp1.getString(Constants.TOKEN, null);
                            Log.d("tag", "latitude:" + latitude);
                            mFirebaseTask = new LocationService.RegisterFirebaseTokenTask(token);
                            mFirebaseTask.execute((Void) null);
                        //}
                    }
                }
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderDisabled(String provider) {
            Toast.makeText(getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT).show();
        }

        public void onProviderEnabled(String provider) {
            Toast.makeText(getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
        }
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
                    if (result.getString("status").equals("success")) {
                        if (locationManager != null) {
                            locationManager.removeUpdates(listener);
                        }
                        Boolean result_status = result.getBoolean("result");
                        if(result_status){
                            SharedPreferences sp2 = getSharedPreferences(Constants.SHARE_PREF, 0);
                            SharedPreferences.Editor Ed2 = sp2.edit();
                            Ed2.putString(Constants.USER_SendPos, todayShiftId);
                            Ed2.putInt(Constants.USER_STIME, 0);
                            Ed2.apply();
                        }

                        //return false;
                    }

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
                success = true;
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

    private void setFieldInfo(String field_name) {
        String fieldId = null;
        if (shiftAddress != null) {
            for (int j = 0; j < shiftAddress.length(); j++) {
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
        if (staffAddress != null) {
            for (int i = 0; i < staffAddress.length(); i++) {
                try {
                    JSONObject explrObject = staffAddress.getJSONObject(i);
                    if (fieldId != null) {
                        if (explrObject.has("address") && !explrObject.isNull("address")) {
                            String fid = explrObject.getString("address");
                            if (fid.equals(fieldId)) {
                                if (explrObject.has("field") && !explrObject.isNull("field")) {
                                    // Do something with object.
                                    JSONObject field = explrObject.getJSONObject("field");
                                    String fieldName = field.getString("name");
                                    if (fieldName.equals(field_name)) {
                                        if (explrObject.has("required_time") && !explrObject.isNull("required_time")) {
                                            int required_time = explrObject.getInt("required_time");
                                            user_stime = user_stime - required_time;
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (explrObject.has("field") && !explrObject.isNull("field")) {
                            // Do something with object.
                            JSONObject field = explrObject.getJSONObject("field");
                            String fieldName = field.getString("name");
                            if (fieldName.equals(field_name)) {
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
}