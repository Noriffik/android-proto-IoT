package io.relayr.iotsmartphone.ui.readings;

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
import butterknife.BindView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import io.relayr.iotsmartphone.IotApplication;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.storage.Constants;
import io.relayr.iotsmartphone.storage.Storage;
import io.relayr.iotsmartphone.ui.readings.widgets.ReadingWidget;
import io.relayr.iotsmartphone.ui.utils.UiUtil;
import io.relayr.java.model.models.transport.DeviceReading;

import static android.os.Build.MANUFACTURER;
import static android.os.Build.MODEL;
import static android.os.Build.VERSION.SDK_INT;
import static android.support.v7.widget.StaggeredGridLayoutManager.VERTICAL;
import static io.relayr.iotsmartphone.storage.Constants.DeviceType.PHONE;
import static io.relayr.iotsmartphone.storage.Constants.DeviceType.WATCH;

public class FragmentReadings extends Fragment {

    @BindView(R.id.readings_phone_grid) protected RecyclerView mPhoneGrid;
    @BindView(R.id.readings_watch_grid) protected RecyclerView mWatchGrid;
    @BindView(R.id.fab) protected FloatingActionButton mFab;

    private ReadingsAdapter mPhoneAdapter;
    private ReadingsAdapter mWatchAdapter;

    public FragmentReadings() {}

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Storage.instance().getDeviceName(PHONE) == null)
            Storage.instance().savePhoneData(MANUFACTURER, MODEL, SDK_INT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        final View view = inflater.inflate(R.layout.activity_tab_fragment_readings, container, false);
        ButterKnife.bind(this, view);

        final int columns = getResources().getInteger(R.integer.num_columns);

        setUpAdapters(columns);

        mFab.setVisibility(UiUtil.isWearableConnected(getActivity()) ? View.VISIBLE : View.GONE);

        if (IotApplication.sCurrent == PHONE) onPhoneClicked();
        else onWatchClicked();

        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) EventBus.getDefault().register(this);
        else EventBus.getDefault().unregister(this);
        sendEvent(isVisibleToUser);
    }

    @SuppressWarnings("unused") @OnClick(R.id.fab)
    public void onFabClicked() {
        if (IotApplication.isVisible(PHONE)) onWatchClicked();
        else onPhoneClicked();
        sendEvent(true);
    }

    @SuppressWarnings("unused")
    public void onEvent(final Constants.DeviceModelEvent event) {
        getActivity().runOnUiThread(new Runnable() {
            @Override public void run() {
                if (mPhoneAdapter != null && IotApplication.isVisible(PHONE))
                    mPhoneAdapter.update(Storage.instance().loadReadings(PHONE), PHONE);
                if (mWatchAdapter != null && IotApplication.isVisible(WATCH))
                    mWatchAdapter.update(Storage.instance().loadReadings(WATCH), WATCH);
            }
        });
    }

    private void setUpAdapters(int columns) {
        mPhoneAdapter = new ReadingsAdapter(Storage.instance().loadReadings(PHONE), PHONE);
        mPhoneGrid.setLayoutManager(new StaggeredGridLayoutManager(columns, VERTICAL));
        mPhoneGrid.setAdapter(mPhoneAdapter);

        mWatchAdapter = new ReadingsAdapter(Storage.instance().loadReadings(WATCH), WATCH);
        mWatchGrid.setLayoutManager(new StaggeredGridLayoutManager(columns, VERTICAL));
        mWatchGrid.setAdapter(mWatchAdapter);
    }

    private void onPhoneClicked() {
        IotApplication.sCurrent = PHONE;
        IotApplication.visible(true, false);
        mFab.setImageResource(R.drawable.ic_graphic_watch);

        mPhoneGrid.setVisibility(View.VISIBLE);
        mWatchGrid.setVisibility(View.GONE);

        if (mPhoneAdapter != null)
            mPhoneAdapter.update(Storage.instance().loadReadings(PHONE), PHONE);
    }

    private void onWatchClicked() {
        IotApplication.sCurrent = WATCH;
        IotApplication.visible(false, true);
        mFab.setImageResource(R.drawable.ic_graphic_phone);

        mPhoneGrid.setVisibility(View.GONE);
        mWatchGrid.setVisibility(View.VISIBLE);

        if (mWatchAdapter != null)
            mWatchAdapter.update(Storage.instance().loadReadings(WATCH), WATCH);
    }

    private void sendEvent(boolean visibleToUser) {
        if (visibleToUser && IotApplication.isVisible(PHONE))
            EventBus.getDefault().post(new Constants.DeviceChange(PHONE));
        if (visibleToUser && IotApplication.isVisible(WATCH))
            EventBus.getDefault().post(new Constants.DeviceChange(WATCH));
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
                            meaning.equals("touch") ? R.layout.widget_reading_graph_bar : R.layout.widget_reading_map;
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
