package com.planetkershaw.heatingtest.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.planetkershaw.heatingtest.HeatingTestApp;
import com.planetkershaw.heatingtest.R;
import com.planetkershaw.heatingtest.restmethod.RestAPI;
import com.planetkershaw.heatingtest.utils.MultiSeekBar;
import com.planetkershaw.heatingtest.zwayservice.DataChangedReceiver;
import com.planetkershaw.heatingtest.zwayservice.RoomList;
import com.planetkershaw.heatingtest.zwayservice.Schedule;
import com.planetkershaw.heatingtest.zwayservice.TimeService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

/**********************************************************************************
 *
 * A fragment representing a single Room detail screen.
 * This fragment is either contained in a {@link RoomListActivity}
 * in two-pane mode (on tablets in landscape) or a {@link RoomDetailActivity}
 * on handsets.
 *
 **********************************************************************************/
public class RoomDetailFragment extends Fragment implements Handler.Callback, TempDialogFragment.TempDialogListener {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";
    private int roomId;

    private HeatingTestApp app;

    private LinearLayout boostLayout;
    private LinearLayout pumpLayout;
    private RadioGroup scheduleDays;
    private ListView listview;
    private TextView currentText, tempText, boostTemp, boostTime, pumpStatus, pumpTime,titleText;
    Button pumpDurationBtn, boostDurationBtn, boostTempBtn, scheduleBtn;
    private RadioGroup modeSelector;
    private String modes [] = {"off","schedule","boost"};
    private View.OnClickListener modeBtnListener;
    private Button onoffBtn, timerBtn, boostBtn;
    private Button modeBtn [];
    private int modeId [] = {R.id.modeOnOff, R.id.modeTimer, R.id.modeBoost};

    private RestAPI.RequestType reqType;
    private View.OnClickListener dayBtnListener;
    private View.OnClickListener addTimerBtnListener;
    private Button monBtn, tueBtn, wedBtn, thuBtn, friBtn, satBtn, sunBtn;
    private Button addTimerBtn;
    private Switch pumpBtn;
    private int dayId [] = {R.id.sun,R.id.mon, R.id.tue, R.id.wed, R.id.thu, R.id.fri, R.id.sat};
    private Button dayBtn [];
    private ArrayList<MyTimerObject> timers;
    private MyTimerObjectArrayAdapter timerAdapter;
    private CompoundButton.OnCheckedChangeListener pumpBtnChangeListener;
    private Dialog mOverlayDialog;

    private Handler.Callback callback;

    private DataChangedReceiver mReceiver;


    private View rootView;
    private static final int DIALOG_BOOST_TEMP=0;
    private static final int DIALOG_BOOST_DURATION=1;

    private boolean mTwoPane;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public RoomDetailFragment()
    {
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        app = (HeatingTestApp)(getActivity().getApplication());
        callback = this;

        if (getArguments().containsKey(ARG_ITEM_ID))
        {
            roomId = getArguments().getInt(ARG_ITEM_ID);
        }

        mTwoPane = (getActivity().findViewById(R.id.room_detail_container) != null);

        // register for changes to the data set
        mReceiver = new DataChangedReceiver() {
            public void onReceive(Context context, Intent intent) {
                updateRoomSummary(roomId);
            }

        };

    }

    @Override
    public void onResume()
    {
        super.onResume();
        getActivity().registerReceiver(mReceiver, new IntentFilter(DataChangedReceiver.ACTION_HEATING_DATA_CHANGED));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver (mReceiver);
    }

    @Override
    public void onStart()
    {
        super.onStart();
        // we already have the information we need, so update everything
        updateRoomSummary(roomId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        rootView = inflater.inflate(R.layout.room_container, container, false);

        // these fields are present for all modes
        titleText = (TextView) rootView.findViewById(R.id.roomname);
        currentText = (TextView)rootView.findViewById(R.id.currenttemp);
        tempText = (TextView)rootView.findViewById(R.id.roomtemp);

        // mode buttons
        modeBtnListener = new View.OnClickListener() {
            public void onClick(View v) {
                int index = getModeIndex(v.getId());
                RoomList.Mode mode = RoomList.Mode.values()[index + 1];
                RoomList.RoomItem room = RoomList.get(roomId);
                if (mode != room.mode) {
                    Log.d("ROOMDETAILFRAGMENT", "mode change for " + room.title);
                    reqType = RestAPI.RequestType.SET_MODE;
                    app.restAPI.modeRequest(new Handler(callback), room.id, mode.ordinal());
                }
            }
        };
        onoffBtn = (Button) rootView.findViewById(R.id.modeOnOff);
        timerBtn = (Button) rootView.findViewById(R.id.modeTimer);
        boostBtn = (Button) rootView.findViewById(R.id.modeBoost);
        modeBtn = new Button[]{onoffBtn, timerBtn, boostBtn};
        for (int i = 0; i < modeBtn.length; i++) {
            modeBtn[i].setOnClickListener(modeBtnListener);
        }
        modeSelector = (RadioGroup) rootView.findViewById(R.id.modeSelector);

        // each of these layouts relate to the mode that the room is currently in
        // a mode layout is only visible if the mode is active

        // hide all mode related sections initially
        boostLayout = (LinearLayout) rootView.findViewById(R.id.boostDetails);
        boostLayout.setVisibility(View.INVISIBLE);
        scheduleDays = (RadioGroup) rootView.findViewById(R.id.scheduleDays);
        scheduleDays.setVisibility(View.INVISIBLE);
        pumpLayout = (LinearLayout) rootView.findViewById(R.id.hotWaterPump);
        pumpLayout.setVisibility(View.INVISIBLE);

        // BOOST MODE layout
        boostTemp = ((TextView) rootView.findViewById(R.id.boosttemp));
        boostTime = ((TextView) rootView.findViewById(R.id.boosttime));
        boostTempBtn = (Button) rootView.findViewById(R.id.boostTempBtn);
        boostDurationBtn = (Button) rootView.findViewById(R.id.boostTimeBtn);
        boostTempBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    showBoostTempDialog();
                }
            });
        boostDurationBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    showBoostDurationDialog();
                }
            });

        // PUMP MODE layout
        pumpDurationBtn = (Button) rootView.findViewById(R.id.pumpTimeBtn);
        pumpStatus = ((TextView) rootView.findViewById(R.id.pumpStatus));
        pumpTime = ((TextView) rootView.findViewById(R.id.pumptime));
        pumpBtnChangeListener = new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Log.v("Switch State=", "" + isChecked);
                    // send a request to turn the hot water pump on/off
                    reqType = RestAPI.RequestType.SET_PUMP;
                    app.restAPI.pumpStatusRequest(new Handler(callback), roomId, isChecked);

                    // update the status and hide / show the pump related fields
                    pumpStatus.setText("Hot Water Pump is " + (isChecked ? "ON" : "OFF"));
                    pumpDurationBtn.setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE);
                    pumpTime.setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE);
                }

            };
        pumpBtn = ((Switch) rootView.findViewById(R.id.pumpButton));
        pumpBtn.setOnCheckedChangeListener(pumpBtnChangeListener);

        // SCHEDULE layout
        // day buttons across top of schedule
        dayBtnListener = new View.OnClickListener() {
                public void onClick(View v) {
                    updateSchedule(v.getId());
                }
        };
        monBtn = (Button) rootView.findViewById(R.id.mon);
        tueBtn = (Button) rootView.findViewById(R.id.tue);
        wedBtn = (Button) rootView.findViewById(R.id.wed);
        thuBtn = (Button) rootView.findViewById(R.id.thu);
        friBtn = (Button) rootView.findViewById(R.id.fri);
        satBtn = (Button) rootView.findViewById(R.id.sat);
        sunBtn = (Button) rootView.findViewById(R.id.sun);
        dayBtn = new Button[]{sunBtn, monBtn, tueBtn, wedBtn, thuBtn, friBtn, satBtn};
        for (int i = 0; i < dayBtn.length; i++) {
            dayBtn[i].setOnClickListener(dayBtnListener);
        }

        // button for adding an event to the schedule
        addTimerBtn = (Button) rootView.findViewById(R.id.addTimerBtn);
        addTimerBtnListener = new View.OnClickListener() {
            public void onClick(View v) {
                Context context = v.getContext();
                Intent intent = new Intent(context, EditEventActivity.class);
                intent.putExtra(EditEventActivity.ARG_ITEM_ID, roomId);
                int day = getDayIndex(scheduleDays.getCheckedRadioButtonId());
                intent.putExtra(EditEventActivity.ARG_DAY_ID, day);
                intent.putExtra(EditEventActivity.ARG_TIMER_ID, -1);
                startActivity(intent);
            }
        };
        addTimerBtn.setOnClickListener (addTimerBtnListener);

        // schedule for a single day - list of timer intervals
        listview = (ListView) rootView.findViewById(R.id.listview);
        listview.setVisibility(View.INVISIBLE);
        MyTimerObject[] emptyTimers = new MyTimerObject[]{};
        timers = new ArrayList<MyTimerObject>(Arrays.asList(emptyTimers));
        timerAdapter = new MyTimerObjectArrayAdapter(app, timers);
        listview.setAdapter(timerAdapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    MyTimerObject entry = (MyTimerObject) parent.getAdapter().getItem(position);
                    Context context = view.getContext();
                    Intent intent = new Intent(context, EditEventActivity.class);
                    intent.putExtra(EditEventActivity.ARG_ITEM_ID, roomId);
                    int day = getDayIndex(scheduleDays.getCheckedRadioButtonId());
                    intent.putExtra(EditEventActivity.ARG_DAY_ID, day);
                    intent.putExtra(EditEventActivity.ARG_TIMER_ID, position);
                    startActivity(intent);

                }
            });

        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        scheduleDays.check(dayBtn[day].getId());

        // schedule button - only available for tablets
        // clicking button opens up a graphical schedule editor
        scheduleBtn = (Button) rootView.findViewById(R.id.scheduleBtn);
        if (app.mTwoPane) {
            scheduleBtn.setOnClickListener (new View.OnClickListener() {
                public void onClick(View v) {
                    Context context = v.getContext();
                    Intent intent = new Intent(context, ScheduleActivity.class);
                    intent.putExtra(ScheduleActivity.ARG_ITEM_ID, roomId);
                    context.startActivity(intent);
                }
            });
        }
        else {
            scheduleBtn.setVisibility(View.INVISIBLE);
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        app = ((HeatingTestApp) getActivity().getApplication());
    }

    public void changeRoomID (int id) {
        roomId = id;
        updateRoomSummary (roomId);
    }

    private void updateRoomSummary (int id) {
        RoomList.RoomItem room = RoomList.get(id);

        titleText.setText(RoomList.get(id).title);

        if (room.type == RoomList.Type.ONOFF) {
            tempText.setText(room.targetTemp == 0 ? "OFF" : "ON");
            tempText.setTextColor(room.targetTemp == 0 ? app.getResources().getColor(R.color.black) : app.getResources().getColor(R.color.red));
        }
        else if (room.hasTempSensor) {
            currentText.setText(""+app.df.format(room.currentTemp));
            currentText.setTextColor(app.convertTempToColorResourceID(room.currentTemp));
            tempText.setText(" (set " + app.df.format(room.targetTemp) + "°C)");
        }
        else {
            currentText.setText("");
            tempText.setText("(set " + app.df.format(room.targetTemp) + "°C)");
        }

        // TODO: setup the mode radio group here
        if (room.mode == RoomList.Mode.OFF) {
            modeSelector.check(modeBtn[0].getId());
        }
        else if (room.mode == RoomList.Mode.TIMER) {
            modeSelector.check(modeBtn[1].getId());
        }
        else {
            modeSelector.check(modeBtn[2].getId());
        }

        // BOOST DETAILS
        boostTime.setText("Boost time remaining: " + room.boostTime + " minutes");
        boostTemp.setText("Boost temperature: " + room.boostTemp + " °C");
        boostLayout.setVisibility((room.mode == RoomList.Mode.BOOST) ? View.VISIBLE : View.INVISIBLE);

        // HOT WATER PUMP STATUS
        pumpStatus.setText("Hot Water Pump is " + (room.pumpStatus ? "ON" : "OFF"));
        pumpTime.setText("Pump time remaining: " + room.pumpTime + " minutes");
        // this hack is required to stop programmatically generated sets from
        // triggering an on/off request
        pumpBtn.setOnCheckedChangeListener(null);
        pumpBtn.setChecked(room.pumpStatus);
        pumpBtn.setOnCheckedChangeListener(pumpBtnChangeListener);

        if (room.type == RoomList.Type.ONOFF) {
            pumpLayout.setVisibility(View.VISIBLE);
            pumpDurationBtn.setVisibility(room.pumpStatus ? View.VISIBLE : View.INVISIBLE);
            pumpTime.setVisibility(room.pumpStatus ? View.VISIBLE : View.INVISIBLE);
        }
        else {
            pumpLayout.setVisibility(View.INVISIBLE);
        }

        // SCHEDULE DETAILS
        // check which day button is selected
        // and force the correct schedule to display
        int btnid = scheduleDays.getCheckedRadioButtonId();
        int day = -1;
        for (int i=0; i<dayId.length; i++) {
            if (dayId[i] == btnid) {
                day = i;
                break;
            }
        }
        dayBtn[day].performClick();
        scheduleDays.setVisibility((room.mode == RoomList.Mode.TIMER) ? View.VISIBLE : View.INVISIBLE);
        listview.setVisibility((room.mode == RoomList.Mode.TIMER) ? View.VISIBLE : View.INVISIBLE);
    }

    private int getModeIndex (int viewId) {
        int mode = -1;
        for (int i=0; i<modeId.length; i++) {
            if (modeId[i] == viewId) {
                mode = i;
                break;
            }
        }
        return mode;
    }

    private int getDayIndex (int viewId) {
        int day = -1;
        for (int i=0; i<dayId.length; i++) {
            if (dayId[i] == viewId) {
                day = i;
                break;
            }
        }
        return day;
    }

    private void updateSchedule (int id) {

        int day = getDayIndex(id);
        RoomList.RoomItem room = RoomList.get(roomId);
        Schedule schedule = room.schedule;

        timers.clear();

        for (int i=0; i<schedule.getSize(); i++) {
            Schedule.TimerItem t = schedule.getTimer(i);
            if (t.day == day) {
                Schedule.TimerItem next;
                String extra = "";
                if (i == schedule.getSize()-1) next = schedule.getTimer(0);
                else next = schedule.getTimer(i+1);
                if (next.day != day) extra = " ("+next.dayToString()+")";
                timers.add (new MyTimerObject (t.sp,t.timeToString(),next.timeToString()+extra));
            }

        }

        // replace the values in the adapter
        timerAdapter.notifyDataSetChanged();
    }

    public class MyTimerObject {
        double setPoint;
        String startTime;
        String endTime;

        public MyTimerObject (double sp, String start, String end) {
            this.setPoint = sp;
            this.startTime = new String(start);
            this.endTime = new String(end);
        }
    }

    public class MyTimerObjectArrayAdapter extends ArrayAdapter<MyTimerObject> {
        private final Context context;
        private ArrayList<MyTimerObject> values;

        public MyTimerObjectArrayAdapter(Context context, ArrayList<MyTimerObject> values) {
            super(context, -1, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.timerrow, parent, false);
            TextView setPoint = (TextView) rowView.findViewById(R.id.setpoint);
            TextView startTime = (TextView) rowView.findViewById(R.id.start);
            TextView endTime = (TextView) rowView.findViewById(R.id.end);
            Button delBtn = (Button) rowView.findViewById(R.id.timer_delete);
            delBtn.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
            delBtn.setTag(position);

            delBtn.setOnClickListener (new View.OnClickListener() {
                public void onClick(View v) {
                    removeTimer(v);
                }
            });

            RoomList.RoomItem room = RoomList.get(roomId);
            int color;
            String text;
            if (room.type == RoomList.Type.ONOFF) {
                if ((int)values.get(position).setPoint == 0) {
                    text = "off";
                    color = R.color.black;
                }
                else {
                    text = "on";
                    color = R.color.red;
                }
            }
            else {
                int sp = (int) values.get(position).setPoint;
                text = "" +  sp + "°";
                color = app.convertTempToColorResourceID(sp);
            }


            Drawable drawable = getResources().getDrawable(R.drawable.temp_circle_white);
            drawable.mutate().setColorFilter(getResources().getColor(color), PorterDuff.Mode.SRC_IN);
            setPoint.setText(text);
            setPoint.setTextColor(getResources().getColor(color));
            setPoint.setBackgroundDrawable(drawable);
            startTime.setText(values.get(position).startTime);
            endTime.setText(" - " + values.get(position).endTime);

            return rowView;
        }

    }


    private void showBoostTempDialog() {
        Bundle bundle = new Bundle();
        bundle.putString("DIALOG_TEXT", "Boost Temperature");
        bundle.putInt("DIALOG_ID", DIALOG_BOOST_TEMP);
        FragmentManager fm = getFragmentManager();
        TempDialogFragment editNameDialog = new TempDialogFragment();
        editNameDialog.setTargetFragment (this,1);
        editNameDialog.setArguments(bundle);
        editNameDialog.show(fm, "fragment_edit_name");
    }

    private void showBoostDurationDialog() {
        Bundle bundle = new Bundle();
        bundle.putString("DIALOG_TEXT","Boost Duration");
        bundle.putInt("DIALOG_ID", DIALOG_BOOST_DURATION);
        FragmentManager fm = getFragmentManager();
        TempDialogFragment editNameDialog = new TempDialogFragment();
        editNameDialog.setTargetFragment(this, 1);
        editNameDialog.setArguments(bundle);
        editNameDialog.show(fm, "fragment_edit_name");
    }

 /*   // custom adapter to allow displaying of images and text in the mode spinner
    public class ModeAdapter extends BaseAdapter {
        Context context;
        int icons[];
        String[] modes;
        LayoutInflater inflter;

        public ModeAdapter(Context applicationContext, int[] icons, String[] modes) {
            this.context = applicationContext;
            this.icons = icons;
            this.modes = modes;
            inflter = (LayoutInflater.from(applicationContext));
        }

        @Override
        public int getCount() {
            return icons.length;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            view = inflter.inflate(R.layout.modespinner, null);
            ImageView icon = (ImageView) view.findViewById(R.id.modeicon);
            icon.setImageResource(icons[i]);
            return view;
        }
    }*/


    // The dialog fragment receives a reference to this fragment via
    // getTargetFrame() and calls this function
    // defined by the TempDialogFragment.TempDialogListener interface
    @Override
    public void onTempDialogSave(int id, String value) {
        switch (id) {
            case DIALOG_BOOST_TEMP:
                double temp = Double.parseDouble(value);
                Log.d("ROOM", "boost temp change for " + roomId + " of " + temp);
                reqType = RestAPI.RequestType.SET_BOOST_TEMP;
                app.restAPI.boostTempRequest(new Handler(callback), roomId, temp);
                break;
            case DIALOG_BOOST_DURATION:
                int duration = Integer.parseInt(value);
                Log.d("ROOM", "boost duration change for " + roomId + " of " + duration);
                reqType = RestAPI.RequestType.SET_BOOST_DURATION;;
                app.restAPI.boostDurationRequest(new Handler(callback), roomId, duration);
                break;
        }
    }


    public boolean handleMessage (Message msg) {
        //TODO: need to handle errors
        showProgress (false);

        switch (reqType) {
            default:

                // we arrive here after a set request, if we force an update
                // of the main data everything should appear up to date

                // NOTE: because of a bug in the zway server, we need to delay
                // the request to give time for the new value to be accepted

                Runnable r = new Runnable() {
                    @Override
                    public void run(){
                        // force an update of the main data
                        TimeService timeService = TimeService.getInstance();
                        if (timeService != null) {
                            timeService.forceRefresh();
                        }
                    }
                };
                Handler h = new Handler();
                h.postDelayed(r, 1000); // <-- the "1000" is the delay time in miliseconds.

                break;
        }

        return true;
    }

    private void showProgress(final boolean show) {
        if (show) {
            mOverlayDialog = new Dialog(getActivity(), android.R.style.Theme_Panel); //display an invisible overlay dialog to prevent user interaction and pressing back
            mOverlayDialog.setCancelable(false);
            mOverlayDialog.show();
        }
        else if (mOverlayDialog != null) {
            mOverlayDialog.cancel();
            mOverlayDialog = null;
        }
    }

    private void removeTimer (View v) {

        Context context = v.getContext();
        int timerId = (int)v.getTag();

        int day = getDayIndex(scheduleDays.getCheckedRadioButtonId());

        Schedule scheduleCopy;
        int scheduleEntryIdx = -1;

        RoomList.RoomItem room = RoomList.get(roomId);

        // create a copy of the schedule
        // make a copy of the received schedule
        // we use the copy to display schedule details which allows the user
        // to change them
        // user needs to press save to save in controller
        //TODO: use clone?
        scheduleCopy = new Schedule();
        int scheduleLength = room.schedule.getSize();
        int idx = 0;
        for (int j=0; j<scheduleLength; j++)
        {
            Schedule.TimerItem timer = room.schedule.getTimer(j);
            scheduleCopy.addTimer (timer.day,timer.hour,timer.minute,timer.sp);
            if (timer.day == day) {
                if (idx == timerId) {
                    scheduleEntryIdx = j;
                }
                idx++;
            }
        }

        if (idx > 1) {

            // remove this timer from the schedule
            scheduleCopy.removeTimer(scheduleEntryIdx);

            // send the new schedule to the Zway server
            reqType = RestAPI.RequestType.SET_SCHEDULE;
            app.restAPI.scheduleRequest(roomId, scheduleCopy, new Handler(this));
        }
        else {
            Snackbar.make(v, "There must be at least one timer per day", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }


    }

}
