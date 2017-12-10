package com.planetkershaw.heatingtest.zwayservice;

import android.app.Service;
import android.content.Context;
import android.content.Intent;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.planetkershaw.heatingtest.HeatingTestApp;
import com.planetkershaw.heatingtest.restmethod.RestAPI;

import java.util.Timer;
import java.util.TimerTask;

import static com.planetkershaw.heatingtest.restmethod.RestTask.ERROR;
import static com.planetkershaw.heatingtest.restmethod.RestTask.PAYLOAD;
import static com.planetkershaw.heatingtest.restmethod.RestTask.REQID;

/***************************************************************************************
 TimeService

 This service is used for retrieving the main data from the hub.
 It is setup to run once a minute and retrieves the heating and lighting data by two
 simultaneous requests. These requests are only sent when connected via wifi.

 TODO: this may not be the optimum solution to background polling as it is designed to
 run even when the application has closed / been closed. Might be better to spawn a thread
 from the main activity. Services are memory intensive and should not be left running for
 longer than necessary.

 ***************************************************************************************/

public class TimeService extends Service implements Handler.Callback {
    // constant
    public static final long NOTIFY_INTERVAL = 1 * 60 * 1000; // 1 minute

    private static TimeService myInstance = null;

    // run on another Thread to avoid crash
    private Handler mHandler = new Handler();
    // timer handling
    private Timer mTimer = null;

    private HeatingTestApp app;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        myInstance = this;
        // cancel if already existed
        if (mTimer != null) {
            mTimer.cancel();
        } else {
            // recreate new
            mTimer = new Timer();
        }
        // schedule task
        mTimer.scheduleAtFixedRate(new TimeDisplayTimerTask(), 0, NOTIFY_INTERVAL);
        app = (HeatingTestApp)getApplication();
    }

    static public TimeService getInstance () {
        return myInstance;
    }

    public void forceRefresh () {
        requestDataSet();
    }

    private void requestDataSet () {
//        Toast.makeText(app.getApplicationContext(), "request data",
//                Toast.LENGTH_LONG).show();
        //TODO: send in series by implementing a state machine
        // get the rooms data
        app.restAPI.roomsRequest(new Handler(this));
        // get the lighting data
        app.restAPI.lightsRequest(new Handler(this));
    }

    // handle the response to the rooms request
    public boolean handleMessage (Message msg) {
        // handle errors by displaying details
        // user should choose what to do
        int error = msg.getData().getInt(ERROR);
        if (error != 0) {
            // TODO: retry a few times before alerting listeners
            Intent intent = new Intent(DataChangedReceiver.ACTION_NETWORK_ERROR);
            intent.putExtra("error", RestAPI.getStatusText(error));
            app.sendBroadcast(new Intent(intent));
        }
        else {
            int reqType = msg.getData().getInt(REQID);
            if (reqType == RestAPI.RequestType.GET_ROOMS.ordinal()) {
                app.restAPI.roomsResponse(msg.getData().getString(PAYLOAD));
                // this request contains a view of the whole system and any activity requiring this information should
                // register to receive this notification
                app.sendBroadcast(new Intent(DataChangedReceiver.ACTION_HEATING_DATA_CHANGED));
            }
            else {
                app.restAPI.lightsResponse(msg.getData().getString(PAYLOAD));
                // this request contains a view of the whole system and any activity requiring this information should
                // register to receive this notification
                app.sendBroadcast(new Intent(DataChangedReceiver.ACTION_LIGHTING_DATA_CHANGED));
            }
        }
        return true;
    }

    private boolean checkWifiOnAndConnected() {
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (wifiMgr.isWifiEnabled()) { // Wi-Fi adapter is ON

            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();

            if( wifiInfo.getNetworkId() == -1 ){
                return false; // Not connected to an access point
            }
            return true; // Connected to an access point
        }
        else {
            return false; // Wi-Fi adapter is OFF
        }
    }

    class TimeDisplayTimerTask extends TimerTask {

        @Override
        public void run() {
            // run on another thread
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    // we only want to be getting this data if we are on wifi
                    if (checkWifiOnAndConnected ()){
                        requestDataSet();
                    }
                }
            });
        }
    }
}
