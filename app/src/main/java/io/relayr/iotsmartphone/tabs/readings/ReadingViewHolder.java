package io.relayr.iotsmartphone.tabs.readings;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.tabs.readings.widgets.ReadingWidget;
import io.relayr.java.model.models.transport.DeviceReading;

public class ReadingViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private final ReadingWidget widget;
    @InjectView(R.id.reading_meaning) TextView mMeaningTv;

    public ReadingViewHolder(ReadingWidget widget) {
        super(widget);
        widget.setOnClickListener(this);
        ButterKnife.inject(this, widget);
        this.widget = widget;
    }

    @Override
    public void onClick(View view) {
        Toast.makeText(view.getContext(), "Clicked Position = " + getPosition(), Toast.LENGTH_SHORT).show();
    }

    public void refresh(DeviceReading reading) {
        String path = "";
        final String readingPath = reading.getPath();
        if (readingPath != null && readingPath.length() > 1) path = readingPath + "/";

        mMeaningTv.setText(path + reading.getMeaning());
        widget.setUp(readingPath, reading.getMeaning(), reading.getValueSchema());
    }
}
