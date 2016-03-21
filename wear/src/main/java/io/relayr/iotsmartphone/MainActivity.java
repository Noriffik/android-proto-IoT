package io.relayr.iotsmartphone;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.DelayedConfirmationView;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import static android.hardware.SensorManager.SENSOR_DELAY_NORMAL;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener, SensorEventListener,
        DelayedConfirmationView.DelayedConfirmationListener {

    private static final String TAG = "MainActivity";

    private TextView mInfo;
    private DelayedConfirmationView mBtn;

    private GoogleApiClient mGoogleApiClient;

    private Sensor mLight;
    private SensorManager mSensorManager;

    private boolean animation;
    private boolean mSendingData = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mSendingData = getIntent().getBooleanExtra(ListenerService.ACTIVATE, false);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

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
        mSensorManager.registerListener(MainActivity.this, mLight, SENSOR_DELAY_NORMAL);
        showInfo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSensorManager != null) mSensorManager.unregisterListener(this);
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override protected void onDestroy() {
        mLight = null;
        mSensorManager = null;
        super.onDestroy();
    }

    //Not implemented
    @Override public final void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public final void onSensorChanged(SensorEvent event) {
        if (!mSendingData) return;
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(ListenerService.SENSOR_PATH);
        putDataMapRequest.getDataMap().putInt(ListenerService.SENSOR, (int) event.values[0]);
        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(@NonNull DataApi.DataItemResult result) {
                        if (!result.getStatus().isSuccess())
                            Log.e(TAG, "Failed to send light data " + result.getStatus().toString());
                    }
                });
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Google API client - onConnected()");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.w(TAG, "onConnectionSuspended()");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.e(TAG, "onConnectionFailed(): " + result);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged(): " + dataEvents);
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                if (ListenerService.ACTIVATE_PATH.equals(path)) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    mSendingData = dataMapItem.getDataMap().getBoolean(ListenerService.ACTIVATE);
                    showInfo();
                } else {
                    Log.d(TAG, "Unrecognized path: " + path);
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.d(TAG, "Unrecognized DataItem Deleted");
            } else {
                Log.d(TAG, "Unknown data event " + event.getType());
            }
        }
    }

    @Override
    public void onTimerFinished(View view) {
        Log.d(TAG, "onTimerFinished");
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
        Log.d(TAG, "onTimerSelected");
        if (!animation) {
            Log.d(TAG, "onTimerSelected Start");
            mBtn.setImageResource(R.drawable.ic_cancel);
            mBtn.start();
            animation = true;
        } else {
            Log.d(TAG, "onTimerSelected Cancel");
            ((DelayedConfirmationView) view).reset();
            showInfo();
            animation = false;
        }
    }

    private void showInfo() {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                if (mInfo != null)
                    mInfo.setText(mSendingData ? getString(R.string.sending_data) : getString(R.string.not_sending_data));
                if (mBtn != null)
                    mBtn.setImageResource(mSendingData ? R.drawable.ic_stop : R.drawable.ic_start);
            }
        });
    }
}
