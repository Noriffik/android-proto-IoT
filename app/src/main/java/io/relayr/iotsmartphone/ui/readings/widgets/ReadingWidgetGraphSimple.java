package io.relayr.iotsmartphone.ui.readings.widgets;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.storage.Constants;
import io.relayr.iotsmartphone.handler.LimitedQueue;
import io.relayr.iotsmartphone.handler.ReadingHandler;
import io.relayr.java.model.action.Reading;
import io.relayr.java.model.models.schema.NumberSchema;

import static io.relayr.iotsmartphone.storage.Constants.DeviceType.PHONE;

public class ReadingWidgetGraphSimple extends ReadingWidget {

    @BindView(R.id.chart) LineChart mChart;

    public ReadingWidgetGraphSimple(Context context) {
        this(context, null);
    }

    public ReadingWidgetGraphSimple(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReadingWidgetGraphSimple(final Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        update();
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override void update() {
        setGraphParameters();
    }

    @Override void refresh(LimitedQueue<Reading> readings) {
        if (mChart != null && isShown()) setData(readings);
    }

    @SuppressWarnings("unchecked")
    private void setGraphParameters() {
        if (mSchema == null) return;
        if (mSchema.isIntegerSchema() || mSchema.isNumberSchema()) {
            final NumberSchema schema = mSchema.asNumber();
            initGraph(schema.getMin() != null ? schema.getMin().intValue() : 0,
                    schema.getMax() != null ? schema.getMax().intValue() : 100);
        } else {
            Crashlytics.log(Log.WARN, "RWGSimple", "Object not supported");
        }
    }

    private void initGraph(int min, int max) {
        mChart.setDescription("");
        mChart.setTouchEnabled(false);
        mChart.setDragEnabled(false);
        mChart.setScaleEnabled(false);
        mChart.setPinchZoom(false);

        mChart.getLegend().setEnabled(false);
        mChart.getAxisRight().setEnabled(true);

        initAxis(mChart.getAxisLeft(), min, max);
        initAxis(mChart.getAxisRight(), min, max);

        refresh(mType == PHONE ? ReadingHandler.readingsPhone.get(mMeaning) : ReadingHandler.readingsWatch.get(mMeaning));
    }

    private void initAxis(YAxis axis, int min, int max) {
        axis.setTextColor(ContextCompat.getColor(getContext(), R.color.axis));
        axis.setAxisLineColor(ContextCompat.getColor(getContext(), R.color.axis));
        axis.setAxisMaxValue(max);
        axis.setAxisMinValue(min);
        axis.setStartAtZero(min == 0);
    }

    @SuppressWarnings("unchecked")
    private void setData(List<Reading> readings) {
        if (readings == null) return;

        long mDiff;
        long mFirstPoint;

        List<Entry> values = new ArrayList<>();

        mFirstPoint = System.currentTimeMillis() - Constants.GRAPH_FRAME;
        mDiff = Constants.GRAPH_FRAME / mMaxPoints;

        for (int i = 0; i < readings.size(); i++) {
            final Reading reading = readings.get(i);
            final int index = (int) ((reading.recorded - mFirstPoint) / mDiff);
            if (index < 0) continue;
            if (index >= mMaxPoints) break;

            final int value = ((Number) reading.value).intValue();
            values.add(new Entry(value, index));
        }

        LineData data = new LineData(axisX, createDataSet(mMeaning, values,
                R.color.graph_yellow, R.color.graph_yellow));

        mChart.setData(data);
        mChart.invalidate();
    }

    private LineDataSet createDataSet(String name, List<Entry> entrys, int dotColor, int lineColor) {
        LineDataSet set = new LineDataSet(entrys, name);
        set.setColor(ContextCompat.getColor(getContext(), lineColor));
        set.setCircleColor(ContextCompat.getColor(getContext(), dotColor));
        set.setLineWidth(1f);
        set.setDrawCircleHole(false);
        set.setDrawValues(false);
        set.setCircleRadius(2);
        set.setValueTextColor(ContextCompat.getColor(getContext(), R.color.axis));
        set.setFillColor(ContextCompat.getColor(getContext(), dotColor));
        return set;
    }
}
