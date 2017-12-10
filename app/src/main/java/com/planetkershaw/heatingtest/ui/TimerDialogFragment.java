package com.planetkershaw.heatingtest.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.planetkershaw.heatingtest.R;

// TUTORIAL FROM
// http://www.vogella.com/tutorials/AndroidDialogs/article.html

public class TimerDialogFragment extends DialogFragment implements TextView.OnEditorActionListener
{
    private EditText mEditText;
    private OnAddTimerListener callback;
    private int day;
    private int index;

    public interface OnAddTimerListener {
        public void onAddTimerSubmit(int day, int index, double temp);
    }

    // Empty constructor required for DialogFragment
    public TimerDialogFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timer, container);
        mEditText = (EditText) view.findViewById(R.id.timerHour);

        // set this instance as callback for editor action
        mEditText.setOnEditorActionListener(this);
        mEditText.requestFocus();
        Dialog dlg = getDialog();
        assert dlg != null;
        Window wnd = dlg.getWindow();
        assert wnd != null;
        wnd.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        getDialog().setTitle("Please enter new temperature:");

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        day = getArguments().getInt("day");
        index = getArguments().getInt("index");

        try {
            callback = (OnAddTimerListener) getTargetFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException("Calling Fragment must implement OnAddTimerListener");
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        Log.d("ROOMDETAILFRAGMENT", "finished dialog");

        String input = mEditText.getText().toString();
        double temp = Double.parseDouble(input);
        callback.onAddTimerSubmit (day,index,temp);
        this.dismiss();
        return true;
    }
}

