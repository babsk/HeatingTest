package com.planetkershaw.heatingtest.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.planetkershaw.heatingtest.R;

// TUTORIAL FROM
// http://www.vogella.com/tutorials/AndroidDialogs/article.html

public class TempDialogFragment extends DialogFragment implements TextView.OnEditorActionListener
{
    private EditText mEditText;

    public interface TempDialogListener {
        public void onTempDialogSave(int id, String value);
    }

    // Use this instance of the interface to deliver action events
    TempDialogListener mListener;

    private int id;

    // Empty constructor required for DialogFragment
    public TempDialogFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timer, container);
        mEditText = (EditText) view.findViewById(R.id.timerHour);

        // set this instance as callback for editor action
        mEditText.setOnEditorActionListener(this);
        mEditText.requestFocus();
        Bundle bundle = getArguments();
        getDialog().setTitle(bundle.getString("DIALOG_TEXT"));
        id = bundle.getInt("DIALOG_ID");

        Button cancelBtn = (Button) view.findViewById(R.id.cancel);
        cancelBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        Button saveBtn = (Button) view.findViewById(R.id.save);
        saveBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String input = mEditText.getText().toString();
                if (!"".equals(input)) {

                    mListener = null;
                    try
                    {
                        mListener = (TempDialogListener) getTargetFragment();
                    }
                    catch (ClassCastException e)
                    {
                        Log.e(this.getClass().getSimpleName(), "TempDialogListener of this class must be implemented by target fragment!", e);
                        throw e;
                    }
                    if (mListener != null) {
                        mListener.onTempDialogSave(id, input);
                    }
                }
                dismiss();
            }
        });

        return view;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

        InputMethodManager inputManager = (InputMethodManager)
                v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.toggleSoftInput(0, 0);
        return true;
    }
}

