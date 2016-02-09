package io.relayr.iotsmartphone;

import android.content.Context;
import android.content.SharedPreferences;

import io.relayr.java.model.Device;
import io.relayr.java.model.User;

public class Storage {

    private static Storage singleton = new Storage();

    private static Device sDevice;
    private final SharedPreferences PREFS;

    private Storage() {
        PREFS = IotApplication.context().getSharedPreferences(
                "io.relayr.iotsp", Context.MODE_PRIVATE);
    }

    public static Storage instance() {return singleton;}

    public Device getDevice() {return sDevice;}

    public void saveDevice(Device device) {sDevice = device;}

    public void saveSettings(boolean[] settings) {
        PREFS.edit().putInt("io.relayr.iotsp.settings.total", settings.length).apply();
        PREFS.edit().putInt("io.relayr.iotsp.settings.value", booleansToInt(settings)).apply();
    }

    public boolean[] loadSettings(int length) {
        final int intValue = PREFS.getInt("io.relayr.iotsp.settings.value", 0);
        final int savedLength = PREFS.getInt("io.relayr.iotsp.settings.total", 0);

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
        PREFS.edit().putBoolean("io.relayr.iotsp.permission.location", granted).apply();
    }

    public boolean locationGranted() {
        return PREFS.getBoolean("io.relayr.iotsp.permission.location", false);
    }

    public void clear() {
        PREFS.edit().clear().apply();
    }

    public void saveUser(User user) {
        PREFS.edit().putString("io.relayr.username", user.getName()).apply();
        PREFS.edit().putString("io.relayr.userId", user.getId()).apply();
    }

    public String getUsername() {
        return PREFS.getString("io.relayr.username", "");
    }
}
