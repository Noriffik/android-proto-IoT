package io.relayr.iotsmartphone.tabs.readings.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import io.relayr.iotsmartphone.tabs.helper.Constants;
import io.relayr.iotsmartphone.tabs.helper.LimitedQueue;
import io.relayr.java.model.action.Reading;
import io.relayr.java.model.models.schema.ValueSchema;

public abstract class ReadingWidget extends LinearLayout {

    protected static final Map<String, LimitedQueue<Reading>> mReadings = new HashMap<String, LimitedQueue<Reading>>() {
        {
            put("acceleration", new LimitedQueue<Reading>(100));
            put("angularSpeed", new LimitedQueue<Reading>(100));
            put("luminosity", new LimitedQueue<Reading>(100));
            put("touch", new LimitedQueue<Reading>(100));
            put("batteryLevel", new LimitedQueue<Reading>(30));
            put("rssi", new LimitedQueue<Reading>(30));
            put("location", new LimitedQueue<Reading>(1));
            put("message", new LimitedQueue<Reading>(1));
        }
    };

    protected final int DELAY_SIMPLE = 30 * 1000;
    protected final int DELAY_COMPLEX = 10 * 1000;

    public ReadingWidget(Context context) {
        this(context, null);
    }

    public ReadingWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReadingWidget(final Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected String mPath;
    protected String mMeaning;
    protected ValueSchema mSchema;
    protected int mMaxPoints = 500;
    protected List<String> axisX = new ArrayList<>();

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ButterKnife.inject(this, this);
        EventBus.getDefault().register(this);

        axisX = new ArrayList<>(mMaxPoints);
        for (int i = 0; i < mMaxPoints; i++) axisX.add("");
        update();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    @SuppressWarnings("unused")
    public void onEvent(final Reading reading) {
        if (!reading.meaning.equals(mMeaning)) return;
        mReadings.get(reading.meaning).add(reading);
        refresh();
    }

    public void setUp(String mPath, String mMeaning, ValueSchema mSchema) {
        this.mPath = mPath;
        this.mMeaning = mMeaning;
        this.mSchema = mSchema;
        if (isShown()) update();
    }

    abstract void update();

    abstract void refresh();
}
