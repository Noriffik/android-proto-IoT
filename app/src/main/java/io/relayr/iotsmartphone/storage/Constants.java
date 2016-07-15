package io.relayr.iotsmartphone.storage;

import java.util.HashMap;
import java.util.Map;

public class Constants {

    public static final int REQUEST_RESOLVE_ERROR = 1000;

    public static final int SAMPLING_COMPLEX = 100;
    public static final int SAMPLING_SIMPLE = 3;
    public static final int SAMPLING_MAX = 28;
    public static final int SAMPLING_PHONE_MIN = 2;
    public static final int SAMPLING_WATCH_MIN = 5;

    public static final int MAX_POINTS = 500;

    public static final int NOTIF_FLASH = 23761;
    public static final int NOTIF_SOUND = 23762;
    public static final int NOTIF_VIB = 23763;

    public static final String ACTIVATE_PATH = "/activate";
    public static final String ACTIVATE = "activate";

    public static final String SENSOR_ACCEL_PATH = "/acceleration";
    public static final String SENSOR_ACCEL = "acceleration";
    public static final String SENSOR_BATTERY_PATH = "/battery";
    public static final String SENSOR_BATTERY = "battery";
    public static final String SENSOR_LIGHT_PATH = "/luminosity";
    public static final String SENSOR_LIGHT = "luminosity";
    public static final String SENSOR_TOUCH_PATH = "/touch";
    public static final String SENSOR_TOUCH = "touch";

    public static final String SAMPLING_PATH = "/sampling";
    public static final String SAMPLING_MEANING = "meaning";
    public static final String SAMPLING = "sampling";

    public static final String DEVICE_INFO_PATH = "/device_info";
    public static final String DEVICE_MANUFACTURER = "manufacturer";
    public static final String DEVICE_MODEL = "model";
    public static final String DEVICE_SDK = "sdk";

    public static final Map<String, Integer> defaultSizes = new HashMap<>();

    static {
        defaultSizes.clear();
        defaultSizes.put("acceleration", 70);
        defaultSizes.put("angularSpeed", 70);
        defaultSizes.put("luminosity", 25);
        defaultSizes.put("touch", 50);
        defaultSizes.put("batteryLevel", 15);
        defaultSizes.put("rssi", 15);
        defaultSizes.put("location", 1);
    }

    public enum DeviceType {PHONE, WATCH}

    public static class DeviceModelEvent {
        public DeviceModelEvent() {}
    }

    public static class DeviceChange {
        private final DeviceType type;

        public DeviceChange(DeviceType type) {
            this.type = type;
        }

        public DeviceType getType() {
            return type;
        }
    }

    public static class WatchSamplingUpdate {
        private final String meaning;
        private final int sampling;

        public WatchSamplingUpdate(String meaning, int sampling) {
            this.meaning = meaning;
            this.sampling = sampling;
        }

        public String getMeaning() {
            return meaning;
        }

        public int getSampling() {
            return sampling;
        }
    }

    public static class PhoneSamplingUpdate {
        private final String meaning;

        public PhoneSamplingUpdate(String meaning) {
            this.meaning = meaning;
        }

        public String getMeaning() {
            return meaning;
        }
    }

    public static class CloudConnected {
        public CloudConnected() {}
    }

    public static class Tutorial {
        private final int position;

        public Tutorial(int position) {this.position = position;}

        public int getPosition() {
            return position;
        }
    }
}
