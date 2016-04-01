package io.relayr.iotsmartphone.tabs.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.tabs.Constants;
import io.relayr.java.model.action.Reading;
import io.relayr.java.model.models.schema.IntegerSchema;
import io.relayr.java.model.models.schema.ValueSchema;

public class ReadingWidgetDefault extends ReadingWidget {

    public ReadingWidgetDefault(Context context) {
        this(context, null);
    }

    public ReadingWidgetDefault(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReadingWidgetDefault(final Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override void update() {}

    @Override void refresh() {}
}
