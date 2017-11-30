package com.planetkershaw.heatingtest.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.planetkershaw.heatingtest.HeatingTestApp;
import com.planetkershaw.heatingtest.R;
import com.planetkershaw.heatingtest.utils.MultiSeekBar;
import com.planetkershaw.heatingtest.zwayservice.RoomList;
import com.planetkershaw.heatingtest.zwayservice.Schedule;

import java.util.ArrayList;

public class ScheduleActivity extends AppCompatActivity implements TimerDialogFragment.OnAddTimerListener, Handler.Callback{

    public static final String ARG_ITEM_ID = "item_id";
    private RoomList.RoomItem mItem;
    private int roomId;

    private Schedule scheduleCopy;

    private HeatingTestApp app;


    private static final int SCHEDULE_ROW_ID = 2000;

    private Button btnSubmit;

    private TableLayout scheduleTable;//, timerTable;
    private TableRow scheduleLabels;
    private RelativeLayout.LayoutParams timerLP;
    private TextView timerView;

    private int colorBlue, colorGreen, colorOrange, colorYellow, colorRed;

    private ArrayList<MultiSeekBar<Integer>> scheduleDisplay = new ArrayList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        app = (HeatingTestApp)getApplication();

        colorBlue = ContextCompat.getColor(this, R.color.tempBlue);
        colorYellow = ContextCompat.getColor(this,R.color.tempYellow);
        colorGreen = ContextCompat.getColor(this,R.color.tempGreen);
        colorOrange = ContextCompat.getColor(this,R.color.tempOrange);
        colorRed = ContextCompat.getColor(this,R.color.tempRed);


        //TODO: need to pass this in via bundle
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            roomId = extras.getInt(ARG_ITEM_ID);
        }
        else {
            roomId = 0;
        }
        mItem = RoomList.get(roomId);

        // make a copy of the received schedule
        // we use the copy to display schedule details which allows the user
        // to change them
        // user needs to press update to save in controller
        //TODO: use clone?
        scheduleCopy = new Schedule();
        int scheduleLength = mItem.schedule.getSize();
        for (int j=0; j<scheduleLength; j++)
        {
            Schedule.TimerItem timer = mItem.schedule.getTimer(j);
            scheduleCopy.addTimer (timer.day,timer.hour,timer.minute,timer.sp);
        }


        btnSubmit = (Button)findViewById(R.id.updateTimers);
        btnSubmit.setOnClickListener(new View.OnClickListener () {
                                         public void onClick(View v) {
                                             int id = v.getId();
                                             switch (id) {
                                                 case R.id.updateTimers:
                                                     updateSchedule();
                                                     break;
                                             }
                                         }
                                     }
        );

        createContent();

        switch (mItem.mode) {
            case TIMER:
                btnSubmit.setVisibility(View.VISIBLE);
                scheduleTable.setVisibility(View.VISIBLE);
                break;
            default:
                btnSubmit.setVisibility(View.INVISIBLE);
                scheduleTable.setVisibility(View.INVISIBLE);
                break;
        }

    }

    private void createContent () {
        TableLayout.LayoutParams lp =
                new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.MATCH_PARENT,
                        TableLayout.LayoutParams.WRAP_CONTENT);

        lp.setMargins(10, 0, 10, 2);

        scheduleTable = (TableLayout) findViewById(R.id.schedule);

        LayoutInflater inflater = (LayoutInflater)getApplicationContext().getSystemService(this.LAYOUT_INFLATER_SERVICE);
        TableRow scheduleLabels = (TableRow)inflater.inflate(R.layout.schedule_day, null);
        scheduleLabels.setLayoutParams(lp);
        LinearLayout labels = (LinearLayout)scheduleLabels.findViewById(R.id.seekbar_placeholder);

        // create the hour labels (2 hour slots) across the top
        int barWidth = MultiSeekBar.getSeekbarWidth();
        for (int hour=0; hour<=24; hour+=2) {
            TextView textView = new TextView(this);
            LinearLayout.LayoutParams lpa = new LinearLayout.LayoutParams(barWidth/24*2,40);
            if (hour < 10) textView.setText("0"+hour);
            else textView.setText(""+hour);
            textView.setTextAppearance(this,android.R.style.TextAppearance_Medium);
            lpa.leftMargin = 0;
            lpa.topMargin = 0;
            lpa.bottomMargin = 0;
            lpa.rightMargin = 0;
            textView.setLayoutParams(lpa);
            labels.addView(textView);
        }
        scheduleTable.addView(scheduleLabels);

        // setup a range seek bar for each day
        for (int day=0; day<7; day++) {
            TableRow scheduleRow = (TableRow)inflater.inflate(R.layout.schedule_day, null);
            scheduleRow.setLayoutParams(lp);

            scheduleRow.setId(SCHEDULE_ROW_ID + day);
            ((TextView)scheduleRow.findViewById(R.id.label)).setText(Schedule.TimerItem.days_short[day]);

            final MultiSeekBar<Integer> daySeekBar = new MultiSeekBar<Integer>(this, day);

            setupSeekBar(daySeekBar);

            // add to this room's schedule
            scheduleDisplay.add (day,daySeekBar);

            // Add to layout
            LinearLayout layout = (LinearLayout) scheduleRow.findViewById(R.id.seekbar_placeholder);
            layout.addView(daySeekBar);

            scheduleTable.addView(scheduleRow);
        }

        // setup the moving timer label
        timerView = (TextView) findViewById(R.id.timer);
        timerView.setBackgroundResource(R.drawable.time_label);
        timerLP = new RelativeLayout.LayoutParams(80, 40);
        timerLP.leftMargin = 40;
        timerLP.topMargin = 50;
        timerLP.bottomMargin = -250;
        timerLP.rightMargin = -250;
        timerView.setLayoutParams(timerLP);
        timerView.setVisibility(View.INVISIBLE);

        // use the details of this room to fill in the schedule
        if (mItem != null) {
            ((TextView) findViewById(R.id.roomtitle)).setText("" + mItem.title + " Schedule");

            // TODO: do this differently for HOT WATER??

            // the very first color is in fact the last color for this room
            int prevColor = Color.BLUE;
            if (scheduleCopy.getSize()>0) {
                Schedule.TimerItem tim = scheduleCopy.getTimer(scheduleCopy.getSize() - 1);
                prevColor = convertTempToColor(tim.sp);
            }

            for (int i=0; i<scheduleCopy.getSize(); i++) {
                Schedule.TimerItem timer = scheduleCopy.getTimer(i);
                int day = timer.day;
                int hour = timer.hour;
                int minute = timer.minute;
                double temp = timer.sp;

                int index = scheduleDisplay.get(day).addMarker();
                int currentColor = convertTempToColor (temp);

                if (index == 0) scheduleDisplay.get(day).setStartColor(prevColor);
                scheduleDisplay.get(day).setTimerColors(index, prevColor, currentColor);
                prevColor = currentColor;
                scheduleDisplay.get(day).setSelectedValue(index, hour * 4 + minute / 15);
                if (timer.sp == 0.0)
                    scheduleDisplay.get(day).setLabel(index,"OFF");
                else if (timer.sp == 1.0)
                    scheduleDisplay.get(day).setLabel(index,"ON");
                else
                    scheduleDisplay.get(day).setLabel(index,""+(int)timer.sp);
            }
        }
    }

    private void setupSeekBar (final MultiSeekBar<Integer> rangeSeekBar) {
        // Set the range
        rangeSeekBar.setRangeValues(0, 96);
        rangeSeekBar.setStartColor(Color.BLUE);

        rangeSeekBar.setNotifyWhileDragging(true);

        rangeSeekBar.setOnRangeSeekBarChangeListener(new MultiSeekBar.OnRangeSeekBarChangeListener<Integer>() {
            @Override
            public void onRangeSeekBarValuesChanged(MultiSeekBar<?> bar, int index, int id, float x) {
                if (index != -1) {
                    int selectedVal = rangeSeekBar.getSelectedValue(index);
                    updateScheduleCopy (id,index,selectedVal);
                    timerView.setVisibility(View.VISIBLE);
                    updateTimer(selectedVal, x);
                }
            }
            @Override
            public void onRangeSeekBarStop () {
                timerView.setVisibility(View.INVISIBLE);
            }
            @Override
            public void onRangeSeekBarPressed (int id, int index) {
                Log.d("test", "seek bar pressed " + id + "  " + index);

                if (index == -1) {
                    // the section of seekbar was not claimed, so it must belong
                    // to the previous seekbar (or the last seekbar)
                    if (id == 0) id = 6;
                    else id--;

                    // and the index is of the last thumb in that seekbar
                    index = scheduleDisplay.get(id).numTimers()-1;
                }

                // close existing dialog fragments
 //               FragmentManager manager = getFragmentManager();

                // setup the moving timer label
                timerView = (TextView)findViewById(R.id.timer);
                timerView.setBackgroundResource(R.drawable.time_label);
                timerLP = new RelativeLayout.LayoutParams(80, 40);
                timerLP.leftMargin = 40;
                timerLP.topMargin = 50;
                timerLP.bottomMargin = -250;
                timerLP.rightMargin = -250;
                timerView.setLayoutParams(timerLP);
                timerView.setVisibility(View.INVISIBLE);

                // get the current fragment
 //               Fragment frag = manager.findFragmentByTag("room_detail_fragment");

 /*               // bring up a dialog to enter a temperature
                TimerDialogFragment timerDialog = new TimerDialogFragment();
                Bundle args = new Bundle();
                args.putInt("day", id);
                args.putInt("index",index);
                timerDialog.setArguments(args);
                timerDialog.setTargetFragment(frag, 0);
                timerDialog.show(manager, "fragment_edit_hour");*/
            }
        });

    }

    private void updateScheduleCopy (int day, int index, int timeVal) {
        int idx=0;
        for (int i=0; i<scheduleCopy.getSize(); i++) {
            if (scheduleCopy.getTimer(i).day == day) {
                if (index == idx) {
                    // copy new time
                    Schedule.TimerItem timer = scheduleCopy.getTimer(i);
                    timer.hour = timeVal*15/60;
                    timer.minute = timeVal*15%60;
                    break;
                }
                else {
                    idx++;
                }
            }
        }
    }

    private void updateScheduleCopy (int day, int index, double temp) {
        int idx=0;
        for (int i=0; i<scheduleCopy.getSize(); i++) {
            if (scheduleCopy.getTimer(i).day == day) {
                if (index == idx) {
                    // copy new temp
                    Schedule.TimerItem timer = scheduleCopy.getTimer(i);
                    timer.sp = temp;
                    break;
                }
                else {
                    idx++;
                }
            }
        }
    }



    private void updateTimer (int val, float x) {
        int hour = val*15/60;
        int minute = val*15%60;
        timerView.setText("" + hour + ":" + minute);
        timerLP.leftMargin = (int)x;
        timerView.setLayoutParams(timerLP);
    }

    public int convertTempToColor (double temp) {
        if      (temp < 10) return colorBlue;
        else if (temp < 15) return colorGreen;
        else if (temp < 20) return colorYellow;
        else if (temp < 25) return colorOrange;
        else                return colorRed;
    }


    public void clearContent () {
        scheduleTable.removeAllViewsInLayout();
        scheduleDisplay.clear();
        createContent();
    }

    public boolean handleMessage (Message msg) {
        app.restAPI.scheduleResponse(msg.getData().getString("payload"));
        clearContent();

        return true;
    }

    public void updateSchedule() {
        app.restAPI.scheduleRequest(mItem.id, scheduleCopy, new Handler(this));
    }

    @Override
    public void onAddTimerSubmit(int day, int index, double temp) {
        // update the schedule copy
        //TODO: implement a countdown timer to save - for now use update button
        updateScheduleCopy(day, index, temp);

        // force a redraw of the whole table
        clearContent();
    }

}
