package io.relayr.iotsmartphone.tabs.readings;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.tabs.helper.Constants;
import io.relayr.iotsmartphone.tabs.helper.SettingsStorage;

import static android.widget.Toast.LENGTH_LONG;
import static io.relayr.iotsmartphone.tabs.helper.Constants.DeviceType.PHONE;
import static io.relayr.iotsmartphone.tabs.helper.Constants.DeviceType.WATCH;

public class DialogView extends LinearLayout {

    @InjectView(R.id.cloud_upload) SwitchCompat mSwitch;
    @InjectView(R.id.frequency_seek) SeekBar mSeek;
    @InjectView(R.id.frequency_info) TextView mSeekInfo;

    private String mMeaning;
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

    public void setMeaning(String meaning, Constants.DeviceType type) {
        this.mMeaning = meaning;
        this.mType = type;
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ButterKnife.inject(this, this);

        mSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int frequency, boolean fromUser) {
                mSeekInfo.setText((frequency + 1) + " sec");
                SettingsStorage.instance().saveFrequency(mMeaning, mType, frequency + 1);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        final int frequency = SettingsStorage.instance().loadFrequency(mMeaning, mType);
        mSeek.setProgress(frequency - 1);
        mSeekInfo.setText(frequency + " sec");

        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mMeaning.equals("acceleration") || mMeaning.equals("angularSpeed"))
                    showAccelerometerWarning();
                SettingsStorage.instance().saveActivity(mMeaning, mType, isChecked);
            }
        });

        if (mType == PHONE) mSwitch.setChecked(SettingsStorage.ACTIVITY_PHONE.get(mMeaning));
        else if (mType == WATCH) mSwitch.setChecked(SettingsStorage.ACTIVITY_WATCH.get(mMeaning));
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
