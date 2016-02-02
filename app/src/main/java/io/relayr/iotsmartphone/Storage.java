package io.relayr.iotsmartphone;

import io.relayr.java.model.Device;

public class Storage {

    private static Storage singleton = new Storage();

    private static Device sDevice;

    private Storage() {}

    public static Storage instance() {return singleton;}

    public Device getDevice() {return sDevice;}

    public void saveDevice(Device device) {sDevice = device;}

}
