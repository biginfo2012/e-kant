package com.business.ekant;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.business.ekant.data.Constants;
import com.business.ekant.data.HttpPostRequest;
import com.business.ekant.data.HttpUtils;
import com.business.ekant.ui.login.LoginActivity;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

import static android.util.TypedValue.COMPLEX_UNIT_SP;
import static android.view.Gravity.CENTER;
import static android.view.View.TEXT_ALIGNMENT_CENTER;

public class ShowHistoryActivity extends AppCompatActivity {

    TableLayout shiftTableLayout;
    private UserLogoutTask mLogoutTask = null;
    String token;
    String valid = "not_valid";
    int rowVal = 1, colVal = 3;
    int month = 0, month_tmp, valid_int = -1;
    Boolean firstLoad = true;
    JSONArray shiftIdList = new JSONArray();
    JSONArray shiftDayListLabel = new JSONArray();
    JSONArray shiftBreakStime = new JSONArray();
    JSONArray shiftDayList = new JSONArray();
    JSONArray shiftArriveList = new JSONArray();
    JSONArray shiftListArr;
    JSONArray modifyShiftList = new JSONArray();
    //SharedPreferences sp1 = getSharedPreferences(Constants.SHARE_PREF, 0);
    String todayShiftId = null;
    Button btnRequestList;
    private View mProgressView;

    final Calendar myCalendar = Calendar.getInstance();

    private UserInfoTask mAuthTask = null;
    private RegisterFirebaseTokenTask mFirebaseTask = null;


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_history);

        //final ImageView logoutIcon = (ImageView) findViewById(R.id.logout_list);
        final TextView staffName = (TextView) findViewById(R.id.staff_name_list);
        btnRequestList = (Button) findViewById(R.id.btn_request_list);
        final TextView prevMonth = (TextView) findViewById(R.id.prev_month);
        final TextView nextMonth = (TextView) findViewById(R.id.next_month);
        final TextView backTxt = (TextView) findViewById(R.id.back1);
        mProgressView = findViewById(R.id.loading_list);

        SharedPreferences sp1 = getSharedPreferences(Constants.SHARE_PREF, 0);
        String displayName = sp1.getString(Constants.USER_NAME, null);
        SharedPreferences sp2 = getSharedPreferences(Constants.SHARE_MAIN, 0);
        todayShiftId = sp2.getString(Constants.TODAY_SHIFT_ID, null);
        token = sp1.getString(Constants.TOKEN, null);
        staffName.setText(displayName);

        shiftTableLayout = (TableLayout) findViewById(R.id.shiftTableLayout);

        mAuthTask = new UserInfoTask(token, month);
        mAuthTask.execute((Void) null);

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        backTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnRequestList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String popStr = "";
                Boolean validPost = true;
                Boolean validPostT = false;
                for (int i = 1; i < rowVal; i++) {
                    JSONObject itemObj = new JSONObject();
                    try {
                        itemObj.put("shift_id", shiftIdList.getInt(i - 1));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    View chkView = getTableLayoutCell(shiftTableLayout, i, 4);
                    LinearLayout layout = (LinearLayout) chkView;
                    CheckBox chk = (CheckBox) layout.getChildAt(0);
                    for (int j = 1; j <= colVal; j++) {
                        View elView = getTableLayoutCell(shiftTableLayout, i, j);

                        if(chk.isChecked()){
                            switch (j) {
                                case 1:
                                    String val = null;
                                    try {
                                        val = GetFirstCol(elView, shiftDayList.getString(i-1));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    if (val == null) {
                                        try {
                                            itemObj.put("arrive_time", null);
//                                            popStr +=  shiftDayListLabel.getString(i-1) +  " 打刻漏れが発生しているため勤怠申請(出勤)をお願いします。\n";
//                                            validPost = false;
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    else if(val != valid) {
                                        try {
                                            if(Integer.parseInt(val) >= 1440){
                                                itemObj.put("arrive_time", null);
                                                popStr +=  shiftDayListLabel.getString(i-1) +  " 出勤時間は24:00より前でなければなりません。\n";
                                                validPost = false;
                                            }
                                            else{
                                                itemObj.put("arrive_time", Integer.parseInt(val));
                                                validPostT = true;
                                            }

                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    else if(val == valid){
                                        try {
                                            itemObj.put("arrive_time", val);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    break;
                                case 2:
                                    String val2 = null;
                                    try {
                                        val2 = GetFirstCol(elView, shiftDayList.getString(i-1));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    if (val2 == null) {
                                        try {
                                            itemObj.put("leave_time", null);
                                            String shiftDay = shiftDayList.getString(i-1);
                                            String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).format(new Date());
                                            if (itemObj.has("arrive_time") && !itemObj.isNull("arrive_time") && !shiftDay.equals(todayStr)) {
                                                popStr +=  shiftDayListLabel.getString(i-1) +  " 打刻漏れが発生しているため勤怠申請(退勤)をお願いします。\n";
                                                validPost = false;
                                            }else{
                                                validPostT = true;
                                            }

                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    else if(val2 != valid){
                                        try {
                                            if (itemObj.has("arrive_time") && !itemObj.isNull("arrive_time")) {

                                                String arrive = itemObj.getString("arrive_time");

                                                if(arrive != valid){
                                                    int arrive_int = Integer.parseInt(arrive);
                                                    int val_int = Integer.parseInt(val2);
                                                    if(val_int > arrive_int){
                                                        itemObj.put("leave_time", Integer.parseInt(val2));
                                                        validPostT = true;
                                                    }
                                                    else{
                                                        popStr +=  shiftDayListLabel.getString(i-1) +  " 始業時間は終業時間よりも小さくします。\n";
                                                        validPost = false;
//                                                        Toast.makeText(getApplicationContext(), "始業時間は終業時間よりも小さくします", Toast.LENGTH_SHORT).show();
//                                                        return;
                                                    }
                                                }
                                                else{
                                                    String arrive_checked_at = shiftArriveList.getString(i-1);
                                                    if(arrive_checked_at != null){
                                                        Date arrive_check_at = null;
                                                        try {
                                                            if (arrive_checked_at != null) {
                                                                arrive_check_at = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(arrive_checked_at);
                                                            }

                                                        } catch (ParseException e) {
                                                            e.printStackTrace();
                                                        }
                                                        int cs_time = -1;
                                                        int hour = 0;
                                                        int min = 0;
                                                        if (arrive_check_at != null) {
                                                            int val_int = Integer.parseInt(val2);
                                                            hour = arrive_check_at.getHours();
                                                            min = arrive_check_at.getMinutes();
                                                            cs_time = hour * 60 + min;
                                                            Date shiftDay = null;

                                                            try {
                                                                shiftDay = new SimpleDateFormat("yyyy-MM-dd").parse(shiftDayList.getString(i-1));
                                                            } catch (ParseException e) {
                                                                e.printStackTrace();
                                                            }

                                                            if(shiftDay.getDay() < arrive_check_at.getDay()){
                                                                hour += 24;
                                                                cs_time += 1440;
                                                            }
                                                            if(val_int > cs_time){
                                                                itemObj.put("arrive_time", cs_time);
                                                                itemObj.put("leave_time", Integer.parseInt(val2));
                                                                validPostT = true;
                                                            }
                                                            else{
                                                                popStr +=  shiftDayListLabel.getString(i-1) +  " 始業時間は終業時間よりも小さくします。\n";
                                                                validPost = false;
//                                                        Toast.makeText(getApplicationContext(), "始業時間は終業時間よりも小さくします", Toast.LENGTH_SHORT).show();
//                                                        return;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            else if(itemObj.has("arrive_time") && itemObj.isNull("arrive_time")){


                                            }
                                            else if(!itemObj.has("arrive_time")){
                                                popStr +=  shiftDayListLabel.getString(i-1) +  " 打刻漏れが発生しているため勤怠申請(出勤)をお願いします。\n";
                                                validPost = false;

                                            }

//                                            else{
//                                                Toast.makeText(getApplicationContext(), "終業時間を入力することができません。始業時間を先に入力してください", Toast.LENGTH_SHORT).show();
//                                                return;
//                                            }

                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    else if(val2 == valid){
                                        if (itemObj.has("arrive_time") && !itemObj.isNull("arrive_time")){
                                            try {
                                                String arrive = itemObj.getString("arrive_time");
                                                if(arrive ==  valid){
                                                    popStr +=  shiftDayListLabel.getString(i-1) +  " 申請中です。\n";
                                                    validPost = false;
                                                }
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                    }
                                    break;
                                case 3:
                                int val3 = 0;
                                try {
                                    val3 = GetThirdCol(elView, shiftDayList.getString(i-1));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                if (val3 == 0) {
                                    try {
                                        itemObj.put("break_time", null);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    try {
                                        //itemObj.put("break_at", val3);

                                        if(itemObj.has("arrive_time") && !itemObj.isNull("arrive_time")){
                                            String arrive = itemObj.getString("arrive_time");
                                            if(arrive != valid){
                                                int arrive_int = Integer.parseInt(arrive);
                                            //int leave_int = Integer.parseInt(leave);
                                                int breakStime = shiftBreakStime.getInt(i - 1);

                                            //int break_int = val3;
                                                if(breakStime < 0){
                                                    itemObj.put("break_s_time", (int)(arrive_int));
                                                }
                                                else{
                                                    if(breakStime < arrive_int){
                                                        itemObj.put("break_s_time", (int)(arrive_int));
                                                    }
                                                    else{
                                                        itemObj.put("break_s_time", breakStime);
                                                    }
                                                }

                                                itemObj.put("break_time", val3);
                                            validPostT = true;
                                            }
                                            //String leave = itemObj.getString("leave_time");

                                        }
//                                        else if(itemObj.isNull("arrive_time") && itemObj.isNull("leave_time")){
//                                            Toast.makeText(getApplicationContext(), "休憩時間を入力することができません。始業時間と終業時間を先に入力してください。", Toast.LENGTH_SHORT).show();
//                                            return;
//                                        }
                                        else if(itemObj.has("leave_time") && !itemObj.isNull("leave_time")){
                                            String arrive = itemObj.getString("leave_time");

                                            int arrive_int = Integer.parseInt(arrive);
                                            //int break_int = val3;
                                            int breakStime = shiftBreakStime.getInt(i - 1);

                                            if(breakStime < 0){
                                                itemObj.put("break_s_time", (int)(arrive_int - val3));
                                            }
                                            else{
                                                itemObj.put("break_s_time", breakStime);
                                            }

                                            itemObj.put("break_time", val3);
                                            validPostT = true;
                                        }

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                                break;
                            }
                        }
                    }
                    modifyShiftList.put(itemObj);

                }
                if(!validPost){
                    String title = "以下の勤怠申請をお願いします。";
                    AlertDialog alertDialog = new AlertDialog.Builder(ShowHistoryActivity.this).create();
                    alertDialog.setTitle(title);
                    alertDialog.setCancelable(true);

                    alertDialog.setMessage(popStr);

                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "確認しました",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    if(!popStr.equals("")){
                        alertDialog.show();
                    }

                }
                else if(validPostT){
                    showProgress(true);
                    btnRequestList.setEnabled(false);
                    mFirebaseTask = new RegisterFirebaseTokenTask(token);
                    mFirebaseTask.execute((Void) null);
                }

            }
        });

        prevMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                month_tmp = month;
                month--;
                firstLoad = true;
                shiftIdList = new JSONArray();
                shiftDayListLabel = new JSONArray();
                shiftBreakStime = new JSONArray();
                shiftDayList = new JSONArray();
                shiftArriveList = new JSONArray();
                modifyShiftList = new JSONArray();

                mAuthTask = new UserInfoTask(token, month);
                mAuthTask.execute((Void) null);
            }
        });

        nextMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                month_tmp = month;
                month++;
                firstLoad = true;
                shiftIdList = new JSONArray();
                shiftDayListLabel = new JSONArray();
                shiftBreakStime = new JSONArray();
                shiftDayList = new JSONArray();
                shiftArriveList = new JSONArray();
                modifyShiftList = new JSONArray();
                mAuthTask = new UserInfoTask(token, month);
                mAuthTask.execute((Void) null);
            }
        });
    }

    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);


            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);

        }
    }

    private class UserInfoTask extends AsyncTask<Void, Void, Boolean> {

        private final String mToken;
        private String mErrorMsg;
        private int mMonth = 0;

        UserInfoTask(String token, int month) {
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

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.
            if (isConnected()) {
                ContentValues postParams = new ContentValues();
                JSONObject result = null;
                try {

                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.MONTH, mMonth);
                    calendar.set(Calendar.DATE, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
                    Date firstDay = calendar.getTime();
                    postParams.put("s_date", new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).format(firstDay));
                    calendar.set(Calendar.DATE, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                    Date lastDay = calendar.getTime();
                    postParams.put("e_date", new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).format(lastDay));


                    HttpPostRequest httpPostRequest = new HttpPostRequest();
                    result = httpPostRequest.GET(Constants.GET_SHIFT_LIST, postParams, mToken);

                    if (result == null) return false;
                    if (!result.getString("status").equals("success")) {
                        mErrorMsg = result.getString("message");
                        return false;
                    }

                    shiftListArr = result.optJSONArray("result");

                    if (shiftListArr.length() == 0) {
                        mErrorMsg = getString(R.string.error_exist_month_shift);
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

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            //month = 0;
            if (success) {
                SetShiftList();


            } else {
                month = month_tmp;
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

    @SuppressLint("ResourceType")
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void SetShiftList() {
        for (int i = 1; i < rowVal; i++) {
            TableRow row = (TableRow) shiftTableLayout.getChildAt(1);
            ViewGroup container = ((ViewGroup) row.getParent());
            // delete the row and invalidate your view so it gets redrawn
            container.removeView(row);
            container.invalidate();
        }
        rowVal = 1;
        String popStr = "";
        String title = "以下の勤怠申請をお願いします。";
        Boolean over_day = false, is_today = false;

                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        TextView myMsg = new TextView(this);
        myMsg.setText(title);
        myMsg.setGravity(Gravity.CENTER_HORIZONTAL);
        myMsg.setTextSize(18);
        myMsg.setTextColor(R.color.white);
        myMsg.setPadding(0,30,0,10);
        alertDialog.setCustomTitle(myMsg);
        alertDialog.setCancelable(true);

        LinearLayout poplayout = new LinearLayout(this);
        LinearLayout.LayoutParams popparms = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        //layout.setOrientation(LinearLayout.VERTICAL);
        poplayout.setLayoutParams(popparms);

        if(shiftListArr == null || shiftListArr.length() ==0)
            return;
        for (int i = 0; i < shiftListArr.length(); i++) {

            JSONObject dayShift = null;
            String shiftDate = null, alt_date = null;
            int s_time = 0, e_time = 0, ks_time = -1, ke_time = -1, break_time = 0, night_break_time = 0;
            String arrive_checked_at = null, leave_checked_at = null, break_at = null, night_break_at = null,
                    e_leave_at = null, e_leave_checked_at = null, over_time_at = null, over_time_checked_at = null, late_at = null, late_checked_at = null,
                    arrive_changed_at = null, arrive_changed_checked_at = null, leave_changed_at = null, leave_changed_checked_at = null;
            String rest_date = getString(R.string.rest_date), rest_request = getString(R.string.rest_request), rest_at = null, rest_check_at = null;
            Boolean e_arrive = false, l_arrive = false, over_time = false, e_leave = false, miss = false, check = false,
                    checked_at = false, misschk_at = false, rest_day = false;

            try {
                dayShift = shiftListArr.getJSONObject(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            int shiftId = 0;
            try {
                shiftId = Integer.parseInt(dayShift.getString("id"));

//                if(todayShiftId != null && todayShiftId.equals(dayShift.getString("id"))){
//                    over_day = true;
//                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            shiftIdList.put(shiftId);

            TableRow row = new TableRow(this);


            TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
            row.setLayoutParams(lp);
            LinearLayout linearLayoutD = new LinearLayout(this);
            TextView tv = new TextView(this);
            tv.setTextAlignment(TEXT_ALIGNMENT_CENTER);

            try {
                shiftDate = dayShift.getString("shift_date");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            shiftDayList.put(shiftDate);

            Date shiftDay = null;
            if (shiftDate != null) {
                try {
                    shiftDay = new SimpleDateFormat("yyyy-MM-dd").parse(shiftDate);
                } catch (ParseException e) {
                    e.printStackTrace();
                }


                String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).format(new Date());

                try {
                    Date today = new SimpleDateFormat("yyyy-MM-dd").parse(todayStr);
                    if (today.compareTo(shiftDay) < 0) {
                        over_day = true;
                        //break;
                        //checked_at = false;
                    }
                    if (today.compareTo(shiftDay) == 0) {
                        is_today = true;
                        //break;
                        //checked_at = false;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            if(over_day){
                row.setBackgroundColor(Color.LTGRAY);
            }
            rowVal++;
            if (dayShift.has("rest_at") && !dayShift.isNull("rest_at")) {
                // Do something with object.
                try {
                    rest_at = dayShift.getString("rest_at");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (dayShift.has("rest_checked_at") && !dayShift.isNull("rest_checked_at")) {
                // Do something with object.
                try {
                    rest_check_at = dayShift.getString("rest_checked_at");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (rest_at != null) {
                if (rest_check_at != null) {
                    rest_day = true;
                }
            }


            String[] splStr = shiftDate.split("-");
            String shift_date = splStr[1] + "月" + splStr[2] + "日";
            tv.setTextAlignment(TEXT_ALIGNMENT_CENTER);
            tv.setText(shift_date);
            shiftDayListLabel.put(shift_date);
            tv.setTextSize(COMPLEX_UNIT_SP, 14);
            LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params2.setMargins(0,20,0,0);
            tv.setLayoutParams(params2);
            linearLayoutD.addView(tv);

            row.addView(linearLayoutD);

            LinearLayout linearLayout = new LinearLayout(this);

            if (dayShift.has("arrive_checked_at") && !dayShift.isNull("arrive_checked_at")) {
                // Do something with object.
                try {
                    arrive_checked_at = dayShift.getString("arrive_checked_at");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (dayShift.has("late_at") && !dayShift.isNull("late_at")) {
                // Do something with object.
                try {
                    late_at = dayShift.getString("late_at");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (dayShift.has("late_checked_at") && !dayShift.isNull("late_checked_at")) {
                // Do something with object.
                try {
                    late_checked_at = dayShift.getString("late_checked_at");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (dayShift.has("arrive_changed_at") && !dayShift.isNull("arrive_changed_at")) {
                // Do something with object.
                try {
                    arrive_changed_at = dayShift.getString("arrive_changed_at");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (dayShift.has("arrive_changed_checked_at") && !dayShift.isNull("arrive_changed_checked_at")) {
                // Do something with object.
                try {
                    arrive_changed_checked_at = dayShift.getString("arrive_changed_checked_at");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            Date arrive_check_at = null;
            try {
                if (arrive_checked_at != null) {
                    arrive_check_at = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(arrive_checked_at);
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }
            int cs_time = -1;
            int hour = 0;
            int min = 0;
            if (arrive_check_at != null) {

                hour = arrive_check_at.getHours();
                min = arrive_check_at.getMinutes();
                cs_time = hour * 60 + min;
                if(shiftDay.getDay() < arrive_check_at.getDay()){
                    hour += 24;
                    cs_time += 1440;
                }
            }

            EditText rsTime = new EditText(this);
            int pixels = (int) (50 * this.getResources().getDisplayMetrics().density);
            rsTime.setWidth(pixels);
            rsTime.setTextAlignment(TEXT_ALIGNMENT_CENTER);
            if(over_day){
                rsTime.setFocusable(false);
                rsTime.setEnabled(false);
            }
            else{
                rsTime.setId(0003);
                rsTime.setOnTouchListener(otl);
            }
            if (hour != 0 || min != 0) {
                String minStr = String.valueOf(min);
                String hourStr = String.valueOf(hour);;
                if(min<10){
                    minStr = "0" + min;
                }

                if(hour < 10){
                    hourStr = "0" + hour;
                }
                if(late_at != null && late_checked_at == null){
                    rsTime.setText("申請中");
                    shiftArriveList.put(late_at);
                }
                else if(arrive_changed_at != null && arrive_changed_checked_at == null){
                    rsTime.setText("申請中");
                    shiftArriveList.put(arrive_changed_at);
                }
                else{
                    rsTime.setText(hourStr + ":" + minStr);
                    shiftArriveList.put(arrive_checked_at);
                }

            } else {
                if(arrive_changed_at != null && arrive_changed_checked_at == null){
                    rsTime.setText("申請中");
                    shiftArriveList.put(arrive_changed_at);
                }
                else{
                    rsTime.setText("");
                    shiftArriveList.put(null);
                }

            }

            rsTime.setTextSize(COMPLEX_UNIT_SP, 14);
            linearLayout.addView(rsTime);

            TextView sTime = new TextView(this);

            try {
                s_time = dayShift.getInt("s_time");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (cs_time != -1 && cs_time > s_time + 15) {
                l_arrive = true;
            }
            if (cs_time != -1 && cs_time < s_time - 30) {
                e_arrive = true;
            }
            String sTime_text = convertIntToTime(s_time);

            sTime.setText("(" + sTime_text + ")");
            sTime.setTextSize(COMPLEX_UNIT_SP, 14);

            linearLayout.addView(sTime);

            row.addView(linearLayout);


            LinearLayout elinearLayout = new LinearLayout(this);


            if (dayShift.has("leave_checked_at") && !dayShift.isNull("leave_checked_at")) {
                // Do something with object.
                try {
                    leave_checked_at = dayShift.getString("leave_checked_at");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (dayShift.has("e_leave_at") && !dayShift.isNull("e_leave_at")) {
                // Do something with object.
                try {
                    e_leave_at = dayShift.getString("e_leave_at");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (dayShift.has("e_leave_checked_at") && !dayShift.isNull("e_leave_checked_at")) {
                // Do something with object.
                try {
                    e_leave_checked_at = dayShift.getString("e_leave_checked_at");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (dayShift.has("over_time_at") && !dayShift.isNull("over_time_at")) {
                // Do something with object.
                try {
                    over_time_at = dayShift.getString("over_time_at");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (dayShift.has("over_time_checked_at") && !dayShift.isNull("over_time_checked_at")) {
                // Do something with object.
                try {
                    over_time_checked_at = dayShift.getString("over_time_checked_at");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (dayShift.has("leave_changed_at") && !dayShift.isNull("leave_changed_at")) {
                // Do something with object.
                try {
                    leave_changed_at = dayShift.getString("leave_changed_at");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (dayShift.has("leave_changed_checked_at") && !dayShift.isNull("leave_changed_checked_at")) {
                // Do something with object.
                try {
                    leave_changed_checked_at = dayShift.getString("leave_changed_checked_at");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }


            Date leave_check_at = null;
            try {
                if (leave_checked_at != null) {
                    leave_check_at = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(leave_checked_at);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if (leave_checked_at != null && arrive_checked_at != null) {
                checked_at = true;
            }
            if (leave_checked_at == null && arrive_checked_at == null && !over_day) {
                misschk_at = true;
            }
            int ce_time = -1;
            int ehour = 0;
            int emin = 0;
            if (leave_check_at != null) {
                ehour = leave_check_at.getHours();
                emin = leave_check_at.getMinutes();
                ce_time = ehour * 60 + emin;
                if(shiftDay.getDay() < leave_check_at.getDay()){
                    ehour += 24;
                    ce_time += 1440;
                }
            }

            EditText reTime = new EditText(this);

            reTime.setWidth(pixels);
            reTime.setTextAlignment(TEXT_ALIGNMENT_CENTER);
            if(over_day){
                reTime.setFocusable(false);
                reTime.setEnabled(false);
            }
            else{
                reTime.setId(0004);
                reTime.setOnTouchListener(otl);
            }
            if (ehour != 0 || emin != 0) {
                String minStr = String.valueOf(emin);
                String hourStr = String.valueOf(ehour);;
                if(emin<10){
                    minStr = "0" + emin;
                }

                if(ehour < 10){
                    hourStr = "0" + ehour;
                }
                if(over_time_at != null && over_time_checked_at == null){
                    reTime.setText("申請中");
                }
                else if(e_leave_at != null && e_leave_checked_at == null){
                    reTime.setText("申請中");
                }
                else if(leave_changed_at != null && leave_changed_checked_at == null){
                    reTime.setText("申請中");
                }
                else{
                    reTime.setText(hourStr + ":" + minStr);
                }
            } else {
                if(leave_changed_at != null && leave_changed_checked_at == null){
                    reTime.setText("申請中");
                }
                else{
                    reTime.setText("");
                }

            }
            if(arrive_check_at == null && leave_check_at == null){
                if((arrive_changed_at == null || arrive_changed_checked_at != null) && (leave_changed_at == null || leave_changed_checked_at != null)){
                    LinearLayout layout = new LinearLayout(this);
                    LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    //layout.setOrientation(LinearLayout.VERTICAL);
                    layout.setLayoutParams(parms);

                    //layout.setGravity(Gravity.CLIP_VERTICAL);
                    //layout.setPadding(2, 2, 2, 2);

                    TextView tv1 = new TextView(this);
                    tv1.setPadding(30,0,10,10);
                    tv1.setText(shift_date);
                    TextView et = new TextView(this);
                    et.setPadding(0,0,30,10);
                    et.setText("シフト外勤務が発生しているため勤怠申請（欠勤）をお願いします。");

                    layout.addView(tv1);
                    layout.addView(et);

                    poplayout.addView(layout);
                    if(!over_day && !is_today){
                        popStr += shift_date + "　シフト外勤務が発生しているため勤怠申請（欠勤）をお願いします。\n";
                    }


                }
                else{
                    if(arrive_changed_at == null || arrive_changed_checked_at != null){
                        LinearLayout layout = new LinearLayout(this);
                        LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        //layout.setOrientation(LinearLayout.VERTICAL);
                        layout.setLayoutParams(parms);

                        //layout.setGravity(Gravity.CLIP_VERTICAL);
                        //layout.setPadding(2, 2, 2, 2);

                        TextView tv1 = new TextView(this);
                        tv1.setPadding(30,0,10,10);
                        tv1.setText(shift_date);
                        TextView et = new TextView(this);
                        et.setPadding(0,0,30,10);
                        et.setText("打刻漏れが発生しているため勤怠申請(出勤)をお願いします。");

                        layout.addView(tv1);
                        layout.addView(et);
                        poplayout.addView(layout);
                        if(!over_day && !is_today){
                            popStr += shift_date + "　打刻漏れが発生しているため勤怠申請(出勤)をお願いします。\n";
                        }

                    }
                    if(leave_changed_at == null || leave_changed_checked_at != null){
                        LinearLayout layout = new LinearLayout(this);
                        LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        //layout.setOrientation(LinearLayout.VERTICAL);
                        layout.setLayoutParams(parms);

                        //layout.setGravity(Gravity.CLIP_VERTICAL);
                        //layout.setPadding(2, 2, 2, 2);

                        TextView tv1 = new TextView(this);
                        tv1.setPadding(30,0,10,10);
                        tv1.setText(shift_date);
                        TextView et = new TextView(this);
                        et.setPadding(0,0,30,10);
                        et.setText("打刻漏れが発生しているため勤怠申請(退勤)をお願いします。");

                        layout.addView(tv1);
                        layout.addView(et);
                        poplayout.addView(layout);

                        if(!over_day && !is_today){
                            popStr += shift_date + "　打刻漏れが発生しているため勤怠申請(退勤)をお願いします。\n";
                        }

                    }

                }


            }
            else if(arrive_check_at == null){
                if(arrive_changed_at != null && arrive_changed_checked_at == null){

                }
                else{
                    LinearLayout layout = new LinearLayout(this);
                    LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    //layout.setOrientation(LinearLayout.VERTICAL);
                    layout.setLayoutParams(parms);

                    //layout.setGravity(Gravity.CLIP_VERTICAL);
                    //layout.setPadding(2, 2, 2, 2);

                    TextView tv1 = new TextView(this);
                    tv1.setPadding(30,0,10,10);
                    tv1.setText(shift_date);
                    TextView et = new TextView(this);
                    et.setPadding(0,0,30,10);
                    et.setText("打刻漏れが発生しているため勤怠申請(出勤)をお願いします。");

                    layout.addView(tv1);
                    layout.addView(et);
                    poplayout.addView(layout);
                    if(!over_day && !is_today){
                        popStr += shift_date + "　打刻漏れが発生しているため勤怠申請(出勤)をお願いします。\n";
                    }

                }

            }
            else if(leave_check_at == null) {
               if(leave_changed_at != null && leave_changed_checked_at == null){

               }
               else{
                   LinearLayout layout = new LinearLayout(this);
                   LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                   //layout.setOrientation(LinearLayout.VERTICAL);
                   layout.setLayoutParams(parms);

                   //layout.setGravity(Gravity.CLIP_VERTICAL);
                   //layout.setPadding(2, 2, 2, 2);

                   TextView tv1 = new TextView(this);
                   tv1.setPadding(30,0,10,10);
                   tv1.setText(shift_date);
                   TextView et = new TextView(this);
                   et.setPadding(0,0,30,10);
                   et.setText("打刻漏れが発生しているため勤怠申請(退勤)をお願いします。");

                   layout.addView(tv1);
                   layout.addView(et);
                   poplayout.addView(layout);
                   if(!over_day && !is_today){
                       popStr += shift_date + "　打刻漏れが発生しているため勤怠申請(退勤)をお願いします。\n";
                   }

               }

            }
//            if(arrive_check_at != null){
//                if(late_at != null && late_checked_at == null)
//                    popStr += shift_date + "　打刻漏れが発生しているため勤怠申請(出勤)をお願いします。\n";
//            }
//            if(leave_check_at != null) {
//                if((e_leave_at != null || e_leave_checked_at == null) || (over_time_at != null || over_time_checked_at == null))
//                    popStr += shift_date + "　打刻漏れが発生しているため勤怠申請(退勤)をお願いします。\n";
//            }
            reTime.setTextSize(COMPLEX_UNIT_SP, 14);
            elinearLayout.addView(reTime);

            TextView eTime = new TextView(this);

            try {
                e_time = dayShift.getInt("e_time");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (ce_time != -1 && ce_time > e_time + 30) {
                over_time = true;
            }
            if (ce_time != -1 && ce_time < e_time - 30) {
                e_leave = true;
            }

            String eTime_text = convertIntToTime(e_time);

            eTime.setText("(" + eTime_text + ")");
            eTime.setTextSize(COMPLEX_UNIT_SP, 14);

            elinearLayout.addView(eTime);

            row.addView(elinearLayout);

            //break_at

            if (dayShift.has("break_at") && !dayShift.isNull("break_at")) {
                // Do something with object.
                try {
                    break_at = dayShift.getString("break_at");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            Date break_at_date = null;

            try {
                if (break_at != null) {
                    break_at_date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(break_at);
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }


            int break_end = -1, hour_break = 0, min_break = 0;

            if (break_at_date != null) {
                hour_break = break_at_date.getHours();
                min_break = break_at_date.getMinutes();
                if(shiftDay.getDay() < break_at_date.getDay()){
                    hour_break += 24;
                }
                break_end = hour_break * 60 + min_break;
            }
            shiftBreakStime.put(break_end);


            //rest time
            if (dayShift.has("break_time") && !dayShift.isNull("break_time")) {
                // Do something with object.
                try {
                    break_time = dayShift.getInt("break_time");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            EditText rest_time = new EditText(this);
            rest_time.setTextAlignment(TEXT_ALIGNMENT_CENTER);
            rest_time.setText(String.valueOf(break_time));
            rest_time.setTextSize(COMPLEX_UNIT_SP, 14);
            //row.addView(rest_time);

            LinearLayout rlinearLayout = new LinearLayout(this);

            final Spinner spinner = new Spinner(this);
            spinner.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            final String[] personNames = {"0", "15", "30", "45", "60", "75", "90"};
            ArrayAdapter arrayAdapter = new ArrayAdapter(this, R.layout.my_spinner, personNames);
            spinner.setAdapter(arrayAdapter);
            spinner.setBackground(null);
            spinner.setGravity(CENTER);
            if(over_day){
                spinner.setEnabled(false);
            }
            int spinnerPosition = arrayAdapter.getPosition(String.valueOf(break_time));
//            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.my_spinner, arrayList);
//            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//            spinner.setAdapter(arrayAdapter);
            spinner.setSelection(spinnerPosition);

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    //Toast.makeText(this, getString(R.string.selected_item) + " " + personNames[position], Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            // Add Spinner to LinearLayout
            if (linearLayout != null) {
                LinearLayout.LayoutParams params3 = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                int pixels_l = (int) (1 * this.getResources().getDisplayMetrics().density);
                int pixels_r = (int) (40 * this.getResources().getDisplayMetrics().density);
                params3.setMargins(pixels_l,10,-pixels_r,0);
                spinner.setLayoutParams(params3);
                rlinearLayout.addView(spinner);
            }

            row.addView(rlinearLayout);

            LinearLayout cLinearLayout = new LinearLayout(this);
            CheckBox checkBox = new CheckBox(this);
            checkBox.setChecked(false);
            if(over_day){
                checkBox.setEnabled(false);
            }
            else{
                checkBox.setEnabled(true);
            }


            LinearLayout.LayoutParams params_d = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            );
            params_d.setMargins(15,0,15,0);
            checkBox.setLayoutParams(params_d);
            cLinearLayout.addView(checkBox);
            //row.setOnClickListener(mListener);
            row.addView(cLinearLayout);
            shiftTableLayout.addView(row, i + 1);
        }

        TableLayout yourRootLayout = shiftTableLayout;
        int count = yourRootLayout.getChildCount();
        for(int i = 0; i < count; i++){
            View v = yourRootLayout.getChildAt(i);
            if(v instanceof TableRow){
                TableRow row = (TableRow)v;
                int rowCount = row.getChildCount();
                for (int r = 0; r < rowCount; r++){
                    View v2 = row.getChildAt(r);

                    if (v2 instanceof Button){
                        Button b = (Button)v2;
                        b.setOnClickListener(mListener);
                    }
                }
            }
        }
        alertDialog.setMessage(popStr);
       //alertDialog.setView(poplayout);
        if(firstLoad && !popStr.equals("")){
            firstLoad = false;
            alertDialog.show();
            int pixels_w = (int) (350 * this.getResources().getDisplayMetrics().density);
            int pixels_h = (int) (500 * this.getResources().getDisplayMetrics().density);
            alertDialog.getWindow().setLayout(pixels_w, pixels_h);
//            TextView textView = (TextView) alertDialog.findViewById(android.R.id.message);
//            textView.setTextSize(15);
//            ViewGroup group = (ViewGroup) alertDialog.getView();
//            TextView messageTextView = (TextView) group.getChildAt(0);
//            messageTextView.setTextSize(20);
            //toast.show();
        }




    }


    private View.OnTouchListener otl = new View.OnTouchListener() {
        public boolean onTouch (View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                selectAltDateView = v;
                String title = "";
                int vid = v.getId();
                if(vid == 0002){
                    title = "深夜休憩終了";
                }
                if(vid == 0003){
                    title = "始業";
                }
                if(vid == 0004){
                    title = "終業";
                }
                if(vid == 0005){
                    title = "休憩開始";
                }
                if(vid == 0007){
                    title = "休憩終了";
                }
                if(vid == 0006) {
                    title = "深夜休憩開始";
                }
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(ShowHistoryActivity.this);
                alertDialog.setTitle(title + "時間を選択してください。");
                View sView = getLayoutInflater().inflate(R.layout.spinner_time, null);
                Spinner tSpiner = (Spinner) sView.findViewById(R.id.spinner);
                ArrayAdapter<String> adapter;
                if(vid == 0002 || vid == 0006){
                    adapter = new ArrayAdapter<String>(ShowHistoryActivity.this, R.layout.my_spinner, getResources().getStringArray(R.array.night_hour_thirty));
                }
                else{
                    adapter = new ArrayAdapter<String>(ShowHistoryActivity.this, R.layout.my_spinner, getResources().getStringArray(R.array.hour_thirty));
                }

                adapter.setDropDownViewResource(R.layout.spinner_layout);
                tSpiner.setAdapter(adapter);
                Spinner mSpiner = (Spinner) sView.findViewById(R.id.spinner2);
                ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(ShowHistoryActivity.this, R.layout.my_spinner, getResources().getStringArray(R.array.minute_thirty));
                adapter2.setDropDownViewResource(R.layout.spinner_layout);
                mSpiner.setAdapter(adapter2);
                alertDialog.setPositiveButton(
                        "取消",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                alertDialog.setNegativeButton(
                        "確認",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String hour = tSpiner.getSelectedItem().toString();
                                String min = mSpiner.getSelectedItem().toString();
                                EditText ed = (EditText) selectAltDateView;
                                ed.setText(hour+":"+min);
                                dialog.cancel();
                            }
                        });

                alertDialog.setView(sView);
                AlertDialog ad = alertDialog.create();
                ad.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        Button negButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        params.setMargins(20,0,20,0);
                        negButton.setLayoutParams(params);
                    }
                });
                ad.show();
            }

            return true; // the listener has consumed the event
        }
    };

    private View.OnTouchListener otl1 = new View.OnTouchListener() {
        public boolean onTouch (View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                selectAltDateView = v;
                new DatePickerDialog(ShowHistoryActivity.this, date, myCalendar
                        .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }

            return true; // the listener has consumed the event
        }
    };
    private View selectAltDateView = null;

    private View.OnClickListener mListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            int vid = v.getId();
            //vid =-1;
            if(vid == -1){
                return;
            }
            else{
                getRequestResponse(String.valueOf(vid));
            }
        }
    };

    DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth) {
            // TODO Auto-generated method stub
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, monthOfYear);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateLabel(selectAltDateView);
        }

    };

    private void updateLabel(View v) {
        TextView textView = (TextView) v;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            textView.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).format(myCalendar.getTime()));
        }
    }

    private String GetFirstCol(View elView, String day) {
        LinearLayout layout = (LinearLayout) elView;
        EditText edit = (EditText) layout.getChildAt(0);
        String val = edit.getText().toString();

        return convertStringTimeToString(val, day);
    }

    private String GetSecondCol(View elView, String day) {
        EditText edit = (EditText) elView;
        String val = edit.getText().toString();

        return convertStringTimeToString(val, day);
    }
    private int GetThirdCol(View elView, String day) {
        LinearLayout layout = (LinearLayout) elView;
        Spinner mySpinner = (Spinner) layout.getChildAt(0);
        String val = mySpinner.getSelectedItem().toString();
//        EditText edit = (EditText) elView;
//        String val = edit.getText().toString();
        return Integer.parseInt(val);
    }

    private int GetFourthCol(View elView, String at) {
        EditText edit = (EditText) elView;
        String val = edit.getText().toString();
        int reVal = 0;
        if (val == null || val.equals("")) {
            return reVal;
        } else {
            int at_int = Integer.parseInt(at);
            String[] valStr = val.split(":");
            if(valStr.length != 2){
                return valid_int;
            }

            if(!valStr[0].matches("\\d+(?:\\.\\d+)?") || !valStr[1].matches("\\d+(?:\\.\\d+)?"))
            {
                return valid_int;
            }
            if(Integer.parseInt(valStr[0]) > 23 || Integer.parseInt(valStr[1]) > 59 ) {
                return valid_int;
            }
            reVal = Integer.parseInt(valStr[0]) * 60 + Integer.parseInt(valStr[1]) - at_int;
            if(reVal<=0) return valid_int;
        }
        return reVal;
    }

    public void getRequestResponse(String shift_id) {
        try {
            // TODO: handle loggedInUser authentication

            RequestParams rp = new RequestParams();
            rp.add("shift_id", shift_id);

            HttpUtils.addToken(token);
            HttpUtils.post(Constants.REQUEST_REST, rp, new JsonHttpResponseHandler() {
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
                        if(rpMessage == "ERR_INVALID_TOKEN"){
                            Toast.makeText(getApplicationContext(), "失敗しました。", Toast.LENGTH_SHORT).show();
                            ToLogin();
                            return;
                        }

                        Toast.makeText(getApplicationContext(), rpMessage, Toast.LENGTH_LONG).show();
                        mAuthTask = new UserInfoTask(token, month);
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

    private String GetSeventhCol(View elView) {
        EditText edit = (EditText) elView;
        if (edit.getText().toString().equals(""))
            return null;

        String date = edit.getText().toString();
        String[] dateStr = date.split("-");
        if (dateStr.length != 3){
            return valid;
        }
        if(!dateStr[0].matches("\\d+(?:\\.\\d+)?") || !dateStr[1].matches("\\d+(?:\\.\\d+)?") || !dateStr[2].matches("\\d+(?:\\.\\d+)?"))
            return valid;
        if(Integer.parseInt(dateStr[1]) > 12 || Integer.parseInt(dateStr[2]) > 31)
            return valid;
        if(Integer.parseInt(dateStr[1]) < 10){
            dateStr[1] = "0" + Integer.parseInt(dateStr[1]);
        }
        if(Integer.parseInt(dateStr[2]) < 10){
            dateStr[2] = "0" + Integer.parseInt(dateStr[2]);
        }
        return dateStr[0] + "-" + dateStr[1] + "-" + dateStr[2];
    }

    private String convertStringTimeToString(String time, String day) {
        if (time == null || time.equals("")) {
            return null;
        }
        String[] splTime = time.split(":");
        if(splTime.length != 2){
            return valid;
        }

        if(!splTime[0].matches("\\d+(?:\\.\\d+)?") || !splTime[1].matches("\\d+(?:\\.\\d+)?"))
        {
            return valid;
        }

        if(Integer.parseInt(splTime[0]) > 36 || Integer.parseInt(splTime[1]) > 59){
            return valid;
        }

        return String.valueOf(Integer.parseInt(splTime[0]) * 60 + Integer.parseInt(splTime[1]));
    }

    private String convertIntToTime(int time) {
        if(time == -1) return null;
        int hour = time / 60;
        int min = time - hour * 60;
        String minStr = String.valueOf(min);
        String hourStr = String.valueOf(hour);;
        if(min<10){
            minStr = "0" + min;
        }

        if(hour < 10){
            hourStr = "0" + hour;
        }
        //reTime.setText(hourStr + ":" + minStr);
        String t = hourStr + ":" + minStr;
        return t;
    }

    public View getTableLayoutCell(TableLayout layout, int rowNo, int columnNo) {
        if (rowNo >= layout.getChildCount()) return null;
        TableRow row = (TableRow) layout.getChildAt(rowNo);

        if (columnNo >= row.getChildCount()) return null;
        return row.getChildAt(columnNo);

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

                //if (!userPassword.equals(mPassword))
                return false;
            }
            // TODO: register the new account here.
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
                    postParams.put("shift_list", String.valueOf(modifyShiftList));
                    modifyShiftList = new JSONArray();

                    HttpPostRequest httpPostRequest = new HttpPostRequest();
                    result = httpPostRequest.POST(Constants.UPDATE_SHIFT_LIST, postParams, token);

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

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        protected void onPostExecute(final Boolean success) {
            mFirebaseTask = null;
            showProgress(false);
            mAuthTask = new UserInfoTask(token, month);
            mAuthTask.execute((Void) null);
            btnRequestList.setEnabled(true);
            if (success) {
                Toast.makeText(getApplicationContext(), "変更要求を送信しました。\n" +
                        "管理者が承認するまでお待ちください。", Toast.LENGTH_LONG).show();

            } else {

                if (mErrorMsg == null) {
                    Toast.makeText(getApplicationContext(), "失敗しました", Toast.LENGTH_SHORT).show();
                    return;
                }
                switch (mErrorMsg) {
                    case "ERR_INVALID_ARRIVE_TIME":
                        Toast.makeText(getApplicationContext(), "出勤時刻の形式が間違っていました", Toast.LENGTH_SHORT).show();
                        break;
                    case "ERR_INVALID_LEAVE_TIME":
                        Toast.makeText(getApplicationContext(), "退勤時刻の形式が間違っていました", Toast.LENGTH_SHORT).show();
                        break;
                    case "ERR_INVALID_BREAK_S_TIME":
                        Toast.makeText(getApplicationContext(), "休憩時間の形式が間違っていました", Toast.LENGTH_SHORT).show();
                        break;
                    case "ERR_INVALID_BREAK_TIME":
                        Toast.makeText(getApplicationContext(), "休憩終了時刻の形式が間違っていました", Toast.LENGTH_SHORT).show();
                        break;
                    case "ERR_INVALID_NIGHT_BREAK_S_TIME":
                        Toast.makeText(getApplicationContext(), "夜休憩時間の形式が間違っていました", Toast.LENGTH_SHORT).show();
                        break;
                    case "ERR_INVALID_NIGHT_BREAK_TIME":
                        Toast.makeText(getApplicationContext(), "夜休憩終了時刻の形式が間違っていました", Toast.LENGTH_SHORT).show();
                        break;
                    case "ERR_INVALID_ALT_DATE":
                        Toast.makeText(getApplicationContext(), "振替日形式が間違っていました", Toast.LENGTH_SHORT).show();
                        break;
                    case "ERR_INVALID_UNKNOWN":
                        Toast.makeText(getApplicationContext(), "不明なエラーが発生しました", Toast.LENGTH_SHORT).show();
                        break;
                    case "ERR_INVALID_SHIFT_LIST":
                        Toast.makeText(getApplicationContext(), "失敗しました", Toast.LENGTH_SHORT).show();
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

    public void ToLogin() {
        SharedPreferences sp = getSharedPreferences(Constants.SHARE_PREF, 0);
        SharedPreferences.Editor Ed = sp.edit();
        Ed.putString(Constants.SHARE_EMAIL, "");
        Ed.putString(Constants.SHARE_PWD, "");
        Ed.putString(Constants.TOKEN, null);
        Ed.putString(Constants.USER_NAME, "");
        Ed.putString(Constants.USER_PLACE, "");
        Ed.apply();
        Intent intent;
        intent = new Intent(ShowHistoryActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}