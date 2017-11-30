package com.planetkershaw.heatingtest;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.planetkershaw.heatingtest.restmethod.RestAPI;

import java.text.DecimalFormat;

/***************************************************************************************
 HeatingTestApp

 An Application object has a well-defined lifecycle. It is created when the process for
 an application is created and it is not bound to any particular Activity.

 By extending the Application class, we are creating a simple way to hold onto and
 share nontrivial and nonpersistent data between activities and services within an
 application. Such data would be cumbersome to pass around as Intent extras everywhere.

 Taken from "Android in Practice" by Collins et al (Chapter 2) but appears to be quite a
 common technique. This blog post describes it in more detail:

 www.devahead.com/blog/2011/06/extending-the-android-application-class-and-dealing-with-singleton/

 To gain access to this data from an activity:

 HeatingTestApp app = (HeatingTestApp)getApplication()

 ***************************************************************************************/
public class HeatingTestApp extends Application {
    public RestAPI restAPI;
    public String zway_url;
    public String zway_port;
    public String zway_user;
    public String zway_password;
    public boolean mTwoPane = false;

    public DecimalFormat df = new DecimalFormat("#.#");

    private int colorBlue, colorGreen, colorOrange, colorYellow, colorRed;


    @Override
    public void onCreate() {
        super.onCreate();
        restAPI = new RestAPI(this);

        // read in the preferences
        SharedPreferences prefs = getSharedPreferences("myPrefs", Context.MODE_PRIVATE);

        // if any of our entries are in the preferences, then all of them should be
        zway_url = prefs.getString("ZWAY_URL", "");
        zway_port = prefs.getString("ZWAY_PORT", "");
        zway_user = prefs.getString("ZWAY_USER", "");
        zway_password = prefs.getString("ZWAY_PASS", "");

        colorBlue = ContextCompat.getColor(this, R.color.tempBlue);
        colorYellow = ContextCompat.getColor(this,R.color.tempYellow);
        colorGreen = ContextCompat.getColor(this,R.color.tempGreen);
        colorOrange = ContextCompat.getColor(this,R.color.tempOrange);
        colorRed = ContextCompat.getColor(this,R.color.tempRed);
    }

    public static Bitmap changeImageColor(Bitmap sourceBitmap, int color) {
        Bitmap resultBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0,
                sourceBitmap.getWidth() - 1, sourceBitmap.getHeight() - 1);
        Paint p = new Paint();
        ColorFilter filter = new LightingColorFilter(color, 1);
        p.setColorFilter(filter);

        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(resultBitmap, 0, 0, p);
        return resultBitmap;
    }

    //
    // preferencesIsEmpty
    //
    // returns true if any of the preferences are missing
    //
    public boolean preferencesIsEmpty () {
        return TextUtils.isEmpty(zway_url) || TextUtils.isEmpty(zway_port) ||
                TextUtils.isEmpty(zway_user) || TextUtils.isEmpty(zway_password);

    }

    //
    // updatePreferences
    //
    // commit new values to preferences
    //
    public void updatePreferences (String url, String port, String user, String password) {
        SharedPreferences prefs = getSharedPreferences("myPrefs",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("ZWAY_URL",url);
        editor.putString("ZWAY_PORT",port);
        editor.putString("ZWAY_USER",user);
        editor.putString("ZWAY_PASS",password);
        editor.commit();
        zway_url = url;
        zway_port = port;
        zway_user = user;
        zway_password = password;
    }

    public int convertTempToColorResourceID (double temp) {
        if      (temp < 10) return R.color.tempBlue;
        else if (temp < 15) return R.color.MediumSeaGreen;
        else if (temp < 20) return R.color.Gold;
        else if (temp < 25) return R.color.Orange;
        else                return R.color.tempRed;
    }

    public int convertTempToColor (double temp) {
        if      (temp < 10) return colorBlue;
        else if (temp < 15) return colorGreen;
        else if (temp < 20) return colorYellow;
        else if (temp < 25) return colorOrange;
        else                return colorRed;
    }
}
