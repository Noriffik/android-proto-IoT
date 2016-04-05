package io.relayr.iotsmartphone.tabs.readings;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.tabs.readings.widgets.ReadingWidget;
import io.relayr.java.model.models.transport.DeviceReading;

public class ReadingViewHolder extends RecyclerView.ViewHolder {

    @InjectView(R.id.reading_meaning) TextView mMeaningTv;

    private String mMeaning;
    private final Context mContext;
    private final ReadingWidget widget;

    public ReadingViewHolder(ReadingWidget widget, Context context) {
        super(widget);
        ButterKnife.inject(this, widget);

        this.widget = widget;
        this.mContext = context;
    }

    @SuppressWarnings("unused") @OnClick(R.id.reading_settings)
    public void onSettingsClick() {
        final DialogView view = (DialogView) View.inflate(mContext, R.layout.dialog_content, null);
        view.setMeaning(mMeaning, true);

        new AlertDialog.Builder(mContext, R.style.AppTheme_DialogOverlay)
                .setView(view)
                .setTitle(mMeaning + " settings")
                .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();

    }

    public void refresh(DeviceReading reading) {
        this.mMeaning = reading.getMeaning();

        String path = "";
        final String readingPath = reading.getPath();
        if (readingPath != null && readingPath.length() > 1) path = readingPath + "/";

        mMeaningTv.setText(path + reading.getMeaning());
        widget.setUp(readingPath, reading.getMeaning(), reading.getValueSchema());
    }
}
