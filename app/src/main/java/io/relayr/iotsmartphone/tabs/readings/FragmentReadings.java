package io.relayr.iotsmartphone.tabs.readings;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
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
import io.relayr.iotsmartphone.IotApplication;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.tabs.IotFragment;
import io.relayr.iotsmartphone.tabs.helper.Constants;
import io.relayr.iotsmartphone.tabs.helper.ReadingUtils;
import io.relayr.iotsmartphone.tabs.helper.SettingsStorage;
import io.relayr.iotsmartphone.tabs.helper.UiHelper;
import io.relayr.iotsmartphone.tabs.readings.widgets.ReadingWidget;
import io.relayr.java.model.models.transport.DeviceReading;

import static android.os.Build.MANUFACTURER;
import static android.os.Build.MODEL;
import static android.os.Build.VERSION.SDK_INT;
import static android.support.v7.widget.StaggeredGridLayoutManager.VERTICAL;
import static io.relayr.iotsmartphone.tabs.helper.Constants.DeviceType.PHONE;
import static io.relayr.iotsmartphone.tabs.helper.Constants.DeviceType.WATCH;

public class FragmentReadings extends IotFragment {

    @InjectView(R.id.readings_phone_grid) protected RecyclerView mPhoneGrid;
    @InjectView(R.id.readings_watch_grid) protected RecyclerView mWatchGrid;
    @InjectView(R.id.fab) protected FloatingActionButton mFab;

    private ReadingsAdapter mPhoneAdapter;
    private ReadingsAdapter mWatchAdapter;

    public FragmentReadings() {}

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (SettingsStorage.instance().getDeviceName(PHONE) == null)
            SettingsStorage.instance().savePhoneData(MANUFACTURER, MODEL, SDK_INT);
    }

    @Override public void onResume() {
        super.onResume();
        setTitle(getActivity().getString(R.string.app_title_phone));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        final View view = inflater.inflate(R.layout.activity_tab_fragment_readings, container, false);
        ButterKnife.inject(this, view);
        EventBus.getDefault().register(this);

        final int columns = getResources().getInteger(R.integer.num_columns);

        setUpAdapters(columns);

        if (UiHelper.isWearableConnected(getActivity())) mFab.setVisibility(View.VISIBLE);
        else mFab.setVisibility(View.GONE);
        onFabClicked();

        return view;
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
        IotApplication.visible(PHONE, false);
        IotApplication.visible(WATCH, false);
    }

    @SuppressWarnings("unused") @OnClick(R.id.fab)
    public void onFabClicked() {
        if (IotApplication.isVisible(PHONE)) onWatchClicked();
        else onPhoneClicked();
    }

    @SuppressWarnings("unused")
    public void onEvent(final Constants.DeviceModelEvent event) {
        getActivity().runOnUiThread(new Runnable() {
            @Override public void run() {
                if (mPhoneAdapter != null && IotApplication.isVisible(PHONE))
                    mPhoneAdapter.update(SettingsStorage.instance().loadReadings(PHONE), PHONE);
                if (mWatchAdapter != null && IotApplication.isVisible(WATCH))
                    mWatchAdapter.update(SettingsStorage.instance().loadReadings(WATCH), WATCH);
            }
        });
    }

    private void setUpAdapters(int columns) {
        mPhoneAdapter = new ReadingsAdapter(SettingsStorage.instance().loadReadings(PHONE), PHONE);
        mPhoneGrid.setLayoutManager(new StaggeredGridLayoutManager(columns, VERTICAL));
        mPhoneGrid.setAdapter(mPhoneAdapter);

        mWatchAdapter = new ReadingsAdapter(SettingsStorage.instance().loadReadings(WATCH), WATCH);
        mWatchGrid.setLayoutManager(new StaggeredGridLayoutManager(columns, VERTICAL));
        mWatchGrid.setAdapter(mWatchAdapter);
    }

    private void onPhoneClicked() {
        IotApplication.visible(PHONE, true);
        ReadingUtils.initializeReadings(PHONE);

        mFab.setImageResource(R.drawable.ic_graphic_watch);

        mPhoneGrid.setVisibility(View.VISIBLE);
        mWatchGrid.setVisibility(View.GONE);

        if (mPhoneAdapter != null)
            mPhoneAdapter.update(SettingsStorage.instance().loadReadings(PHONE), PHONE);

        setTitle(getActivity().getString(R.string.app_title_phone));
    }

    private void onWatchClicked() {
        IotApplication.visible(WATCH, true);
        ReadingUtils.initializeReadings(WATCH);

        EventBus.getDefault().post(new Constants.WatchSelected());

        mFab.setImageResource(R.drawable.ic_graphic_phone);

        mPhoneGrid.setVisibility(View.GONE);
        mWatchGrid.setVisibility(View.VISIBLE);

        if (mWatchAdapter != null)
            mWatchAdapter.update(SettingsStorage.instance().loadReadings(WATCH), WATCH);

        setTitle(getActivity().getString(R.string.app_title_watch));
    }

    static class ReadingsAdapter extends RecyclerView.Adapter<ReadingViewHolder> {

        private Constants.DeviceType mType;
        private final List<DeviceReading> mReadings = new ArrayList<>();

        public ReadingsAdapter(List<DeviceReading> items, Constants.DeviceType type) {
            update(items, type);
        }

        @Override
        public ReadingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(viewType, null);
            return new ReadingViewHolder((ReadingWidget) view, parent.getContext());
        }

        @Override public int getItemViewType(int position) {
            final String meaning = mReadings.get(position).getMeaning();
            return meaning.equals("angularSpeed") || meaning.equals("acceleration") ? R.layout.widget_reading_graph :
                    meaning.equals("rssi") || meaning.equals("batteryLevel") || meaning.equals("luminosity") ? R.layout.widget_reading_graph_simple :
                            meaning.equals("touch") ? R.layout.widget_reading_graph_bar : R.layout.widget_reading_default;
        }

        @Override
        public void onBindViewHolder(ReadingViewHolder holder, int position) {
            holder.refresh(mReadings.get(position), mType);
        }

        @Override
        public int getItemCount() {
            return this.mReadings.size();
        }

        public void update(List<DeviceReading> readings, Constants.DeviceType type) {
            mType = type;
            mReadings.clear();
            mReadings.addAll(readings);
            notifyDataSetChanged();
        }
    }
}
