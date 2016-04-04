package io.relayr.iotsmartphone.tabs.readings.widgets;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import butterknife.InjectView;
import io.relayr.iotsmartphone.R;
import io.relayr.java.model.AccelGyroscope;
import io.relayr.java.model.action.Reading;
import io.relayr.java.model.models.schema.NumberSchema;
import io.relayr.java.model.models.schema.ObjectSchema;
import io.relayr.java.model.models.schema.ValueSchema;

public class ReadingWidgetGraph extends ReadingWidget {

    @InjectView(R.id.history_chart) LineChart mChart;

    public ReadingWidgetGraph(Context context) {
        this(context, null);
    }

    public ReadingWidgetGraph(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReadingWidgetGraph(final Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private Gson mGson = new Gson();
    private Map<String, List<Entry>> mAxisYKeys = new HashMap<>();
    private int[] mColors = new int[]{R.color.graph_yellow, R.color.graph_red, R.color.graph_green};
    private int mColor = mColors[new Random().nextInt(2)];

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        update();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override void update() {
        setGraphParameters();
    }

    @Override void refresh() {
        if (mChart != null) setData(mReadings);
    }

    @SuppressWarnings("unchecked")
    private void setGraphParameters() {
        if (mSchema.isIntegerSchema() || mSchema.isNumberSchema()) {
            mAxisYKeys.put(mMeaning, new ArrayList<Entry>());
            extractParameters(mSchema);
        }

        if (mSchema.isObjectSchema()) {
            final ObjectSchema schema = mSchema.asObject();
            final LinkedTreeMap<String, Object> properties = (LinkedTreeMap<String, Object>) schema.getProperties();
            if (properties != null) {
                for (java.util.Map.Entry<String, Object> obj : properties.entrySet()) {
                    mAxisYKeys.put(obj.getKey(), new ArrayList<Entry>());
                    try {
                        final NumberSchema fromJson = mGson.fromJson(obj.getValue().toString(), NumberSchema.class);
                        extractParameters(fromJson);
                    } catch (Exception e) {
                        Crashlytics.log(Log.WARN, "RWG", "Object not supported");
                    }
                }
            }
        }
    }

    private void extractParameters(ValueSchema valueSchema) {
        int min = 0;
        int max = 100;
        final NumberSchema schema = valueSchema.asNumber();
        if (schema.getMin() != null)
            min = (int) (schema.getMin().intValue() - (schema.getMax().intValue() * 0.1));
        if (schema.getMax() != null)
            max = (int) (schema.getMax().intValue() + (schema.getMax().intValue() * 0.1));
        initGraph(min, max);
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

        refresh();
    }

    private void initAxis(YAxis axis, int min, int max) {
        axis.setTextColor(ContextCompat.getColor(getContext(), R.color.colorSecondary));
        axis.setAxisMaxValue(max);
        axis.setAxisMinValue(min);
        axis.setStartAtZero(min == 0);
    }

    @SuppressWarnings("unchecked")
    private void setData(List<Reading> points) {
        long mFirstPoint;
        long mDiff;

        for (String key : mAxisYKeys.keySet()) mAxisYKeys.get(key).clear();

        if (mAxisYKeys.size() == 1) {
            mFirstPoint = System.currentTimeMillis() - DELAY_SIMPLE;
            mDiff = DELAY_SIMPLE / mMaxPoints;

            for (int i = 0; i < points.size(); i++) {
                final Reading reading = points.get(i);
                final int index = (int) ((reading.recorded - mFirstPoint) / mDiff);
                if (index < 0) continue;
                if (index >= mMaxPoints) break;

                final int value = ((Number) reading.value).intValue();
                mAxisYKeys.get(mMeaning).add(new Entry(value, index));
            }
        } else {
            mFirstPoint = System.currentTimeMillis() - DELAY_COMPLEX;
            mDiff = DELAY_COMPLEX / mMaxPoints;

            for (int i = 0; i < points.size(); i++) {
                final Reading reading = points.get(i);
                final int index = (int) ((reading.recorded - mFirstPoint) / mDiff);
                if (index < 0) continue;
                if (index >= mMaxPoints) break;

                AccelGyroscope.Acceleration accel = (AccelGyroscope.Acceleration) reading.value;
                mAxisYKeys.get("x").add(new Entry(accel.x, index));
                mAxisYKeys.get("y").add(new Entry(accel.y, index));
                mAxisYKeys.get("z").add(new Entry(accel.z, index));
            }
        }

        LineData data;
        if (mAxisYKeys.size() == 1) {
            data = new LineData(axisX, createDataSet(mMeaning, mAxisYKeys.get(mMeaning), mColor, mColor));
        } else {
            List<ILineDataSet> dataSets = new ArrayList<>();
            int color = 0;
            for (Map.Entry<String, List<Entry>> entry : mAxisYKeys.entrySet()) {
                final int axisColor = mColors[color++ % mColors.length];
                dataSets.add(createDataSet(entry.getKey(), entry.getValue(), axisColor, axisColor));
            }
            data = new LineData(axisX, dataSets);
        }

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
        set.setValueTextColor(ContextCompat.getColor(getContext(), R.color.colorSecondary));
        set.setFillColor(ContextCompat.getColor(getContext(), dotColor));
        return set;
    }
}
