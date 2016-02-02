package io.relayr.iotsmartphone;

import android.app.Application;

import io.relayr.android.RelayrSdk;

public class IotApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        new RelayrSdk.Builder(this).build();
    }
}
