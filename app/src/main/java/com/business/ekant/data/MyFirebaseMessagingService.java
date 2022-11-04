package com.business.ekant.data;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.business.ekant.MainActivity;
import com.business.ekant.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    String token, fToken;

    private RegisterFirebaseTokenTask mFirebaseTask = null;


    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages
        // are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data
        // messages are the type
        // traditionally used with GCM. Notification messages are only received here in
        // onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated
        // notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages
        // containing both notification
        // and data payloads are treated as notification messages. The Firebase console always
        // sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        //handleMessage(remoteMessage);

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            if (/* Check if data needs to be processed by long running job */ true) {
                // For long-running tasks (10 seconds or more) use WorkManager.
                scheduleJob();
            } else {
                // Handle message within 10 seconds
                handleNow();
            }

        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        String data = String.valueOf(remoteMessage.getData());
        JSONObject json = null;
        try {
            json = new JSONObject(data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String type = remoteMessage.getData().get("type");
        String check_type = remoteMessage.getData().get("check_type");
        String shift_id = remoteMessage.getData().get("shift_id");
        String shift_time = remoteMessage.getData().get("shift_time");
        String admin_tel = remoteMessage.getData().get("admin_tel");
        String field_name = remoteMessage.getData().get("field_name");
        String field_tel = remoteMessage.getData().get("field_tel");
        String emergency_tel = remoteMessage.getData().get("emergency_tel");


        Intent dialogIntent = new Intent(getActivity(), MainActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        dialogIntent.putExtra("msg", "remote");
        dialogIntent.putExtra("type", type);
        dialogIntent.putExtra("check_type", check_type);
        dialogIntent.putExtra("shift_id", shift_id);
        dialogIntent.putExtra("shift_time", shift_time);
        dialogIntent.putExtra("admin_tel", admin_tel);
        dialogIntent.putExtra("field_name", field_name);
        dialogIntent.putExtra("field_tel", field_tel);
        dialogIntent.putExtra("emergency_tel", emergency_tel);
        startActivity(dialogIntent);

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

    public static Activity getActivity() {
        try {
            Class activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);

            Map<Object, Object> activities = (Map<Object, Object>) activitiesField.get(activityThread);
            if (activities == null)
                return null;

            for (Object activityRecord : activities.values()) {
                Class activityRecordClass = activityRecord.getClass();
                Field pausedField = activityRecordClass.getDeclaredField("paused");
                pausedField.setAccessible(true);
                if (!pausedField.getBoolean(activityRecord)) {
                    Field activityField = activityRecordClass.getDeclaredField("activity");
                    activityField.setAccessible(true);
                    return (Activity) activityField.get(activityRecord);
                }
            }

            return null;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private void handleMessage(RemoteMessage remoteMessage) {
        String title = remoteMessage.getNotification().getTitle();
        sendNotification(remoteMessage.getData().get("type"));
    }
    // [END receive_message]


    // [START on_new_token]
    /**
     * Called if FCM registration token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * FCM registration token is initially generated so this is where you would retrieve
     * the token.
     */
    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);

        fToken = token;
        SharedPreferences sp = getSharedPreferences(Constants.SHARE_PREF, 0);
        SharedPreferences.Editor Ed = sp.edit();
        Ed.putString(Constants.FTOKEN, token);
        Ed.apply();

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token);
    }
    // [END on_new_token]

    /**
     * Schedule async work using WorkManager.
     */
    private void scheduleJob() {
        // [START dispatch_job]
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(MyWorker.class)
                .build();
        WorkManager.getInstance().beginWith(work).enqueue();
        // [END dispatch_job]
    }

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private void handleNow() {
        Log.d(TAG, "Short lived task is done.");
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM registration token with any
     * server-side account maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        // TODO: Implement this method to send token to your app server.
        SharedPreferences sp1 = getSharedPreferences(Constants.SHARE_PREF, 0);
        String userToken = sp1.getString(Constants.FTOKEN, null);
        if(userToken != null){
            mFirebaseTask = new RegisterFirebaseTokenTask(userToken);
            mFirebaseTask.execute((Void) null);
        }

    }

    private void sendYConfirm(String messageBdoy){
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private void sendNotification(String messageBody) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setContentTitle(getString(R.string.fcm_message))
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

    private void saveMessageInfo(RemoteMessage remoteMessage){
        String title = null;
        String bodyArg;
        String type = null, titleStr = null, bodyStr = null;
        type = remoteMessage.getData().get("type");
        titleStr = remoteMessage.getNotification().getTitle();
        bodyStr = remoteMessage.getNotification().getBody();
        SharedPreferences sp = getSharedPreferences(Constants.SHARE_PREF, 0);
        SharedPreferences.Editor Ed = sp.edit();
        Ed.putString(Constants.REMOTE_BODY, bodyStr);
        Ed.putString(Constants.REMOTE_TITLE, titleStr);
        Ed.putString(Constants.REMOTE_TYPE, type);
        Ed.putString(Constants.REMOTE_ADMIN_TEL, remoteMessage.getData().get("admin_tel"));
        Ed.putString(Constants.REMOTE_EMERGENCY_TEL, remoteMessage.getData().get("emergency_tel"));
        Ed.putString(Constants.REMOTE_FIELD_NAME, remoteMessage.getData().get("field_name"));
        Ed.putString(Constants.REMOTE_FIELD_TEL, remoteMessage.getData().get("field_tel"));
        Ed.putString(Constants.REMOTE_SHIFT_ID, remoteMessage.getData().get("shift_id"));
        Ed.putString(Constants.REMOTE_SHIFT_TIME, remoteMessage.getData().get("shift_time"));
        Ed.apply();
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
                    postParams.put("fcm_token", fToken);

                    HttpPostRequest httpPostRequest = new HttpPostRequest();
                    result = httpPostRequest.POST(Constants.UPDATE_DEVICE_TOKEN, postParams, token);

                    if (result == null) return false;
                    if (!result.getString("status").equals("success")) {
                        mErrorMsg = result.getString("message");
                        return false;
                    }

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
        protected void onPostExecute(final Boolean success) {
            mFirebaseTask = null;

            if (success) {

            } else{

                if(mErrorMsg == null){
                    Toast.makeText(getApplicationContext(), "失敗しました。", Toast.LENGTH_SHORT).show();
                    return;
                }
                switch (mErrorMsg){
                    case "ERR_INVALID_FCM_TOKEN":
                        Toast.makeText(getApplicationContext(), "失敗しました。", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        Toast.makeText(getApplicationContext(), mErrorMsg, Toast.LENGTH_SHORT).show();

                }
            }
        }

        @Override
        protected void onCancelled() {
            mFirebaseTask = null;
        }
    }
}
