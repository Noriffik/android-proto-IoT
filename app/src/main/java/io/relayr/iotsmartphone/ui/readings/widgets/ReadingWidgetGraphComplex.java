package io.relayr.iotsmartphone.ui.readings.widgets;

import android.content.Context;
import android.util.AttributeSet;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.handler.LimitedQueue;
import io.relayr.iotsmartphone.storage.Constants;
import io.relayr.java.model.AccelGyroscope;
import io.relayr.java.model.action.Reading;

public class ReadingWidgetGraphComplex extends ReadingWidget {

    @BindView(R.id.chart) LineChart mChart;

    private List<Entry> valuesX = new ArrayList<>();
    private List<Entry> valuesY = new ArrayList<>();
    private List<Entry> valuesZ = new ArrayList<>();

    public ReadingWidgetGraphComplex(Context context) {
        this(context, null);
    }

    public ReadingWidgetGraphComplex(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReadingWidgetGraphComplex(final Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mMin = -10;
        mMax = 10;
        mSimple = false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        update();
        calculateFrame();
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override void update() {
        initGraph();
    }

    @Override void refresh(LimitedQueue<Reading> readings) {
        if (mChart != null && isShown()) setData(readings);
    }

    private void initGraph() {
        super.initGraph(mChart);
        mChart.setDragEnabled(false);
        mChart.setScaleEnabled(false);
        mChart.setPinchZoom(false);
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

        valuesX.clear();
        valuesY.clear();
        valuesZ.clear();

        for (int i = 0; i < readings.size(); i++) {
            final Reading reading = readings.get(i);
            final int index = (int) ((reading.recorded - mFirstPoint) / mDiff);
            if (index < 0) continue;
            if (index >= Constants.MAX_POINTS) break;

            if (reading.value instanceof AccelGyroscope.Acceleration) {
                AccelGyroscope.Acceleration accel = (AccelGyroscope.Acceleration) reading.value;
                valuesX.add(new Entry(accel.x, index));
                valuesY.add(new Entry(accel.y, index));
                valuesZ.add(new Entry(accel.z, index));
                if (checkValue(accel.x) || checkValue(accel.y) || checkValue(accel.z)) initAxises();
            } else if (reading.value instanceof AccelGyroscope.AngularSpeed) {
                AccelGyroscope.AngularSpeed gyro = (AccelGyroscope.AngularSpeed) reading.value;
                valuesX.add(new Entry(gyro.x, index));
                valuesY.add(new Entry(gyro.y, index));
                valuesZ.add(new Entry(gyro.z, index));
                if (checkValue(gyro.x) || checkValue(gyro.y) || checkValue(gyro.z)) initAxises();
            }
        }

        mChart.setData(new LineData(axisX, createAllDataSets()));
        mChart.invalidate();
    }

    private List<ILineDataSet> createAllDataSets() {
        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(createDataSet("x", valuesX, colRed, colRed));
        dataSets.add(createDataSet("y", valuesY, colGreen, colGreen));
        dataSets.add(createDataSet("z", valuesZ, colBlue, colBlue));
        return dataSets;
    }

    private LineDataSet createDataSet(String name, List<Entry> entrys, int dotColor, int lineColor) {
        LineDataSet set = new LineDataSet(entrys, name);
        set.setColor(lineColor);
        set.setCircleColor(dotColor);
        set.setLineWidth(1f);
        set.setDrawCircleHole(false);
        set.setDrawValues(false);
        set.setCircleRadius(2);
        set.setValueTextColor(colAxis);
        set.setFillColor(dotColor);
        return set;
    }
}
