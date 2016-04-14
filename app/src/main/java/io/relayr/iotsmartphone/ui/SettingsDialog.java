package io.relayr.iotsmartphone.ui;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.storage.Storage;

public class SettingsDialog extends LinearLayout {

    @InjectView(R.id.cloud_upload) SwitchCompat mUploadSwitch;
    @InjectView(R.id.cloud_uploading) TextView mUploading;

    public SettingsDialog(Context context) {
        this(context, null);
    }

    public SettingsDialog(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SettingsDialog(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ButterKnife.inject(this, this);

        setState();
        mUploadSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Storage.instance().activeInBackground(isChecked);
                setState();
            }
        });
    }

    private void setState() {
        if (Storage.instance().isActiveInBackground()) {
            mUploadSwitch.setChecked(true);
            mUploading.setTextColor(ContextCompat.getColor(getContext(), R.color.accent));
        } else {
            mUploadSwitch.setChecked(false);
            mUploading.setTextColor(ContextCompat.getColor(getContext(), R.color.text_color));
        }
    }
}
