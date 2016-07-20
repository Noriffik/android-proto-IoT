package io.relayr.iotsmartphone.ui.readings.widgets;

import android.content.Context;
import android.util.AttributeSet;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.handler.LimitedQueue;
import io.relayr.iotsmartphone.storage.Constants;
import io.relayr.iotsmartphone.ui.readings.ReadingType;
import io.relayr.java.model.action.Reading;

public class ReadingWidgetGraphStatic extends ReadingWidget {

    @BindView(R.id.chart) LineChart mChart;

    protected List<Entry> entries = new ArrayList<>();
    private LineDataSet mDataSet;

    public ReadingWidgetGraphStatic(Context context) {
        this(context, null);
    }

    public ReadingWidgetGraphStatic(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReadingWidgetGraphStatic(final Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mMin = 0;
        mMax = 101;
        mFrameType = ReadingType.STATIC;
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        entries.clear();
        System.gc();
    }

    @Override void update() {
        initGraph();
    }

    @Override void refresh(LimitedQueue<Reading> readings) {
        if (mChart != null && isShown()) setData(readings);
    }

    private void initGraph() {
        super.initGraph(mChart);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.getAxisRight().setEnabled(true);
        initAxises();
    }

    private void initAxises() {
        initAxis(this.mChart.getAxisLeft(), mMin, mMax);
        initAxis(this.mChart.getAxisRight(), mMin, mMax);
    }

    @SuppressWarnings("unchecked")
    private void setData(List<Reading> readings) {
        if (readings == null || mFrame <= 0) return;

        long firstTs = System.currentTimeMillis() - mFrame;

        entries.clear();
        for (int i = 0; i < readings.size(); i++) {
            final int index = (int) ((readings.get(i).recorded - firstTs) / mDiff);
            if (index < 0) continue;
            if (index >= Constants.MAX_POINTS) break;

            final int value = ((Number) readings.get(i).value).intValue();
            entries.add(new Entry(value, index));
            if (checkValue(value)) initAxises();
        }

        mChart.setData(new LineData(axisX, createDataSet()));
        mChart.invalidate();
    }

    private LineDataSet createDataSet() {
        if (mDataSet != null) return mDataSet;

        mDataSet = new LineDataSet(entries, mMeaning);
        mDataSet.setColor(colYellow);
        mDataSet.setCircleColor(colYellow);
        mDataSet.setLineWidth(1f);
        mDataSet.setDrawCircleHole(false);
        mDataSet.setDrawValues(false);
        mDataSet.setCircleRadius(2);
        mDataSet.setValueTextColor(colAxis);
        mDataSet.setFillColor(colYellow);
        return mDataSet;
    }
}
