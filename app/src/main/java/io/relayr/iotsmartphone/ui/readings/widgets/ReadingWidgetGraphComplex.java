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
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.util.ArrayList;
import java.util.List;

import butterknife.InjectView;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.storage.Constants;
import io.relayr.iotsmartphone.utils.LimitedQueue;
import io.relayr.iotsmartphone.utils.ReadingUtils;
import io.relayr.java.model.AccelGyroscope;
import io.relayr.java.model.action.Reading;
import io.relayr.java.model.models.schema.NumberSchema;
import io.relayr.java.model.models.schema.ObjectSchema;

import static io.relayr.iotsmartphone.storage.Constants.DeviceType.PHONE;
import static io.relayr.iotsmartphone.storage.Storage.FREQS_PHONE;
import static io.relayr.iotsmartphone.storage.Storage.FREQS_WATCH;

public class ReadingWidgetGraphComplex extends ReadingWidget {

    @InjectView(R.id.history_chart) LineChart mChart;

    public ReadingWidgetGraphComplex(Context context) {
        this(context, null);
    }

    public ReadingWidgetGraphComplex(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReadingWidgetGraphComplex(final Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private Gson mGson = new Gson();
    private int[] mColors = new int[]{R.color.graph_green, R.color.graph_blue, R.color.graph_red};

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
        if (mSchema.isObjectSchema()) {
            final ObjectSchema schema = mSchema.asObject();
            final LinkedTreeMap<String, Object> properties = (LinkedTreeMap<String, Object>) schema.getProperties();
            if (properties != null) {
                final Object x = properties.get("x");
                final NumberSchema fromJson = mGson.fromJson(x.toString(), NumberSchema.class);
                initGraph(fromJson.getMin() != null ? fromJson.getMin().intValue() : 0,
                        fromJson.getMax() != null ? fromJson.getMax().intValue() : 100);
            }
        } else {
            Crashlytics.log(Log.ERROR, "RWGComplex", "Object not supported");
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

        refresh(mType == PHONE ? ReadingUtils.readingsPhone.get(mMeaning) : ReadingUtils.readingsWatch.get(mMeaning));
    }

    private void initAxis(YAxis axis, int min, int max) {
        axis.setTextColor(ContextCompat.getColor(getContext(), R.color.secondary));
        axis.setAxisMaxValue(max);
        axis.setAxisMinValue(min);
        axis.setStartAtZero(min == 0);
    }

    @SuppressWarnings("unchecked")
    private void setData(List<Reading> readings) {
        long mDiff;
        long mFirstPoint;

        final int frame = calculateFrame();
        mFirstPoint = System.currentTimeMillis() - frame;
        mDiff = (long) (frame / mMaxPoints);

        List<Entry> valuesX = new ArrayList<>();
        List<Entry> valuesY = new ArrayList<>();
        List<Entry> valuesZ = new ArrayList<>();

        for (int i = 0; i < readings.size(); i++) {
            final Reading reading = readings.get(i);
            final int index = (int) ((reading.recorded - mFirstPoint) / mDiff);
            if (index < 0) continue;
            if (index >= mMaxPoints) break;

            if (reading.value instanceof AccelGyroscope.Acceleration) {
                AccelGyroscope.Acceleration accel = (AccelGyroscope.Acceleration) reading.value;
                valuesX.add(new Entry(accel.x, index));
                valuesY.add(new Entry(accel.y, index));
                valuesZ.add(new Entry(accel.z, index));
            } else if (reading.value instanceof AccelGyroscope.AngularSpeed) {
                AccelGyroscope.AngularSpeed gyro = (AccelGyroscope.AngularSpeed) reading.value;
                valuesX.add(new Entry(gyro.x, index));
                valuesY.add(new Entry(gyro.y, index));
                valuesZ.add(new Entry(gyro.z, index));
            }
        }

        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(createDataSet("x", valuesX, mColors[0], mColors[0]));
        dataSets.add(createDataSet("y", valuesY, mColors[1], mColors[1]));
        dataSets.add(createDataSet("z", valuesZ, mColors[2], mColors[2]));

        LineData data = new LineData(axisX, dataSets);

        mChart.setData(data);
        mChart.invalidate();
    }

    private int calculateFrame() {
        final int places = Constants.defaultSizes.get(mMeaning);
        final int frequency = mType == PHONE ? FREQS_PHONE.get(mMeaning) : FREQS_WATCH.get(mMeaning);
        return (int) (places * frequency / 3f);
    }

    private LineDataSet createDataSet(String name, List<Entry> entrys, int dotColor, int lineColor) {
        LineDataSet set = new LineDataSet(entrys, name);
        set.setColor(ContextCompat.getColor(getContext(), lineColor));
        set.setCircleColor(ContextCompat.getColor(getContext(), dotColor));
        set.setLineWidth(1f);
        set.setDrawCircleHole(false);
        set.setDrawValues(false);
        set.setCircleRadius(2);
        set.setValueTextColor(ContextCompat.getColor(getContext(), R.color.secondary));
        set.setFillColor(ContextCompat.getColor(getContext(), dotColor));
        return set;
    }
}
