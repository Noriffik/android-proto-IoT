package io.relayr.iotsmartphone.tabs.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.tabs.Constants;
import io.relayr.java.model.action.Reading;
import io.relayr.java.model.models.schema.IntegerSchema;
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
    protected ValueSchema mSchema;
    protected LimitedQueue<Reading> mReadings = new LimitedQueue<>(20);

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ButterKnife.inject(this, this);
        EventBus.getDefault().register(this);
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
