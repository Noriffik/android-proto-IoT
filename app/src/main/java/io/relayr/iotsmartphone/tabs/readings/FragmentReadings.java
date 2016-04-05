package io.relayr.iotsmartphone.tabs.readings;

import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import io.relayr.iotsmartphone.IotApplication;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.tabs.helper.Constants;
import io.relayr.iotsmartphone.tabs.helper.SettingsStorage;
import io.relayr.iotsmartphone.tabs.readings.widgets.ReadingWidget;
import io.relayr.java.model.models.transport.DeviceReading;

import static android.support.v7.widget.StaggeredGridLayoutManager.VERTICAL;
import static io.relayr.iotsmartphone.tabs.helper.Constants.DeviceType.PHONE;
import static io.relayr.iotsmartphone.tabs.helper.Constants.DeviceType.WATCH;

public class FragmentReadings extends Fragment {

    @InjectView(R.id.readings_phone_grid) protected RecyclerView mPhoneGrid;
    @InjectView(R.id.readings_watch_grid) protected RecyclerView mWatchGrid;
    @InjectView(R.id.fab) protected FloatingActionButton mFab;

    private ReadingsAdapter mPhoneAdapter;
    private ReadingsAdapter mWatchAdapter;

    public FragmentReadings() {}

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);

        if (SettingsStorage.instance().getPhoneManufacturer() == null)
            SettingsStorage.instance().savePhoneData(Build.MANUFACTURER, Build.MODEL, Build.VERSION.SDK_INT);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        IotApplication.visible(WATCH, false);
        EventBus.getDefault().unregister(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        final View view = inflater.inflate(R.layout.activity_tab_fragment_readings, container, false);
        ButterKnife.inject(this, view);

        final int columns = getResources().getInteger(R.integer.num_columns);

        mPhoneAdapter = new ReadingsAdapter(SettingsStorage.instance().loadPhoneReadings());
        mPhoneGrid.setLayoutManager(new StaggeredGridLayoutManager(columns, VERTICAL));
        mPhoneGrid.setAdapter(mPhoneAdapter);

        mWatchAdapter = new ReadingsAdapter(SettingsStorage.instance().loadWatchReadings());
        mWatchGrid.setLayoutManager(new StaggeredGridLayoutManager(columns, VERTICAL));
        mWatchGrid.setAdapter(mWatchAdapter);

        onFabClicked();

        return view;
    }

    @SuppressWarnings("unused") @OnClick(R.id.fab)
    public void onFabClicked() {
        if (IotApplication.isVisible(PHONE)) onWatchClicked();
        else onPhoneClicked();
    }

    private void onPhoneClicked() {
        IotApplication.visible(PHONE, true);
        mFab.setImageResource(R.drawable.ic_graphic_watch);

        mPhoneGrid.setVisibility(View.VISIBLE);
        mWatchGrid.setVisibility(View.GONE);

        if (mPhoneAdapter != null)
            mPhoneAdapter.update(SettingsStorage.instance().loadPhoneReadings());

        setTitle("IoT smartphone");
    }

    private void onWatchClicked() {
        IotApplication.visible(WATCH, true);
        mFab.setImageResource(R.drawable.ic_graphic_phone);

        mPhoneGrid.setVisibility(View.GONE);
        mWatchGrid.setVisibility(View.VISIBLE);

        if (mWatchAdapter != null)
            mWatchAdapter.update(SettingsStorage.instance().loadWatchReadings());

        setTitle("IoT watch");
    }

    @SuppressWarnings("unused")
    public void onEvent(final Constants.DeviceModelEvent event) {
        getActivity().runOnUiThread(new Runnable() {
            @Override public void run() {
                if (mPhoneAdapter != null && IotApplication.isVisible(PHONE))
                    mPhoneAdapter.update(SettingsStorage.instance().loadPhoneReadings());
                if (mWatchAdapter != null && IotApplication.isVisible(WATCH))
                    mWatchAdapter.update(SettingsStorage.instance().loadWatchReadings());
            }
        });
    }

    public void setTitle(String title) {
        if (title != null && getActivity() != null && ((AppCompatActivity) getActivity()).getSupportActionBar() != null)
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(title);
    }

    static class ReadingsAdapter extends RecyclerView.Adapter<ReadingViewHolder> {

        private final List<DeviceReading> mReadings = new ArrayList<>();

        public ReadingsAdapter(List<DeviceReading> items) {
            update(items);
        }

        @Override
        public ReadingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(viewType, null);
            return new ReadingViewHolder((ReadingWidget) view, parent.getContext());
        }

        @Override public int getItemViewType(int position) {
            final String meaning = mReadings.get(position).getMeaning();
            return meaning.equals("rssi") ||
                    meaning.equals("batteryLevel") ||
                    meaning.equals("acceleration") ||
                    meaning.equals("angularSpeed") ||
                    meaning.equals("luminosity") ? R.layout.widget_reading_graph :
                    meaning.equals("touch") ? R.layout.widget_reading_graph_bar :
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

        public void update(List<DeviceReading> readings) {
            mReadings.clear();
            mReadings.addAll(readings);
            notifyDataSetChanged();
        }
    }
}
