package io.relayr.iotsmartphone;

import android.app.Application;
import android.content.Context;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

public class IotApplication extends Application {

    private static Context mContext;
    private static boolean sVisible = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();

        Fabric.with(this, new Crashlytics());
        RelayrSdkInitializer.initSdk(this);
    }

    public static Context context() {
        return mContext;
    }

    public static void activityVisible(boolean visible) {sVisible = visible;}

    public static boolean issVisible() {return sVisible;}
}
