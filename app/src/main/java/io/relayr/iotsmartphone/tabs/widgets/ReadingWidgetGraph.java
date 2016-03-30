package io.relayr.iotsmartphone.tabs.widgets;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.relayr.iotsmartphone.R;
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

    private int mMaxPoints = 20;
    private long mFirstPoint;
    private long mDiff;
    private SimpleDateFormat mDateFormat;

    private Gson mGson = new Gson();
    private Map<String, List<Entry>> mAxisYKeys = new HashMap<>();
    private int[] mAxisYColors = new int[]{R.color.colorPrimaryDark, R.color.colorPrimary, R.color.colorAccent};

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mDateFormat = new SimpleDateFormat("dd.MM HH:mm:ss",
                getResources().getConfiguration().locale);
        setGraphParameters();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ButterKnife.reset(this);
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
                        Crashlytics.log(Log.WARN, "RW", "Object not supported");
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
//        mChart.setDescription("");
//        mChart.setNoDataTextDescription("Desc");
//        mChart.setDescriptionColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
//        mChart.setDescriptionTextSize(getResources().getDimensionPixelSize(R.dimen.tiny_text));

        mChart.setTouchEnabled(false);
        mChart.setDragEnabled(false);
        mChart.setScaleEnabled(false);
        mChart.setPinchZoom(false);

        mChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
//        mChart.getXAxis().setLabelRotationAngle(10);
        mChart.getXAxis().setDrawLabels(false);
        mChart.getAxisRight().setEnabled(false);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setAxisMaxValue(max);
        leftAxis.setAxisMinValue(min);
        leftAxis.setStartAtZero(min == 0);
    }

    @SuppressWarnings("unchecked")
    private void setData(List<Reading> points) {
        clearOldData();
        mFirstPoint = points.get(0).ts;
        mDiff = (System.currentTimeMillis() - mFirstPoint) / mMaxPoints;

        List<String> axisX = setUpXaxis();

        if (mAxisYKeys.size() == 1) {
            for (int i = 0; i < points.size(); i++) {
                final Reading reading = points.get(i);
                final int index = i == 0 ? 0 : (int) ((reading.recorded - mFirstPoint) / mDiff);
                if (index >= axisX.size()) break;

                final int value = ((Float) reading.value).intValue();
                mAxisYKeys.get(mMeaning).add(new Entry(value, index));
            }
        } else {
            for (int i = 0; i < points.size(); i++) {
                final Reading reading = points.get(i);
                final int index = i == 0 ? 0 : (int) ((reading.recorded - mFirstPoint) / mDiff);
                if (index >= axisX.size()) break;

                final LinkedTreeMap<String, Double> value = (LinkedTreeMap<String, Double>) reading.value;
                for (String key : value.keySet())
                    mAxisYKeys.get(key).add(new Entry(value.get(key).floatValue(), index));
            }
        }

        LineData data;
        if (mAxisYKeys.size() == 1) {
            data = new LineData(axisX, createDataSet(mMeaning, mAxisYKeys.get(mMeaning),
                    R.color.colorPrimaryDark, R.color.colorPrimary));
        } else {
            List<ILineDataSet> dataSets = new ArrayList<>();
            int color = 0;
            for (Map.Entry<String, List<Entry>> entry : mAxisYKeys.entrySet()) {
                final int axisColor = mAxisYColors[color++ % mAxisYColors.length];
                dataSets.add(createDataSet(entry.getKey(), entry.getValue(), axisColor, axisColor));
            }
            data = new LineData(axisX, dataSets);
        }

        mChart.setData(data);
        mChart.invalidate();
    }

    private void clearOldData() {
        mDiff = 0;
        mFirstPoint = 0;
        for (String key : mAxisYKeys.keySet()) mAxisYKeys.get(key).clear();
    }

    @NonNull private List<String> setUpXaxis() {
        List<String> axisX = new ArrayList<>();
        for (int i = 0; i < mMaxPoints; i++)
            axisX.add("");
//            axisX.add(mDateFormat.format(mFirstPoint + (mDiff * i)));
        return axisX;
    }

    private LineDataSet createDataSet(String name, List<Entry> entrys, int dotColor, int lineColor) {
        LineDataSet set = new LineDataSet(entrys, name);
        set.setColor(ContextCompat.getColor(getContext(), lineColor));
        set.setCircleColor(ContextCompat.getColor(getContext(), dotColor));
        set.setLineWidth(0f);
        set.setDrawCircleHole(false);
        set.setDrawValues(false);
        set.setCircleRadius(10);
        set.setFillColor(ContextCompat.getColor(getContext(), dotColor));
        return set;
    }
}
