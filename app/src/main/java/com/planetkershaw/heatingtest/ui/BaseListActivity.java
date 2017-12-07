package com.planetkershaw.heatingtest.ui;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.planetkershaw.heatingtest.HeatingTestApp;
import com.planetkershaw.heatingtest.R;
import com.planetkershaw.heatingtest.zwayservice.DataChangedReceiver;
import com.planetkershaw.heatingtest.zwayservice.LightList;
import com.planetkershaw.heatingtest.zwayservice.TimeService;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class BaseListActivity extends AppCompatActivity
{
    protected HeatingTestApp app;

    private DataChangedReceiver errorReceiver;

    protected Toolbar toolbar;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;

    private Activity thisActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // setup the app object
        app = (HeatingTestApp)getApplication();

        if (app.restAPI.cookie == null) {
            // cookie has been lost, so we need to start again
            Intent i = getBaseContext().getPackageManager()
                    .getLaunchIntentForPackage( getBaseContext().getPackageName() );
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            finish();
        }

        // register for errors in receiving data
        // TODO: report errors in toolbar instead with option to click and check error
        errorReceiver = new DataChangedReceiver() {
            public void onReceive(Context context, Intent intent) {
                String error = intent.getExtras().getString("error");
                AlertDialog alertDialog = new AlertDialog.Builder(thisActivity).create();
                alertDialog.setTitle("Unable to fetch data");
                alertDialog.setMessage("Error: " + error);
                // Alert dialog button
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Alert dialog action goes here
                                // onClick button code here
                                dialog.dismiss();// use dismiss to cancel alert dialog
                            }
                        });
                alertDialog.show();
            }

        };
//        registerReceiver(errorReceiver, new IntentFilter(DataChangedReceiver.ACTION_NETWORK_ERROR));

        thisActivity = this;
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        registerReceiver(errorReceiver, new IntentFilter(DataChangedReceiver.ACTION_NETWORK_ERROR));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver (errorReceiver);
    }

    // create an action bar button for a data refresh
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mymenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // handle button activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Pass the event to ActionBarDrawerToggle
        // If it returns true, then it has handled
        // the nav drawer indicator touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        if (id == R.id.mybutton) {
            // force an update of the data
            TimeService timeService = TimeService.getInstance();
            if (timeService != null) {
                timeService.forceRefresh();
            }
            Snackbar.make(findViewById(android.R.id.content), "Requested a data refresh", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();}
        return super.onOptionsItemSelected(item);
    }

    // this sets up use of the hamburger icon
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }


    // display status information in the toolbar
    protected void updateStatus () {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String strDate = sdf.format(c.getTime());
        toolbar.setSubtitle("Last updated: " + strDate);
    }

    protected void setupToolBar (int id, String title) {
        toolbar = (Toolbar) findViewById(id);
        setSupportActionBar(toolbar);
        toolbar.setTitle(title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    protected void setupMenuDrawer () {
        // set up the contents of the right hand drawer (the menu)
        ListView rightMenu = (ListView)findViewById(R.id.right_drawer);
        String[] menuItems = {
                "Settings",
                "Holiday Mode",
                "Away Mode"

        };
        rightMenu.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,menuItems));


        rightMenu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String entry = (String) parent.getAdapter().getItem(position);
                Intent intent = new Intent("com.planetkershaw.SETTINGS");
                startActivity(intent);
            }
        });

        // DrawerLayout
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);

                invalidateOptionsMenu();
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }
}
