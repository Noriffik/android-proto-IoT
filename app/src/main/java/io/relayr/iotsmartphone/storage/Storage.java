package io.relayr.iotsmartphone.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.relayr.iotsmartphone.IotApplication;
import io.relayr.iotsmartphone.handler.ReadingHandler;
import io.relayr.iotsmartphone.handler.RuleHandler;
import io.relayr.java.model.Device;
import io.relayr.java.model.models.transport.DeviceCommand;
import io.relayr.java.model.models.transport.DeviceReading;

import static io.relayr.iotsmartphone.storage.Constants.DeviceType.PHONE;
import static io.relayr.iotsmartphone.storage.Constants.DeviceType.WATCH;

public class Storage {

    public static final String MODEL_PHONE = "86e0a7d7-5e18-449c-b7aa-f3b089c33b67";
    public static final String MODEL_WATCH = "b42cc83d-5766-406c-872d-9294d96bdf69";

    private static final String PREFS_NAME = "io.relayr.iotsp.settings";

    private static final String PREFS_WARNING = "io.relayr.warning";
    private static final String PREFS_SETTINGS_LOCATION = "io.relayr.iotsp.permission.location";

    private static final String ACTIVE_PHONE = "active_p";
    private static final String ACTIVE_WATCH = "active_w";
    private static final String FREQUENCY_PHONE = "freq_p";
    private static final String FREQUENCY_WATCH = "freq_w";

    private static final String PHONE_ID = "phone_id";
    private static final String PHONE_NAME = "phone_name";
    private static final String PHONE_SDK = "phone_sdk";

    private static final String WATCH_ID = "watch_id";
    private static final String WATCH_NAME = "watch_name";
    private static final String WATCH_SDK = "watch_sdk";

    private static final String BACKGROUND_UPLOAD = "background";
    private static final String RULE_ID = "rule_id";

    private static List<DeviceReading> sReadingsPhone = new ArrayList<>();
    private static List<DeviceReading> sReadingsWatch = new ArrayList<>();
    private static List<DeviceCommand> sCommandsPhone = new ArrayList<>();

    private static Device sPhoneDevice;
    private static Device sWatchDevice;

    private final static Storage singleton = new Storage();
    private final SharedPreferences PREFS;
    private String sPhoneId;
    private String sWatchId;

    private Storage() {
        PREFS = IotApplication.context().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static Map<String, Integer> READINGS_PRIORITY = new HashMap<String, Integer>() {
        {
            put("acceleration", 1);
            put("angularSpeed", 2);
            put("luminosity", 3);
            put("location", 4);
            put("touch", 5);
            put("batteryLevel", 6);
            put("rssi", 7);
        }
    };
    public static Map<String, Integer> FREQS_PHONE = new HashMap<String, Integer>() {
        {
            put("acceleration", Storage.instance().loadFrequency("acceleration", PHONE));
            put("angularSpeed", Storage.instance().loadFrequency("angularSpeed", PHONE));
            put("batteryLevel", Storage.instance().loadFrequency("batteryLevel", PHONE));
            put("luminosity", Storage.instance().loadFrequency("luminosity", PHONE));
            put("location", Storage.instance().loadFrequency("location", PHONE));
            put("touch", Storage.instance().loadFrequency("touch", PHONE));
            put("rssi", Storage.instance().loadFrequency("rssi", PHONE));
        }
    };

    public static Map<String, Integer> FREQS_WATCH = new HashMap<String, Integer>() {
        {
            put("acceleration", Storage.instance().loadFrequency("acceleration", WATCH));
            put("batteryLevel", Storage.instance().loadFrequency("batteryLevel", WATCH));
            put("luminosity", Storage.instance().loadFrequency("luminosity", WATCH));
            put("touch", Storage.instance().loadFrequency("touch", WATCH));
        }
    };

    public static Map<String, Boolean> ACTIVITY_PHONE = new HashMap<String, Boolean>() {
        {
            put("acceleration", Storage.instance().loadActivity("acceleration", PHONE));
            put("angularSpeed", Storage.instance().loadActivity("angularSpeed", PHONE));
            put("batteryLevel", Storage.instance().loadActivity("batteryLevel", PHONE));
            put("luminosity", Storage.instance().loadActivity("luminosity", PHONE));
            put("location", Storage.instance().loadActivity("location", PHONE));
            put("touch", Storage.instance().loadActivity("touch", PHONE));
            put("rssi", Storage.instance().loadActivity("rssi", PHONE));
        }
    };

    public static Map<String, Boolean> ACTIVITY_WATCH = new HashMap<String, Boolean>() {
        {
            put("acceleration", Storage.instance().loadActivity("acceleration", WATCH));
            put("batteryLevel", Storage.instance().loadActivity("batteryLevel", WATCH));
            put("luminosity", Storage.instance().loadActivity("luminosity", WATCH));
            put("touch", Storage.instance().loadActivity("touch", WATCH));
        }
    };

    public static Storage instance() {return singleton;}

    public void saveDevice(Device device, Constants.DeviceType type) {
        if (type == PHONE) {
            sPhoneDevice = device;
            sPhoneId = device.getId();
            PREFS.edit().putString(PHONE_ID, device.getId()).apply();
            PREFS.edit().putString(PHONE_NAME, device.getName()).apply();
        } else {
            sWatchDevice = device;
            sWatchId = device.getId();
            PREFS.edit().putString(WATCH_ID, device.getId()).apply();
            PREFS.edit().putString(WATCH_NAME, device.getName()).apply();
        }
    }

    public Device getDevice(Constants.DeviceType type) {
        if (type == PHONE) return sPhoneDevice;
        else return sWatchDevice;
    }

    public void saveActivity(String meaning, Constants.DeviceType type, boolean active) {
        if (type == PHONE) ACTIVITY_PHONE.put(meaning, active);
        else ACTIVITY_WATCH.put(meaning, active);
        PREFS.edit().putBoolean((type == PHONE ? ACTIVE_PHONE : ACTIVE_WATCH) + meaning, active).apply();
    }

    private boolean loadActivity(String meaning, Constants.DeviceType type) {
        return PREFS.getBoolean((type == PHONE ? ACTIVE_PHONE : ACTIVE_WATCH) + meaning, false);
    }

    public int saveFrequency(String meaning, Constants.DeviceType type, int freq) {
        int frequency = ReadingHandler.isComplex(meaning) ? freq * Constants.SAMPLING_COMPLEX : freq;
        if (type == PHONE) FREQS_PHONE.put(meaning, frequency);
        else FREQS_WATCH.put(meaning, frequency);

        PREFS.edit().putInt((type == PHONE ? FREQUENCY_PHONE : FREQUENCY_WATCH) + meaning, frequency).apply();
        return frequency;
    }

    public int loadFrequency(String meaning, Constants.DeviceType type) {
        int sampling = ReadingHandler.isComplex(meaning) ? Constants.SAMPLING_COMPLEX : Constants.SAMPLING_SIMPLE;
        sampling *= type == PHONE ? Constants.SAMPLING_PHONE_MIN : Constants.SAMPLING_WATCH_MIN;
        return PREFS.getInt((type == PHONE ? FREQUENCY_PHONE : FREQUENCY_WATCH) + meaning, sampling);
    }

    public void locationPermission(boolean granted) {
        PREFS.edit().putBoolean(PREFS_SETTINGS_LOCATION, granted).apply();
    }

    public boolean locationGranted() {
        return PREFS.getBoolean(PREFS_SETTINGS_LOCATION, true);
    }

    public void savePhoneData(String manufacturer, String model, int sdk) {
        if (PREFS.getString(PHONE_NAME, null) == null)
            PREFS.edit().putString(PHONE_NAME, manufacturer + " " + model).apply();
        PREFS.edit().putInt(PHONE_SDK, sdk).apply();
    }

    public void saveWatchData(String manufacturer, String model, int sdk) {
        if (PREFS.getString(WATCH_NAME, null) == null)
            PREFS.edit().putString(WATCH_NAME, manufacturer + " " + model).apply();
        PREFS.edit().putInt(WATCH_SDK, sdk).apply();
    }

    public void updateDeviceName(String name, Constants.DeviceType type) {
        if (type == PHONE) PREFS.edit().putString(PHONE_NAME, name).apply();
        else if (type == WATCH) PREFS.edit().putString(WATCH_NAME, name).apply();
    }

    public String getDeviceName(Constants.DeviceType type) {
        if (type == PHONE) return PREFS.getString(PHONE_NAME, null);
        else return PREFS.getString(WATCH_NAME, null);
    }

    public String getDeviceId(Constants.DeviceType type) {
        if (type == PHONE) {
            if (sPhoneId == null) sPhoneId = PREFS.getString(PHONE_ID, null);
            return sPhoneId;
        } else {
            if (sWatchId == null) sWatchId = PREFS.getString(WATCH_ID, null);
            return sWatchId;
        }
    }

    public Constants.DeviceType getDeviceType(String deviceId) {
        if (deviceId == null) return null;
        if (deviceId.equals(PREFS.getString(PHONE_ID, null))) return PHONE;
        else if (deviceId.equals(PREFS.getString(WATCH_ID, null))) return WATCH;
        else return null;
    }

    public void saveRule(String id) {
        PREFS.edit().putString(RULE_ID, id).apply();
    }

    public String loadRule() {
        return PREFS.getString(RULE_ID, null);
    }

    public int getDeviceSdk(Constants.DeviceType type) {
        if (type == PHONE) return PREFS.getInt(PHONE_SDK, 0);
        else return PREFS.getInt(WATCH_SDK, 0);
    }

    public void savePhoneReadings(List<DeviceReading> readings) {
        sReadingsPhone.clear();
        sReadingsPhone.addAll(readings);
        Collections.sort(sReadingsPhone, new Comparator<DeviceReading>() {
            @Override public int compare(DeviceReading lhs, DeviceReading rhs) {
                return READINGS_PRIORITY.get(lhs.getMeaning()) - READINGS_PRIORITY.get(rhs.getMeaning());
            }
        });
    }

    public List<DeviceReading> loadReadings(Constants.DeviceType type) {
        if (type == PHONE) return sReadingsPhone;
        else return sReadingsWatch;
    }

    public void savePhoneCommands(List<DeviceCommand> commands) {
        sCommandsPhone.clear();
        sCommandsPhone.addAll(commands);
        Collections.sort(sCommandsPhone, new Comparator<DeviceCommand>() {
            @Override public int compare(DeviceCommand lhs, DeviceCommand rhs) {
                return lhs.getName().compareToIgnoreCase(rhs.getName());
            }
        });
    }

    public List<DeviceCommand> loadCommands(Constants.DeviceType type) {
        if (type == PHONE) return sCommandsPhone;
        else return new ArrayList<>();
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

    public void activate(Constants.DeviceType type) {
        changeActivity(type, true);
    }

    private void changeActivity(Constants.DeviceType type, boolean activity) {
        Set<String> meanings;
        if (type == PHONE) {
            meanings = new HashSet<>(ACTIVITY_PHONE.keySet());
            for (String meaning : meanings) saveActivity(meaning, PHONE, activity);
        } else {
            meanings = new HashSet<>(ACTIVITY_WATCH.keySet());
            for (String meaning : meanings) saveActivity(meaning, WATCH, activity);
        }
    }

    public void logOut() {
        changeActivity(PHONE, false);
        changeActivity(WATCH, false);

        PREFS.edit().remove(PHONE_ID).apply();
        PREFS.edit().remove(WATCH_ID).apply();
        PREFS.edit().remove(PREFS_WARNING).apply();

        RuleHandler.clearAfterLogOut();
        ReadingHandler.clearAfterLogOut();
        System.gc();
    }

    public boolean isActiveInBackground() {
        return PREFS.getBoolean(BACKGROUND_UPLOAD, false);
    }

    public void activeInBackground(boolean active) {
        PREFS.edit().putBoolean(BACKGROUND_UPLOAD, active).apply();

    }
}

