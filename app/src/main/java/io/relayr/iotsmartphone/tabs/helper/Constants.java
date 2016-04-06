package io.relayr.iotsmartphone.tabs.helper;

import io.relayr.java.model.action.Reading;

public class Constants {

    public enum DeviceType {PHONE, WATCH}

    public static class DeviceModelEvent {
        public DeviceModelEvent() {}
    }

    public static class ReadingEvent {
        private Reading reading;

        public ReadingEvent(Reading reading) {
            this.reading = reading;
        }

        public Reading getReading() {
            return reading;
        }
    }
}
