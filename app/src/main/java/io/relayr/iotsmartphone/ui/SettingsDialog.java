package io.relayr.iotsmartphone.ui;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.storage.Storage;
import io.relayr.iotsmartphone.ui.utils.TutorialUtil;

import static io.relayr.iotsmartphone.storage.Storage.TUTORIAL;

public class SettingsDialog extends LinearLayout {

    @InjectView(R.id.cloud_upload) SwitchCompat mUploadSwitch;
    @InjectView(R.id.cloud_uploading) TextView mUploading;
    @InjectView(R.id.cloud_upload_warning) View mUploadingWarning;

    @InjectView(R.id.tutorial) SwitchCompat mTutorialSwitch;
    @InjectView(R.id.tutorial_state) TextView mTutorial;

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

        mUploadSwitch.setChecked(Storage.instance().isActiveInBackground());
        mUploadSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Storage.instance().activeInBackground(isChecked);
                setText(mUploading, isChecked);
                mUploadingWarning.setVisibility(isChecked ? VISIBLE : GONE);
            }
        });
        mUploadingWarning.setVisibility(Storage.instance().isActiveInBackground() ? VISIBLE : GONE);

        mTutorialSwitch.setChecked(!Storage.instance().tutorialFinished(TUTORIAL));
        mTutorialSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TutorialUtil.updateTutorial(TUTORIAL, !isChecked);
                setText(mTutorial, !isChecked);
            }
        });
    }

    private void setText(TextView controlInfo, boolean status) {
        if (status)
            controlInfo.setTextColor(ContextCompat.getColor(getContext(), R.color.accent));
        else
            controlInfo.setTextColor(ContextCompat.getColor(getContext(), R.color.text_color));
    }
}
