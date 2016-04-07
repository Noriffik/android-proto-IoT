package io.relayr.iotsmartphone.tabs.readings;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.tabs.helper.Constants;
import io.relayr.iotsmartphone.tabs.readings.widgets.ReadingWidget;
import io.relayr.java.model.models.transport.DeviceReading;

public class ReadingViewHolder extends RecyclerView.ViewHolder {

    private static final Map<String, String> sNameMap = new HashMap<>();

    @InjectView(R.id.reading_title) TextView mMeaningTv;

    private final Context mContext;
    private final ReadingWidget widget;
    private String mMeaning;
    private Constants.DeviceType mType;

    public ReadingViewHolder(ReadingWidget widget, Context context) {
        super(widget);
        this.widget = widget;
        this.mContext = context;

        ButterKnife.inject(this, widget);

        if (sNameMap.isEmpty()) {
            sNameMap.put("acceleration", context.getString(R.string.reading_title_acceleration));
            sNameMap.put("angularSpeed", context.getString(R.string.reading_title_gyro));
            sNameMap.put("batteryLevel", context.getString(R.string.reading_title_battery));
            sNameMap.put("luminosity", context.getString(R.string.reading_title_light));
            sNameMap.put("location", context.getString(R.string.reading_title_location));
            sNameMap.put("rssi", context.getString(R.string.reading_title_rssi));
            sNameMap.put("touch", context.getString(R.string.reading_title_touch));
        }
    }

    @SuppressWarnings("unused") @OnClick(R.id.reading_title)
    public void onTitleClick() {
        showSettings();
    }

    @SuppressWarnings("unused") @OnClick(R.id.reading_settings)
    public void onSettingsClick() {
        showSettings();
    }

    private void showSettings() {
        final DialogView view = (DialogView) View.inflate(mContext, R.layout.dialog_content, null);
        view.setMeaning(mMeaning, mType);

        new AlertDialog.Builder(mContext, R.style.AppTheme_DialogOverlay)
                .setView(view)
                .setTitle(mContext.getString(R.string.reading_settings_dialog_title, sNameMap.get(mMeaning)))
                .setPositiveButton(mContext.getString(R.string.close), new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }

    public void refresh(DeviceReading reading, Constants.DeviceType type) {
        mType = type;
        mMeaning = reading.getMeaning();
        mMeaningTv.setText(sNameMap.get(mMeaning));

        widget.setUp(reading.getPath(), reading.getMeaning(), reading.getValueSchema());
    }
}
