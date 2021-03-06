package com.planetkershaw.heatingtest.ui;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.planetkershaw.heatingtest.R;


public class RoomDetailActivity extends AppCompatActivity
{
    /**
     * An activity representing a single Room detail screen. This
     * activity is only used narrow width devices. On tablet-size devices,
     * item details are presented side-by-side with a list of items
     * in a {@link RoomListActivity}.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_detail);

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putInt(RoomDetailFragment.ARG_ITEM_ID,
                    getIntent().getIntExtra(RoomDetailFragment.ARG_ITEM_ID,0));
            RoomDetailFragment fragment = new RoomDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.room_detail_container, fragment)
                    .commit();
        }
    }

}
