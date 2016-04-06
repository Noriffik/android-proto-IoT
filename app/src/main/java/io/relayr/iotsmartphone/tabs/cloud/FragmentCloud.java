package io.relayr.iotsmartphone.tabs.cloud;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.tabs.UiHelper;
import io.relayr.iotsmartphone.tabs.helper.SettingsStorage;

public class FragmentCloud extends Fragment {

    @InjectView(R.id.cloud) ImageView mCloudImg;
    @InjectView(R.id.cloud_connection) View mCloudConnection;
    @InjectView(R.id.cloud_info_text) TextView mCloudInfoText;
    @InjectView(R.id.cloud_info_button) Button mCloudInfoBtn;

    @InjectView(R.id.phone_info_name) TextView mPhoneName;
    @InjectView(R.id.phone_info_version) TextView mPhoneVersion;

    @InjectView(R.id.watch) ImageView mWatchImg;
    @InjectView(R.id.watch_connection) View mWatchConnection;
    @InjectView(R.id.watch_info_name) TextView mWatchName;
    @InjectView(R.id.watch_info_version) TextView mWatchVersion;

    public FragmentCloud() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        final View view = inflater.inflate(R.layout.activity_tab_fragment_cloud, container, false);
        ButterKnife.inject(this, view);

        mPhoneName.setText(SettingsStorage.instance().getPhoneManufacturer() + " " + SettingsStorage.instance().getPhoneModel());
        mPhoneVersion.setText(getString(R.string.cloud_phone_version, SettingsStorage.instance().getPhoneSdk()));

        setUpCloud();
        setUpWearable();
        return view;
    }

    private void setUpCloud() {
        if (isCloudConnected()) {
            mCloudImg.setBackgroundResource(R.drawable.cloud_circle);
            mCloudConnection.setBackgroundResource(R.drawable.cloud_line);
            mCloudInfoText.setVisibility(View.GONE);
            mCloudInfoBtn.setText(R.string.cloud_log_out);
        } else {
            mCloudImg.setBackgroundResource(R.drawable.cloud_circle_dark);
            mCloudConnection.setBackgroundResource(R.drawable.cloud_dotted_vertical_line);
            mCloudInfoText.setVisibility(View.VISIBLE);
            mCloudInfoBtn.setText(R.string.cloud_log_in);
        }
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

    public boolean isCloudConnected() {
        return false;
    }
}
