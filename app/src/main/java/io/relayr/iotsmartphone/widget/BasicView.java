package io.relayr.iotsmartphone.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import io.relayr.iotsmartphone.helper.ControlListener;

public class BasicView extends LinearLayout {

    protected ControlListener mListener;

    public BasicView(Context context) {
        super(context);
    }

    public BasicView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BasicView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setListener(ControlListener listener) {mListener = listener;}
}
