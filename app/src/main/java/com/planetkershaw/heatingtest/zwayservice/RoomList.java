package com.planetkershaw.heatingtest.zwayservice;

import com.planetkershaw.heatingtest.zwayservice.Schedule;

import java.util.ArrayList;
import java.util.List;

public class RoomList
{
    // array of room items
    public static final List<RoomItem> ITEMS = new ArrayList<RoomItem>();

    // map of room items by id
    // TODO: research hash mapping
//    public static final Map<String, RoomItem> ITEM_MAP = new HashMap<String, RoomItem>();

    public static final void clear ()
    {
        ITEMS.clear();
    }

    public static final void addRoom (int id, String title, double desired, double current, double external,
                                      boolean hasTempSensor, Type type, Mode base, Mode mode, Schedule schedule,
                                      boolean callForHeat, String location, double boostTemp, int boostDuration, int boostTime,
                                      boolean pumpStatus, int pumpDuration, int pumpTime)
    {
        RoomItem room = new RoomItem (id, title, desired, current, external, hasTempSensor, type, base, mode, schedule,
                callForHeat, location, boostTemp, boostDuration, boostTime, pumpStatus, pumpDuration, pumpTime);
        ITEMS.add (room);
    }

    public static final RoomItem get (int id) {
        for (int i=0; i<ITEMS.size(); i++) {
            if (ITEMS.get(i).id == id)
                return ITEMS.get(i);
        }

        return null;
    }

    private static String makeDetails(int position)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Details about Item: ").append(position);
        for (int i = 0; i < position; i++)
        {
            builder.append("\nMore details information here.");
        }
        return builder.toString();
    }

    public enum Mode {
        DUMMY, OFF, TIMER, BOOST
    }
    public enum Type {
        DUMMY, ONOFF, RADIATOR
    }

    // room item
    public static class RoomItem
    {
        public final int id;
        public final Type type;
        public final Mode mode;
        public final Mode baseMode;
        public final double targetTemp;
        public final double currentTemp;
        public final double externalTemp;
        public final boolean hasTempSensor;
        public final String title;
        public final Schedule schedule;
        public final boolean callForHeat;
        public final String location;
        public final double boostTemp;
        public final int boostDuration;
        public final int boostTime;
        public final boolean pumpStatus;
        public final int pumpTime;
        public final int pumpDuration;

        public RoomItem(int id, String title, double desired, double current, double external,
                        boolean hasTempSensor, Type type, Mode mode, Mode baseMode, Schedule schedule,
                        boolean callForHeat, String location, double boostTemp, int boostDuration, int boostTime,
                        boolean pumpStatus, int pumpDuration, int pumpTime)
        {
            this.id = id;
            this.title = title;
            this.targetTemp = desired;
            this.currentTemp = current;
            this.externalTemp = external;
            this.hasTempSensor = hasTempSensor;
            this.mode = mode;
            this.baseMode = baseMode;
            this.type = type;
            this.schedule = schedule;
            this.callForHeat = callForHeat;
            this.location = location;
            this.boostTemp = boostTemp;
            this.boostDuration = boostDuration;
            this.boostTime = boostTime;
            this.pumpStatus = pumpStatus;
            this.pumpTime = pumpTime;
            this.pumpDuration = pumpDuration;
        }

        @Override
        public String toString() {
            return title;
        }
    }

}
