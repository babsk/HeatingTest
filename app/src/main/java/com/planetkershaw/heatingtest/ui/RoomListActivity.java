package com.planetkershaw.heatingtest.ui;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.planetkershaw.heatingtest.R;
import com.planetkershaw.heatingtest.zwayservice.DataChangedReceiver;
import com.planetkershaw.heatingtest.zwayservice.RoomList;

import java.util.List;

/**************************************************************************
 *
 * An activity representing a list of Rooms. This activity
 * has different presentations for handset and tablet-size devices.
 *
 * On handsets, the activity presents a list of rooms, which when touched,
 * lead to a {@link RoomDetailActivity} representing
 * room details.
 *
 * On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 *
 **************************************************************************/

public class RoomListActivity extends BaseListActivity
{
    // whether or not the activity is in two pane mode - determined by which resource file
    // loaded
    private boolean mTwoPane;

    private SimpleItemRecyclerViewAdapter adapter;

    private DataChangedReceiver mReceiver;

    private RoomDetailFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_list);

        setupToolBar(R.id.toolbar,"Rooms");

        if (findViewById(R.id.room_detail_container) != null)
        {
            // The schedule container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
            Log.d("ROOMLISTACTIVITY","two pane mode");
        }
        else {
            mTwoPane = false;
        }
        app.mTwoPane = mTwoPane;

        // set up the list of rooms
        View recyclerView = findViewById(R.id.room_list);
        assert recyclerView != null;
        adapter = setupRecyclerView((RecyclerView) recyclerView);

        setupMenuDrawer ();

        // register for changes to the data set
        mReceiver = new DataChangedReceiver() {
            public void onReceive(Context context, Intent intent) {
                adapter.notifyDataSetChanged();
                updateData();
            }

        };
        registerReceiver(mReceiver, new IntentFilter(DataChangedReceiver.ACTION_HEATING_DATA_CHANGED));

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, new IntentFilter(DataChangedReceiver.ACTION_HEATING_DATA_CHANGED));
        adapter.notifyDataSetChanged();
        updateData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver (mReceiver);
    }

    // display new data
    private void updateData () {
        if (RoomList.ITEMS.size() > 0) {
            double internal = RoomList.ITEMS.get(0).currentTemp;
            double external = RoomList.ITEMS.get(0).externalTemp;
            TextView externalTemp = (TextView) findViewById(R.id.outside);
            externalTemp.setText("Outside: " + app.df.format(external) + " °C " + "   Inside: " + app.df.format(internal) + " °C ");


            updateStatus();

            // on tablets, we update the room details pane
            // we need to provide the room id based on the selected position
            if (mTwoPane && (fragment == null)) {
                // create the fragment that will display the room details
                Bundle arguments = new Bundle();
                int roomId = RoomList.ITEMS.get(adapter.selectedPos).id;
                arguments.putInt(RoomDetailFragment.ARG_ITEM_ID, roomId);
                fragment = new RoomDetailFragment();
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.room_detail_container, fragment)
                        .commit();
            }
        }

    }

    // setup the view adapter for the rooms list
    private SimpleItemRecyclerViewAdapter setupRecyclerView(@NonNull RecyclerView recyclerView) {
        SimpleItemRecyclerViewAdapter adapter = new SimpleItemRecyclerViewAdapter(RoomList.ITEMS);
        recyclerView.setAdapter(adapter);
        return adapter;
    }

    // custom adapter for the rooms view (Generated by Android Studio when two pane view chosen)
    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final List<RoomList.RoomItem> mValues;
        public int selectedPos = 0;

        public SimpleItemRecyclerViewAdapter(List<RoomList.RoomItem> items) {
            mValues = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.room_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position)
        {
            holder.mItem = mValues.get(position);
            holder.mContentView.setText(mValues.get(position).title);
            if (mValues.get(position).type == RoomList.Type.ONOFF)
                holder.mTempView.setText("");
            else if (mValues.get(position).hasTempSensor)
                holder.mTempView.setText("" + app.df.format(mValues.get(position).currentTemp) + " (set " + app.df.format(mValues.get(position).targetTemp) + "°C)");
            else
                holder.mTempView.setText("(set "+app.df.format(mValues.get(position).targetTemp)+"°C)");

            if (selectedPos == position) {
                holder.mRoomView.setBackgroundColor(Color.GRAY);
            }
            else {
                holder.mRoomView.setBackgroundColor(Color.LTGRAY);
            }
            holder.mHeatView.setVisibility(holder.mItem.callForHeat ? View.VISIBLE : View.INVISIBLE);
            holder.mPumpView.setVisibility(holder.mItem.pumpStatus ? View.VISIBLE : View.INVISIBLE);


            switch (mValues.get(position).mode) {
                    case BOOST:
                        holder.mModeView.setImageResource(R.drawable.status_boost);
                        break;
                    case TIMER:
                        holder.mModeView.setImageResource(R.drawable.status_timer);
                        break;
                    case OFF:
                        holder.mModeView.setImageResource(R.drawable.status_off);
                        break;
            }


            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mTwoPane) {
                        selectedPos = holder.getAdapterPosition();
                        holder.mRoomView.setBackgroundColor(Color.GRAY);

                        // new room selected, so we need to feed the selection into the fragment
                        fragment.changeRoomID(holder.mItem.id);
/*                        Bundle arguments = new Bundle();
                        arguments.putInt(RoomDetailFragment.ARG_ITEM_ID, holder.mItem.id);
                        RoomDetailFragment fragment = new RoomDetailFragment();
                        fragment.setArguments(arguments);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.room_detail_container, fragment)
                                .commit();*/
                        adapter.notifyDataSetChanged();
                    }
                    else
                    {
                        Context context = v.getContext();
                        Intent intent = new Intent(context, RoomDetailActivity.class);
                        intent.putExtra(RoomDetailFragment.ARG_ITEM_ID, holder.mItem.id);
                        context.startActivity(intent);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mContentView;
            public final TextView mTempView;
            public final ImageView mModeView;
            public final ImageView mHeatView;
            public final ImageView mPumpView;
            public final RelativeLayout mRoomView;
            public RoomList.RoomItem mItem;

            public ViewHolder(View view)
            {
                super(view);
                mView = view;
                mContentView = (TextView) view.findViewById(R.id.title);
                mTempView = (TextView) view.findViewById(R.id.temp);
                mModeView = (ImageView) view.findViewById(R.id.mode);
                mRoomView = (RelativeLayout) view.findViewById(R.id.item);
                mHeatView = (ImageView)view.findViewById(R.id.heat);
                mPumpView = (ImageView)view.findViewById(R.id.pump);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mContentView.getText() + "'";
            }
        }
    }
}
