package com.planetkershaw.heatingtest.ui;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import com.planetkershaw.heatingtest.HeatingTestApp;
import com.planetkershaw.heatingtest.R;
import com.planetkershaw.heatingtest.restmethod.RestAPI;
import com.planetkershaw.heatingtest.zwayservice.RoomList;
import com.planetkershaw.heatingtest.zwayservice.Schedule;

public class EditEventActivity extends AppCompatActivity implements Handler.Callback{

    public static final String ARG_ITEM_ID = "item_id";
    public static final String ARG_DAY_ID = "day_id";
    public static final String ARG_TIMER_ID = "timer_id";
    private RoomList.RoomItem roomItem;
    private int roomId;
    private int dayId;
    private int timerId;
    private boolean newEvent;

    private RestAPI.RequestType reqType;


    private TextView startTimeTv, endTimeTv;
    private TextView dayTv;
    private TextView titleTv;
    private RadioGroup btnGroup;
    private RadioButton startBtn;
    private RadioButton endBtn;
    private TimePicker tp;
    private ImageView tempBck;
    private NumberPicker tempNp;

    private Button saveBtn;
    private Button cancelBtn;
    private Button deleteBtn;

    private boolean start = true;

    private Schedule scheduleCopy;

    private Schedule.TimerItem currentTimer;
    private double temperature;

    private View.OnClickListener timerBtnListener;
    private View.OnClickListener deleteBtnListener;
    private TimePicker.OnTimeChangedListener timerListener;

    private HeatingTestApp app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_event);

        app = (HeatingTestApp)getApplication();

        // extract details about the event we are editing
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            roomId = extras.getInt(ARG_ITEM_ID);
            dayId = extras.getInt(ARG_DAY_ID);
            timerId = extras.getInt(ARG_TIMER_ID);
        }
        else {
            roomId = 0;
            dayId = 0;
            timerId = 0;
        }
        roomItem = RoomList.get(roomId);

        newEvent = (timerId == -1);


        // make a copy of the received schedule
        // we use the copy to display schedule details which allows the user
        // to change them
        // user needs to press save to save in controller
        //TODO: use clone?
        scheduleCopy = new Schedule();
        int scheduleLength = roomItem.schedule.getSize();

        for (int j=0; j<scheduleLength; j++) {
            Schedule.TimerItem timer = roomItem.schedule.getTimer(j);
            scheduleCopy.addTimer (timer.day,timer.hour,timer.minute,timer.sp);
        }


        // we now have a copy of the schedule, either remove the one we are editing
        // or create a new timer
        if (!newEvent){
            int currentTimerIdx = getCurrentTimerIndex();
            Schedule.TimerItem copyTimer = scheduleCopy.getTimer (currentTimerIdx);
            currentTimer = new Schedule.TimerItem(copyTimer.day, copyTimer.hour, copyTimer.minute, copyTimer.sp);
            scheduleCopy.removeTimer (currentTimerIdx);
        }
        else {
            currentTimer = new Schedule.TimerItem(dayId, 0, 0, 4.0);
        }

        temperature = currentTimer.sp;

        // get the widgets reference from XML layout
        startTimeTv = (TextView) findViewById(R.id.edit_start_time);
        endTimeTv = (TextView) findViewById(R.id.edit_end_time);
        dayTv = (TextView) findViewById(R.id.edit_day);
        titleTv = (TextView) findViewById(R.id.edit_title);

        deleteBtn = (Button)findViewById(R.id.edit_delete);
        Drawable deleteIcon = deleteBtn.getBackground();
        deleteIcon.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        if (newEvent) {
            deleteBtn.setVisibility (View.INVISIBLE);
        }


        tp = (TimePicker) findViewById(R.id.edit_tp);
        // display the TimePicker current time
        tp.setCurrentHour(currentTimer.hour);
        tp.setCurrentMinute(currentTimer.minute);

        timerListener = new TimePicker.OnTimeChangedListener() {
            public void onTimeChanged(TimePicker view, int selectedHour, int selectedMinute) {

                // put the new values into the current timer
                currentTimer.hour = selectedHour;
                currentTimer.minute = selectedMinute;
                // and display them in the time slot
                updateTimeDisplay();
            }
        };

        tp.setOnTimeChangedListener(timerListener);
        // set the TimePicker view to 24 hour view
        tp.setIs24HourView(true);

        // TEMPERATURE SELECTOR - only visible for radiator type rooms
        tempNp = (NumberPicker) findViewById(R.id.edit_temp_picker);
        tempBck = (ImageView) findViewById(R.id.edit_temp_background);
        if (roomItem.type == RoomList.Type.RADIATOR) {
            final double MAX_TEMP = 28.0;
            final double MIN_TEMP = 4.0;
            int NUMBER_OF_VALUES = (int) (MAX_TEMP - MIN_TEMP) + 1;
            String[] displayedValues = new String[NUMBER_OF_VALUES];

            double value = MIN_TEMP;
            for (int i = 0; i < NUMBER_OF_VALUES; i++) {
                displayedValues[i] = "" + value + "°";
                value++;
            }

            tempNp.setMinValue(0);
            tempNp.setMaxValue(NUMBER_OF_VALUES - 1);
            tempNp.setDisplayedValues(displayedValues);
            tempNp.setValue((int) (temperature - MIN_TEMP));
            tempNp.setWrapSelectorWheel(false);

            tempNp.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                    temperature = (double) (newVal) + MIN_TEMP;
                    currentTimer.sp = temperature;
                }
            });
            tempNp.setVisibility(View.VISIBLE);
            tempBck.setVisibility(View.VISIBLE);
        }
        else {
            // TODO:
            // ON / OFF SELECTOR - only visible for ON/OFF type rooms
            tempNp.setVisibility(View.INVISIBLE);
            tempBck.setVisibility(View.INVISIBLE);
        }



        // save and cancel buttons
        saveBtn = (Button)findViewById(R.id.edit_save);
        cancelBtn = (Button)findViewById(R.id.edit_cancel);

        saveBtn.setOnClickListener (new View.OnClickListener() {
            public void onClick(View v) {
                updateSchedule();
            }
        });
        saveBtn.setTransformationMethod(null);

        cancelBtn.setOnClickListener (new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        cancelBtn.setTransformationMethod(null);

        // display information about room and day
        dayTv.setText(Schedule.TimerItem.daysOfTheWeek[dayId]);
        if (newEvent) {
            titleTv.setText(roomItem.title + ": new time slot");
        }
        else {
            titleTv.setText(roomItem.title + ": edit time slot");
        }
        // and the time slot
        updateTimeDisplay();
    }

    public boolean handleMessage (Message msg) {
        // TODO: errors need addressing
        switch (reqType) {
            case GET_ROOMS:
                app.restAPI.roomsResponse(msg.getData().getString("payload"));
                finish();
                break;
            default:
                app.restAPI.scheduleResponse(msg.getData().getString("payload"));
                Snackbar.make(titleTv, "Updated successfully", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                // TODO: force service instead
                reqType = RestAPI.RequestType.GET_ROOMS;
                app.restAPI.roomsRequest(new Handler(this));
                break;
        }


        return true;
    }

    // validate the values the user has entered
    // if they are ok, add the timer to the schedule, sort and update the zway server with the new values
    private void updateSchedule() {
        // before applying changes, validate the values
        if (!scheduleCopy.isDuplicate(currentTimer)) {
            // add the current timer to the schedule copy
            scheduleCopy.addTimer (currentTimer.day, currentTimer.hour, currentTimer.minute, currentTimer.sp);
            scheduleCopy.sort();
            reqType = RestAPI.RequestType.SET_SCHEDULE;
            app.restAPI.scheduleRequest(roomItem.id, scheduleCopy, new Handler(this));
        }
        else {
            Snackbar.make(titleTv, "There is already an event set for this time", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    private void updateTimeDisplay () {
        // and we would like to display the next timer
        Schedule.TimerItem nextTimer = null;
        nextTimer = scheduleCopy.getNextTimer(currentTimer);
        // display the timer interval
        startTimeTv.setText(currentTimer.timeToString());

        if (nextTimer.day != dayId)
            endTimeTv.setText(" - "+nextTimer.timeToString()+" ("+nextTimer.dayToString()+")");
        else
            endTimeTv.setText(" - "+nextTimer.timeToString());
    }

    private int getCurrentTimerIndex () {

        int idx = 0;
        for (int j=0; j<scheduleCopy.getSize(); j++)
        {
            Schedule.TimerItem copyTimer = scheduleCopy.getTimer(j);
            if (copyTimer.day == dayId) {
                if (idx == timerId) {
                    return j;
                }
                idx++;
            }
        }

        return -1;
    }
}
