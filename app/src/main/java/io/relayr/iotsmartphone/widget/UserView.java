package io.relayr.iotsmartphone.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.relayr.android.RelayrSdk;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.Storage;
import io.relayr.java.model.Device;
import io.relayr.java.model.User;
import io.relayr.java.model.models.DeviceModel;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.widget.Toast.LENGTH_LONG;

public class UserView extends BasicView {

    @InjectView(R.id.used_device_name) TextView mUsedDeviceName;
    @InjectView(R.id.used_device_id) TextView mUsedDeviceId;
    @InjectView(R.id.user_name) TextView mUsername;
    @InjectView(R.id.user_email) TextView mEmail;
    @InjectView(R.id.user_id) TextView mUserId;
    @InjectView(R.id.user_devices) ListView mDevicesList;

    private List<Device> mDevices = new ArrayList<>();
    private DeviceAdapter mDeviceAdapter;

    public UserView(Context context) {
        super(context);
    }

    public UserView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UserView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ButterKnife.inject(this);

        if (Storage.instance().getDevice() != null) {
            mUsedDeviceName.setText(Storage.instance().getDevice().getName());
            mUsedDeviceId.setText(Storage.instance().getDevice().getId());
        }

        mDeviceAdapter = new DeviceAdapter(getContext(), mDevices);
        mDevicesList.setAdapter(mDeviceAdapter);

        RelayrSdk.getUser()
                .timeout(3, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<User>() {
                    @Override public void onCompleted() {}

                    @Override public void onError(Throwable e) {
                        Toast.makeText(getContext(), R.string.something_went_wrong, LENGTH_LONG).show();
                        e.printStackTrace();
                    }

                    @Override public void onNext(User user) {
                        mUsername.setText(user.getName());
                        mEmail.setText(user.getEmail());
                        mUserId.setText(user.getId());
                        showDevices(user);
                    }
                });
    }

    private void showDevices(User user) {
        user.getDevices()
                .timeout(10, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<Device>>() {
                    @Override public void onCompleted() {}

                    @Override public void onError(Throwable e) {
                        Toast.makeText(getContext(), R.string.something_went_wrong, LENGTH_LONG).show();
                        e.printStackTrace();
                    }

                    @Override public void onNext(List<Device> devices) {
                        mDevices.addAll(devices);
                        mDeviceAdapter.notifyDataSetChanged();
                        setHeightBasedOnChildren(mDevicesList);
                    }
                });
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ButterKnife.reset(this);
    }

    private void setHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null)
            return;

        int desiredWidth = MeasureSpec.makeMeasureSpec(listView.getWidth(), UNSPECIFIED);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            if (i == 0)
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, WRAP_CONTENT));

            view.measure(desiredWidth, UNSPECIFIED);
            totalHeight += view.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    class DeviceAdapter extends ArrayAdapter<Device> {

        public DeviceAdapter(Context context, List<Device> devices) {
            super(context, R.layout.device_item, devices);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            final ViewHolder holder;
            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                view = inflater.inflate(R.layout.device_item, parent, false);
                holder = new ViewHolder(view, getItem(position));
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            holder.setData(getItem(position));

            return view;
        }

        class ViewHolder {

            @InjectView(R.id.device_name) TextView name;
            @InjectView(R.id.device_model) TextView model;

            public ViewHolder(View view, Device device) {
                ButterKnife.inject(this, view);
                setData(device);
            }

            public void setData(Device device) {
                name.setText(device.getName());
                device.getDeviceModel()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<DeviceModel>() {
                            @Override public void onCompleted() {}

                            @Override public void onError(Throwable e) {
                                e.printStackTrace();
                            }

                            @Override public void onNext(DeviceModel deviceModel) {
                                if (deviceModel != null)
                                    model.setText(deviceModel.getName());
                            }
                        });
            }
        }
    }
}
