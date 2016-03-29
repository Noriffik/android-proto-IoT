package io.relayr.iotsmartphone.tabs;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.relayr.iotsmartphone.R;
import io.relayr.java.model.action.Reading;
import io.relayr.java.model.models.schema.IntegerSchema;
import io.relayr.java.model.models.schema.ValueSchema;

public class ReadingWidget extends LinearLayout {

    @InjectView(R.id.reading_path) TextView mPathTv;
    @InjectView(R.id.reading_meaning) TextView mMeaningTv;

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

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ButterKnife.inject(this, this);

        mMeaningTv.setText(mMeaning);
        mPathTv.setText(mPath == null ? "/" : mPath);

        final IntegerSchema schema = mSchema.asInteger();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ButterKnife.reset(this);
    }

    public void setPath(String mPath) {
        this.mPath = mPath;
    }

    public void setMeaning(String mMeaning) {
        this.mMeaning = mMeaning;
    }

    public void setSchema(ValueSchema mSchema) {
        this.mSchema = mSchema;
    }

    public void updateReading(Reading reading) {
        if (!isShown()) return;
        if (!reading.meaning.equals(mMeaning)) return;
    }
}
