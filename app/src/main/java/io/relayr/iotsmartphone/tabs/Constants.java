package io.relayr.iotsmartphone.tabs;

import io.relayr.java.model.action.Reading;

public class Constants {

    public static class DeviceModelEvent {
        public DeviceModelEvent() {}
    }

    public static class HistoryEvent {
        private final String meaning;
        private final String path;

        public HistoryEvent(String meaning, String path) {
            this.meaning = meaning;
            this.path = path;
        }

        public String getMeaning() {
            return meaning;
        }

        public String getPath() {
            return path;
        }
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
