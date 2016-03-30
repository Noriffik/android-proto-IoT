package io.relayr.iotsmartphone.tabs;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.Storage;
import io.relayr.iotsmartphone.tabs.widgets.ReadingWidget;
import io.relayr.java.model.models.transport.DeviceReading;

public class FragmentReadings extends Fragment {

    @InjectView(R.id.grid) protected GridView mGridView;
    private ReadingsAdapter mGridAdapter;

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

        mGridAdapter = new ReadingsAdapter(getContext(), Storage.instance().getPhoneReadings());
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

    class ReadingsAdapter extends BaseAdapter {

        private final Context mContext;
        private final List<DeviceReading> mReadings;

        public ReadingsAdapter(Context context, List<DeviceReading> readings) {
            mContext = context;
            mReadings = readings;
        }

        @Override
        public int getCount() {
            return mReadings.size();
        }

        @Override
        public DeviceReading getItem(int position) {
            return mReadings.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View view, ViewGroup parent) {
            final DeviceReading reading = mReadings.get(position);

            final ViewHolder holder;
            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                final int layout = reading.getMeaning().equals("rssi") ||
                        reading.getMeaning().equals("batteryLevel") ||
                        reading.getMeaning().equals("acceleration") ||
                        reading.getMeaning().equals("luminosity") ? R.layout.widget_reading_graph :
                        R.layout.widget_reading_default;
                view = inflater.inflate(layout, parent, false);
                holder = new ViewHolder((ReadingWidget) view, reading);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            holder.refresh((ReadingWidget) view, reading);

            return view;
        }

        class ViewHolder {

            public ViewHolder(ReadingWidget widget, DeviceReading reading) {
                refresh(widget, reading);
            }

            public void refresh(ReadingWidget widget, DeviceReading reading) {
                widget.setPath(reading.getPath());
                widget.setMeaning(reading.getMeaning());
                widget.setSchema(reading.getValueSchema());
            }
        }
    }
}
