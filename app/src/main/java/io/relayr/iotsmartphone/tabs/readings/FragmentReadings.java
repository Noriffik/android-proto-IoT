package io.relayr.iotsmartphone.tabs.readings;

import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
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

    @InjectView(R.id.readings_grid) protected RecyclerView mGrid;
    @InjectView(R.id.fab) protected FloatingActionButton mFab;

    private ReadingsAdapter mPhoneAdapter;
    private ReadingsAdapter mWatchAdapter;
    private String title;

    public FragmentReadings() {}

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);

        if (SettingsStorage.instance().getPhoneManufacturer() == null)
            SettingsStorage.instance().savePhoneData(Build.MANUFACTURER, Build.MODEL, Build.VERSION.SDK_INT);
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
        mGrid.setLayoutManager(new StaggeredGridLayoutManager(columns, VERTICAL));

        onPhoneClicked();

        return view;
    }

    @SuppressWarnings("unused") @OnClick(R.id.fab)
    public void onFabClicked() {
        if (mGrid.getVisibility() == View.VISIBLE) onWatchClicked();
        else onPhoneClicked();
    }

    private void onPhoneClicked() {
        mFab.setImageResource(R.drawable.ic_graphic_watch);

        mPhoneAdapter = new ReadingsAdapter(SettingsStorage.instance().getPhoneReadings());
        mGrid.setAdapter(mPhoneAdapter);
        setTitle("IoT smartphone");
    }

    private void onWatchClicked() {
        mFab.setImageResource(R.drawable.ic_graphic_phone);

        mWatchAdapter = new ReadingsAdapter(new ArrayList<DeviceReading>());
        mGrid.setAdapter(mWatchAdapter);
        setTitle("IoT watch");
    }

    @SuppressWarnings("unused")
    public void onEvent(final Constants.DeviceModelEvent event) {
        getActivity().runOnUiThread(new Runnable() {
            @Override public void run() {
                if (mPhoneAdapter != null) mPhoneAdapter.notifyDataSetChanged();
                if (mWatchAdapter != null) mWatchAdapter.notifyDataSetChanged();
            }
        });
    }

    public void setTitle(String title) {
        if (getActivity() == null) return;
        if (((AppCompatActivity) getActivity()).getSupportActionBar() == null) return;
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(title);
    }

    static class ReadingsAdapter extends RecyclerView.Adapter<ReadingViewHolder> {

        private List<DeviceReading> mReadings = new ArrayList<>();

        public ReadingsAdapter(List<DeviceReading> items) {
            this.mReadings = items;
        }

        @Override
        public ReadingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View layoutView = LayoutInflater.from(parent.getContext()).inflate(viewType, null);
            return new ReadingViewHolder((ReadingWidget) layoutView, parent.getContext());
        }

        @Override public int getItemViewType(int position) {
            final DeviceReading reading = mReadings.get(position);
            return reading.getMeaning().equals("rssi") ||
                    reading.getMeaning().equals("batteryLevel") ||
                    reading.getMeaning().equals("acceleration") ? R.layout.widget_reading_graph :
                    reading.getMeaning().equals("luminosity") ? R.layout.widget_reading_graph_bar :
                            R.layout.widget_reading_default;
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
