package io.relayr.iotsmartphone;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;

import io.relayr.java.model.Device;

public class Storage {

    private static Storage singleton = new Storage();

    private static Device sDevice;

    private Storage() {}

    public static Storage instance() {return singleton;}

    public Device getDevice() {return sDevice;}

    public void saveDevice(Device device) {sDevice = device;}

    public void saveSettings(Context context, boolean[] settings) {
        SharedPreferences prefs = context.getSharedPreferences(
                "io.relayr.iotsp", Context.MODE_PRIVATE);

        prefs.edit().putInt("io.relayr.iotsp.settings.total", settings.length).apply();
        prefs.edit().putInt("io.relayr.iotsp.settings.value", booleansToInt(settings)).apply();
    }

    public boolean[] loadSettings(Context context, int length) {
        SharedPreferences prefs = context.getSharedPreferences(
                "io.relayr.iotsp", Context.MODE_PRIVATE);

        final int intValue = prefs.getInt("io.relayr.iotsp.settings.value", 0);
        final int savedLength = prefs.getInt("io.relayr.iotsp.settings.total", 0);

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
}
