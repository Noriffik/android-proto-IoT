package io.relayr.iotsmartphone.ui.readings;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.storage.Constants;
import io.relayr.iotsmartphone.ui.readings.widgets.ReadingWidget;
import io.relayr.iotsmartphone.utils.UiHelper;
import io.relayr.java.model.models.transport.DeviceReading;

public class ReadingViewHolder extends RecyclerView.ViewHolder {

    @InjectView(R.id.reading_title) TextView mMeaningTv;

    private final Context mContext;
    private final ReadingWidget widget;

    private String mUnit;
    private String mPath;
    private String mMeaning;
    private Constants.DeviceType mType;

    public ReadingViewHolder(ReadingWidget widget, Context context) {
        super(widget);
        this.widget = widget;
        this.mContext = context;

        ButterKnife.inject(this, widget);
    }

    @SuppressWarnings("unused") @OnClick(R.id.reading_settings)
    public void onSettingsClick() {
        showSettings();
    }

    private void showSettings() {
        final SamplingDialog view = (SamplingDialog) View.inflate(mContext, R.layout.dialog_sampling, null);
        view.setUp(mMeaning, mPath, mUnit, mType);

        new AlertDialog.Builder(mContext, R.style.AppTheme_DialogOverlay)
                .setView(view)
                .setTitle(mContext.getString(R.string.reading_settings_dialog_title,
                        UiHelper.getNameForMeaning(mContext, mMeaning)))
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
        mPath = reading.getPath();
        mMeaning = reading.getMeaning();
        mUnit = reading.getValueSchema().isObjectSchema() ? mContext.getString(R.string.acceleration_unit) : reading.getValueSchema().getUnit();

        mMeaningTv.setText(UiHelper.getNameForMeaning(mContext, mMeaning));

        widget.setUp(reading.getPath(), reading.getMeaning(), reading.getValueSchema(), mType);
    }
}
