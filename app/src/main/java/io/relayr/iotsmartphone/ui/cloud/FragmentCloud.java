package io.relayr.iotsmartphone.ui.cloud;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import io.relayr.android.RelayrSdk;
import io.relayr.android.storage.DataStorage;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.ui.IotFragment;
import io.relayr.iotsmartphone.storage.Constants;
import io.relayr.iotsmartphone.storage.Storage;
import io.relayr.iotsmartphone.utils.ReadingUtils;
import io.relayr.iotsmartphone.utils.UiHelper;
import io.relayr.java.helper.observer.SimpleObserver;
import io.relayr.java.model.CreateDevice;
import io.relayr.java.model.Device;
import io.relayr.java.model.User;
import rx.android.schedulers.AndroidSchedulers;

import static io.relayr.iotsmartphone.storage.Constants.DeviceType.PHONE;
import static io.relayr.iotsmartphone.storage.Constants.DeviceType.WATCH;

public class FragmentCloud extends IotFragment {

    private static final String TAG = "FragCloud";

    @InjectView(R.id.cloud) ImageView mCloudImg;
    @InjectView(R.id.cloud_connection) View mCloudConnection;
    @InjectView(R.id.cloud_connection_speed) TextView mCloudSpeed;
    @InjectView(R.id.cloud_info) TextView mCloudInfoText;
    @InjectView(R.id.cloud_button) Button mCloudInfoBtn;

    @InjectView(R.id.phone_info_name) TextView mPhoneName;
    @InjectView(R.id.phone_info_version) TextView mPhoneVersion;

    @InjectView(R.id.watch) ImageView mWatchImg;
    @InjectView(R.id.watch_connection) View mWatchConnection;
    @InjectView(R.id.watch_connection_speed) TextView mWatchSpeed;
    @InjectView(R.id.watch_info_name) TextView mWatchName;
    @InjectView(R.id.watch_info_version) TextView mWatchVersion;

    private ProgressDialog mProgress;
    private TimerTask mSpeedTimer;

    public FragmentCloud() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        final View view = inflater.inflate(R.layout.activity_tab_fragment_cloud, container, false);
        ButterKnife.inject(this, view);

        setUpCloud();
        setUpPhone();
        setUpWearable();

        setSpeedTimer();

        if (UiHelper.isCloudConnected()) loadUserInfo();

        return view;
    }

    @Override public void onResume() {
        super.onResume();
        setTitle(getString(R.string.cloud_title));
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (mSpeedTimer != null) mSpeedTimer.cancel();
        mSpeedTimer = null;
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

    @SuppressWarnings("unused") @OnClick(R.id.cloud)
    public void onCloudClick() {
        new AlertDialog.Builder(getContext(), R.style.AppTheme_DialogOverlay)
                .setView(View.inflate(getContext(), R.layout.cloud_user_dialog, null))
                .setTitle(getString(R.string.cloud_device_dialog_title))
                .setPositiveButton(getString(R.string.close), new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }

    @SuppressWarnings("unused") @OnClick(R.id.phone)
    public void onPhoneClick() {showDeviceDialog(PHONE);}

    @SuppressWarnings("unused") @OnClick(R.id.watch)
    public void onWatchClick() {showDeviceDialog(WATCH);}

    @SuppressWarnings("unused") @OnClick(R.id.cloud_info)
    public void onLinkClick() {
        if (UiHelper.isCloudConnected()) UiHelper.openDashboard(getContext());
    }

    private void setSpeedTimer() {
        if (mSpeedTimer != null) mSpeedTimer.cancel();
        mSpeedTimer = new TimerTask() {
            @Override public void run() {
                ReadingUtils.calculateSpeeds();
                getActivity().runOnUiThread(new TimerTask() {
                    @Override public void run() {
                        mWatchSpeed.setText(getSpeed(ReadingUtils.sWatchSpeed));
                        mCloudSpeed.setText(getSpeed(ReadingUtils.sPhoneSpeed + ReadingUtils.sWatchSpeed));
                    }
                });
            }
        };
        new Timer().scheduleAtFixedRate(mSpeedTimer, 5000, 5000);
    }

    private String getSpeed(float speed) {
        if (speed <= 0) return "";
        if (speed > 1024) return String.format("%.2f KB/s", speed / 1024f);
        else return String.format("%.2f B/s", speed);
    }

    private void showDeviceDialog(Constants.DeviceType type) {
        final Device device = Storage.instance().getDevice(type);
        if (device == null) return;

        final CloudDeviceDialog view = (CloudDeviceDialog) View.inflate(getContext(), R.layout.cloud_device_dialog, null);
        view.setUp(device, type);

        new AlertDialog.Builder(getContext(), R.style.AppTheme_DialogOverlay)
                .setView(view)
                .setTitle(getString(R.string.cloud_device_dialog_title))
                .setPositiveButton(getString(R.string.close), new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override public void onDismiss(DialogInterface dialog) {
                        setUpPhone();
                        setUpWearable();
                    }
                })
                .create()
                .show();
    }

    private void setUpCloud() {
        if (UiHelper.isCloudConnected()) {
            mCloudImg.setBackgroundResource(R.drawable.cloud_connected_circle);
            mCloudConnection.setBackgroundResource(R.drawable.cloud_line);
            mCloudInfoText.setText(R.string.cloud_connection_established);
            mCloudInfoBtn.setText(R.string.cloud_log_out);
        } else {
            mCloudImg.setBackgroundResource(R.drawable.cloud_disconnected_circle);
            mCloudConnection.setBackgroundResource(R.drawable.cloud_dotted_vertical_line);
            mCloudInfoText.setText(R.string.cloud_establish_connection);
            mCloudInfoBtn.setText(R.string.cloud_log_in);
        }
    }

    private void setUpPhone() {
        mPhoneName.setText(Storage.instance().getDeviceName(PHONE));
        mPhoneVersion.setText(getString(R.string.cloud_phone_version, Storage.instance().getDeviceSdk(PHONE)));
    }

    private void setUpWearable() {
        if (UiHelper.isWearableConnected(getActivity())) {
            mWatchImg.setBackgroundResource(R.drawable.cloud_circle);
            mWatchConnection.setBackgroundResource(R.drawable.cloud_line);
            mWatchName.setText(Storage.instance().getDeviceName(WATCH));
            mWatchVersion.setText(getString(R.string.cloud_phone_version, Storage.instance().getDeviceSdk(WATCH)));
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
                        EventBus.getDefault().post(new Constants.LoggedIn());
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
        if (Storage.instance().getDeviceId(PHONE) != null)
            getDeviceFromCloud(user, PHONE);
        else
            createDevice(PHONE);

        if (!UiHelper.isWearableConnected(getActivity())) return;
        if (Storage.instance().getDeviceId(WATCH) != null)
            getDeviceFromCloud(user, WATCH);
        else
            createDevice(WATCH);
    }

    private void getDeviceFromCloud(User user, final Constants.DeviceType type) {
        showProgress(R.string.cloud_progress_loading_devices);

        Crashlytics.log(Log.DEBUG, TAG, "Fetch " + Storage.instance().getDeviceId(type));

        user.getDevice(Storage.instance().getDeviceId(type))
                .timeout(5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleObserver<Device>() {
                    @Override public void error(Throwable e) {
                        hideProgress();
                        UiHelper.showSnackBar(getActivity(), R.string.cloud_error_device_data);
                        Crashlytics.log(Log.DEBUG, TAG, "Failed to load " + type.name() + " device.");
                        if (!(e instanceof TimeoutException)) {
                            loadUserInfo();
                            Crashlytics.logException(e);
                        }
                    }

                    @Override public void success(Device device) {
                        refreshDevice(device, type);
                    }
                });
    }

    private void createDevice(final Constants.DeviceType type) {
        showProgress(R.string.cloud_progress_creating_device);

        final String name = Storage.instance().getDeviceName(type);
        final String modelId = type == PHONE ? Storage.MODEL_PHONE : Storage.MODEL_WATCH;
        final String description = type == PHONE ? getString(R.string.app_title_phone) : getString(R.string.app_title_watch);
        final CreateDevice toCreate = new CreateDevice(name, description, modelId, DataStorage.getUserId(), null, "1.0.0");

        RelayrSdk.getDeviceApi()
                .createDevice(toCreate)
                .timeout(5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleObserver<Device>() {
                    @Override public void error(Throwable e) {
                        if (e instanceof TimeoutException) createDevice(type);
                        else {
                            hideProgress();
                            UiHelper.showSnackBar(getActivity(), R.string.cloud_error_create_device);
                            Crashlytics.log(Log.DEBUG, TAG, "Failed to create " + type.name() + " device.");
                            Crashlytics.logException(e);
                        }
                    }

                    @Override public void success(Device device) {
                        Crashlytics.log(Log.DEBUG, TAG, "Created device " + device.getId());
                        refreshDevice(device, type);
                    }
                });
    }

    private void refreshDevice(Device device, Constants.DeviceType type) {
        Storage.instance().saveDevice(device, type);
        if (type == PHONE) setUpPhone();
        else setUpWearable();
        hideProgress();

        UiHelper.showSnackBar(getActivity(), R.string.cloud_device_success);
    }

    private void logOut() {
        Storage.instance().logOut();
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
