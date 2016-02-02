package io.relayr.iotsmartphone.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.relayr.android.RelayrSdk;
import io.relayr.iotsmartphone.R;
import io.relayr.java.model.Device;
import io.relayr.java.model.User;
import io.relayr.java.model.models.error.DeviceModelsException;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

public class UserView extends BasicView {

    @InjectView(R.id.user_name) TextView mUsername;
    @InjectView(R.id.user_email) TextView mEmail;
    @InjectView(R.id.user_id) TextView mUserId;
    @InjectView(R.id.user_devices) ListView mDevices;

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

        RelayrSdk.getUser()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<User>() {
                    @Override public void onCompleted() {}

                    @Override public void onError(Throwable e) {
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
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<Device>>() {
                    @Override public void onCompleted() {}

                    @Override public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override public void onNext(List<Device> devices) {
                        mDevices.setAdapter(new DeviceAdapter(getContext(), devices));
                    }
                });
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ButterKnife.reset(this);
    }

    class DeviceAdapter extends ArrayAdapter<Device> {

        public DeviceAdapter(Context context, List<Device> devices) {
            super(context, R.layout.content_user_device, devices);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            final ViewHolder holder;
            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                view = inflater.inflate(R.layout.content_user_device, parent, false);
                holder = new ViewHolder(view, getItem(position));
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
                holder.setData(getItem(position));
            }

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
                try {
                    model.setText(device.getDeviceModel().getName());
                } catch (DeviceModelsException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
