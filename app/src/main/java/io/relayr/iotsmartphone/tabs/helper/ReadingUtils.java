package io.relayr.iotsmartphone.tabs.helper;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import de.greenrobot.event.EventBus;
import io.relayr.android.RelayrSdk;
import io.relayr.iotsmartphone.IotApplication;
import io.relayr.java.helper.observer.ErrorObserver;
import io.relayr.java.helper.observer.SimpleObserver;
import io.relayr.java.model.AccelGyroscope;
import io.relayr.java.model.action.Reading;
import io.relayr.java.model.models.DeviceModel;
import io.relayr.java.model.models.error.DeviceModelsException;
import io.relayr.java.model.models.transport.Transport;
import rx.schedulers.Schedulers;

import static io.relayr.iotsmartphone.tabs.helper.Constants.DeviceType.PHONE;
import static io.relayr.iotsmartphone.tabs.helper.Constants.DeviceType.WATCH;

public class ReadingUtils {

    private static final String TAG = "ReadingUtils";

    public static boolean isComplex(String meaning) {
        return meaning.equals("acceleration") || meaning.equals("angularSpeed") || meaning.equals("luminosity");
    }

    public static void getReadings() {
        RelayrSdk.getDeviceModelsApi().getDeviceModelById(SettingsStorage.MODEL_PHONE)
                .subscribe(new SimpleObserver<DeviceModel>() {
                    @Override public void error(Throwable e) {
                        Log.e(TAG, "PHONE model error");
                        e.printStackTrace();
                    }

                    @Override public void success(DeviceModel deviceModel) {
                        try {
                            final Transport transport = deviceModel.getLatestFirmware().getDefaultTransport();
                            SettingsStorage.instance().savePhoneReadings(transport.getReadings());
                            EventBus.getDefault().post(new Constants.DeviceModelEvent());
                        } catch (DeviceModelsException e) {
                            e.printStackTrace();
                        }
                    }
                });

        RelayrSdk.getDeviceModelsApi().getDeviceModelById(SettingsStorage.MODEL_WATCH)
                .subscribe(new SimpleObserver<DeviceModel>() {
                    @Override public void error(Throwable e) {
                        Log.e(TAG, "WATCH model error");
                        e.printStackTrace();
                    }

                    @Override public void success(DeviceModel deviceModel) {
                        try {
                            final Transport transport = deviceModel.getLatestFirmware().getDefaultTransport();
                            SettingsStorage.instance().saveWatchReadings(transport.getReadings());
                            EventBus.getDefault().post(new Constants.DeviceModelEvent());
                        } catch (DeviceModelsException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    public static Reading createAccelReading(float x, float y, float z) {
        final AccelGyroscope.Acceleration acceleration = new AccelGyroscope.Acceleration();
        acceleration.x = x;
        acceleration.y = y;
        acceleration.z = z;
        return new Reading(0, System.currentTimeMillis(), "acceleration", "/", acceleration);
    }

    public static Reading createGyroReading(float x, float y, float z) {
        final AccelGyroscope.AngularSpeed angularSpeed = new AccelGyroscope.AngularSpeed();
        angularSpeed.x = x;
        angularSpeed.y = y;
        angularSpeed.z = z;
        return new Reading(0, System.currentTimeMillis(), "angularSpeed", "/", angularSpeed);
    }

    public static void publish(Reading reading) {
        if (IotApplication.isVisible(PHONE))
            EventBus.getDefault().post(reading);
        if (IotApplication.isVisible(PHONE) && SettingsStorage.ACTIVITY_PHONE.get(reading.meaning))
            RelayrSdk.getWebSocketClient()
                    .publish(SettingsStorage.instance().getDeviceId(PHONE), reading)
                    .subscribeOn(Schedulers.io())
                    .subscribe(new ErrorObserver<Void>() {
                        @Override public void error(Throwable e) {
                            Crashlytics.log(Log.ERROR, TAG, "publish phone reading - error");
                            e.printStackTrace();
                        }
                    });
    }

    public static void publishWatch(DataItem dataItem) {
        final String path = dataItem.getUri().getPath();
        final DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
        if (Constants.SENSOR_ACCEL_PATH.equals(path)) {
            final float[] array = dataMap.getFloatArray(Constants.SENSOR_ACCEL);
            publishWatch(createAccelReading(array[0], array[1], array[2]));
        } else if (Constants.SENSOR_BATTERY_PATH.equals(path)) {
            String batteryData = dataMap.getString(Constants.SENSOR_BATTERY);
            final long ts = Long.parseLong(batteryData.split("#")[0]);
            final float val = Float.parseFloat(batteryData.split("#")[1]);
            publishWatch(new Reading(0, ts, "batteryLevel", "/", val));
        } else if (Constants.SENSOR_LIGHT_PATH.equals(path)) {
            float val = dataMap.getFloat(Constants.SENSOR_LIGHT);
            publishWatch(new Reading(0, System.currentTimeMillis(), "luminosity", "/", val));
        } else if (Constants.SENSOR_TOUCH_PATH.equals(path)) {
            String touchData = dataMap.getString(Constants.SENSOR_TOUCH);
            final long ts = Long.parseLong(touchData.split("#")[0]);
            final boolean val = Boolean.parseBoolean(touchData.split("#")[1]);
            publishWatch(new Reading(0, ts, "touch", "/", val));
        }
    }

    private static void publishWatch(Reading reading) {
        if (IotApplication.isVisible(WATCH))
            EventBus.getDefault().post(reading);
        if (IotApplication.isVisible(WATCH) && SettingsStorage.ACTIVITY_WATCH.get(reading.meaning))
            RelayrSdk.getWebSocketClient()
                    .publish(SettingsStorage.instance().getDeviceId(WATCH), reading)
                    .subscribeOn(Schedulers.io())
                    .subscribe(new ErrorObserver<Void>() {
                        @Override public void error(Throwable e) {
                            Crashlytics.log(Log.ERROR, TAG, "publish watch reading - error");
                            e.printStackTrace();
                        }
                    });
    }

    public static void publishLocation(Context context, Location location) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses.isEmpty()) return;

            Address obj = addresses.get(0);
            String address = obj.getCountryName() + ", ";
            address += obj.getAddressLine(1) + ", ";
            address += obj.getAddressLine(0);

            ReadingUtils.publish(new Reading(0, System.currentTimeMillis(), "location", "/", address));
        } catch (IOException e) {
            Crashlytics.log(Log.DEBUG, TAG, "Failed to get location.");
            e.printStackTrace();
        }
    }
}
