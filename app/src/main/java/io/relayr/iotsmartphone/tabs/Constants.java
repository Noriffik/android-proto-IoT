package io.relayr.iotsmartphone.tabs;

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

}
