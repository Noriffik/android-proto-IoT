package io.relayr.iotsmartphone.helper;

import io.relayr.java.model.action.Reading;

public interface ControlListener {

    void onDeviceCreated();

    void startSettings();

    void openDashboard();

    void activateWearable(boolean active);

    void publishReading(Reading reading);

    void showNotification(boolean show, boolean wearEnabled);
}
