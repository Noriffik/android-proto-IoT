package io.relayr.iotsmartphone;

import android.app.Application;
import android.content.Context;

import com.crashlytics.android.Crashlytics;

import java.util.HashMap;
import java.util.Map;

import io.fabric.sdk.android.Fabric;
import io.relayr.iotsmartphone.tabs.helper.Constants;

import static io.relayr.iotsmartphone.tabs.helper.Constants.DeviceType.PHONE;
import static io.relayr.iotsmartphone.tabs.helper.Constants.DeviceType.WATCH;

public class IotApplication extends Application {

    private static Context mContext;
    private static Map<Constants.DeviceType, Boolean> sVisible = new HashMap<>();

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

    public static void visible(Constants.DeviceType type, boolean visible) {
        sVisible.put(type, visible);
        sVisible.put(type == WATCH ? PHONE : WATCH, false);
    }

    public static boolean isVisible(Constants.DeviceType type) {
        final Boolean status = sVisible.get(type);
        return status == null ? false : status;
    }
}
