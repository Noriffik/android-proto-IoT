package io.relayr.iotsmartphone.ui.readings.widgets;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.YAxis;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindColor;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.handler.LimitedQueue;
import io.relayr.iotsmartphone.handler.ReadingHandler;
import io.relayr.iotsmartphone.storage.Constants;
import io.relayr.iotsmartphone.ui.ActivityMain;
import io.relayr.iotsmartphone.ui.readings.ReadingType;
import io.relayr.iotsmartphone.ui.utils.UiUtil;
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

    protected List<String> axisX = new ArrayList<>(Constants.MAX_POINTS);

    public ReadingWidget(Context context) {
        this(context, null);
    }

    public ReadingWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReadingWidget(final Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        for (int i = 0; i < Constants.MAX_POINTS; i++) axisX.add("");
    }

    protected String mPath;
    protected String mMeaning;
    protected Constants.DeviceType mType;
    protected ValueSchema mSchema;

    protected int mMin = 0, mMax = 100;

    protected ReadingType mFrameType = ReadingType.STATIC;
    protected int mFrame = 0;
    protected long mDiff;

    private Timer mTimer;

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);

        calculateFrame();
        setTimer();
        update();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
        killTimer();
    }

    @SuppressWarnings("unused")
    public void onEvent(final Constants.PhoneSamplingUpdate reading) {
        if (!reading.getMeaning().equals(mMeaning)) return;
        calculateFrame();
        setTimer();
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
        switch (mFrameType) {
            case SIMPLE:
                mFrame = calculateFrameSimple();
                break;
            case COMPLEX:
                mFrame = calculateFrameComplex();
                break;
            case STATIC:
                mFrame = calculateFrameStatic();
                break;
        }
        mDiff = mFrame / Constants.MAX_POINTS;

        Log.e(mMeaning, "frame " + mFrame + " " + mDiff);
    }

    private int calculateFrameComplex() {
        final int frequency = mType == PHONE ? FREQS_PHONE.get(mMeaning) : FREQS_WATCH.get(mMeaning);
        return (int) (Constants.defaultSizes.get(mMeaning) * frequency / 3f);
    }

    private int calculateFrameSimple() {
        final int frequency = mType == PHONE ? FREQS_PHONE.get(mMeaning) : FREQS_WATCH.get(mMeaning);
        return Constants.defaultSizes.get(mMeaning) * frequency;
    }

    private int calculateFrameStatic() {
        return 30 * 1000;
    }

    private void setTimer() {
        final int freq = UiUtil.getFreq(mMeaning, mType);

        killTimer();
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                ((ActivityMain) getContext()).runOnUiThread(new Runnable() {
                    @Override public void run() {
                        refresh(ReadingHandler.readings(mType).get(mMeaning));
                    }
                });
            }
        }, 0, freq);
    }

    private void killTimer() {
        if (mTimer == null) return;
        mTimer.cancel();
        mTimer.purge();
        mTimer = null;
    }
}
