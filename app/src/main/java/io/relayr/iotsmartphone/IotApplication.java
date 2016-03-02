package io.relayr.iotsmartphone;

import android.app.Application;
import android.content.Context;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;
import io.relayr.android.RelayrSdk;
import retrofit.RestAdapter;

public class IotApplication extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();

        Fabric.with(this, new Crashlytics());
        new RelayrSdk.Builder(this).setLogLevel(RestAdapter.LogLevel.BASIC).build();
    }

    public static Context context() {
        return mContext;
    }
}
