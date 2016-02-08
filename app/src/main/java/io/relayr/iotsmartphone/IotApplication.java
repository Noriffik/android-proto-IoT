package io.relayr.iotsmartphone;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
import io.relayr.android.RelayrSdk;

public class IotApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        new RelayrSdk.Builder(this).build();
    }
}
