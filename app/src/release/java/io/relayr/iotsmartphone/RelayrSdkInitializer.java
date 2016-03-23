package io.relayr.iotsmartphone;

import android.content.Context;

import io.relayr.android.RelayrSdk;
import retrofit.RestAdapter;

import static retrofit.RestAdapter.LogLevel.FULL;

public abstract class RelayrSdkInitializer {
    static void initSdk(Context context) {
        new RelayrSdk.Builder(context).setLogLevel(RestAdapter.LogLevel.NONE).build();
    }
}
