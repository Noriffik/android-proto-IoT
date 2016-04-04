package io.relayr.iotsmartphone.tabs.readings;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.tabs.helper.Constants;
import io.relayr.iotsmartphone.tabs.helper.SettingsStorage;
import io.relayr.iotsmartphone.tabs.readings.widgets.ReadingWidget;
import io.relayr.java.model.models.transport.DeviceReading;

import static android.support.v7.widget.StaggeredGridLayoutManager.VERTICAL;

public class FragmentReadings extends Fragment {

    @InjectView(R.id.phone_grid) protected RecyclerView mPhoneGrid;
    @InjectView(R.id.wearable_grid) protected RecyclerView mWatchGrid;
    @InjectView(R.id.fab) protected FloatingActionButton mFab;

    private ReadingsAdapter mPhoneAdapter;
    private ReadingsAdapter mWatchAdapter;

    public FragmentReadings() {}

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        final View view = inflater.inflate(R.layout.activity_tab_fragment_readings, container, false);
        ButterKnife.inject(this, view);

        final int columns = getResources().getInteger(R.integer.num_columns);
        mPhoneGrid.setLayoutManager(new StaggeredGridLayoutManager(columns, VERTICAL));
        mWatchGrid.setLayoutManager(new StaggeredGridLayoutManager(columns, VERTICAL));

        setUpDeviceData();
        onPhoneClicked();

        return view;
    }

    private void setUpDeviceData() {
        //        mPhoneName.setText(MANUFACTURER + " " + MODEL);
        //        mPhoneType.setText("Android OS v " + SDK_INT);
    }

    @OnClick(R.id.fab)
    public void onFabClicked() {
        if (mPhoneGrid.getVisibility() == View.VISIBLE) onWatchClicked();
        else onPhoneClicked();
    }

    private void onPhoneClicked() {
        mWatchGrid.setVisibility(View.GONE);
        mPhoneGrid.setVisibility(View.VISIBLE);
        mFab.setImageResource(R.drawable.ic_graphic_watch);

        //        if (mPhoneAdapter != null) {
        //            mPhoneAdapter.notifyDataSetChanged();
        //            return;
        //        }
        mPhoneAdapter = new ReadingsAdapter(SettingsStorage.instance().getPhoneReadings());
        mPhoneGrid.setAdapter(mPhoneAdapter);
    }

    private void onWatchClicked() {
        mWatchGrid.setVisibility(View.VISIBLE);
        mPhoneGrid.setVisibility(View.GONE);
        mFab.setImageResource(R.drawable.ic_graphic_phone);

        //        if (mWatchAdapter != null) {
        //            mWatchAdapter.notifyDataSetChanged();
        //            return;
        //        }
        mWatchAdapter = new ReadingsAdapter(new ArrayList<DeviceReading>());
        mWatchGrid.setAdapter(mWatchAdapter);
    }

    @SuppressWarnings("unused")
    public void onEvent(final Constants.DeviceModelEvent event) {
        getActivity().runOnUiThread(new Runnable() {
            @Override public void run() {
                if (mPhoneAdapter != null) mPhoneAdapter.notifyDataSetChanged();
            }
        });
    }

    static class ReadingsAdapter extends RecyclerView.Adapter<ReadingViewHolder> {

        private List<DeviceReading> mReadings = new ArrayList<>();

        public ReadingsAdapter(List<DeviceReading> items) {
            this.mReadings = items;
        }

        @Override
        public ReadingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View layoutView = LayoutInflater.from(parent.getContext()).inflate(viewType, null);
            ReadingViewHolder holder = new ReadingViewHolder((ReadingWidget) layoutView);
            return holder;
        }

        @Override public int getItemViewType(int position) {
            final DeviceReading reading = mReadings.get(position);
            int layout = reading.getMeaning().equals("rssi") ||
                    reading.getMeaning().equals("batteryLevel") ||
                    reading.getMeaning().equals("acceleration") ? R.layout.widget_reading_graph :
                    reading.getMeaning().equals("luminosity") ? R.layout.widget_reading_graph_bar :
                            R.layout.widget_reading_default;
            return layout;
        }

        @Override
        public void onBindViewHolder(ReadingViewHolder holder, int position) {
            holder.refresh(mReadings.get(position));
        }

        @Override
        public int getItemCount() {
            return this.mReadings.size();
        }
    }
}
