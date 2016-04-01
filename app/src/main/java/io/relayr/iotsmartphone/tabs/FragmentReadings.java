package io.relayr.iotsmartphone.tabs;

import android.os.Bundle;
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
import de.greenrobot.event.EventBus;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.Storage;
import io.relayr.iotsmartphone.tabs.widgets.ReadingWidget;
import io.relayr.java.model.models.transport.DeviceReading;

public class FragmentReadings extends Fragment {

    @InjectView(R.id.grid) protected RecyclerView mGridView;
    private ReadingsAdapter mGridAdapter;

    private StaggeredGridLayoutManager gridLayoutManager;

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

        gridLayoutManager = new StaggeredGridLayoutManager(getResources().getInteger(R.integer.num_columns), StaggeredGridLayoutManager.VERTICAL);
        mGridView.setLayoutManager(gridLayoutManager);

        mGridAdapter = new ReadingsAdapter(Storage.instance().getPhoneReadings());
        mGridView.setAdapter(mGridAdapter);
        return view;
    }

    @SuppressWarnings("unused")
    public void onEvent(final Constants.DeviceModelEvent event) {
        getActivity().runOnUiThread(new Runnable() {
            @Override public void run() {
                if (mGridAdapter != null) mGridAdapter.notifyDataSetChanged();
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
                    reading.getMeaning().equals("acceleration") ||
                    reading.getMeaning().equals("luminosity") ? R.layout.widget_reading_graph :
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
