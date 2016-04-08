package io.relayr.iotsmartphone.tabs.readings;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.tabs.helper.Constants;
import io.relayr.iotsmartphone.tabs.helper.ReadingUtils;
import io.relayr.iotsmartphone.tabs.helper.SettingsStorage;

import static android.widget.Toast.LENGTH_LONG;
import static io.relayr.iotsmartphone.tabs.helper.Constants.DeviceType.PHONE;
import static io.relayr.iotsmartphone.tabs.helper.Constants.DeviceType.WATCH;

public class DialogView extends LinearLayout {

    @InjectView(R.id.dialog_unit) TextView mUnitTv;
    @InjectView(R.id.dialog_identifier) TextView mIdentifierTv;

    @InjectView(R.id.sampling_low) TextView mSamplingLow;
    @InjectView(R.id.sampling_high) TextView mSamplingHigh;
    @InjectView(R.id.sampling_seek) SeekBar mSamplingSeek;
    @InjectView(R.id.sampling_info) TextView mSamplingInfo;

    @InjectView(R.id.cloud_local) TextView mCloudLocal;
    @InjectView(R.id.cloud_upload) SwitchCompat mSwitch;
    @InjectView(R.id.cloud_uploading) TextView mCloudUploading;

    private String mMeaning;
    private String mPath;
    private String mUnit;
    private Constants.DeviceType mType;

    public DialogView(Context context) {
        this(context, null);
    }

    public DialogView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DialogView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setUp(String meaning, String path, String unit, Constants.DeviceType type) {
        this.mMeaning = meaning;
        this.mPath = path;
        this.mUnit = unit;
        this.mType = type;
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ButterKnife.inject(this, this);

        setInfo();
        setSampling();
        setCloudSwitch();
    }

    private void setInfo() {
        mUnitTv.setText(mUnit);
        mIdentifierTv.setText((mPath == null ? "" : mPath.equals("/") ? mPath : mPath + "/") + mMeaning);
    }

    private void setSampling() {
        final boolean complex = ReadingUtils.isComplex(mMeaning);
        int frequency = 0;
        if (mType == PHONE) frequency = SettingsStorage.FREQS_PHONE.get(mMeaning);
        else if (mType == WATCH) frequency = SettingsStorage.FREQS_WATCH.get(mMeaning);
        setFrequency(frequency, complex);

        mSamplingLow.setText(complex ? getContext().getString(R.string.dialog_low) : (Constants.SAMPLING_MIN + " s"));
        mSamplingHigh.setText(complex ? getContext().getString(R.string.dialog_high) : (Constants.SAMPLING_MAX + " s"));

        mSamplingSeek.setMax(Constants.SAMPLING_MAX);
        if (complex)
            mSamplingSeek.setProgress((frequency / Constants.SAMPLING_COMPLEX) - 1);
        else mSamplingSeek.setProgress(frequency - 1);

        mSamplingSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int frequency, boolean fromUser) {
                final int freq = SettingsStorage.instance().saveFrequency(mMeaning, mType, frequency + 1);
                setFrequency(freq, complex);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setFrequency(int freq, boolean complex) {
        if (complex) mSamplingInfo.setText(freq >= 1000 ? (freq / 1000f) + " s" : freq + " ms");
        else mSamplingInfo.setText(freq + " s");
    }

    private void setCloudSwitch() {
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setSwitchInfo(isChecked);
                if (mMeaning.equals("acceleration") || mMeaning.equals("angularSpeed"))
                    showAccelerometerWarning();
                SettingsStorage.instance().saveActivity(mMeaning, mType, isChecked);
            }
        });

        boolean uploading = false;
        if (mType == PHONE) uploading = SettingsStorage.ACTIVITY_PHONE.get(mMeaning);
        else if (mType == WATCH) uploading = SettingsStorage.ACTIVITY_WATCH.get(mMeaning);

        mSwitch.setChecked(uploading);
        setSwitchInfo(uploading);
    }

    private void setSwitchInfo(boolean uploading) {
        mCloudLocal.setTextColor(ContextCompat.getColor(getContext(), uploading ? R.color.text_color : R.color.accent));
        mCloudUploading.setTextColor(ContextCompat.getColor(getContext(), uploading ? R.color.accent : R.color.text_color));
    }

    private void showAccelerometerWarning() {
        if (SettingsStorage.instance().isWarningShown())
            Toast.makeText(getContext(), getContext().getResources().getString(R.string.sv_warning_toast), LENGTH_LONG).show();
        else
            new AlertDialog.Builder(getContext(), R.style.AppTheme_DialogOverlay)
                    .setTitle(getContext().getResources().getString(R.string.sv_warning_dialog_title))
                    .setIcon(R.drawable.ic_warning)
                    .setMessage(getContext().getResources().getString(R.string.sv_warning_dialog_text))
                    .setPositiveButton(getContext().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int i) {
                            SettingsStorage.instance().warningShown();
                            dialog.dismiss();
                        }
                    }).show();
    }
}
