package io.relayr.iotsmartphone.ui.readings.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import butterknife.BindDimen;
import butterknife.BindView;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.handler.LimitedQueue;
import io.relayr.iotsmartphone.handler.ReadingHandler;
import io.relayr.java.model.action.Reading;

public class ReadingWidgetMap extends ReadingWidget {

    @BindView(R.id.map_image) ImageView mMapImage;
    @BindView(R.id.map_pin_container) LinearLayout mPinContainer;
    @BindDimen(R.dimen.indicator_height) int pinSize;

    private View mPin;

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
        mPin = View.inflate(getContext(), R.layout.widget_reading_map_pin, null);
        update();
    }

    @Override void update() {
        showLocation(1, 1);
    }

    @Override void refresh(LimitedQueue<Reading> readings) {
        if (isShown()) setData(readings);
    }

    private void setData(LimitedQueue<Reading> data) {
        if (data == null || data.isEmpty()) return;
        final ReadingHandler.LocationReading reading = (ReadingHandler.LocationReading) data.getLast().value;
        showLocation(reading.latitude(), reading.longitude());
    }

    private void showLocation(double lat, double lon) {
        if (mPinContainer == null || mPin == null) return;

        final double latOffset = (mPinContainer.getBottom() - mPinContainer.getTop()) / 180f * lat;
        final double lonOffset = (mPinContainer.getRight() - mPinContainer.getLeft()) / 360f * lon;

        RelativeLayout.LayoutParams mapParams = new RelativeLayout.LayoutParams(mMapImage.getWidth(), mMapImage.getHeight());
        mPinContainer.setLayoutParams(mapParams);

        LayoutParams mPinLayoutParams = new LayoutParams(pinSize, pinSize);
        mPinLayoutParams.setMargins((int) (lonOffset - pinSize / 2f), (int) (latOffset - pinSize / 2f), 0, 0);
        mPin.setLayoutParams(mPinLayoutParams);

        mPinContainer.removeAllViews();
        mPinContainer.addView(mPin);
    }

}
