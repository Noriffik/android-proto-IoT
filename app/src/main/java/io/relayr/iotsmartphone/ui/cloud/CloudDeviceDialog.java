package io.relayr.iotsmartphone.ui.cloud;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.relayr.android.RelayrSdk;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.storage.Constants;
import io.relayr.iotsmartphone.storage.Storage;
import io.relayr.iotsmartphone.ui.utils.UiUtil;
import io.relayr.java.helper.observer.SimpleObserver;
import io.relayr.java.model.Device;
import rx.android.schedulers.AndroidSchedulers;

public class CloudDeviceDialog extends LinearLayout {

    @BindView(R.id.cloud_device_id) TextView mIdTv;
    @BindView(R.id.cloud_device_name) EditText mNameEt;
    @BindView(R.id.cloud_device_description) EditText mDescriptionEt;

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
        ButterKnife.bind(this, this);

        setInfo();
        setActions();
    }

    @Override protected void onDetachedFromWindow() {
        updateName();
        updateDescription();
        super.onDetachedFromWindow();
    }

    private void setInfo() {
        mIdTv.setText(mDevice.getId());
        mNameEt.setText(mDevice.getName());
        mDescriptionEt.setText(mDevice.getDescription());
    }

    private void setActions() {
        mNameEt.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            updateName();
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
                            updateDescription();
                            return true;
                        }
                        return false;
                    }
                });
    }

    private void updateName() {
        UiUtil.hideKeyboard(getContext(), mNameEt);
        final String newName = mNameEt.getText().toString();
        if (newName.equals(mDevice.getName())) return;

        mDevice.setName(newName);
        RelayrSdk.getDeviceApi()
                .updateDevice(mDevice.getId(), mDevice)
                .timeout(5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleObserver<Device>() {
                    @Override public void error(Throwable e) {
                        Toast.makeText(getContext(), R.string.dialog_device_name_update_failed, Toast.LENGTH_LONG).show();
                    }

                    @Override public void success(Device device) {
                        Storage.instance().saveDevice(device, mType);
                        Storage.instance().updateDeviceName(device.getName(), mType);
                        Toast.makeText(getContext(), R.string.update_success, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateDescription() {
        UiUtil.hideKeyboard(getContext(), mDescriptionEt);
        final String newDesc = mDescriptionEt.getText().toString();
        if (newDesc.equals(mDevice.getDescription())) return;

        mDevice.setDescription(newDesc);
        RelayrSdk.getDeviceApi()
                .updateDevice(mDevice.getId(), mDevice)
                .timeout(5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleObserver<Device>() {
                    @Override public void error(Throwable e) {
                        Toast.makeText(getContext(), R.string.something_went_wrong, Toast.LENGTH_LONG).show();
                    }

                    @Override public void success(Device device) {
                        Storage.instance().saveDevice(device, mType);
                        Toast.makeText(getContext(), R.string.update_success, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
