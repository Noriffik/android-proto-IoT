package io.relayr.iotsmartphone;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.concurrent.TimeUnit;

import io.relayr.iotsmartphone.notif.NotificationReceiver;

import static java.util.concurrent.TimeUnit.SECONDS;

public class ListenerService extends WearableListenerService {

    private static final String TAG = "ListenerService";
    public static final String ACTIVATE_PATH = "/activate";
    public static final String ACTIVATE = "activate";
    public static final String SENSOR_PATH = "/sensor";
    public static final String SENSOR = "sensor";

    GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(Wearable.API).build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.e(TAG, "onDataChanged: " + dataEvents);
        if (!mGoogleApiClient.isConnected() || !mGoogleApiClient.isConnecting()) {
            ConnectionResult connectionResult = mGoogleApiClient.blockingConnect(30, SECONDS);
            if (!connectionResult.isSuccess()) {
                Log.e(TAG, "Failed to connect to GoogleApiClient err code: " + connectionResult.getErrorCode());
                return;
            }
        }

        for (DataEvent event : dataEvents) {
            Uri uri = event.getDataItem().getUri();
            String path = uri.getPath();
            if (ACTIVATE_PATH.equals(path)) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                boolean start = dataMapItem.getDataMap().getBoolean(ListenerService.ACTIVATE);
                if (start) {
                    Intent startIntent = new Intent(this, MainActivity.class);
                    startIntent.putExtra(ListenerService.ACTIVATE, true);
                    startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(startIntent);
                }
            }
        }
    }
}
