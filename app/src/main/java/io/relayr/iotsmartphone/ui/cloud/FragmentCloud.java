package io.relayr.iotsmartphone.ui.cloud;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.ButterKnife;
import butterknife.BindView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import io.relayr.android.RelayrSdk;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.handler.CloudHandler;
import io.relayr.iotsmartphone.handler.ReadingHandler;
import io.relayr.iotsmartphone.storage.Constants;
import io.relayr.iotsmartphone.storage.Storage;
import io.relayr.iotsmartphone.ui.utils.TutorialUtil;
import io.relayr.iotsmartphone.ui.utils.UiUtil;
import io.relayr.java.helper.observer.SimpleObserver;
import io.relayr.java.model.Device;
import rx.android.schedulers.AndroidSchedulers;

import static io.relayr.iotsmartphone.storage.Constants.DeviceType.PHONE;
import static io.relayr.iotsmartphone.storage.Constants.DeviceType.WATCH;

public class FragmentCloud extends Fragment {

    private static final String TAG = "FragCloud";

    @BindView(R.id.cloud) ImageView mCloudImg;
    @BindView(R.id.cloud_connection) View mCloudConnection;
    @BindView(R.id.cloud_connection_speed) TextView mCloudSpeed;
    @BindView(R.id.cloud_info) TextView mCloudInfoText;
    @BindView(R.id.cloud_button) Button mCloudBtn;

    @BindView(R.id.phone_info_name) TextView mPhoneName;
    @BindView(R.id.phone_info_version) TextView mPhoneVersion;

    @BindView(R.id.watch) ImageView mWatchImg;
    @BindView(R.id.watch_connection) View mWatchConnection;
    @BindView(R.id.watch_connection_speed) TextView mWatchSpeed;
    @BindView(R.id.watch_info_name) TextView mWatchName;
    @BindView(R.id.watch_info_version) TextView mWatchVersion;

    private TimerTask mSpeedTimer;
    private AlertDialog mWarningDialog;
    private AlertDialog mLoadingDialog;

    public FragmentCloud() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        final View view = inflater.inflate(R.layout.activity_tab_fragment_cloud, container, false);
        ButterKnife.bind(this, view);

        setUpCloud();
        setUpPhone();
        setUpWearable();

        loadDeviceData();

        return view;
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (mWarningDialog != null) mWarningDialog.dismiss();
        mWarningDialog = null;
        if (mLoadingDialog != null) mLoadingDialog.dismiss();
        mLoadingDialog = null;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            setSpeedTimer();
        } else {
            if (mSpeedTimer != null) mSpeedTimer.cancel();
            mSpeedTimer = null;
            if (mWarningDialog != null) mWarningDialog.dismiss();
            mWarningDialog = null;
        }
    }

    @SuppressWarnings("unused") @OnClick(R.id.cloud_button)
    public void onButtonClick() {
        if (!UiUtil.isCloudConnected())
            CloudHandler.logIn(getActivity())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SimpleObserver<Pair<Constants.DeviceType, Device>>() {
                        @Override public void error(Throwable e) {}

                        @Override public void success(Pair<Constants.DeviceType, Device> pair) {
                            EventBus.getDefault().post(new Constants.Tutorial(1));
                            if (pair != null) setUpDevice(pair.second, pair.first);
                            setUpCloud();
                        }
                    });
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
        if (UiUtil.isCloudConnected())
            new AlertDialog.Builder(getContext(), R.style.AppTheme_DialogOverlay)
                    .setView(View.inflate(getContext(), R.layout.dialog_cloud_user, null))
                    .setTitle(getString(R.string.cloud_user_dialog_title))
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
        if (UiUtil.isCloudConnected()) UiUtil.openDashboard(getContext());
    }

    private void loadDeviceData() {
        if (UiUtil.isCloudConnected())
            CloudHandler.loadDevices(getActivity())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SimpleObserver<Pair<Constants.DeviceType, Device>>() {
                        @Override public void error(Throwable e) {
                            if (mLoadingDialog != null) mLoadingDialog.dismiss();
                            mLoadingDialog = new AlertDialog.Builder(getContext(), R.style.AppTheme_DialogOverlay)
                                    .setTitle(getString(R.string.something_went_wrong))
                                    .setPositiveButton(getString(R.string.retry), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            loadDeviceData();
                                        }
                                    })
                                    .create();
                            mLoadingDialog.show();
                        }

                        @Override public void success(Pair<Constants.DeviceType, Device> pair) {
                            setUpDevice(pair.second, pair.first);
                            if (mLoadingDialog != null) mLoadingDialog.dismiss();
                            showWarning();
                        }
                    });
    }

    private void setSpeedTimer() {
        if (mSpeedTimer != null) mSpeedTimer.cancel();
        mSpeedTimer = new TimerTask() {
            @Override public void run() {
                ReadingHandler.calculateSpeeds();
                if (getActivity() != null)
                    getActivity().runOnUiThread(new TimerTask() {
                        @Override public void run() {
                            mWatchSpeed.setText(getSpeed(ReadingHandler.sWatchSpeed));
                            if (UiUtil.isCloudConnected())
                                mCloudSpeed.setText(getSpeed(ReadingHandler.sPhoneSpeed + ReadingHandler.sWatchSpeed));
                        }
                    });
            }
        };
        new Timer().scheduleAtFixedRate(mSpeedTimer, 1000, 5000);
    }

    private String getSpeed(float speed) {
        if (speed <= 0) return "";
        if (speed > 1024) return String.format("%.2f KB/s", speed / 1024f);
        else return String.format("%.2f B/s", speed);
    }

    private void showDeviceDialog(Constants.DeviceType type) {
        final Device device = Storage.instance().getDevice(type);
        if (device == null) {
            if (type == PHONE)
                UiUtil.showSnackBar(getActivity(), R.string.cloud_establish_connection);
            else UiUtil.showSnackBar(getActivity(), R.string.cloud_no_wearable);
            return;
        }

        final CloudDeviceDialog view = (CloudDeviceDialog) View.inflate(getContext(), R.layout.dialog_cloud_device, null);
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
        if (UiUtil.isCloudConnected()) {
            mCloudImg.setBackgroundResource(R.drawable.cloud_connected_circle);
            mCloudConnection.setBackgroundResource(R.drawable.cloud_line);
            mCloudInfoText.setText(R.string.cloud_connection_established);
            mCloudBtn.setText(R.string.cloud_log_out);
            EventBus.getDefault().post(new Constants.CloudConnected());
        } else {
            mCloudImg.setBackgroundResource(R.drawable.cloud_disconnected_circle);
            mCloudConnection.setBackgroundResource(R.drawable.cloud_dotted_vertical_line);
            mCloudInfoText.setText(R.string.cloud_establish_connection);
            mCloudBtn.setText(R.string.cloud_log_in);
        }
    }

    private void setUpPhone() {
        mPhoneName.setText(Storage.instance().getDeviceName(PHONE));
        mPhoneVersion.setText(getString(R.string.cloud_phone_version, Storage.instance().getDeviceSdk(PHONE)));
    }

    private void setUpWearable() {
        if (UiUtil.isWearableConnected(getActivity())) {
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

    private void setUpDevice(Device device, Constants.DeviceType type) {
        Storage.instance().saveDevice(device, type);
        if (type == PHONE) setUpPhone();
        else setUpWearable();

        UiUtil.showSnackBar(getActivity(), R.string.cloud_device_success);
        EventBus.getDefault().post(new Constants.CloudConnected());
    }

    private void logOut() {
        mCloudSpeed.setText("");
        RelayrSdk.logOut();
        Storage.instance().logOut();
        setUpCloud();
    }

    private void showWarning() {
        if (mWarningDialog == null)
            mWarningDialog = new AlertDialog.Builder(getContext(), R.style.AppTheme_DialogOverlay)
                    .setTitle(getContext().getResources().getString(R.string.cloud_upload_warning_title))
                    .setIcon(R.drawable.ic_warning)
                    .setMessage(getContext().getResources().getString(R.string.cloud_upload_warning_message))
                    .setPositiveButton(getContext().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int i) {
                            dialog.dismiss();
                        }
                    }).show();
    }
}
