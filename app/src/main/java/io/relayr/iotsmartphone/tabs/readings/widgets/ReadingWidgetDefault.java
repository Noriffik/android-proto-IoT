package io.relayr.iotsmartphone.tabs.readings.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import butterknife.InjectView;
import io.relayr.iotsmartphone.R;

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
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override void update() {}

    @Override void refresh() {
//        if (mReadings.isEmpty()) return;
//        if (mReadings.get(mMeaning) == null) return;
//        if (mReadings.get(mMeaning).getLast().value == null) return;
//        if (!(mReadings.get(mMeaning).getLast().value instanceof String)) return;
//
//        mData.setText((String) mReadings.get(mMeaning).getLast().value);
    }
}
