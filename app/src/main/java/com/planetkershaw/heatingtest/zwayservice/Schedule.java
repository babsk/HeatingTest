package com.planetkershaw.heatingtest.zwayservice;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**************************************************************************

 An object of type Schedule is created for each room or light group that is
 returned in the zway API.

 Essentially a Schedule is a list of TimerItems and methods allowing for
 the addition, deletion, clearing, sorting, comparison and getting of timers.

 In addition it has a toJSON method for encoding requests to set schedules.

 The list of TimerItems should always be sorted after additions and deletions.

 **************************************************************************/
public class Schedule implements Cloneable
{
    // array of timer items
    private List<TimerItem> ITEMS = new ArrayList<>();

    // default constructor which just creates an empty list
    public Schedule () {
    }

    // this constructor is used when cloning a schedule
    protected Schedule (Schedule another) {
        for (int i=0; i<another.getSize(); i++) {
            TimerItem timer = another.getTimer(i);
            this.addTimer(timer.day, timer.hour, timer.minute, timer.sp);
        }
    }

    // empty the schedule
    public void clear ()
    {
        ITEMS.clear();
    }

    // sort the schedule from earliest to latest timer event
    public void sort () {
        Collections.sort(this.ITEMS);
    }

    // compare timer with ones in the list
    // if the list already contains a timer for the same day and time, return true
    // otherwise false
    public boolean isDuplicate (TimerItem timer) {
        for (int i=0; i<ITEMS.size(); i++) {
            if (ITEMS.get(i).compareTo(timer) == 0) {
                return true;
            }
        }

        return false;
    }

    // given a timer, find the one in the list that would be next if
    // the given timer was added to the list
    // assumes a sorted list
    public TimerItem getNextTimer (TimerItem timer) {
        int i;
        for (i=0; i<ITEMS.size(); i++)
        {
            if (ITEMS.get(i).compareTo(timer) > 0) {
                // first entry after the timer
                break;
            }
        }

        // i should now be set to the next value, however if we have wrapped,
        // then the very first entry is the one we want
        if (i >= ITEMS.size()) {
            i = 0;
        }

        return ITEMS.get(i);
    }

    // add a timer event to the list
    public TimerItem addTimer (int day, int hour, int minute, double sp) {
        TimerItem timer = new TimerItem (day, hour, minute, sp);
        ITEMS.add(timer);
        return timer;
    }

    // remove the timer with the given index
    public boolean removeTimer (int id) {
        ITEMS.remove(id);
        return true;
    }

    public TimerItem getTimer (int id)
    {
        return ITEMS.get (id);
    }

    public int getSize ()
    {
        return ITEMS.size();
    }

    @Override
    // TODO: not using clone yet, needs implementing properly
    public Object clone() {
        return new Schedule(this);
    }

    @Override
    public String toString()
    {
        return ITEMS.toString();
    }

    public String toJSON ()
    {
        try {
            // Here we convert Java Object to JSON
            JSONObject jsonObj = new JSONObject();
            JSONArray jsonArr = new JSONArray();
            for (int i=0; i<this.getSize(); i++) {
                JSONObject timerObj = new JSONObject();
                TimerItem timer = getTimer(i);
                timerObj.put("minute",timer.minute);
                timerObj.put("sp",timer.sp);
                timerObj.put("day",timer.day);
                timerObj.put("hour",timer.hour);
                jsonArr.put(timerObj);
            }

            jsonObj.put("data",jsonArr);

            return jsonObj.toString();

        }
        catch(JSONException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public int getNextEvent () {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK)-1;
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        int currTimeVal = day * 24 * 60 + hour * 60 + minute;

        int event;
        for (event=0; event<this.getSize(); event++) {
            // look for the next event past where we are
            int timeVal = this.getTimer(event).day * 24 * 60 + this.getTimer(event).hour * 60 + this.getTimer(event).minute;
            if (timeVal > currTimeVal)
                break;
        }

        // check for wrap, assign first event if reached end
        if (event == this.getSize()) {
            event = 0;
        }

        return event;
    }



    // timer item
    public static class TimerItem implements Comparable<TimerItem>{
        public int day;
        public int hour;
        public int minute;
        public double sp;
        static public String daysOfTheWeek[] = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        static public String days_short [] = {"S","M","T","W","T","F","S"};

        public TimerItem(int day, int hour, int minute, double sp) {
            this.day = day;
            this.hour = hour;
            this.minute = minute;
            this.sp = sp;
        }

        @Override
        // used for sorting, return values as follows:
        // -ve integer - timer argument is before this timer
        // +ve integer - timer argument is after this timer
        // 0 - timer argument is the same as this timer
        public int compareTo(TimerItem timer) {
            if (this.day != timer.day) {
                return this.day - timer.day;
            }
            else if (this.hour != timer.hour) {
                return this.hour - timer.hour;
            }
            else {
                return this.minute - timer.minute;
            }
        }

        @Override
        public String toString() {
            return "day: " + day + " time: " + hour + "." + minute + " sp: " + sp;
        }

        public String dayToString () {
            return daysOfTheWeek [this.day];
        }

        public String timeToString() {
            String hourStr;
            String minStr;
            if (this.hour < 10) hourStr = "0" + this.hour;
            else hourStr = "" + this.hour;
            if (this.minute < 10) minStr = "0" + this.minute;
            else minStr = "" + this.minute;

            return hourStr + ":" + minStr;
        }

        public int timeToInt() {
            return this.hour * 60 + this.minute;
        }
    }

}
