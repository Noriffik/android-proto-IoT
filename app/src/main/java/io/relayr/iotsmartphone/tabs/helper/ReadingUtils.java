package io.relayr.iotsmartphone.tabs.helper;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

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

public class ReadingUtils {

    public static void getReadings() {
        RelayrSdk.getDeviceModelsApi().getDeviceModelById(SettingsStorage.MODEL_PHONE)
                .subscribe(new SimpleObserver<DeviceModel>() {
                    @Override public void error(Throwable e) {
                        Log.e("MTA", "PHONE model error");
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
                        Log.e("MTA", "WATCH model error");
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

    public static void publishReading(Reading reading) {
        if (IotApplication.isVisible(PHONE))
            EventBus.getDefault().post(new Constants.ReadingEvent(reading));
        if (IotApplication.isVisible(PHONE) && SettingsStorage.ACTIVITY_PHONE.get(reading.meaning))
            RelayrSdk.getWebSocketClient()
                    .publish(SettingsStorage.instance().getDeviceId(PHONE), reading)
                    .subscribeOn(Schedulers.io())
                    .subscribe(new ErrorObserver<Void>() {
                        @Override public void error(Throwable e) {
                            Crashlytics.log(Log.ERROR, "ReadingUtils", "publishReading - error");
                            e.printStackTrace();
                        }
                    });
    }

    public static void publishLocation(Context context, double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses.isEmpty()) return;

            Address obj = addresses.get(0);
            String address = obj.getCountryName() + ", ";
            address += obj.getAddressLine(1) + ", ";
            address += obj.getAddressLine(0);

            ReadingUtils.publishReading(new Reading(0, System.currentTimeMillis(), "location", "/", address));
        } catch (IOException e) {
            Crashlytics.log(Log.DEBUG, "ReadingUtils", "Failed to get location.");
            e.printStackTrace();
        }
    }
}
