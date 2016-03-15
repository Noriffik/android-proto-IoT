package io.relayr.iotsmartphone.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.SeekBar;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.Storage;

public class SettingsView extends BasicView {

    @InjectView(R.id.publish_delay) SeekBar mPublishDelay;
    @InjectView(R.id.publish_delay_info) TextView mPublishInfo;
    @InjectView(R.id.acceleration_intensity) SeekBar mAccelerationIntensity;
    @InjectView(R.id.acceleration_info) TextView mAccelerationInfo;

    public SettingsView(Context context) {
        super(context);
    }

    public SettingsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SettingsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ButterKnife.inject(this);

        mPublishDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mPublishInfo.setText(getContext().getString(R.string.stv_delay, (progress + 1)));
                Storage.instance().saveDelay(progress + 1);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        mPublishInfo.setText(getContext().getString(R.string.stv_delay, Storage.instance().loadDelay()));
        mPublishDelay.setProgress(Storage.instance().loadDelay() - 1);

        mAccelerationIntensity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int intensity, boolean fromUser) {
                mAccelerationInfo.setText(intensity == 0 ? getContext().getString(R.string.stv_low) :
                        intensity == 1 ? getContext().getString(R.string.stv_normal) :
                                getContext().getString(R.string.stv_high));
                Storage.instance().saveIntensity(intensity);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        int intensity = Storage.instance().loadIntensity();
        mAccelerationInfo.setText(intensity == 0 ? getContext().getString(R.string.stv_low) :
                intensity == 1 ? getContext().getString(R.string.stv_normal) :
                        getContext().getString(R.string.stv_high));
        mAccelerationIntensity.setProgress(intensity);
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ButterKnife.reset(this);
    }
}
