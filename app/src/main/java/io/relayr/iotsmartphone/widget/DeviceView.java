package io.relayr.iotsmartphone.widget;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import io.relayr.android.RelayrSdk;
import io.relayr.android.storage.DataStorage;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.Storage;
import io.relayr.java.model.CreateDevice;
import io.relayr.java.model.Device;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static android.widget.Toast.LENGTH_LONG;

public class DeviceView extends BasicView {

    private static final String MODEL_ID = "86e0a7d7-5e18-449c-b7aa-f3b089c33b67";

    @InjectView(R.id.device_name) EditText mName;

    private ProgressDialog mCreateProgress;

    public DeviceView(Context context) {
        super(context);
    }

    public DeviceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DeviceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ButterKnife.inject(this);
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.device_create_btn)
    public void onCreateClicked() {
        if (mName.getText().length() == 0) return;

        mCreateProgress = ProgressDialog.show(getContext(), "Creating device",
                "Please wait...", true);

        final CreateDevice device = new CreateDevice(mName.getText().toString(), MODEL_ID, DataStorage.getUserId(), null, null);
        RelayrSdk.getDeviceApi()
                .createDevice(device)
                .timeout(5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Device>() {
                    @Override public void onCompleted() {}

                    @Override public void onError(Throwable e) {
                        if (mCreateProgress != null) mCreateProgress.dismiss();
                        Toast.makeText(getContext(), "Something went wrong... Please try again.", LENGTH_LONG).show();
                        e.printStackTrace();
                    }

                    @Override public void onNext(Device device) {
                        if (mCreateProgress != null) mCreateProgress.dismiss();

                        Storage.instance().saveDevice(device);
                        mListener.onDeviceCreated();
                    }
                });
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        hideKeyboard();
        ButterKnife.reset(this);
    }

    private void hideKeyboard() {
        if (mName != null) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mName.getWindowToken(), 0);
        }
    }
}
