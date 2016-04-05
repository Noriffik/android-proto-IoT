package io.relayr.iotsmartphone.tabs.readings.widgets;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.tabs.helper.Constants;
import io.relayr.iotsmartphone.tabs.helper.LimitedQueue;
import io.relayr.java.model.action.Reading;
import io.relayr.java.model.models.schema.ValueSchema;

public abstract class ReadingWidget extends LinearLayout {

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
    protected LimitedQueue<Reading> mReadings = new LimitedQueue<>(100);
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
        ButterKnife.reset(this);
    }

    @SuppressWarnings("unused")
    public void onEvent(final Constants.ReadingEvent event) {
        if (!event.getReading().meaning.equals(mMeaning)) return;
        mReadings.add(event.getReading());
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
