package io.relayr.iotsmartphone.ui.readings.widgets;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.handler.LimitedQueue;
import io.relayr.iotsmartphone.handler.ReadingHandler;
import io.relayr.iotsmartphone.storage.Constants;
import io.relayr.java.model.action.Reading;

import static io.relayr.iotsmartphone.storage.Constants.DeviceType.PHONE;

public class ReadingWidgetGraphRssi extends ReadingWidgetGraphSimple {

    public ReadingWidgetGraphRssi(Context context) {
        this(context, null);
    }

    public ReadingWidgetGraphRssi(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReadingWidgetGraphRssi(final Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mMin = -80;
        mMax = 0;
        mSimple = true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        update();
    }
}
