package io.relayr.iotsmartphone.ui.readings.widgets;

import android.content.Context;
import android.util.AttributeSet;

import io.relayr.iotsmartphone.ui.readings.ReadingType;

public class ReadingWidgetGraphRssi extends ReadingWidgetGraphStatic {

    public ReadingWidgetGraphRssi(Context context) {
        this(context, null);
    }

    public ReadingWidgetGraphRssi(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReadingWidgetGraphRssi(final Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mMin = -80;
        mMax = -40;
    }
}
