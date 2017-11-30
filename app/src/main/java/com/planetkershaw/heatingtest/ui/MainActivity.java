package com.planetkershaw.heatingtest.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;

import com.planetkershaw.heatingtest.HeatingTestApp;
import com.planetkershaw.heatingtest.R;

/**************************************************************************

 This is the main entry activity of the application.

 If preferences have already been setup from an earlier execution then this
 activity attempts a login to the hub with those details, otherwise it
 opens up the settings screen.

 If login fails, then an error is reported and the user can retry or
 change the settings.

 **************************************************************************/

public class MainActivity extends Activity implements  Handler.Callback{

    // allow access to the rest api and global variables
    private HeatingTestApp app;
    private View mProgressView;
    private DialogInterface.OnClickListener dialogListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mProgressView = findViewById(R.id.login_progress);

        dialogListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case AlertDialog.BUTTON_NEUTRAL:
                        // retry the request
                        attemptLogin();
                        break;
                    case AlertDialog.BUTTON_POSITIVE:
                        // open the settings page
                        startActivityForResult(new Intent("com.planetkershaw.SETTINGS"),1);
                        break;

                }
                // close the dialog
                dialog.dismiss();
            }
        };

        app = (HeatingTestApp) getApplication();
        if (app.preferencesIsEmpty()) {
            // present settings screen
            startActivityForResult(new Intent("com.planetkershaw.SETTINGS"),1);
        }
        else {
            attemptLogin();
        }
    }

    private void attemptLogin () {
        // attempt to login to ZWay server
        // TODO: rather than pass them in, let restAPI retrieve all the info
        showProgress(true);
        app.restAPI.loginRequest(app.zway_user, app.zway_password, new Handler(this));
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                attemptLogin();
            }
            else {
                // present settings screen
                startActivityForResult(new Intent("com.planetkershaw.SETTINGS"),1);
            }
        }
    }



    // This is where we receive notification that our
    // login request has completed
    public boolean handleMessage (Message msg) {
        int error = msg.getData().getInt("error");
        if (error == 0) {
            app.restAPI.loginResponse(msg.getData().getString("payload"));
            // open main activity
//            Intent i = new Intent("com.planetkershaw.ROOMLIST");
            Intent i = new Intent("com.planetkershaw.CHOICE");
            startActivity(i);
            finish();
            showProgress(false);
        }

        else {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Unable to sign in");
            alertDialog.setMessage("Error code " + error + " (" + app.restAPI.getStatusText(error) + ")");
            // Alert dialog buttons
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "retry", dialogListener);
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "settings", dialogListener);
            alertDialog.show();
            showProgress (false);
        }
        return true;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
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


}
