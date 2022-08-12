package com.business.ekant;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.business.ekant.data.BackPressCloseHandler;
import com.business.ekant.data.LocationService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationAvailability;
import com.business.ekant.data.Constants;
import com.business.ekant.data.HttpPostRequest;
import com.business.ekant.data.HttpUtils;
//import com.example.ekant.data.databinding.ActivityMainBinding;
import com.business.ekant.data.SendPosService;
import com.business.ekant.ui.login.LoginActivity;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

import static android.content.SharedPreferences.*;

public class MainActivity extends AppCompatActivity {

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private static final String TAG = "MyFirebaseMsgService";
    private static final int RSS_JOB_ID = 1000;
    private static final String NOSHIFT = "no_shift";
    private static final String BEFORE_ARRIVE = "before_status";
    private static final String CAN_EARLY_LEAVE = "can_early_leave";
    private static final String CAN_LEAVE = "can_leave";
    private static final String CAN_REST = "can_rest";
    private static final String AFTER_LEAVE = "after_leave";

    int user_stime;

    /**
     * Contains parameters used by {@link com.google.android.gms.location.FusedLocationProviderApi}.
     */
    private LocationRequest mLocationRequest;

    /**
     * Provides access to the Fused Location Provider API.
     */
    public FusedLocationProviderClient mFusedLocationClient;
    private Location mLocation;
    String mLatitude = "", mLongitude ="", field_name = "";

    /**
     * Callback for changes in location.
     */
    //private LocationCallback mLocationCallback;
    Location mLastLocation;
    String token, fToken = null;
    TextView latitude;
    Boolean isLast = false;
    String todayShiftId = null;
    Double longitude_field, latitude_field, longitude_cur, latitude_cur, distance;
    ArrayList<String> mylist;

    int break_at_int = 0, break_time = 0;

    int todaySTime = -1;
    int todayETime = -1;
    String r_time = null;

    JSONArray resultShift, staffAddress = null;
    JSONObject todayShift = null, lastShift = null;

    Button arriveBtn_index = null;
    Button earlyLeaveBtn_index = null;
    Button leaveBtn_index = null;
    Button restBtn_index = null;
    Button restNightBtn_index = null;

    private SendPositionTask mSendPositionTask = null;
    private UserInfoTask mAuthTask = null;
    private ConfirmTask mConfirmTask = null;
    private UserLogoutTask mLogoutTask = null;
    private RegisterFirebaseTokenTask mFirebaseTask = null;

    private BackPressCloseHandler backPressCloseHandler;

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setContentView(R.layout.activity_main);
        SharedPreferences sp1 = getSharedPreferences(Constants.SHARE_PREF, 0);
        String displayName = sp1.getString(Constants.USER_NAME, null);
        String staff_address = sp1.getString(Constants.STAFF_ADDRESS, null);
        if (staff_address != null) {
            try {
                staffAddress = new JSONArray(staff_address);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        checkIntent(getIntent());

        token = sp1.getString(Constants.TOKEN, null);
        fToken = sp1.getString(Constants.FTOKEN, null);
        if (fToken != null) {
            mFirebaseTask = new RegisterFirebaseTokenTask(fToken);
            mFirebaseTask.execute((Void) null);
        }

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        if (fToken == null) {
                            fToken = task.getResult();
                            mFirebaseTask = new RegisterFirebaseTokenTask(fToken);
                            mFirebaseTask.execute((Void) null);
                        }
                    }
                });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            String channelId = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            Uri NOTIFICATION_SOUND_URI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();
            final long[] VIBRATE_PATTERN = {0, 500};
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            channel.setSound(NOTIFICATION_SOUND_URI, audioAttributes);
            channel.setVibrationPattern(VIBRATE_PATTERN);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        latitude = (TextView) findViewById(R.id.place_name);

        final Button requestButton = (Button) findViewById(R.id.btn_request);
        Button arriveBtn = (Button) findViewById(R.id.btn_arrive);
        arriveBtn.setEnabled(false);
        arriveBtn_index = arriveBtn;
        final Button earlyLeaveBtn = (Button) findViewById(R.id.btn_leave_early);
        earlyLeaveBtn.setEnabled(false);
        earlyLeaveBtn_index = earlyLeaveBtn;
        final Button leaveBtn = (Button) findViewById(R.id.btn_leave);
        leaveBtn.setEnabled(false);
        leaveBtn_index = leaveBtn;
        final Button restBtn = (Button) findViewById(R.id.btn_rest);
        restBtn.setEnabled(false);
        restBtn_index = restBtn;
        final Button restNightBtn = (Button) findViewById(R.id.btn_rest_night);
        restNightBtn.setEnabled(false);
        restNightBtn_index = restNightBtn;
        backPressCloseHandler = new BackPressCloseHandler(this);

        final TextView tvClock = (TextView) findViewById(R.id.txt_time);
        final TextView tvDate = (TextView) findViewById(R.id.txt_date);
        final TextView displayNameArea = (TextView) findViewById(R.id.staff_name);

        final ImageView logoutIcon = (ImageView) findViewById(R.id.logout_icon);

        displayNameArea.setText(displayName);

        mAuthTask = new UserInfoTask(token);
        mAuthTask.execute((Void) null);

        final Handler someHandler = new Handler(getMainLooper());
        someHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                tvClock.setText(new SimpleDateFormat("HH:mm", Locale.JAPAN).format(new Date()));
                tvDate.setText(new SimpleDateFormat("yyyy年MM月dd日", Locale.JAPAN).format(new Date()));
                JSONObject todayShift = checkTodayShift();
                //checkStatusButton();
                checkButtonStatus(todayShift);
                someHandler.postDelayed(this, 1000);

            }
        }, 10);

        Spinner spinner = findViewById(R.id.spinner_rest_time);
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("15");
        arrayList.add("30");
        arrayList.add("45");
        arrayList.add("60");
        arrayList.add("75");
        arrayList.add("90");
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.my_spinner, arrayList);
        arrayAdapter.setDropDownViewResource(R.layout.spinner_layout);
        spinner.setAdapter(arrayAdapter);
        int spinnerPosition = arrayAdapter.getPosition("60");
        spinner.setSelection(spinnerPosition);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                r_time = selectedItem;
            } // to close the onItemSelected

            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        logoutIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                builder1.setMessage("ログアウトしますか？");
                builder1.setCancelable(true);

                builder1.setPositiveButton(
                        "確認",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mLogoutTask = new UserLogoutTask(token);
                                mLogoutTask.execute((Void) null);
                                dialog.cancel();
                            }
                        });
                AlertDialog alert11 = builder1.create();
                alert11.show();
                final Button positiveButton = alert11.getButton(AlertDialog.BUTTON_POSITIVE);
                LinearLayout.LayoutParams positiveButtonLL = (LinearLayout.LayoutParams) positiveButton.getLayoutParams();
                positiveButtonLL.gravity = Gravity.CENTER;
                positiveButton.setLayoutParams(positiveButtonLL);

            }
        });

        arriveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Location location = getLastKnownLocation();
                if (location == null) {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                    builder1.setMessage("位置情報を得ることができません。\n" +
                            "ネットワークやGPSの状態を確認してください。");
                    builder1.setCancelable(true);

                    builder1.setPositiveButton(
                            "確認",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert11 = builder1.create();
                    alert11.show();
                    return;
                }
                if (!getIsNear(location)) {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                    builder1.setMessage("出勤場所ではありません。\n" +
                            "勤務場所でボタンを押してください。お願いします。");
                    builder1.setCancelable(true);

                    builder1.setPositiveButton(
                            "確認",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert11 = builder1.create();
                    alert11.show();
                    final Button positiveButton = alert11.getButton(AlertDialog.BUTTON_POSITIVE);
                    LinearLayout.LayoutParams positiveButtonLL = (LinearLayout.LayoutParams) positiveButton.getLayoutParams();
                    positiveButtonLL.gravity = Gravity.CENTER;
                    positiveButton.setLayoutParams(positiveButtonLL);

                } else {
                    Date date = new Date();
                    int minute = date.getMinutes();
                    int hours = date.getHours();
                    int nowSTime = 60 * hours + minute;
                    if (isLast) {
                        nowSTime += 1440;
                    }
                    if (nowSTime > todaySTime) {
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                        builder1.setMessage("シフト外勤務が発生しているため勤怠申請(遅刻)をお願いします。");
                        builder1.setCancelable(true);

                        builder1.setPositiveButton(
                                "取消",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                        builder1.setNegativeButton(
                                "申請",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                        Date date = new Date();
                                        int minute = date.getMinutes();
                                        int hours = date.getHours();
                                        int timeInt = 60 * hours + minute;
                                        if (isLast) {
                                            timeInt += 1440;
                                        }
                                        String url = "api/v1/client/confirm-arrive";
                                        getResponse(url, todayShiftId, String.valueOf(timeInt));
                                    }
                                });


                        AlertDialog alert11 = builder1.create();
                        alert11.setOnShowListener(new DialogInterface.OnShowListener() {
                            @Override
                            public void onShow(DialogInterface dialog) {

                                Button negButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                );
                                params.setMargins(20, 0, 20, 0);
                                negButton.setLayoutParams(params);
                            }
                        });
                        alert11.show();

                    } else {
                        String url = "api/v1/client/confirm-arrive";
                        getResponse(url, todayShiftId, null);
                    }
                }
            }
        });

        leaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Location location = getLastKnownLocation();

                if (location == null) {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                    builder1.setMessage("位置情報を得ることができません。\n" +
                            "ネットワークやGPSの状態を確認してください。");
                    builder1.setCancelable(true);

                    builder1.setPositiveButton(
                            "確認",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert11 = builder1.create();
                    alert11.show();
                    return;
                }
                if (!getIsNear(location)) {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                    builder1.setMessage("出勤場所ではありません。\n" +
                            "勤務場所でボタンを押してください。お願いします。");
                    builder1.setCancelable(true);

                    builder1.setPositiveButton(
                            "確認",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert11 = builder1.create();
                    alert11.show();
                    final Button positiveButton = alert11.getButton(AlertDialog.BUTTON_POSITIVE);
                    LinearLayout.LayoutParams positiveButtonLL = (LinearLayout.LayoutParams) positiveButton.getLayoutParams();
                    positiveButtonLL.gravity = Gravity.CENTER;
                    positiveButton.setLayoutParams(positiveButtonLL);
                } else {
                    Date date = new Date();
                    int minute = date.getMinutes();
                    int hours = date.getHours();
                    int nowSTime = 60 * hours + minute;
                    if (isLast) {
                        nowSTime += 1440;
                    }
                    if (nowSTime > todayETime + 14) {
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                        builder1.setMessage("シフト外勤務が発生しているため勤怠申請(残業)をお願いします。");
                        builder1.setCancelable(true);

                        builder1.setPositiveButton(
                                "取消",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                        builder1.setNegativeButton(
                                "申請",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                        Date date = new Date();
                                        int minute = date.getMinutes();
                                        int hours = date.getHours();
                                        int timeInt = 60 * hours + minute;
                                        if (isLast) {
                                            timeInt += 1440;
                                        }
                                        String url = "api/v1/client/confirm-leave";
                                        getResponse(url, todayShiftId, String.valueOf(timeInt));
                                    }
                                });

                        AlertDialog alert11 = builder1.create();
                        alert11.setOnShowListener(new DialogInterface.OnShowListener() {
                            @Override
                            public void onShow(DialogInterface dialog) {

                                Button negButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                );
                                params.setMargins(20, 0, 20, 0);
                                negButton.setLayoutParams(params);
                            }
                        });
                        alert11.show();
                        final Button negButton = alert11.getButton(AlertDialog.BUTTON_NEGATIVE);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        params.setMargins(20, 0, 0, 0);
                        negButton.setLayoutParams(params);

                    } else if (nowSTime < todayETime) {
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                        builder1.setMessage("シフト外勤務が発生しているため勤怠申請(早退)をお願いします。");
                        builder1.setCancelable(true);

                        builder1.setPositiveButton(
                                "取消",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                        builder1.setNegativeButton(
                                "申請",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                        Date date = new Date();
                                        int minute = date.getMinutes();
                                        int hours = date.getHours();
                                        int timeInt = 60 * hours + minute;
                                        if (isLast) {
                                            timeInt += 1440;
                                        }
                                        String url = "api/v1/client/confirm-leave";
                                        getResponse(url, todayShiftId, String.valueOf(timeInt));

                                    }
                                });

                        AlertDialog alert11 = builder1.create();
                        alert11.setOnShowListener(new DialogInterface.OnShowListener() {
                            @Override
                            public void onShow(DialogInterface dialog) {

                                Button negButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                );
                                params.setMargins(20, 0, 20, 0);
                                negButton.setLayoutParams(params);
                            }
                        });
                        alert11.show();
                        final Button negButton = alert11.getButton(AlertDialog.BUTTON_NEGATIVE);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        params.setMargins(20, 0, 20, 0);
                        negButton.setLayoutParams(params);
                    } else {
                        String url = "api/v1/client/confirm-leave";
                        getResponse(url, todayShiftId, null);
                    }
                }

            }
        });

        earlyLeaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Location location = getLastKnownLocation();
                if (location == null) {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                    builder1.setMessage("位置情報を得ることができません。\n" +
                            "ネットワークやGPSの状態を確認してください。");
                    builder1.setCancelable(true);

                    builder1.setPositiveButton(
                            "確認",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert11 = builder1.create();
                    alert11.show();
                    return;
                }
                if (!getIsNear(location)) {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                    builder1.setMessage("出勤場所ではありません。\n" +
                            "勤務場所でボタンを押してください。お願いします。");
                    builder1.setCancelable(true);

                    builder1.setPositiveButton(
                            "確認",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert11 = builder1.create();
                    alert11.show();
                    final Button positiveButton = alert11.getButton(AlertDialog.BUTTON_POSITIVE);
                    LinearLayout.LayoutParams positiveButtonLL = (LinearLayout.LayoutParams) positiveButton.getLayoutParams();
                    positiveButtonLL.gravity = Gravity.CENTER;
                    positiveButton.setLayoutParams(positiveButtonLL);
                } else {
                    Date date = new Date();
                    int minute = date.getMinutes();
                    int hours = date.getHours();
                    int nowSTime = 60 * hours + minute;
//                    if (nowSTime > todaySTime) {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                    builder1.setMessage("シフト外勤務が発生しているため勤怠申請(早退)をお願いします。");
                    builder1.setCancelable(true);

                    builder1.setPositiveButton(
                            "取消",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    builder1.setNegativeButton(
                            "申請",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                    dialog.cancel();
                                    Date date = new Date();
                                    int minute = date.getMinutes();
                                    int hours = date.getHours();
                                    int timeInt = 60 * hours + minute;
                                    if (isLast) {
                                        timeInt += 1440;
                                    }
                                    String url = "api/v1/client/confirm-leave";
                                    getResponse(url, todayShiftId, String.valueOf(timeInt));
                                }
                            });

                    AlertDialog alert11 = builder1.create();
                    alert11.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialog) {

                            Button negButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            params.setMargins(20, 0, 20, 0);
                            negButton.setLayoutParams(params);
                        }
                    });
                    alert11.show();
                    final Button negButton = alert11.getButton(AlertDialog.BUTTON_NEGATIVE);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(20, 0, 0, 0);
                    negButton.setLayoutParams(params);
                }
            }
        });

        restBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Location location = getLastKnownLocation();
                if (!getIsNear(location)) {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                    builder1.setMessage("出勤場所ではありません。\n" +
                            "勤務場所でボタンを押してください。お願いします。");
                    builder1.setCancelable(true);

                    builder1.setPositiveButton(
                            "確認",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert11 = builder1.create();
                    alert11.show();
                    final Button positiveButton = alert11.getButton(AlertDialog.BUTTON_POSITIVE);
                    LinearLayout.LayoutParams positiveButtonLL = (LinearLayout.LayoutParams) positiveButton.getLayoutParams();
                    positiveButtonLL.gravity = Gravity.CENTER;
                    positiveButton.setLayoutParams(positiveButtonLL);
                    return;
                }
                if (r_time == null || r_time == "0") {
                    return;
                }
                Date date = new Date();
                int minute = date.getMinutes();
                int hours = date.getHours();
                int nowSTime = 60 * hours + minute;
                int s_time;

                String url = "api/v1/client/confirm-break";
                try {
                    // TODO: handle loggedInUser authentication

                    RequestParams rp = new RequestParams();
                    rp.add("shift_id", todayShiftId);
                    //rp.add("rest_at", String.valueOf(s_time));
                    rp.add("break_time", r_time);

                    HttpUtils.addToken(token);
                    HttpUtils.post(url, rp, new JsonHttpResponseHandler() {
                        @Override
                        public void onStart() {
                            // called before request is started
                        }

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                            // If the response is JSONObject instead of expected JSONArray

                            Log.d("asd", "---------------- this is response : " + response);
                            try {
                                JSONObject serverResp = new JSONObject(response.toString());

                                String rpMessage = serverResp.optString("message");
                                if (rpMessage.equals("成功"))
                                    rpMessage = "休憩が登録されました。";
                                if (rpMessage == "ERR_INVALID_TOKEN") {
                                    Toast.makeText(getApplicationContext(), "失敗しました。", Toast.LENGTH_SHORT).show();
                                    ToLogin();
                                    return;
                                }
                                Toast toast = Toast.makeText(getApplicationContext(), rpMessage, Toast.LENGTH_LONG);
//                                View view = toast.getView();
//                                TextView text = (TextView) view.findViewById(android.R.id.message);
//                                text.setTextSize(20);
                                ViewGroup group = (ViewGroup) toast.getView();
                                TextView messageTextView = (TextView) group.getChildAt(0);
                                messageTextView.setTextSize(20);
                                toast.show();
                                mAuthTask = new UserInfoTask(token);
                                mAuthTask.execute((Void) null);

                            } catch (JSONException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable e, JSONObject response) {
                            // called when response HTTP status is "4XX" (eg. 401, 403, 404)

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


                } catch (Exception e) {
                    Log.e("REST_API", "GET method failed: " + e.getMessage());
                    e.printStackTrace();

                }

            }
        });

        restNightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Location location = getLastKnownLocation();
                if (!getIsNear(location)) {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                    builder1.setMessage("出勤場所ではありません。\n" +
                            "勤務場所でボタンを押してください。お願いします。");
                    builder1.setCancelable(true);

                    builder1.setPositiveButton(
                            "確認",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert11 = builder1.create();
                    alert11.show();
                    final Button positiveButton = alert11.getButton(AlertDialog.BUTTON_POSITIVE);
                    LinearLayout.LayoutParams positiveButtonLL = (LinearLayout.LayoutParams) positiveButton.getLayoutParams();
                    positiveButtonLL.gravity = Gravity.CENTER;
                    positiveButton.setLayoutParams(positiveButtonLL);
                    return;
                }
                if (r_time == null || r_time == "0") {
                    return;
                }
                Date date = new Date();
                int minute = date.getMinutes();
                int hours = date.getHours();
                int nowSTime = 60 * hours + minute;
                int rest = nowSTime - (int) (nowSTime / 15) * 15;
                int s_time;
                if (rest < 8) {
                    s_time = nowSTime - rest;
                } else {
                    s_time = nowSTime + (15 - rest);
                }

                String url = "api/v1/client/confirm-break";
                try {
                    // TODO: handle loggedInUser authentication

                    RequestParams rp = new RequestParams();
                    rp.add("shift_id", todayShiftId);
                    //rp.add("rest_at", String.valueOf(s_time));
                    rp.add("break_time", r_time);

                    HttpUtils.addToken(token);
                    HttpUtils.post(url, rp, new JsonHttpResponseHandler() {
                        @Override
                        public void onStart() {
                            // called before request is started
                        }

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                            // If the response is JSONObject instead of expected JSONArray

                            Log.d("asd", "---------------- this is response : " + response);
                            try {
                                JSONObject serverResp = new JSONObject(response.toString());

                                String rpMessage = serverResp.optString("message");
                                if (rpMessage.equals("成功"))
                                    rpMessage = "休憩が登録されました。";
                                if (rpMessage == "ERR_INVALID_TOKEN") {
                                    Toast.makeText(getApplicationContext(), "失敗しました。", Toast.LENGTH_SHORT).show();
                                    ToLogin();
                                    return;
                                }
                                Toast toast = Toast.makeText(getApplicationContext(), rpMessage, Toast.LENGTH_LONG);
                                View view = toast.getView();
//                                TextView text = (TextView) view.findViewById(android.R.id.message);
//                                text.setTextSize(20);
//                                toast.show();
                                ViewGroup group = (ViewGroup) toast.getView();
                                TextView messageTextView = (TextView) group.getChildAt(0);
                                messageTextView.setTextSize(20);
                                mAuthTask = new UserInfoTask(token);
                                mAuthTask.execute((Void) null);

                            } catch (JSONException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable e, JSONObject response) {
                            // called when response HTTP status is "4XX" (eg. 401, 403, 404)

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


                } catch (Exception e) {
                    Log.e("REST_API", "GET method failed: " + e.getMessage());
                    e.printStackTrace();

                }

            }
        });

        requestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                intent = new Intent(MainActivity.this, ShowHistoryActivity.class);
                startActivity(intent);
            }
        });

        /*
         * Creates a new Intent to start the RSSPullService
         * JobIntentService. Passes a URI in the
         * Intent's "data" field.
         */

    }

    public void onResume() {
        super.onResume();
        mAuthTask = new UserInfoTask(token);
        mAuthTask.execute((Void) null);
        //startService();
    }

    public void onPause() {
        super.onPause();
    }

    private View.OnTouchListener otl = new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                EditText input = (EditText) v;
                Calendar mcurrentTime = Calendar.getInstance();
                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                int minute = mcurrentTime.get(Calendar.MINUTE);
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        Calendar datetime = Calendar.getInstance();
                        Calendar c = Calendar.getInstance();
                        datetime.set(Calendar.HOUR_OF_DAY, selectedHour);
                        datetime.set(Calendar.MINUTE, selectedMinute);
                        if (selectedHour * 60 + selectedMinute < todaySTime + 14) {
                            Toast.makeText(getApplicationContext(), "正確な時間を入力してください。", Toast.LENGTH_LONG).show();
                        } else if (c.getTime().compareTo(datetime.getTime()) >= 0) {
                            //it's before current
                            input.setText(selectedHour + ":" + selectedMinute);
                        } else {
                            //it's after current'
                            Toast.makeText(getApplicationContext(), "現在の時刻以前に選択してください。", Toast.LENGTH_LONG).show();
                        }

                    }
                }, hour, minute, true);//Yes 24 hour time
                mTimePicker.setTitle("時間の選択");
                mTimePicker.show();
            }

            return true; // the listener has consumed the event
        }
    };
    private View.OnTouchListener otl_leave = new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                EditText input = (EditText) v;
                Calendar mcurrentTime = Calendar.getInstance();
                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                int minute = mcurrentTime.get(Calendar.MINUTE);
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        Calendar datetime = Calendar.getInstance();
                        Calendar c = Calendar.getInstance();
                        datetime.set(Calendar.HOUR_OF_DAY, selectedHour);
                        datetime.set(Calendar.MINUTE, selectedMinute);
                        if (selectedHour * 60 + selectedMinute < todayETime + 14) {
                            Toast.makeText(getApplicationContext(), "正確な時間を入力してください。", Toast.LENGTH_LONG).show();
                        } else if (c.getTime().compareTo(datetime.getTime()) >= 0) {
                            //it's before current
                            input.setText(selectedHour + ":" + selectedMinute);
                        } else {
                            //it's after current'
                            Toast.makeText(getApplicationContext(), "現在の時刻以前に選択してください。", Toast.LENGTH_LONG).show();
                        }

                    }
                }, hour, minute, true);//Yes 24 hour time
                mTimePicker.setTitle("時間の選択");
                mTimePicker.show();
            }

            return true; // the listener has consumed the event
        }
    };
    private View.OnTouchListener otl_eleave = new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                EditText input = (EditText) v;
                Calendar mcurrentTime = Calendar.getInstance();
                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                int minute = mcurrentTime.get(Calendar.MINUTE);
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        Calendar datetime = Calendar.getInstance();
                        Calendar c = Calendar.getInstance();
                        datetime.set(Calendar.HOUR_OF_DAY, selectedHour);
                        datetime.set(Calendar.MINUTE, selectedMinute);
                        if (selectedHour * 60 + selectedMinute > todayETime - 14) {
                            Toast.makeText(getApplicationContext(), "正確な時間を入力してください。", Toast.LENGTH_LONG).show();
                        } else if (c.getTime().compareTo(datetime.getTime()) <= 0) {
                            //it's before current
                            input.setText(selectedHour + ":" + selectedMinute);
                        } else {
                            //it's after current'
                            Toast.makeText(getApplicationContext(), "現在の時刻以降に選択してください。", Toast.LENGTH_LONG).show();
                        }

                    }
                }, hour, minute, true);//Yes 24 hour time
                mTimePicker.setTitle("時間の選択");
                mTimePicker.show();
            }

            return true; // the listener has consumed the event
        }
    };

    public void onBackPressed() {
        backPressCloseHandler.onBackPressed();
    }

    LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationAvailability(LocationAvailability locationAvailability) {
            if (locationAvailability.isLocationAvailable() == false) {
                //Toast.makeText(m_instance, "LocationAvailable false", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                //Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                mLastLocation = location;
            }
        }


    };

    private void checkLocationPermission1() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(getApplicationContext())
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

    private void setFieldInfo(String field_id) {
        if (staffAddress != null) {
            for (int i = 0; i < staffAddress.length(); i++) {
                try {
                    JSONObject explrObject = staffAddress.getJSONObject(i);
                    String fieldId = null;
                    if (explrObject.has("field_id") && !explrObject.isNull("field_id")) {
                        // Do something with object.
                        fieldId = explrObject.getString("field_id");
                    }
                    if (fieldId == field_id) {
                        if (explrObject.has("field") && !explrObject.isNull("field")) {
                            // Do something with object.
                            JSONObject field = explrObject.getJSONObject("field");
                            String fieldName = field.getString("name");
                            latitude.setText(fieldName);
                            SharedPreferences sp = getSharedPreferences(Constants.SHARE_PREF, 0);
                            Editor Ed = sp.edit();
                            Ed.apply();
                            longitude_field = Double.parseDouble(field.getString("longitude"));
                            latitude_field = Double.parseDouble(field.getString("latitude"));
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void setFieldArr(String field) {
        mylist = new ArrayList<String>();

        if (staffAddress != null) {
            for (int i = 0; i < staffAddress.length(); i++) {
                try {
                    JSONObject explrObject = staffAddress.getJSONObject(i);
                    String address = null;
                    if (explrObject.has("field") && !explrObject.isNull("field")) {
                        // Do something with object.
                        JSONObject field_name = explrObject.getJSONObject("field");
                        String fieldName = field_name.getString("name");
                        if (field.equals(fieldName)) {
                            if (explrObject.has("address") && !explrObject.isNull("address")) {
                                // Do something with object.
                                address = explrObject.getString("address");
                                mylist.add(address);
                            }
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getFieldId(String str) {
        if (staffAddress != null) {
            for (int i = 0; i < staffAddress.length(); i++) {
                try {
                    JSONObject explrObject = staffAddress.getJSONObject(i);
                    String address = null;
                    if (explrObject.has("address") && !explrObject.isNull("address")) {
                        // Do something with object.
                        address = explrObject.getString("address");
                        if (str == address) {
                            if (explrObject.has("id") && !explrObject.isNull("id")) {
                                // Do something with object.
                                return explrObject.getString("id");
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    String t_arrive_checked_at = null, t_leave_checked_at = null, t_break_at = null, t_shift_date = null, t_shiftId = null, t_fieldId = null,
            e_leave_at = null, e_leave_checked_at = null, over_time_at = null, over_time_checked_at = null;
    int t_break_time = 0, last_Ttime = 0, last_Etime = 0;

    //get parameters from shiftObj
    private void getParamsObj(JSONObject shiftObj) {
        if (shiftObj == null) {
            t_arrive_checked_at = null;
            t_leave_checked_at = null;
            t_break_at = null;
            t_shift_date = null;
            t_shiftId = null;
            t_fieldId = null;
            t_break_time = 0;
            last_Ttime = 0;
            last_Etime = 0;
            e_leave_at = null;
            e_leave_checked_at = null;
            over_time_at = null;
            over_time_checked_at = null;
            return;
        }
        try {
            t_shift_date = shiftObj.getString("shift_date");
            //get arrive_check_at
            if (shiftObj.has("arrive_checked_at") && !shiftObj.isNull("arrive_checked_at")) {
                // Do something with object.
                t_arrive_checked_at = shiftObj.getString("arrive_checked_at");
            } else {
                t_arrive_checked_at = null;
            }
            //get leave_check_at
            if (shiftObj.has("leave_checked_at") && !shiftObj.isNull("leave_checked_at")) {
                // Do something with object.
                t_leave_checked_at = shiftObj.getString("leave_checked_at");
            } else {
                t_leave_checked_at = null;
            }
            if (shiftObj.has("e_leave_at") && !shiftObj.isNull("e_leave_at")) {
                // e_leave_at
                e_leave_at = shiftObj.getString("e_leave_at");
            } else {
                e_leave_at = null;
            }
            //get leave_check_at
            if (shiftObj.has("e_leave_checked_at") && !shiftObj.isNull("e_leave_checked_at")) {
                // Do something with object.
                e_leave_checked_at = shiftObj.getString("e_leave_checked_at");
            } else {
                e_leave_checked_at = null;
            }
            if (shiftObj.has("over_time_at") && !shiftObj.isNull("over_time_at")) {
                // Do something with object.
                over_time_at = shiftObj.getString("over_time_at");
            } else {
                over_time_at = null;
            }
            //get leave_check_at
            if (shiftObj.has("over_time_checked_at") && !shiftObj.isNull("over_time_checked_at")) {
                // Do something with object.
                over_time_checked_at = shiftObj.getString("over_time_checked_at");
            } else {
                over_time_checked_at = null;
            }
            //get break_at
            if (shiftObj.has("break_at") && !shiftObj.isNull("break_at")) {
                // Do something with object.
                t_break_at = shiftObj.getString("break_at");
            } else {
                t_break_at = null;
            }
            //get break time
            if (shiftObj.has("break_time") && !shiftObj.isNull("break_time")) {
                // Do something with object.
                t_break_time = shiftObj.getInt("break_time");
            } else {
                t_break_time = 0;
            }
            t_shiftId = shiftObj.getString("id");
            if (shiftObj.has("field_id") && !shiftObj.isNull("field_id")) {
                // Do something with object.
                t_fieldId = shiftObj.getString("field_id");
            }
            if (shiftObj.has("s_time") && !shiftObj.isNull("s_time")) {
                // Do something with object.
                last_Ttime = shiftObj.getInt("s_time");
            } else {
                last_Ttime = 0;
            }
            if (shiftObj.has("e_time") && !shiftObj.isNull("e_time")) {
                // Do something with object.
                last_Etime = shiftObj.getInt("e_time");
            } else {
                last_Etime = 0;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //get current shift
    private JSONObject checkTodayShift() {
        Date date = new Date();
        int minute = date.getMinutes();
        int hours = date.getHours();
        int nowSTime = 60 * hours + minute;
//        int nowSTime = 431;
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).format(new Date());
//        String today = "2021-11-01";
        int last_etime = 0, last_stime = 0, nowTime = 0;
        String shift_date = null;
        if (resultShift == null || resultShift.length() == 0)
            return null;

        //get today shift info if present time is between s_time and e_time
        for (int i = 0; i < resultShift.length(); i++) {
            try {
                JSONObject shiftObj = resultShift.getJSONObject(i);
                getParamsObj(shiftObj);
                shift_date = t_shift_date;
                last_stime = last_Ttime;
                last_etime = last_Etime;

                if (!today.equals(shift_date)) {
                    nowTime = nowSTime + 1440;
                } else {
                    nowTime = nowSTime;
                }

                if (last_stime < nowTime && nowTime < last_etime) {
                    if ((e_leave_at != null && e_leave_checked_at != null) || (over_time_at != null && over_time_checked_at != null)) {

                    } else {
                        return shiftObj;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < resultShift.length(); i++) {
            try {
                JSONObject shiftObj = resultShift.getJSONObject(i);
                getParamsObj(shiftObj);
                shift_date = t_shift_date;
                last_stime = last_Ttime;
                if (today.equals(shift_date)) {
                    return shiftObj;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < resultShift.length(); i++) {
            String l_arrive_checked_at = null, l_leave_checked_at = null;
            try {
                JSONObject shiftObj = resultShift.getJSONObject(i);
                getParamsObj(shiftObj);
                l_arrive_checked_at = t_arrive_checked_at;
                l_leave_checked_at = t_leave_checked_at;
                shift_date = t_shift_date;
                //no leave with arrive
                if (l_arrive_checked_at != null && l_leave_checked_at == null) {
                    if (!today.equals(shift_date)) {
                        nowTime = nowSTime + 1440;
                        if (nowTime < 2160) {
                            return shiftObj;
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void checkButtonStatus(JSONObject todayShift) {
        if (todayShift == null) {
            buttonStatusByType(NOSHIFT);
            latitude.setText("");
            return;
        }
        int last_etime = 0, last_stime = 0, l_break_time;
        String shift_date = null, l_arrive_checked_at = null, l_leave_checked_at = null, l_break_at, shiftId, fieldId;
        Date date = new Date();
        int minute = date.getMinutes();
        int hours = date.getHours();
        int nowSTime = 60 * hours + minute;
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).format(new Date());
        getParamsObj(todayShift);
        l_arrive_checked_at = t_arrive_checked_at;
        l_leave_checked_at = t_leave_checked_at;
        shift_date = t_shift_date;
        l_break_at = t_break_at;
        l_break_time = t_break_time;
        shiftId = t_shiftId;
        fieldId = t_fieldId;
        last_etime = last_Etime;
        last_stime = last_Ttime;

        if (l_break_at != null && l_break_time != 0) {
            try {
                Date l_break = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN).parse(l_break_at);
                break_at_int = l_break.getHours() * 60 + l_break.getMinutes();
                break_time = l_break_time;
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        todayShiftId = shiftId;

        todaySTime = last_stime;
        todayETime = last_etime;

        setFieldInfo(fieldId);

        if (!today.equals(shift_date)) {
            nowSTime = nowSTime + 1440;
            isLast = true;
        } else {
            isLast = false;
        }
        //no arrive and no leave
        if (l_arrive_checked_at == null) {
            if (nowSTime < last_etime) {
                buttonStatusByType(BEFORE_ARRIVE);
            }
        }
        //no leave with arrive
        else if (l_arrive_checked_at != null && l_leave_checked_at == null) {
            if (nowSTime < last_etime) {
                buttonStatusByType(CAN_EARLY_LEAVE);
            } else {
                buttonStatusByType(CAN_LEAVE);
            }
        } else if (l_arrive_checked_at != null && l_leave_checked_at != null) {
            if (over_time_at != null && over_time_checked_at == null) {
                buttonStatusByType(AFTER_LEAVE);
                latitude.setText("");
                todayShiftId = null;
            } else {
                if (e_leave_at != null && e_leave_checked_at == null) {
                    if (nowSTime < last_etime) {
                        buttonStatusByType(CAN_REST);
                    } else {
                        buttonStatusByType(AFTER_LEAVE);
                        latitude.setText("");
                        todayShiftId = null;
                    }
                } else {
                    buttonStatusByType(AFTER_LEAVE);
                    latitude.setText("");
                    todayShiftId = null;
                }
            }

        }

        SharedPreferences sp = getSharedPreferences(Constants.SHARE_MAIN, 0);
        Editor Ed = sp.edit();
        Ed.putString(Constants.TODAY_SHIFT_ID, todayShiftId);
        Ed.apply();
        SharedPreferences sp1 = getSharedPreferences(Constants.SHARE_PREF, 0);
        Editor Ed1 = sp1.edit();
        Ed1.putString(Constants.SHIFT_ID, todayShiftId);
        Ed1.apply();
    }

    private void checkTodayShiftOrigin() {
        if (resultShift == null || resultShift.length() == 0)
            return;
        Date date = new Date();
        int minute = date.getMinutes();
        int hours = date.getHours();
        int nowSTime = 60 * hours + minute;
        Boolean isIn = false;
        SharedPreferences sp = getSharedPreferences(Constants.SHARE_PREF, 0);
        String shiftId = sp.getString(Constants.TODAY_SHIFT_ID, null);
        if (shiftId != null) {
            for (int i = 0; i < resultShift.length(); i++) {
                try {
                    JSONObject shiftObj = resultShift.getJSONObject(i);

                    String sT = null;
                    if (shiftObj.has("id") && !shiftObj.isNull("id")) {
                        // Do something with object.
                        sT = shiftObj.getString("id");
                    }
                    if (shiftId.equals(sT)) {
                        if (shiftObj.has("shift_date") && !shiftObj.isNull("shift_date")) {
                            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).format(new Date());
                            String shift_date = null;
                            try {
                                shift_date = shiftObj.getString("shift_date");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            if (!today.equals(shift_date)) {
                                lastShift = shiftObj;
                            } else {
                                todayShift = shiftObj;
                            }
                            return;
                        }
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
        if (resultShift.length() == 1) {
            JSONObject tmp = null;
            try {
                tmp = resultShift.getJSONObject(0);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (tmp.has("shift_date") && !tmp.isNull("shift_date")) {
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).format(new Date());
                String shift_date = null;
                try {
                    shift_date = tmp.getString("shift_date");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (today.equals(shift_date)) {
                    todayShift = tmp;
                } else {
                    lastShift = tmp;
                }
            }
        } else {
            for (int i = 1; i < resultShift.length(); i++) {
                try {
                    JSONObject shiftObj = resultShift.getJSONObject(i);

                    int sT = -1, eT = -1;
                    if (shiftObj.has("s_time") && !shiftObj.isNull("s_time")) {
                        // Do something with object.
                        sT = shiftObj.getInt("s_time");
                    }
                    if (shiftObj.has("e_time") && !shiftObj.isNull("e_time")) {
                        // Do something with object.
                        eT = shiftObj.getInt("e_time");
                    }
                    if (shiftObj.has("shift_date") && !shiftObj.isNull("shift_date")) {
                        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).format(new Date());
                        String shift_date = null;
                        try {
                            shift_date = shiftObj.getString("shift_date");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (!today.equals(shift_date)) {
                            sT = sT - 1440;
                            eT -= 1440;
                        }
                    }
                    if (sT <= nowSTime && nowSTime <= eT) {
                        if (nowSTime < sT) {
                            todayShift = resultShift.getJSONObject(i - 1);
                            isIn = true;
                            break;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (!isIn) {
                try {
                    todayShift = resultShift.getJSONObject(resultShift.length() - 1);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //set btn status by shift status
    private void buttonStatusByType(String status) {
        if (status.equals(NOSHIFT)) {
            arriveBtn_index.setEnabled(false);
            leaveBtn_index.setEnabled(false);
            earlyLeaveBtn_index.setEnabled(false);
            restBtn_index.setEnabled(false);
            restNightBtn_index.setEnabled(false);
        }
        if (status.equals(BEFORE_ARRIVE)) {
            arriveBtn_index.setEnabled(true);
            leaveBtn_index.setEnabled(false);
            earlyLeaveBtn_index.setEnabled(false);
            restBtn_index.setEnabled(false);
            restNightBtn_index.setEnabled(false);
        }
        if (status.equals(CAN_EARLY_LEAVE)) {
            arriveBtn_index.setEnabled(false);
            leaveBtn_index.setEnabled(false);
            earlyLeaveBtn_index.setEnabled(true);
            Date cur = new Date();
            int hour = cur.getHours();
            if (6 < hour && hour < 21) {
                if (hour * 60 + cur.getMinutes() < break_at_int + break_time) {
                    restBtn_index.setEnabled(false);
                } else {
                    restBtn_index.setEnabled(true);
                }

                restNightBtn_index.setEnabled(false);
            } else {
                restBtn_index.setEnabled(false);
                if (hour * 60 + cur.getMinutes() < break_at_int + break_time) {
                    restNightBtn_index.setEnabled(false);
                } else {
                    restNightBtn_index.setEnabled(true);
                }
            }
        }
        if (status.equals(CAN_LEAVE)) {
            arriveBtn_index.setEnabled(false);
            leaveBtn_index.setEnabled(true);
            earlyLeaveBtn_index.setEnabled(false);
            Date cur = new Date();
            int hour = cur.getHours();
            if (6 < hour && hour < 21) {
                if (hour * 60 + cur.getMinutes() < break_at_int + break_time) {
                    restBtn_index.setEnabled(false);
                } else {
                    restBtn_index.setEnabled(true);
                }
                restNightBtn_index.setEnabled(false);
            } else {
                restBtn_index.setEnabled(false);
                if (hour * 60 + cur.getMinutes() < break_at_int + break_time) {
                    restNightBtn_index.setEnabled(false);
                } else {
                    restNightBtn_index.setEnabled(true);
                }
            }
        }
        if (status.equals(CAN_REST)) {
            arriveBtn_index.setEnabled(false);
            leaveBtn_index.setEnabled(false);
            earlyLeaveBtn_index.setEnabled(false);
            Date cur = new Date();
            int hour = cur.getHours();
            if (6 < hour && hour < 21) {
                if (hour * 60 + cur.getMinutes() < break_at_int + break_time) {
                    restBtn_index.setEnabled(false);
                } else {
                    restBtn_index.setEnabled(true);
                }
                restNightBtn_index.setEnabled(false);
            } else {
                restBtn_index.setEnabled(false);
                if (hour * 60 + cur.getMinutes() < break_at_int + break_time) {
                    restNightBtn_index.setEnabled(false);
                } else {
                    restNightBtn_index.setEnabled(true);
                }
            }
        }
        if (status.equals(AFTER_LEAVE)) {
            arriveBtn_index.setEnabled(false);
            leaveBtn_index.setEnabled(false);
            earlyLeaveBtn_index.setEnabled(false);
            restBtn_index.setEnabled(false);
            restNightBtn_index.setEnabled(false);
        }

    }

    public boolean checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            int fine_location_granted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
            int coarse_location_granted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

            if (fine_location_granted != PackageManager.PERMISSION_GRANTED || coarse_location_granted != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
                return false;
            }
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            int fine_location_granted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
            int coarse_location_granted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

            if (fine_location_granted != PackageManager.PERMISSION_GRANTED || coarse_location_granted != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS},
                        MY_PERMISSIONS_REQUEST_LOCATION);
                return false;
            }
        }
        else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION);
        }

        return true;
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.gps_service_enable)
                .setCancelable(false)
                .setPositiveButton("は　い", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                });
        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {

                Button negButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(20, 0, 20, 0);
                negButton.setLayoutParams(params);
            }
        });
        alert.show();
        final Button negButton = alert.getButton(AlertDialog.BUTTON_NEGATIVE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(20, 0, 0, 0);
        negButton.setLayoutParams(params);

    }

    private Boolean getIsNear(Location location) {
        latitude_cur = location.getLatitude();
        longitude_cur = location.getLongitude();

        distance = calDistance(latitude_field, longitude_field, latitude_cur, longitude_cur);

        if (distance < 1) {
            return true;
        } else {
            return false;
        }
    }

    private Location getLastKnownLocation() {
        LocationManager mnLocationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = mnLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            checkLocationPermission();
            Location l = mnLocationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    public void getResponse(String url, String shift_id, String time) {
        try {
            // TODO: handle loggedInUser authentication

            RequestParams rp = new RequestParams();
            rp.add("shift_id", shift_id);
            if (url.equals("api/v1/client/confirm-leave")) {
                rp.add("leave_time", time);
            } else {
                rp.add("arrive_time", time);
            }

            HttpUtils.addToken(token);
            HttpUtils.post(url, rp, new JsonHttpResponseHandler() {
                @Override
                public void onStart() {
                    // called before request is started
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    // If the response is JSONObject instead of expected JSONArray

                    Log.d("asd", "---------------- this is response : " + response);
                    try {
                        JSONObject serverResp = new JSONObject(response.toString());

                        String rpMessage = serverResp.optString("message");
                        if (rpMessage == "ERR_INVALID_TOKEN") {
                            Toast.makeText(getApplicationContext(), "失敗しました。", Toast.LENGTH_SHORT).show();
                            ToLogin();
                            return;
                        }

                        Toast toast = Toast.makeText(getApplicationContext(), rpMessage, Toast.LENGTH_LONG);
//                        View view = toast.getView();
//                        TextView text = (TextView) view.findViewById(android.R.id.message);
//                        text.setTextSize(20);
                        ViewGroup group = (ViewGroup) toast.getView();
                        TextView messageTextView = (TextView) group.getChildAt(0);
                        messageTextView.setTextSize(20);
                        toast.show();
                        mAuthTask = new UserInfoTask(token);
                        mAuthTask.execute((Void) null);

                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable e, JSONObject response) {
                    // called when response HTTP status is "4XX" (eg. 401, 403, 404)

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


        } catch (Exception e) {
            Log.e("REST_API", "GET method failed: " + e.getMessage());
            e.printStackTrace();

        }
    }

    public void ToLogin() {
        SharedPreferences sp = getSharedPreferences(Constants.SHARE_PREF, 0);
        Editor Ed = sp.edit();
        Ed.putString(Constants.SHARE_EMAIL, "");
        Ed.putString(Constants.SHARE_PWD, "");
        Ed.putString(Constants.TOKEN, null);
        Ed.putString(Constants.USER_NAME, "");
        Ed.putString(Constants.USER_PLACE, "");
        Ed.apply();
        Intent intent;
        intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        checkIntent(intent);
    }

    public void checkIntent(Intent intent) {
        Bundle extras = intent.getExtras();

        if (extras != null) {
            String type = null, titleStr = null, bodyStr = null, check_type = null;

            try {
                type = extras.getString("type");
                check_type = extras.getString("check_type");

            } catch (Exception e) {
                return;
            }

            if (type.equals("1")) {
                String field = extras.getString("field_name");
                String time = extras.getString("shift_time");
                String[] shiftDay = time.split(" ");
                String[] shiftDayArr = shiftDay[0].split("-");
                String shift_id = extras.getString("shift_id");
                setFieldArr(field);

                String title = shiftDayArr[0] + "年 " + shiftDayArr[1] + "月 " + shiftDayArr[2] + "日(明日)勤務確認";
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle(title);
                alertDialog.setCancelable(false);
                alertDialog.setMessage("現場名：" + field + "\n" + "現地出社時間：" + time + "\n" + "出発場所を選択してください。");
                View sView = getLayoutInflater().inflate(R.layout.spinner_field, null);
                Spinner mSpiner = (Spinner) sView.findViewById(R.id.spinner3);
                if (mylist.size() != 0) {
                    alertDialog.setMessage("現場名：" + field + "\n" + "現地出社時間：" + time + "\n" + "出発場所を選択してください。");
                    ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(MainActivity.this, R.layout.my_spinner, mylist);
                    adapter2.setDropDownViewResource(R.layout.spinner_layout);
                    mSpiner.setAdapter(adapter2);
                    alertDialog.setView(sView);
                } else {
                    alertDialog.setMessage("現場名：" + field + "\n" + "現地出社時間：" + time);
                }

                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "確認しました",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String fieldId = null;
                                if (mylist.size() != 0) {
                                    fieldId = mSpiner.getSelectedItem().toString();
                                }
                                if (fieldId != null) {
                                    SharedPreferences sp1 = getSharedPreferences(Constants.SHARE_PREF, 0);

                                    try {
                                        JSONArray shiftAddress = new JSONArray();
                                        JSONObject obj = new JSONObject();
                                        obj.put(shift_id, fieldId);
                                        shiftAddress.put(obj);
                                        SharedPreferences.Editor Ed = sp1.edit();
                                        Ed.putString(Constants.SHIFT_ADDRESS, shiftAddress.toString());
                                        Ed.apply();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                                mConfirmTask = new ConfirmTask(token, Constants.CONFIRM_YESTERDAY, null, shift_id, getFieldId(fieldId));
                                mConfirmTask.execute((Void) null);
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            } else if (type.equals("2")) {

                String field_admin = extras.getString("admin_tel");
                String agent_react = extras.getString("emergency_tel");
                String shift_id = extras.getString("shift_id");
                String shift_time = extras.getString("shift_time");

                String[] onlytime = shift_time.split(" ");
                String[] shiftDayArr = onlytime[0].split("-");
                String[] time = onlytime[1].split(":");
                String field_name = extras.getString("field_name");
                String title = shiftDayArr[0] + "年 " + shiftDayArr[1] + "月 " + shiftDayArr[2] + "日(今日)勤務確認";

                int time_int = Integer.parseInt(time[time.length - 3]) * 60 + Integer.parseInt(time[time.length - 2]);
                SharedPreferences sp = getSharedPreferences(Constants.SHARE_PREF, 0);
                Editor Ed = sp.edit();
                Log.d("noti", "---------------- this is todayShiftId : " + time_int);
                Ed.putString(Constants.SHIFT_ID, shift_id);
                Ed.putInt(Constants.USER_STIME, time_int);
                Ed.putString(Constants.FIELD_NAME, field_name);
                user_stime = time_int;
                getFieldInfoTime(field_name);
                SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                Calendar calendar = Calendar.getInstance();
                String cu_time = dateFormat.format(calendar.getTime());
                String[] spTime = cu_time.split(":");
                int now_time = 60 * Integer.parseInt(spTime[0]) + Integer.parseInt(spTime[1]);
                if(now_time <= user_stime + 5){
                    startService();
                }
                else{
                    stopService();
                }
                //Ed.putInt(Constants.USER_LTIME, todaySTime - required_time);
                Ed.apply();

                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle(title);
                alertDialog.setCancelable(false);
                alertDialog.setMessage("本日は、出勤日です。よろしくお願いします。\n" +
                        "本日の体調確認です。\n" +
                        "体温は３７度以下ですか。体調は、問題ございませんか。\n");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "いいえ",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mConfirmTask = new ConfirmTask(token, Constants.CONFIRM_TODAY, "0", shift_id, null);
                                mConfirmTask.execute((Void) null);
                                dialog.dismiss();
                                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                                alertDialog.setTitle("責任者に電話し指示を仰いでください");
                                alertDialog.setMessage("現場責任者:" + field_admin +
                                        "\n" +
                                        "緊急対応者:" + agent_react + "\n");
                                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "確　認",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                alertDialog.show();
                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "は　い",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mConfirmTask = new ConfirmTask(token, Constants.CONFIRM_TODAY, "1", shift_id, null);
                                mConfirmTask.execute((Void) null);
                                dialog.dismiss();

                            }
                        });

                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        Button negButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        params.setMargins(20, 0, 20, 0);
                        negButton.setLayoutParams(params);
                    }
                });

                alertDialog.show();

                final Button negButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(20, 0, 0, 0);
                negButton.setLayoutParams(params);
            } else if (type.equals("12")) {
                stopService();
                String shift_id = extras.getString("shift_id");
                //SharedPreferences sp1 = getSharedPreferences(Constants.SHARE_PREF,  0);
                todayShiftId = shift_id;

                String body = "自宅を出発していますか？\n" +
                        "自宅を出発している場合、位置情報を送信します。";
                String title = "出発の時間です！";
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle(title);
                alertDialog.setMessage(body);

                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "確　認",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                Log.d("asd", "todayShiftId : " + todayShiftId);
                                Location location = getLastKnownLocation();
                                if (location != null) {
                                    mLatitude = String.valueOf(location.getLatitude());
                                    mLongitude = String.valueOf(location.getLongitude());
                                    Log.d("asd", "latitude : " + mLatitude);

                                }

                                mSendPositionTask = new SendPositionTask(token);
                                mSendPositionTask.execute((Void) null);
                            }
                        });
                alertDialog.show();
            } else {
                String body = "";
                String title = "";
                String shift_time = extras.getString("shift_time");
                String[] onlytime = shift_time.split(" ");
                String[] shiftDayArr = onlytime[0].split("-");
                String[] time = onlytime[1].split(":");
                String field_name = extras.getString("field_name");
                String title_add = shiftDayArr[0] + "年 " + shiftDayArr[1] + "月 " + shiftDayArr[2] + "日勤怠申請";

                if (check_type.equals("1")) {
                    title = title_add + "(承認)";
                }
                if (check_type.equals("0")) {
                    title = title_add + "(拒絶)";
                }
                if (check_type.equals("2")) {
                    title = title_add + "(編集承認)";
                }
                if (type.equals("3")) {
                    body = "早退";
                }
                if (type.equals("4")) {
                    body = "休日";
                }
                if (type.equals("5")) {
                    body = "残業";
                }
                if (type.equals("6")) {
                    body = "振替日";
                }
                if (type.equals("7")) {
                    body = "遅刻";
                }
                if (type.equals("8")) {
                    body = "始業時間の変更";
                }
                if (type.equals("9")) {
                    body = "終業時間の変更";
                }
                if (type.equals("10")) {
                    body = "休憩時間の変更";
                }
                if (type.equals("11")) {
                    body = "夜休憩時間の変更";
                }

                if (type.equals("7") || type.equals("3") || type.equals("5")) {
                    AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                    alertDialog.setTitle(title);
                    if (check_type.equals("1")) {
                        alertDialog.setMessage(body + "申請が" + "されれました。ご確認お願いします。");
                    } else if (check_type.equals("3")) {
                        alertDialog.setTitle("出勤通知");
                        alertDialog.setMessage("出勤ボタンをクリックしてください。");
                    } else {
                        alertDialog.setMessage(body + "申請が" + title + "されれました。再申請お願いします。");
                    }



                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "確　認",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    mAuthTask = new UserInfoTask(token);
                                    mAuthTask.execute((Void) null);
                                }
                            });
                    alertDialog.show();
                } else {
                    AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                    alertDialog.setTitle(title);
                    if (check_type.equals("2")) {
                        String new_val = extras.getString("new_value");
                        String old_val = extras.getString("old_value");

                        alertDialog.setMessage(body + "申請が" + "されれました。ご確認お願いします。\n" +
                                "申請内容を管理者が変更し、承認しました。\n" +
                                "申請:" + old_val + "\n" +
                                "変更:" + new_val);

                    } else {
                        alertDialog.setMessage(body + "申請が" + title + "されれました。ご確認お願いします。");
                    }

                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "確　認",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }
            }
        }
    }

    private class ConfirmTask extends AsyncTask<Void, Void, Boolean> {

        private final String mToken;
        private String mErrorMsg;
        private String confirm_url = null;
        private String health_status = null;
        private String shift_id = null;
        private String field_id = null;

        ConfirmTask(String token, String url, String status, String shift, String field) {
            mToken = token;
            confirm_url = url;
            health_status = status;
            shift_id = shift;
            field_id = field;
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
                    if (field_id != null) {
                        postParams.put("staff_address_id", field_id);
                    }

                    if (health_status != null) {
                        postParams.put("health_status", health_status);
                    }

                    HttpPostRequest httpPostRequest = new HttpPostRequest();
                    result = httpPostRequest.POST(confirm_url, postParams, mToken);

                    if (result == null) return false;
                    if (!result.getString("status").equals("success")) {
                        mErrorMsg = result.getString("message");
                        return false;
                    }

                    mErrorMsg = result.getString("message");

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
            mConfirmTask = null;

            if (success) {
                if (health_status != "0") {
                    Toast toast = Toast.makeText(getApplicationContext(), mErrorMsg, Toast.LENGTH_LONG);
                    //View view = toast.getView();
                    //TextView text = (TextView) view.findViewById(android.R.id.message);
                    //text.setTextSize(20);
                    ViewGroup group = (ViewGroup) toast.getView();
                    TextView messageTextView = (TextView) group.getChildAt(0);
                    messageTextView.setTextSize(20);
                    toast.show();
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
            mConfirmTask = null;
        }
    }

    private class SendPositionTask extends AsyncTask<Void, Void, Boolean> {

        private final String mToken;
        private String mErrorMsg;

        SendPositionTask(String ftoken) {
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
                    postParams.put("latitude", mLatitude);
                    postParams.put("longitude", mLongitude);

                    HttpPostRequest httpPostRequest = new HttpPostRequest();
                    result = httpPostRequest.POST(Constants.CONFIRM_START, postParams, mToken);

                    if (result == null) return false;
                    if (!result.getString("status").equals("success")) {
                        mErrorMsg = result.getString("message");
                        return false;
                    }
                    if (result.getString("status").equals("success")) {
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

            } else {

                if (mErrorMsg == null) {
                    Toast.makeText(getApplicationContext(), "失敗しました。", Toast.LENGTH_SHORT).show();
                    return;
                }
                switch (mErrorMsg) {
                    case "ERR_INVALID_FCM_TOKEN":
                        Toast.makeText(getApplicationContext(), "失敗しました。", Toast.LENGTH_SHORT).show();
                        break;
                    case "ERR_INVALID_TOKEN":
                        Toast.makeText(getApplicationContext(), "失敗しました。", Toast.LENGTH_SHORT).show();
                        ToLogin();
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

    private class UserInfoTask extends AsyncTask<Void, Void, Boolean> {

        private final String mToken;
        private String mErrorMsg;
        private String mMonth = null;

        UserInfoTask(String token) {
            mToken = token;
        }

        UserInfoTask(String token, String month) {
            mToken = token;
            mMonth = month;
        }

        private boolean isConnected() {
            ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected())
                return true;
            else
                return false;
        }

        @SuppressLint("WrongThread")
        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.
            if (isConnected()) {
                ContentValues postParams = new ContentValues();
                JSONObject result = null;
                try {

                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.DAY_OF_YEAR, -1);
                    Date tomorrow = calendar.getTime();
                    postParams.put("s_date", new SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).format(tomorrow));
                    postParams.put("e_date", new SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).format(new Date()));

                    HttpPostRequest httpPostRequest = new HttpPostRequest();
                    result = httpPostRequest.GET(Constants.GET_SHIFT_LIST, postParams, mToken);

                    if (result == null) return false;
                    if (!result.getString("status").equals("success")) {
                        mErrorMsg = result.getString("message");
                        return false;
                    }

                    resultShift = result.optJSONArray("result");

                    if (resultShift.length() == 0) {
                        mErrorMsg = getString(R.string.error_exist_today_shift);
                        return false;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            } else {

                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;


            if (success) {


            } else {
                if (mErrorMsg == null) {
                    Toast.makeText(getApplicationContext(), "失敗しました。", Toast.LENGTH_SHORT).show();
                    return;
                }
                switch (mErrorMsg) {
                    case "ERR_INVALID_S_DATE":
                        break;
                    case "ERR_INVALID_E_DATE":
                        break;
                    case "ERR_INVALID_TOKEN":
                        Toast.makeText(getApplicationContext(), "失敗しました。", Toast.LENGTH_SHORT).show();
                        ToLogin();
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

    private class UserLogoutTask extends AsyncTask<Void, Void, Boolean> {

        private final String mToken;
        private String mErrorMsg;

        UserLogoutTask(String token) {
            mToken = token;
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

                    HttpPostRequest httpPostRequest = new HttpPostRequest();
                    result = httpPostRequest.POST(Constants.LOGOUT_URL, postParams, mToken);

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
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mLogoutTask = null;

            if (success) {
                ToLogin();

            } else {

                if (mErrorMsg == null) {
                    Toast.makeText(getApplicationContext(), "失敗しました。", Toast.LENGTH_SHORT).show();
                    return;
                }
                switch (mErrorMsg) {
                    case "ERR_INVALID_S_DATE":

                        break;
                    case "ERR_INVALID_E_DATE":

                        break;
                    case "ERR_INVALID_TOKEN":
                        Toast.makeText(getApplicationContext(), "失敗しました。", Toast.LENGTH_SHORT).show();
                        ToLogin();

                        break;
                    default:
                        Toast.makeText(getApplicationContext(), mErrorMsg, Toast.LENGTH_SHORT).show();

                }
            }
        }

        @Override
        protected void onCancelled() {
            mLogoutTask = null;
        }
    }

    private class RegisterFirebaseTokenTask extends AsyncTask<Void, Void, Boolean> {

        private final String fbToken;
        private String mErrorMsg;

        RegisterFirebaseTokenTask(String ftoken) {
            fbToken = ftoken;
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
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mFirebaseTask = null;

            if (success) {

            } else {

            }
        }

        @Override
        protected void onCancelled() {
            mFirebaseTask = null;
        }
    }

    private double calDistance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

//    private void getLocation() {
//        getLastLocation();
//        if (mLocation != null) {
//            mLatitude = String.valueOf(mLocation.getLatitude());
//            mLongitude = String.valueOf(mLocation.getLongitude());
//            Log.d("asd", "fused latitude : " + mLatitude);
//            Log.d("asd", "fused longitude : " + mLongitude);
//
//        }
//    }

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

    private void getFieldInfoTime(String field_name) {
        String fieldId = null;
        JSONArray shiftAddress = null;
        SharedPreferences sp1 = getSharedPreferences(Constants.SHARE_PREF, 0);
        try {
            staffAddress = new JSONArray(sp1.getString(Constants.STAFF_ADDRESS, null));
            if (sp1.getString(Constants.SHIFT_ADDRESS, null) != null) {
                shiftAddress = new JSONArray(sp1.getString(Constants.SHIFT_ADDRESS, null));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
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

    public void startService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        serviceIntent.putExtra("inputExtra", "Foreground Service Example in Android");
        ContextCompat.startForegroundService(this, serviceIntent);
    }
    public void stopService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        serviceIntent.putExtra("inputExtra", "Foreground Service Example in Android");
        stopService(serviceIntent);
    }
}