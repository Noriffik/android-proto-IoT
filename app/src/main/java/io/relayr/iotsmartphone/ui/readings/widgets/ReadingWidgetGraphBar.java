package io.relayr.iotsmartphone.ui.readings.widgets;

import android.content.Context;
import android.util.AttributeSet;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.handler.LimitedQueue;
import io.relayr.iotsmartphone.storage.Constants;
import io.relayr.iotsmartphone.ui.readings.ReadingType;
import io.relayr.java.model.action.Reading;

public class ReadingWidgetGraphBar extends ReadingWidget {

    @BindView(R.id.chart) BarChart mChart;

    private List<BarEntry> entries = new ArrayList<>();

    public ReadingWidgetGraphBar(Context context) {
        this(context, null);
    }

    public ReadingWidgetGraphBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReadingWidgetGraphBar(final Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mMin = 0;
        mMax = 1;
        mFrameType = ReadingType.STATIC;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        update();
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        entries.clear();
        System.gc();
    }

    @Override void update() {initGraph();}

    @Override void refresh(LimitedQueue<Reading> readings) {
        if (mChart != null && isShown()) setData(readings);
    }

    private void initGraph() {
        super.initGraph(mChart);
        mChart.setDragEnabled(false);
        mChart.setScaleEnabled(false);
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

        long mFirstPoint = System.currentTimeMillis() - mFrame;

        entries.clear();
        for (int i = 0; i < readings.size(); i++) {
            final int index = (int) ((readings.get(i).recorded - mFirstPoint) / mDiff);
            if (index < 0) continue;
            if (index >= Constants.MAX_POINTS) break;

            entries.add(new BarEntry(((Boolean) readings.get(i).value) ? 1 : 0, index));
        }

        mChart.setData(new BarData(axisX, createDataSet()));
        mChart.invalidate();
    }

    private BarDataSet createDataSet() {
        BarDataSet barDataSet = new BarDataSet(entries, mMeaning);
        barDataSet.setColor(colYellow);
        barDataSet.setBarSpacePercent(2f);
        barDataSet.setDrawValues(false);
        return barDataSet;
    }
}
