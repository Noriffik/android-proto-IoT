package io.relayr.iotsmartphone.notif;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import io.relayr.iotsmartphone.R;

public class MyDisplayActivity extends Activity {

    private TextView minfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        minfo = (TextView) findViewById(R.id.info);
    }
}
