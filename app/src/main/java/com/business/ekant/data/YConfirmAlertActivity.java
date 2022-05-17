package com.business.ekant.data;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

public class YConfirmAlertActivity extends Activity {
    private final static String TAG = "YConfirmAlertActivity";
    private Activity context;
    private ConfirmTask mAuthTask = null;
    String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        Bundle extras = getIntent().getExtras();
        RemoteMessage msg = (RemoteMessage) extras.get("msg");
        int bodyId;
        String title = null;
        String bodyArg;
        String type = null, titleStr = null, bodyStr = null;

        SharedPreferences sp1 = getSharedPreferences(Constants.SHARE_PREF, 0);
        token = sp1.getString(Constants.TOKEN, null);
        try {
            type = msg.getData().get("type");
            titleStr = msg.getNotification().getTitle();
            bodyStr = msg.getNotification().getBody();
//            bodyId = getResources().getIdentifier(msg.getNotification().getBodyLocalizationKey(), "string", getPackageName());
//            bodyArg = msg.getNotification().getBodyLocalizationArgs()[0];
        } catch (Exception e){
            context.finish();
            return;
        }
        int titleId = getResources().getIdentifier(msg.getNotification().getTitleLocalizationKey(),"string",getPackageName());

        if(type == "confirm_yesterday"){
            String field = msg.getData().get("field");
            String time = msg.getData().get("time");
            String shift_id = msg.getData().get("shift_id");

            AlertDialog alertDialog = new AlertDialog.Builder(YConfirmAlertActivity.this).create();
            alertDialog.setTitle(titleStr);
            alertDialog.setMessage("現場名：" + field + "\n" + "現地出社時間：" + time);

            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "確認しました",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mAuthTask = new ConfirmTask(token, Constants.CONFIRM_YESTERDAY, null, shift_id);
                            mAuthTask.execute((Void) null);
                            dialog.dismiss();

                        }
                    });
            alertDialog.show();
        }else if(type == "confirm_today"){
            String field_admin = msg.getData().get("field_admin");
            String agent_react = msg.getData().get("agent_react");
            String shift_id = msg.getData().get("shift_id");

            AlertDialog alertDialog = new AlertDialog.Builder(YConfirmAlertActivity.this).create();
            alertDialog.setTitle(titleStr);
            alertDialog.setMessage("本日は、渋谷駐車場の出勤日です。よろしくお願いします。\n" +
                    "本日の体調確認です。\n" +
                    "体温は３７度以下ですか。体調は、問題ございませんか。\n");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "は　い",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mAuthTask = new ConfirmTask(token, Constants.CONFIRM_YESTERDAY, "1", shift_id);
                            mAuthTask.execute((Void) null);
                            dialog.dismiss();

                        }
                    });
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "いいえ",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mAuthTask = new ConfirmTask(token, Constants.CONFIRM_YESTERDAY, "0", shift_id);
                            mAuthTask.execute((Void) null);
                            dialog.dismiss();
                            AlertDialog alertDialog = new AlertDialog.Builder(YConfirmAlertActivity.this).create();
                            alertDialog.setTitle("責任者に電話し指示を仰いでください");
                            alertDialog.setMessage("現場責任者:" + field_admin +
                                    "\n" +
                                    "緊急対応者:"+ agent_react +"\n");
                            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "確　認",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            context.finish();
                                        }
                                    });
                            alertDialog.show();
                        }
                    });

            alertDialog.show();
        }
        else{
            AlertDialog alertDialog = new AlertDialog.Builder(YConfirmAlertActivity.this).create();
            alertDialog.setTitle(titleStr);
            alertDialog.setMessage(bodyStr);
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "確　認",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            context.finish();
                        }
                    });
            alertDialog.show();
        }
    }
    private class ConfirmTask extends AsyncTask<Void, Void, Boolean> {

        private final String mToken;
        private String mErrorMsg;
        private String confirm_url = null;
        private String health_status = null;
        private String shift_id = null;

        ConfirmTask(String token, String url, String status, String shift) {
            mToken = token;
            confirm_url = url;
            health_status = status;
            shift_id = shift;
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

                    postParams.put("shift_id", shift_id);
                    if(health_status != null){
                        postParams.put("health_status", health_status);
                    }

                    HttpPostRequest httpPostRequest = new HttpPostRequest();
                    result = httpPostRequest.GET(confirm_url, postParams, mToken);

                    if (result == null) return false;
                    if (!result.getString("status").equals("success")) {
                        mErrorMsg = result.getString("message");
                        return false;
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
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;

            if (success) {
                if(health_status != "0"){
                    context.finish();
                }

            } else {

                if (mErrorMsg == null) {
                    Toast.makeText(getApplicationContext(), "失敗しました。", Toast.LENGTH_SHORT).show();
                    return;
                }
                switch (mErrorMsg) {
                    case "ERR_INVALID_SHIFT_ID":
                        Toast.makeText(getApplicationContext(), "失敗しました。", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        Toast.makeText(getApplicationContext(), mErrorMsg, Toast.LENGTH_SHORT).show();

                }
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
        }
    }

}