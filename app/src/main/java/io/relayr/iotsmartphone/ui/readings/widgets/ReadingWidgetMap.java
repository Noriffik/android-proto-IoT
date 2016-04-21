package io.relayr.iotsmartphone.ui.readings.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import butterknife.InjectView;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.handler.LimitedQueue;
import io.relayr.iotsmartphone.handler.ReadingHandler;
import io.relayr.java.model.action.Reading;

public class ReadingWidgetMap extends ReadingWidget {

    @InjectView(R.id.map_image) ImageView mMapImage;
    @InjectView(R.id.map_pin_container) LinearLayout mPinContainer;

    public ReadingWidgetMap(Context context) {
        this(context, null);
    }

    public ReadingWidgetMap(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReadingWidgetMap(final Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        update();
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override void update() {

    }

    @Override void refresh(LimitedQueue<Reading> readings) {
        if (isShown()) setData(readings);
    }

    public void setData(LimitedQueue<Reading> data) {
        if (data == null || data.isEmpty() || mPinContainer == null) return;

        RelativeLayout.LayoutParams mapParams = new RelativeLayout.LayoutParams(mMapImage.getWidth(), mMapImage.getHeight());
        mPinContainer.setLayoutParams(mapParams);

        final Reading last = data.getLast();
        final ReadingHandler.LocationReading reading = (ReadingHandler.LocationReading) last.value;
        double lat = reading.latitude() > 0 ? 90 - reading.latitude() : reading.latitude() + 180;
        double lon = reading.longitude() + 180;

        final double latOffset = (mPinContainer.getBottom() - mPinContainer.getTop()) / 180f * lat;
        final double lonOffset = (mPinContainer.getRight() - mPinContainer.getLeft()) / 360f * lon;

        final View pin = View.inflate(getContext(), R.layout.widget_reading_map_pin, null);
        final int pinSize = getResources().getDimensionPixelSize(R.dimen.indicator_height);
        LayoutParams layoutParams = new LayoutParams(pinSize, pinSize);
        layoutParams.setMargins((int) (lonOffset - pinSize / 2f), (int) (latOffset - pinSize / 2f), 0, 0);
        pin.setLayoutParams(layoutParams);

        mPinContainer.removeAllViews();
        mPinContainer.addView(pin);
    }
}
