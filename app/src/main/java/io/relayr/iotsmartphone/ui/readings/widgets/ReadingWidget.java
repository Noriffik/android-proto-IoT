package io.relayr.iotsmartphone.ui.readings.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import io.relayr.iotsmartphone.storage.Constants;
import io.relayr.iotsmartphone.handler.LimitedQueue;
import io.relayr.iotsmartphone.handler.ReadingHandler;
import io.relayr.java.model.action.Reading;
import io.relayr.java.model.models.schema.ValueSchema;

public abstract class ReadingWidget extends LinearLayout {

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
    protected Constants.DeviceType mType;
    protected ValueSchema mSchema;
    protected int mMaxPoints = 500;
    protected List<String> axisX = new ArrayList<>(500);

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ButterKnife.inject(this, this);
        EventBus.getDefault().register(this);

        axisX = new ArrayList<>((int) mMaxPoints);
        for (int i = 0; i < mMaxPoints; i++) axisX.add("");

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

    public void setUp(String path, String meaning, ValueSchema schema, Constants.DeviceType type) {
        this.mType = type;
        this.mPath = path;
        this.mMeaning = meaning;
        this.mSchema = schema;
        if (isShown()) update();
    }

    abstract void update();

    abstract void refresh(LimitedQueue<Reading> readings);
}
