package io.relayr.iotsmartphone;

import android.content.Context;
import android.content.SharedPreferences;

import io.relayr.java.model.Device;
import io.relayr.java.model.User;

public class Storage {

    public static final String MODEL_ID = "86e0a7d7-5e18-449c-b7aa-f3b089c33b67";
    private static final String PREFS_NAME = "io.relayr.iotsp";
    private static final String PREFS_SETTINGS_TOTAL = "io.relayr.iotsp.settings.total";
    private static final String PREFS_SETTINGS_VALUE = "io.relayr.iotsp.settings.value";
    private static final String PREFS_SETTINGS_LOCATION = "io.relayr.iotsp.permission.location";
    private static final String PREFS_USERNAME = "io.relayr.username";
    private static final String PREFS_USER_ID = "io.relayr.userId";
    private static final String PREFS_WARNING = "io.relayr.warning";

    private static final String PREFS_DELAY = "io.relayr.delay";
    private static final String PREFS_INTENSITY = "io.relayr.intensity";

    private final static Storage singleton = new Storage();
    private static Device sDevice;
    private final SharedPreferences PREFS;

    private Storage() {
        PREFS = IotApplication.context().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static Storage instance() {return singleton;}

    public Device getDevice() {return sDevice;}

    public void saveDevice(Device device) {sDevice = device;}

    public void saveSettings(boolean[] settings) {
        PREFS.edit().putInt(PREFS_SETTINGS_TOTAL, settings.length).apply();
        PREFS.edit().putInt(PREFS_SETTINGS_VALUE, booleansToInt(settings)).apply();
    }

    public boolean[] loadSettings(int length) {
        final int intValue = PREFS.getInt(PREFS_SETTINGS_VALUE, 0);
        final int savedLength = PREFS.getInt(PREFS_SETTINGS_TOTAL, 0);

        if (savedLength == 0 || savedLength < length) return new boolean[length];
        else return intToBooleans(intValue, savedLength);
    }

    private boolean[] intToBooleans(int intValue, int total) {
        boolean[] settings = new boolean[total];
        for (int i = 0; i < total; i++)
            settings[total - 1 - i] = (1 << i & intValue) != 0;
        return settings;
    }

    private int booleansToInt(boolean[] arr) {
        int n = 0;
        for (boolean b : arr) n = (n << 1) | (b ? 1 : 0);
        return n;
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

    public void saveUser(User user) {
        PREFS.edit().putString(PREFS_USERNAME, user.getName()).apply();
        PREFS.edit().putString(PREFS_USER_ID, user.getId()).apply();
    }

    public String getUsername() {
        return PREFS.getString(PREFS_USERNAME, "");
    }

    public void warningShown() {
        PREFS.edit().putBoolean(PREFS_WARNING, true).apply();
    }

    public boolean isWarningShown() {
        return PREFS.getBoolean(PREFS_WARNING, false);
    }

    public void saveDelay(int delay) {
        PREFS.edit().putInt(PREFS_DELAY, delay).apply();
    }

    public int loadDelay() {
        return PREFS.getInt(PREFS_DELAY, 10);
    }

    public void saveIntensity(int intensity) {
        PREFS.edit().putInt(PREFS_INTENSITY, intensity).apply();
    }

    public int loadIntensity() {
        return PREFS.getInt(PREFS_INTENSITY, 1);
    }
}
