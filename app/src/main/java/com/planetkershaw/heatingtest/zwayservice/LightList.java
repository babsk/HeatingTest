package com.planetkershaw.heatingtest.zwayservice;

import java.util.ArrayList;
import java.util.List;

public class LightList
{
    // array of light items
    public static final List<LightItem> ITEMS = new ArrayList<LightItem>();

    public static final void clear ()
    {
        ITEMS.clear();
    }

    public static final void addLight (String id, String title, boolean status)
    {
        LightItem light = new LightItem (id, title, status);
        ITEMS.add (light);
    }

    public static final LightItem get (String id) {
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

    // light item
    public static class LightItem
    {
        public final String id;
        public final String title;
        public final boolean status;

        public LightItem(String id, String title, boolean status)
        {
            this.id = id;
            this.title = title;
            this.status = status;
        }

        @Override
        public String toString() {
            return title;
        }
    }

}
