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
//        if (readings.isEmpty()) return;
//        if (readings.get(mMeaning) == null) return;
//        if (readings.get(mMeaning).getLast().value == null) return;
//        if (!(readings.get(mMeaning).getLast().value instanceof String)) return;
//
//        mData.setText((String) readings.get(mMeaning).getLast().value);
    }
}
