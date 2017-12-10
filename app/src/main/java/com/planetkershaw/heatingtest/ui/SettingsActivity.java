package com.planetkershaw.heatingtest.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;

import com.planetkershaw.heatingtest.HeatingTestApp;
import com.planetkershaw.heatingtest.R;


/***************************************************************************************
 SettingsActivity

 This was initially generated from an Android Studio template so there may well be
 redundant parts still included.

 ***************************************************************************************/

public class SettingsActivity extends AppCompatActivity{

    // to allow access to the global variables
    private HeatingTestApp app;

    // UI references.
    private EditText mURLView;
    private EditText mPortView;
    private EditText mUserView;
    private EditText mPasswordView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = (HeatingTestApp) getApplication();

        // display what we have and allow changes
        setContentView(R.layout.activity_settings);

        mURLView = (EditText) findViewById(R.id.serverurl);
        mPortView = (EditText) findViewById(R.id.serverport);
        mUserView = (EditText) findViewById(R.id.username);
        mPasswordView = (EditText) findViewById(R.id.password);

        mURLView.setText (app.zway_url);
        mPortView.setText(app.zway_port);
        mUserView.setText(app.zway_user);
        mPasswordView.setText(app.zway_password);

        Button mSaveButton = (Button) findViewById(R.id.settings_save_button);
        mSaveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (readSettings()) {
                    setResult(RESULT_OK);
                    finish();
                }
            }
        });

        Button mCancelButton = (Button) findViewById(R.id.settings_cancel_button);
        mCancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }


    //
    // readSettings
    //
    // read and verify the settings that the user has entered
    // if all is well, update the preferences and return
    // otherwise, move focus to incorrect values
    //
    private boolean readSettings () {
        // Reset errors.
        mUserView.setError(null);
        mPasswordView.setError(null);
        mURLView.setError(null);
        mPortView.setError(null);

        // Check and store values
        String zwayurl = mURLView.getText().toString();
        String zwayport = mPortView.getText().toString();
        String user = mUserView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean success = true;
        View focusView = null;

        if (!URLUtil.isHttpUrl(zwayurl)) {
            mURLView.setError("invalid url");
            focusView = mURLView;
            success = false;
        }

        if (TextUtils.isEmpty(zwayurl)) {
            mURLView.setError(getString(R.string.error_field_required));
            focusView = mURLView;
            success = false;
        }

        if (TextUtils.isEmpty(zwayport)) {
            mURLView.setError(getString(R.string.error_field_required));
            focusView = mPortView;
            success = false;
        }

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            success = false;
        }

        // Check for a valid user name.
        if (TextUtils.isEmpty(user)) {
            mUserView.setError(getString(R.string.error_field_required));
            focusView = mUserView;
            success = false;
        }

        if (success) {
            app.updatePreferences (zwayurl, zwayport, user, password);
        }
        else {
            focusView.requestFocus();
        }

        return success;

    }
}

