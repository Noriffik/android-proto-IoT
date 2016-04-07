package io.relayr.iotsmartphone.tabs.cloud;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import io.relayr.android.RelayrSdk;
import io.relayr.android.storage.DataStorage;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.tabs.helper.UiHelper;
import io.relayr.iotsmartphone.tabs.helper.Constants;
import io.relayr.iotsmartphone.tabs.helper.SettingsStorage;
import io.relayr.java.helper.observer.SimpleObserver;
import io.relayr.java.model.CreateDevice;
import io.relayr.java.model.Device;
import io.relayr.java.model.User;
import rx.android.schedulers.AndroidSchedulers;

import static io.relayr.iotsmartphone.tabs.helper.Constants.DeviceType.PHONE;

public class FragmentCloud extends Fragment {

    private static final String TAG = "FragCloud";

    @InjectView(R.id.cloud) ImageView mCloudImg;
    @InjectView(R.id.cloud_connection) View mCloudConnection;
    @InjectView(R.id.cloud_info) TextView mCloudInfoText;
    @InjectView(R.id.cloud_button) Button mCloudInfoBtn;

    @InjectView(R.id.phone_info_name) TextView mPhoneName;
    @InjectView(R.id.phone_info_version) TextView mPhoneVersion;

    @InjectView(R.id.watch) ImageView mWatchImg;
    @InjectView(R.id.watch_connection) View mWatchConnection;
    @InjectView(R.id.watch_info_name) TextView mWatchName;
    @InjectView(R.id.watch_info_version) TextView mWatchVersion;

    private ProgressDialog mProgress;

    public FragmentCloud() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        final View view = inflater.inflate(R.layout.activity_tab_fragment_cloud, container, false);
        ButterKnife.inject(this, view);

        setUpCloud();
        setUpPhone();
        setUpWearable();

        if (UiHelper.isCloudConnected()) loadUserInfo();

        return view;
    }

    @SuppressWarnings("unused") @OnClick(R.id.cloud_button)
    public void onButtonClick() {
        if (!UiHelper.isCloudConnected()) logIn();
        else
            new AlertDialog.Builder(getActivity(), R.style.AppTheme_DialogOverlay)
                    .setTitle(getActivity().getString(R.string.cloud_log_out))
                    .setMessage(getActivity().getString(R.string.cloud_log_out_message))
                    .setPositiveButton(getActivity().getString(R.string.yes), new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            logOut();
                        }
                    })
                    .setNegativeButton(getActivity().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create()
                    .show();
    }

    private void setUpCloud() {
        if (UiHelper.isCloudConnected()) {
            mCloudImg.setBackgroundResource(R.drawable.cloud_connected_circle);
            mCloudConnection.setBackgroundResource(R.drawable.cloud_line);
            mCloudInfoText.setVisibility(View.GONE);
            mCloudInfoBtn.setText(R.string.cloud_log_out);

            //            Animation rotation = AnimationUtils.loadAnimation(getActivity(), R.anim.cloud_rotate_animation);
            //            rotation.setRepeatCount(Animation.INFINITE);
            //            mCloudImg.startAnimation(rotation);
        } else {
            mCloudImg.setBackgroundResource(R.drawable.cloud_disconnected_circle);
            mCloudConnection.setBackgroundResource(R.drawable.cloud_dotted_vertical_line);
            mCloudInfoText.setVisibility(View.VISIBLE);
            mCloudInfoBtn.setText(R.string.cloud_log_in);
        }
    }

    private void setUpPhone() {
        mPhoneName.setText(SettingsStorage.instance().getDeviceName(PHONE));
        mPhoneVersion.setText(getString(R.string.cloud_phone_version, SettingsStorage.instance().getDeviceSdk(PHONE)));
    }

    private void setUpWearable() {
        if (UiHelper.isWearableConnected(getActivity())) {
            mWatchImg.setBackgroundResource(R.drawable.cloud_circle);
            mWatchConnection.setBackgroundResource(R.drawable.cloud_line);
            mWatchName.setText("Watch");
            mWatchVersion.setText("Version");
        } else {
            mWatchImg.setBackgroundResource(R.drawable.cloud_circle_dark);
            mWatchConnection.setBackgroundResource(R.drawable.cloud_dotted_vertical_line);
            mWatchName.setText(R.string.cloud_watch_info);
            mWatchVersion.setVisibility(View.GONE);
        }
    }

    private void logIn() {
        RelayrSdk.logIn(getActivity())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleObserver<User>() {
                    @Override public void error(Throwable e) {
                        UiHelper.showSnackBar(getActivity(), R.string.cloud_log_in_failed);

                        Crashlytics.log(Log.DEBUG, TAG, "Login failed.");
                        if (e instanceof TimeoutException) logIn();
                        else Crashlytics.logException(e);
                    }

                    @Override public void success(User user) {
                        loadUserInfo();
                        setUpCloud();
                    }
                });
    }

    private void loadUserInfo() {
        RelayrSdk.getUser()
                .timeout(5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleObserver<User>() {
                    @Override public void error(Throwable e) {
                        UiHelper.showSnackBar(getActivity(), R.string.cloud_error_user_data);

                        Crashlytics.log(Log.DEBUG, TAG, "Failed to load user info.");
                        if (e instanceof TimeoutException) loadUserInfo();
                        else Crashlytics.logException(e);
                    }

                    @Override public void success(User user) {
                        loadDevices(user);
                    }
                });
    }

    private void loadDevices(User user) {
        if (SettingsStorage.instance().getDeviceId(PHONE) != null)
            getDeviceFromCloud(user, PHONE);
        else
            createDevice(PHONE);
    }

    private void getDeviceFromCloud(User user, final Constants.DeviceType type) {
        showProgress(R.string.cloud_progress_loading_devices);

        Crashlytics.log(Log.DEBUG, TAG, "Fetch " + SettingsStorage.instance().getDeviceId(type));

        user.getDevice(SettingsStorage.instance().getDeviceId(type))
                .timeout(5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleObserver<Device>() {
                    @Override public void error(Throwable e) {
                        hideProgress();
                        UiHelper.showSnackBar(getActivity(), R.string.cloud_error_device_data);
                        Crashlytics.log(Log.DEBUG, TAG, "Failed to load " + type.name() + " device.");
                    }

                    @Override public void success(Device device) {
                        refreshDevice(device, type);
                    }
                });
    }

    private void createDevice(final Constants.DeviceType type) {
        showProgress(R.string.cloud_progress_creating_device);

        final String name = SettingsStorage.instance().getDeviceName(type);
        final String modelId = type == PHONE ? SettingsStorage.MODEL_PHONE : SettingsStorage.MODEL_WATCH;
        final CreateDevice toCreate = new CreateDevice(name, modelId, DataStorage.getUserId(), null, "1.0.0");

        RelayrSdk.getDeviceApi()
                .createDevice(toCreate)
                .timeout(5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleObserver<Device>() {
                    @Override public void error(Throwable e) {
                        hideProgress();
                        UiHelper.showSnackBar(getActivity(), R.string.cloud_error_create_device);
                        Crashlytics.log(Log.DEBUG, TAG, "Failed to create " + type.name() + " device.");
                    }

                    @Override public void success(Device device) {
                        Crashlytics.log(Log.DEBUG, TAG, "Created device " + device.getId());
                        refreshDevice(device, type);
                    }
                });
    }

    private void refreshDevice(Device device, Constants.DeviceType type) {
        SettingsStorage.instance().saveDevice(device, type);
        if (type == PHONE) setUpPhone();
        else setUpWearable();
        hideProgress();

        UiHelper.showSnackBar(getActivity(), R.string.cloud_device_success);
    }

    private void logOut() {
        SettingsStorage.instance().logOut();
        RelayrSdk.logOut();
        setUpCloud();
    }

    private void showProgress(int stringId) {
        if (mProgress == null)
            mProgress = ProgressDialog.show(getActivity(), getActivity().getString(R.string.cloud_progress_title),
                    getActivity().getString(stringId), true);
        else mProgress.setMessage(getActivity().getString(stringId));
    }

    private void hideProgress() {
        if (mProgress != null) mProgress.dismiss();
        mProgress = null;
    }
}
