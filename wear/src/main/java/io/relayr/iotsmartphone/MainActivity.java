package io.relayr.iotsmartphone;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.DelayedConfirmationView;
import android.support.wearable.view.DelayedConfirmationView.DelayedConfirmationListener;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.Sensor.TYPE_ALL;
import static android.hardware.Sensor.TYPE_LIGHT;
import static android.hardware.SensorManager.SENSOR_DELAY_NORMAL;
import static android.os.BatteryManager.EXTRA_LEVEL;
import static android.os.BatteryManager.EXTRA_SCALE;

public class MainActivity extends WearableActivity implements ConnectionCallbacks,
        DataApi.DataListener, SensorEventListener, DelayedConfirmationListener {

    private static final String TAG = "MainActivity";

    private TextView mInfo;
    private DelayedConfirmationView mBtn;

    private GoogleApiClient mGoogleApiClient;
    private SensorManager mSensorManager;

    private boolean animation;
    private boolean mSendingData = false;
    private TimerTask mBatteryTimer;
    private TimerTask mTouchTimer;

    private long mAccelCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mInfo = (TextView) stub.findViewById(R.id.info);
                mBtn = (DelayedConfirmationView) findViewById(R.id.send_btn);
                mBtn.setListener(MainActivity.this);
                mBtn.setTotalTimeMs(1000);
                showInfo();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
        initSensors();
        showInfo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSensorManager != null) mSensorManager.unregisterListener(this);
        if (mBatteryTimer != null) mBatteryTimer.cancel();
        if (mTouchTimer != null) mTouchTimer.cancel();

        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override protected void onDestroy() {
        mSensorManager = null;
        super.onDestroy();
    }

    //Not implemented
    @Override public final void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public final void onSensorChanged(SensorEvent e) {
        if (e.sensor.getType() == TYPE_ACCELEROMETER && mAccelCounter++ % 10 == 0) {
            PutDataMapRequest request = PutDataMapRequest.create(Constants.SENSOR_ACCEL_PATH);
            request.getDataMap().putFloatArray(Constants.SENSOR_ACCEL, new float[]{e.values[0], e.values[1], e.values[2]});
            send(request.asPutDataRequest());
        } else if (e.sensor.getType() == TYPE_LIGHT) {
            PutDataMapRequest request = PutDataMapRequest.create(Constants.SENSOR_LIGHT_PATH);
            request.getDataMap().putFloat(Constants.SENSOR_LIGHT, e.values[0]);
            send(request.asPutDataRequest());
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) sendTouch(true);
        else if (e.getAction() == MotionEvent.ACTION_UP) sendTouch(false);
        return super.dispatchTouchEvent(e);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override public void onConnectionSuspended(int i) {}

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                if (Constants.ACTIVATE_PATH.equals(path)) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    mSendingData = dataMapItem.getDataMap().getBoolean(Constants.ACTIVATE);
                    showInfo();
                }
            }
        }
    }

    @Override
    public void onTimerFinished(View view) {
        animation = false;
        mSendingData = !mSendingData;
        Intent intent = new Intent(MainActivity.this, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                mSendingData ? getString(R.string.sending_data) : getString(R.string.not_sending_data));
        startActivity(intent);
    }

    @Override
    public void onTimerSelected(View view) {
        if (!animation) {
            mBtn.setImageResource(R.drawable.ic_cancel);
            mBtn.start();
            animation = true;
        } else {
            ((DelayedConfirmationView) view).reset();
            showInfo();
            animation = false;
        }
    }

    private void send(final PutDataRequest request) {
        Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(@NonNull DataApi.DataItemResult result) {
                        if (!result.getStatus().isSuccess())
                            Log.e(TAG, "Failed " + result.getStatus().toString());
                    }
                });
    }

    private void initSensors() {
        setBatteryTimer();
        setTouchTimer();

        if (mSensorManager == null)
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        final Sensor light = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (light != null) mSensorManager.registerListener(this, light, SENSOR_DELAY_NORMAL);

        final Sensor accel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accel != null) mSensorManager.registerListener(this, accel, SENSOR_DELAY_NORMAL);
    }

    private void setBatteryTimer() {
        if (mBatteryTimer != null) mBatteryTimer.cancel();
        mBatteryTimer = new TimerTask() {
            @Override public void run() {
                monitorBattery();
            }
        };
        new Timer().scheduleAtFixedRate(mBatteryTimer, 1000, 5000);
    }

    private void setTouchTimer() {
        if (mTouchTimer != null) mTouchTimer.cancel();
        mTouchTimer = new TimerTask() {
            @Override public void run() {
                sendTouch(false);
            }
        };
        new Timer().scheduleAtFixedRate(mTouchTimer, 1000, 1000);
    }

    private void sendTouch(boolean touch) {
        PutDataMapRequest request = PutDataMapRequest.create(Constants.SENSOR_TOUCH_PATH);
        request.getDataMap().putString(Constants.SENSOR_TOUCH, System.currentTimeMillis() + "#" + touch);
        send(request.asPutDataRequest());
    }

    private void monitorBattery() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(ACTION_BATTERY_CHANGED));
        int level = batteryIntent != null ? batteryIntent.getIntExtra(EXTRA_LEVEL, -1) : 0;
        int scale = batteryIntent != null ? batteryIntent.getIntExtra(EXTRA_SCALE, -1) : 0;

        float bat;
        if (level == -1 || scale == -1) bat = 50.0f;
        else bat = ((float) level / (float) scale) * 100.0f;

        PutDataMapRequest request = PutDataMapRequest.create(Constants.SENSOR_BATTERY_PATH);
        request.getDataMap().putString(Constants.SENSOR_BATTERY, System.currentTimeMillis() + "#" + bat);
        send(request.asPutDataRequest());
    }

    private void showInfo() {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                if (mInfo != null)
                    mInfo.setText(mSendingData ? R.string.sending_data : R.string.not_sending_data);
                if (mBtn != null)
                    mBtn.setImageResource(mSendingData ? R.drawable.ic_stop : R.drawable.ic_start);
            }
        });
    }
}
