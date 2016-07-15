package io.relayr.iotsmartphone.ui.readings.widgets;

import android.content.Context;
import android.util.AttributeSet;

import io.relayr.iotsmartphone.ui.readings.ReadingType;

public class ReadingWidgetGraphLum extends ReadingWidgetGraphStatic {

    public ReadingWidgetGraphLum(Context context) {
        this(context, null);
    }

    public ReadingWidgetGraphLum(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReadingWidgetGraphLum(final Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mMin = 0;
        mMax = 200;
        mFrameType = ReadingType.SIMPLE;
    }
}
