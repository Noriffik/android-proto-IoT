package io.relayr.iotsmartphone.ui.readings.widgets;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindColor;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.handler.LimitedQueue;
import io.relayr.iotsmartphone.handler.ReadingHandler;
import io.relayr.iotsmartphone.storage.Constants;
import io.relayr.java.model.action.Reading;
import io.relayr.java.model.models.schema.ValueSchema;

import static io.relayr.iotsmartphone.storage.Constants.DeviceType.PHONE;
import static io.relayr.iotsmartphone.storage.Storage.FREQS_PHONE;
import static io.relayr.iotsmartphone.storage.Storage.FREQS_WATCH;

public abstract class ReadingWidget extends LinearLayout {

    @BindColor(R.color.graph_red) int colRed;
    @BindColor(R.color.graph_blue) int colBlue;
    @BindColor(R.color.graph_green) int colGreen;
    @BindColor(R.color.graph_yellow) int colYellow;
    @BindColor(R.color.axis) int colAxis;

    public ReadingWidget(Context context) {
        this(context, null);
    }

    public ReadingWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReadingWidget(final Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected boolean mSimple;
    protected String mPath;
    protected String mMeaning;
    protected Constants.DeviceType mType;
    protected ValueSchema mSchema;
    protected List<String> axisX = new ArrayList<>(Constants.MAX_POINTS);

    protected int mMin = 0, mMax = 100;
    protected long mDiff;
    protected int mFrame = 0;

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);

        axisX = new ArrayList<>(Constants.MAX_POINTS);
        for (int i = 0; i < Constants.MAX_POINTS; i++) axisX.add("");

        update();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    @SuppressWarnings("unused")
    public void onEvent(final Constants.ReadingRefresh reading) {
        if (reading.getMeaning().equals(mMeaning) && reading.getType() == mType)
            refresh(ReadingHandler.readings(mType).get(mMeaning));
    }

    @SuppressWarnings("unused")
    public void onEvent(final Constants.PhoneSamplingUpdate reading) {
        if (reading.getMeaning().equals(mMeaning)) calculateFrame();
    }

    public void setUp(String path, String meaning, ValueSchema schema, Constants.DeviceType type) {
        this.mType = type;
        this.mPath = path;
        this.mMeaning = meaning;
        this.mSchema = schema;
        if (isShown()) update();
    }

    abstract void update();

    abstract void refresh(LimitedQueue<Reading> readings);

    protected void initGraph(Chart chart) {
        chart.setDescription("");
        chart.setTouchEnabled(false);
        chart.getLegend().setEnabled(false);
        refresh(mType == PHONE ? ReadingHandler.readingsPhone.get(mMeaning) : ReadingHandler.readingsWatch.get(mMeaning));
    }

    protected void initAxis(YAxis axis, int min, int max) {
        axis.setTextColor(ContextCompat.getColor(getContext(), R.color.axis));
        axis.setAxisLineColor(ContextCompat.getColor(getContext(), R.color.axis));
        axis.setAxisMaxValue(max);
        axis.setAxisMinValue(min);
        axis.setStartAtZero(min == 0);
    }

    protected boolean checkValue(float val) {
        if (val > mMax) {
            mMax = ((int) val) + 1;
            return true;
        } else if (val < mMin) {
            mMin = ((int) val) - 1;
            return true;
        }
        return false;
    }

    protected void calculateFrame() {
        if (mSimple) mFrame = calculateFrameSimple();
        else mFrame = calculateFrameComplex();
        mDiff = mFrame / Constants.MAX_POINTS;
    }

    private int calculateFrameComplex() {
        final int frequency = mType == PHONE ? FREQS_PHONE.get(mMeaning) : FREQS_WATCH.get(mMeaning);
        return (int) (Constants.defaultSizes.get(mMeaning) * frequency / 3f);
    }

    private int calculateFrameSimple() {
        final int frequency = mType == PHONE ? FREQS_PHONE.get(mMeaning) : FREQS_WATCH.get(mMeaning);
        return Constants.defaultSizes.get(mMeaning) * frequency;
    }
}
