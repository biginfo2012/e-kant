package com.business.ekant.ui.login;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.business.ekant.MainActivity;
import com.business.ekant.R;
import com.business.ekant.data.BackPressCloseHandler;
import com.business.ekant.data.Constants;
import com.business.ekant.data.HttpPostRequest;
import com.business.ekant.data.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.json.JSONArray;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "MyFirebaseMsgService";
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private LocationManager mLocationManager;
    String locationProvider;

    private LoginViewModel loginViewModel;
    private UserLoginTask mAuthTask = null;
    private EditText mPasswordView;
    private View mProgressView;
    private EditText mEmailView;

    String fToken;

    public String userToken = null;
    public String userName;
    public String userPlace;

    public Double latitude = 0.0;
    public Double longitude = 0.0;

    int userSTime;

    private BackPressCloseHandler backPressCloseHandler;

    @SuppressLint("ResourceAsColor")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

//        FirebaseOptions options = new FirebaseOptions.Builder()
//                .setApplicationId("com.business.ekant") // Required for Analytics.
//                .setProjectId("e-kant-60667") // Required for Firebase Installations.
//                .setApiKey("AIzaSyCRPtrlohev6zGkG-wUQdU7r7AzqiblZEI") // Required for Auth.
//                .build();
//        FirebaseApp.initializeApp(this, options, "EKANT");

        loginViewModel = new ViewModelProvider(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

//        Button crashButton = new Button(this);
//        crashButton.setText("Crash!");
//        crashButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View view) {
//                throw new RuntimeException("Test Crash"); // Force a crash
//            }
//        });
//
//        addContentView(crashButton, new ViewGroup.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT));

        //setting the title
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setSubtitleTextColor(Color.WHITE);
        toolbar.setTitle(R.string.app_name);

        //setSupportActionBar(toolbar);

        final EditText usernameEditText = findViewById(R.id.username);
        mEmailView = findViewById(R.id.username);
        final EditText passwordEditText = findViewById(R.id.password);
        mPasswordView = findViewById(R.id.password);
        final Button loginButton = findViewById(R.id.login);
        final ProgressBar loadingProgressBar = findViewById(R.id.loading);
        mProgressView = findViewById(R.id.loading);

        GoogleApiAvailability googleApiAvailability =  GoogleApiAvailability.getInstance();

        int success = googleApiAvailability.isGooglePlayServicesAvailable(this);

        if(success != ConnectionResult.SUCCESS)
        {
            googleApiAvailability.makeGooglePlayServicesAvailable(this);
        }

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationProvider = mLocationManager.getBestProvider(new Criteria(), true);
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
            locationProvider = LocationManager.GPS_PROVIDER;
        }
        checkLocationPermission();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !Settings.canDrawOverlays(this)){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            String title = "権限要請";

            builder.setMessage("別の画面の上に描くという権限が必要です。\n" +
                    "承認お願いします。");
            builder.setCancelable(false);

            builder.setPositiveButton(
                    "確認",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            String s = "package:com.business.ekant";
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse(s));
                            startActivityForResult(intent, 1);
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();

        }


        backPressCloseHandler = new BackPressCloseHandler(this);

        loginViewModel.getLoginFormState().observe(this, new Observer<LoginFormState>() {
            @Override
            public void onChanged(@Nullable LoginFormState loginFormState) {
                if (loginFormState == null) {
                    return;
                }
                loginButton.setEnabled(loginFormState.isDataValid());
                if (loginFormState.getUsernameError() != null) {
                    usernameEditText.setError(getString(loginFormState.getUsernameError()));
                }
                if (loginFormState.getPasswordError() != null) {
                    passwordEditText.setError(getString(loginFormState.getPasswordError()));
                }
            }
        });

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
                loginViewModel.loginDataChanged(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        };
        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loginViewModel.login(usernameEditText.getText().toString(),
                            passwordEditText.getText().toString());
                }
                return false;
            }
        });




        SharedPreferences sp1 = getSharedPreferences(Constants.SHARE_PREF, 0);
        String saved_token = sp1.getString(Constants.TOKEN, null);

        if (saved_token != null) {
            if (!saved_token.equals("")) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }

        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });
    }

    public void onResume() {
        super.onResume();

        checkLocationPermission();

        //mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mlocationListener);
    }

    public void onPause() {
        super.onPause();

        //mLocationManager.removeUpdates(mlocationListener);
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

    public void statusCheck() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();

        }
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
        alert.show();
        final Button negButton = alert.getButton(AlertDialog.BUTTON_NEGATIVE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(20,0,0,0);
        negButton.setLayoutParams(params);
    }

    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        }else if (!Utils.isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.

            SharedPreferences sp1 = getSharedPreferences(Constants.SHARE_PREF, 0);
            //String u_email = sp1.getString(Constants.SHARE_EMAIL, null);

            showProgress(true);
            mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute((Void) null);
        }
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

    private class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;
        private String mErrorMsg;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
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
                    postParams.put("email", mEmail);
                    postParams.put("password", mPassword);

                    HttpPostRequest httpPostRequest = new HttpPostRequest();
                    result = httpPostRequest.POST(Constants.LOGIN_URL, postParams);

                    if (result == null) return false;
                    if (!result.getString("status").equals("success")) {
                        mErrorMsg = result.getString("message");
                        return false;
                    }
                    //TODO
                    //JSONObject content = result.getJSONObject("content");
                    JSONObject rt = result.getJSONObject("result");
                    userToken = rt.optString("token");
                    JSONObject si = rt.getJSONObject("staff_info");
                    userName = si.optString("name");
                    JSONArray sf = null;

                    if (si.has("staff_addresses") && !si.isNull("staff_addresses")) {
                        // Do something with object.
                        sf = si.optJSONArray("staff_addresses");

//                        else{
//                            Toast.makeText(getApplicationContext(), getString(R.string.login_failed), Toast.LENGTH_SHORT).show();
//                        }

                    }
                    if(sf != null && sf.length() != 0){
                        SharedPreferences sp = getSharedPreferences(Constants.SHARE_PREF, 0);
                        SharedPreferences.Editor Ed = sp.edit();
                        Ed.putString(Constants.STAFF_ADDRESS, sf.toString());
                        Ed.putString(Constants.SHIFT_ADDRESS, null);
                        Ed.apply();
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

                SharedPreferences sp = getSharedPreferences(Constants.SHARE_PREF, 0);
                SharedPreferences.Editor Ed = sp.edit();
                Ed.putString(Constants.SHARE_EMAIL, mEmail);
                Ed.putString(Constants.SHARE_PWD, mPassword);
                Ed.putString(Constants.TOKEN, userToken);
                Ed.putString(Constants.USER_NAME, userName);
                Ed.apply();

                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else{
                showProgress(false);
                if(mErrorMsg == null){
                    Toast.makeText(getApplicationContext(), getString(R.string.login_failed), Toast.LENGTH_SHORT).show();
                    return;
                }
                switch (mErrorMsg){
                    case "ERR_INVALID_PASSWORD":
                        mPasswordView.setError(getString(R.string.error_incorrect_password));
                        mPasswordView.requestFocus();
                        break;
                    case "ERR_INVALID_USER_EMAIL":
                        mEmailView.setError(getString(R.string.error_invalid_email));
                        mEmailView.requestFocus();
                        break;
                    default:
                        Toast.makeText(getApplicationContext(), getString(R.string.login_failed), Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

    public void onBackPressed() {
        backPressCloseHandler.onBackPressed();
    }
    private void updateUiWithUser(LoggedInUserView model) {
        String welcome = getString(R.string.welcome) + model.getDisplayName();
        // TODO : initiate successful logged in experience
        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
    }

    private void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }
    public boolean isOnline(Context context) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        //should check null because in airplane mode it will be null
        return (netInfo != null && netInfo.isConnected());
    }
}