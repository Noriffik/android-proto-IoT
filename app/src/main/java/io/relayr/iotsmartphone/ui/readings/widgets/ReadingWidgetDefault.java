package io.relayr.iotsmartphone.ui.readings.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import butterknife.InjectView;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.utils.LimitedQueue;
import io.relayr.iotsmartphone.handler.ReadingHandler;
import io.relayr.java.model.action.Reading;

public class ReadingWidgetDefault extends ReadingWidget {

    @InjectView(R.id.reading_data) TextView mData;

    public ReadingWidgetDefault(Context context) {
        this(context, null);
    }

    public ReadingWidgetDefault(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReadingWidgetDefault(final Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        final LimitedQueue<Reading> readings = ReadingHandler.readingsPhone.get(mMeaning);
        if (readings == null || readings.isEmpty()) return;
        final Reading last = readings.getLast();
        if (last == null) return;
        mData.setText((String) last.value);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override void update() {}

    @Override void refresh(LimitedQueue<Reading> readings) {
        if (readings == null || readings.isEmpty()) return;
        mData.setText((String) readings.getLast().value);
    }
}
