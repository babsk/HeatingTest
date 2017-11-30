package com.planetkershaw.heatingtest.zwayservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class DataChangedReceiver extends BroadcastReceiver {
    public static String ACTION_HEATING_DATA_CHANGED = "com.planetkershaw.HEATING_DATA_CHANGED";
    public static String ACTION_LIGHTING_DATA_CHANGED = "com.planetkershaw.LIGHTING_DATA_CHANGED";
    public static String ACTION_NETWORK_ERROR = "com.planetkershaw.NETWORK_ERROR";

    @Override
    public void onReceive(Context context, Intent intent) {

    }
}
