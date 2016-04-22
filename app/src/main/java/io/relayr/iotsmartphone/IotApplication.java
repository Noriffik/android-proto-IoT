package io.relayr.iotsmartphone;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;
import io.relayr.iotsmartphone.storage.Constants;

import static io.relayr.iotsmartphone.storage.Constants.DeviceType.PHONE;

public class IotApplication extends Application {

    private static Context mContext;
    private static boolean sPhoneVisibility = true;
    private static boolean swatchVisibility = false;
    public static Constants.DeviceType sCurrent = PHONE;

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

    public static void visible(boolean phone, boolean watch) {
        sPhoneVisibility = phone;
        swatchVisibility = watch;
    }

    public static boolean isVisible(Constants.DeviceType type) {
        if (type == null || type == PHONE) return sPhoneVisibility;
        else return swatchVisibility;
    }
}
