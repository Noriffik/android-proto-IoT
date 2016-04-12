package io.relayr.iotsmartphone.tabs.cloud;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.relayr.android.RelayrSdk;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.tabs.helper.Constants;
import io.relayr.iotsmartphone.tabs.helper.SettingsStorage;
import io.relayr.java.helper.observer.SimpleObserver;
import io.relayr.java.model.Device;
import rx.android.schedulers.AndroidSchedulers;

public class CloudDeviceDialog extends LinearLayout {

    @InjectView(R.id.cloud_device_id) TextView mIdTv;
    @InjectView(R.id.cloud_device_name) EditText mNameEt;
    @InjectView(R.id.cloud_device_description) EditText mDescriptionEt;

    private Device mDevice;
    private Constants.DeviceType mType;

    public CloudDeviceDialog(Context context) {
        this(context, null);
    }

    public CloudDeviceDialog(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CloudDeviceDialog(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setUp(Device device, Constants.DeviceType type) {
        this.mDevice = device;
        this.mType = type;
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ButterKnife.inject(this, this);

        setInfo();
        setActions();
    }

    private void setActions() {
        mNameEt.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            final String newName = mNameEt.getText().toString();
                            if (newName.equals(mDevice.getName())) return false;

                            mDevice.setName(newName);
                            RelayrSdk.getDeviceApi()
                                    .updateDevice(mDevice.getId(), mDevice)
                                    .timeout(5, TimeUnit.SECONDS)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new SimpleObserver<Device>() {
                                        @Override public void error(Throwable e) {
                                            Toast.makeText(getContext(), "Failed to update device name!", Toast.LENGTH_LONG).show();
                                        }

                                        @Override public void success(Device device) {
                                            SettingsStorage.instance().saveDevice(device, mType);
                                            SettingsStorage.instance().updateDeviceName(device.getName(), mType);
                                            Toast.makeText(getContext(), "Device name updated!", Toast.LENGTH_LONG).show();
                                        }
                                    });
                            return true;
                        }
                        return false;
                    }
                });

        mDescriptionEt.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            final String newDesc = mDescriptionEt.getText().toString();
                            if (newDesc.equals(mDevice.getDescription())) return false;

                            mDevice.setDescription(newDesc);
                            RelayrSdk.getDeviceApi()
                                    .updateDevice(mDevice.getId(), mDevice)
                                    .timeout(5, TimeUnit.SECONDS)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new SimpleObserver<Device>() {
                                        @Override public void error(Throwable e) {
                                            Toast.makeText(getContext(), "Failed to update description!", Toast.LENGTH_LONG).show();
                                        }

                                        @Override public void success(Device device) {
                                            SettingsStorage.instance().saveDevice(device, mType);
                                            Toast.makeText(getContext(), "Device description updated!", Toast.LENGTH_LONG).show();
                                        }
                                    });
                            return true;
                        }
                        return false;
                    }
                });
    }

    private void setInfo() {
        mIdTv.setText(mDevice.getId());
        mNameEt.setText(mDevice.getName());
        mDescriptionEt.setText(mDevice.getDescription());
    }
}
