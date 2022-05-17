package com.business.ekant.data;

import android.app.Activity;
import android.widget.Toast;

public class BackPressCloseHandler extends Activity {

    private long backKeyPressedTime = 0;

    private Toast toast;

    private Activity activity;

    public BackPressCloseHandler(Activity context) {
        this.activity = context;
    }


    public void onBackPressed() {

        if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
            backKeyPressedTime = System.currentTimeMillis();
            showGuide();
            return;
        }

        if (System.currentTimeMillis() <= backKeyPressedTime + 2000) {
            activity.finishAffinity();
            toast.cancel();
        }

    }


    private void showGuide() {
        toast = Toast.makeText(activity, "戻るボタンをタッチして終了します。",
                Toast.LENGTH_SHORT);
        toast.show();
    }

}