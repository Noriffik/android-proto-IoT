package io.relayr.iotsmartphone.tabs.helper;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.relayr.iotsmartphone.IotApplication;
import io.relayr.java.model.Device;
import io.relayr.java.model.models.transport.DeviceReading;

public class SettingsStorage {

    public static final String MODEL_PHONE = "86e0a7d7-5e18-449c-b7aa-f3b089c33b67";
    public static final String MODEL_WATCH = "b42cc83d-5766-406c-872d-9294d96bdf69";

    private static final String PREFS_NAME = "io.relayr.iotsp.settings";

    private static final String PREFS_WARNING = "io.relayr.warning";
    private static final String PREFS_SETTINGS_LOCATION = "io.relayr.iotsp.permission.location";

    private static final String PREFS_ACTIVE_PHONE = "active_p";
    private static final String PREFS_ACTIVE_WATCH = "active_w";
    private static final String PREFS_FREQUENCY_PHONE = "freq_p";
    private static final String PREFS_FREQUENCY_WATCH = "freq_w";

    private static final String PREFS_PHONE_MANUF = "phone_manuf";
    private static final String PREFS_PHONE_MODEL = "phone_model";
    private static final String PREFS_PHONE_SDK = "phone_sdk";

    private final static SettingsStorage singleton = new SettingsStorage();
    private static Device sDevice;
    private final SharedPreferences PREFS;

    private static List<DeviceReading> sReadingsPhone = new ArrayList<>();
    private static List<DeviceReading> sReadingsWatch = new ArrayList<>();

    private SettingsStorage() {
        PREFS = IotApplication.context().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static Map<String, Integer> FREQS = new HashMap<String, Integer>() {
        {
            put("acceleration", SettingsStorage.instance().loadFrequency("acceleration", true));
            put("angularSpeed", SettingsStorage.instance().loadFrequency("angularSpeed", true));
            put("batteryLevel", SettingsStorage.instance().loadFrequency("batteryLevel", true));
            put("location", SettingsStorage.instance().loadFrequency("location", true));
            put("rssi", SettingsStorage.instance().loadFrequency("rssi", true));
        }
    };

    public static SettingsStorage instance() {return singleton;}

    public Device getDevice() {return sDevice;}

    public void saveDevice(Device device) {sDevice = device;}

    public void saveActivity(String meaning, boolean phone, boolean active) {
        PREFS.edit().putBoolean((phone ? PREFS_ACTIVE_PHONE : PREFS_ACTIVE_WATCH) + meaning, active).apply();
    }

    public boolean loadActivity(String meaning, boolean phone) {
        return PREFS.getBoolean((phone ? PREFS_ACTIVE_PHONE : PREFS_ACTIVE_WATCH) + meaning, false);
    }

    public void saveFrequency(String meaning, boolean phone, int freq) {
        FREQS.put(meaning, freq);
        PREFS.edit().putInt((phone ? PREFS_FREQUENCY_PHONE : PREFS_FREQUENCY_WATCH) + meaning, freq).apply();
    }

    public int loadFrequency(String meaning, boolean phone) {
        return PREFS.getInt((phone ? PREFS_FREQUENCY_PHONE : PREFS_FREQUENCY_WATCH) + meaning, 3);
    }

    public void locationPermission(boolean granted) {
        PREFS.edit().putBoolean(PREFS_SETTINGS_LOCATION, granted).apply();
    }

    public boolean locationGranted() {
        return PREFS.getBoolean(PREFS_SETTINGS_LOCATION, true);
    }

    public void clear() {
        PREFS.edit().clear().apply();
        sDevice = null;
    }

    public void warningShown() {
        PREFS.edit().putBoolean(PREFS_WARNING, true).apply();
    }

    public boolean isWarningShown() {
        return PREFS.getBoolean(PREFS_WARNING, false);
    }

    public void savePhoneReadings(List<DeviceReading> readings) {
        sReadingsPhone.clear();
        sReadingsPhone.addAll(readings);
        Collections.sort(sReadingsPhone, new Comparator<DeviceReading>() {
            @Override public int compare(DeviceReading lhs, DeviceReading rhs) {
                return lhs.getMeaning().compareTo(rhs.getMeaning());
            }
        });
    }

    public List<DeviceReading> loadPhoneReadings() {
        return sReadingsPhone;
    }

    public void saveWatchReadings(List<DeviceReading> readings) {
        sReadingsWatch.clear();
        sReadingsWatch.addAll(readings);
        Collections.sort(sReadingsWatch, new Comparator<DeviceReading>() {
            @Override public int compare(DeviceReading lhs, DeviceReading rhs) {
                return lhs.getMeaning().compareTo(rhs.getMeaning());
            }
        });
    }

    public List<DeviceReading> loadWatchReadings() {
        return sReadingsWatch;
    }

    public void savePhoneData(String manufacturer, String model, int sdk) {
        PREFS.edit().putString(PREFS_PHONE_MANUF, manufacturer).apply();
        PREFS.edit().putString(PREFS_PHONE_MODEL, model).apply();
        PREFS.edit().putInt(PREFS_PHONE_SDK, sdk).apply();
    }

    public String getPhoneManufacturer() {return PREFS.getString(PREFS_PHONE_MANUF, null); }

    public String getPhoneModel() {return PREFS.getString(PREFS_PHONE_MODEL, null); }

    public int getPhoneSdk() {return PREFS.getInt(PREFS_PHONE_SDK, 0); }
}
