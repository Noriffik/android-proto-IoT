package io.relayr.iotsmartphone.widget;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import io.relayr.android.RelayrSdk;
import io.relayr.iotsmartphone.MainActivity;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.Storage;
import io.relayr.java.model.AccelGyroscope;
import io.relayr.java.model.action.Reading;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.LOCATION_SERVICE;
import static android.content.Context.SENSOR_SERVICE;
import static android.content.Context.WIFI_SERVICE;
import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.SensorManager.SENSOR_DELAY_NORMAL;
import static android.location.LocationManager.GPS_PROVIDER;
import static android.net.ConnectivityManager.TYPE_WIFI;
import static android.os.BatteryManager.EXTRA_LEVEL;
import static android.os.BatteryManager.EXTRA_SCALE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.widget.Toast.LENGTH_SHORT;

public class SettingsView extends BasicView implements SensorEventListener, LocationListener {

    @InjectView(R.id.send_battery_switch) SwitchCompat mBatterySwitch;
    @InjectView(R.id.send_wifi_switch) SwitchCompat mWiFiSwitch;
    @InjectView(R.id.send_location_switch) SwitchCompat mLocSwitch;
    @InjectView(R.id.send_acceleration_switch) SwitchCompat mAccelSwitch;

    @InjectView(R.id.message_text) EditText mMessage;

    @InjectView(R.id.receive_commands_switch) SwitchCompat mCommandsSwitch;

    private Flash mFlash;
    private boolean mHasFlash;

    private SensorManager mSensorManager;
    private LocationManager mLocationManager;
    private ConnectivityManager mConnectivityManager;
    private WifiManager mWifiManager;

    private long mNow = System.currentTimeMillis();

    //battery, wifi, location, acceleration
    private boolean[] mDataSwitches = new boolean[]{false, false, false, false};
    private boolean mReceiveCommands = false;
    private String mDeviceId;

    public SettingsView(Context context) {
        super(context);
    }

    public SettingsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SettingsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ButterKnife.inject(this);

        mDeviceId = Storage.instance().getDevice().getId();

        mFlash = new Flash();
        mHasFlash = mFlash.hasFlash(getContext());
        mFlash.open();

        mSensorManager = (SensorManager) getContext().getSystemService(SENSOR_SERVICE);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(TYPE_ACCELEROMETER), SENSOR_DELAY_NORMAL);

        mConnectivityManager = (ConnectivityManager) getContext().getSystemService(CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) getContext().getSystemService(WIFI_SERVICE);

        mLocationManager = (LocationManager) getContext().getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(getContext(), ACCESS_FINE_LOCATION) != PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((MainActivity) getContext(), new String[]{ACCESS_FINE_LOCATION}, 2376);
        }
        mLocationManager.requestLocationUpdates(GPS_PROVIDER, 0, 0, this);

        mBatterySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mDataSwitches[0] = isChecked;
                monitorBattery();
            }
        });
        mWiFiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mDataSwitches[1] = isChecked;
                monitorWiFi();
            }
        });
        mLocSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mDataSwitches[2] = isChecked;
                monitorLocation();
            }
        });
        mAccelSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mDataSwitches[3] = isChecked;
            }
        });

        mCommandsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mReceiveCommands = isChecked;
                if (isChecked) subscribeToCommands();
            }
        });
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ButterKnife.reset(this);
        mSensorManager.unregisterListener(this);

        if (mFlash != null) mFlash.close();
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != TYPE_ACCELEROMETER) return;
        if (!mDataSwitches[3]) return;

        final AccelGyroscope.Acceleration acceleration = new AccelGyroscope.Acceleration();
        switch (getDisplay().getRotation()) {
            case Surface.ROTATION_0:
                acceleration.x = event.values[0];
                acceleration.y = event.values[1];
                acceleration.z = event.values[2];
                break;
            case Surface.ROTATION_90:
                acceleration.x = -event.values[1];
                acceleration.y = event.values[0];
                acceleration.z = event.values[2];
                break;
            case Surface.ROTATION_180:
                acceleration.x = -event.values[0];
                acceleration.y = -event.values[1];
                acceleration.z = event.values[2];
                break;
            case Surface.ROTATION_270:
                acceleration.x = event.values[1];
                acceleration.y = -event.values[0];
                acceleration.z = event.values[2];
        }

        publish(new Reading(0, 0, "acceleration", "", acceleration));
    }

    @Override public void onLocationChanged(Location location) {
        if (!mDataSwitches[2]) return;
        publishAddress(location.getLatitude(), location.getLongitude());
    }

    @OnClick(R.id.message_send)
    public void onMessageSend() {
        final String message = mMessage.getText().toString().trim();
        if (message.isEmpty()) return;

        mMessage.setText("");
        publish(new Reading(mNow, mNow, "message", "", message));
    }

    public void refreshData() {
        mNow = System.currentTimeMillis();
        monitorWiFi();
        monitorBattery();
        monitorLocation();
    }

    private void monitorBattery() {
        if (!mDataSwitches[0]) return;

        Intent batteryIntent = getContext().registerReceiver(null, new IntentFilter(ACTION_BATTERY_CHANGED));
        int level = batteryIntent != null ? batteryIntent.getIntExtra(EXTRA_LEVEL, -1) : 0;
        int scale = batteryIntent != null ? batteryIntent.getIntExtra(EXTRA_SCALE, -1) : 0;

        float bat;
        if (level == -1 || scale == -1) bat = 50.0f;
        else bat = ((float) level / (float) scale) * 100.0f;

        publish(new Reading(mNow, mNow, "batteryLevel", "", bat));
    }

    private void monitorWiFi() {
        if (!mDataSwitches[1]) return;

        if (!checkWifi(mConnectivityManager))
            Toast.makeText(getContext(), "Not connected to Wifi", LENGTH_SHORT).show();

        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo != null)
            publish(new Reading(mNow, mNow, "rssi", "", wifiInfo.getRssi()));
    }

    private boolean checkWifi(ConnectivityManager cm) {
        if (SDK_INT >= LOLLIPOP) {
            for (Network net : cm.getAllNetworks()) {
                NetworkInfo networkInfo = cm.getNetworkInfo(net);
                if (networkInfo != null && networkInfo.getType() == TYPE_WIFI)
                    return networkInfo.isConnected();
            }
        } else {
            NetworkInfo networkInfo = cm.getNetworkInfo(TYPE_WIFI);
            return networkInfo != null && networkInfo.isConnected();
        }
        return false;
    }

    private void monitorLocation() {
        if (!mDataSwitches[2]) return;

        if (ActivityCompat.checkSelfPermission(getContext(), ACCESS_FINE_LOCATION) == PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED) {
            Location location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null)
                publishAddress(location.getLatitude(), location.getLongitude());
            else
                Toast.makeText(getContext(), "Location is turned off...", Toast.LENGTH_SHORT).show();
        }
    }

    public void publishAddress(double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            Address obj = addresses.get(0);
            String address = obj.getCountryName() + ", ";
            address += obj.getAddressLine(1) + ", ";
            address += obj.getAddressLine(0);

            publish(new Reading(mNow, mNow, "address", "", address));
        } catch (IOException e) {
            Toast.makeText(getContext(), "Geocoder problem.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void publish(final Reading reading) {
        if (reading == null || reading.meaning == null) return;

        RelayrSdk.getWebSocketClient()
                .publish(mDeviceId, reading)
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<Void>() {
                    @Override public void onCompleted() {}

                    @Override public void onError(Throwable e) {
                        Log.e("PUBLISH", "ERROR");
                        e.printStackTrace();
                    }

                    @Override public void onNext(Void aVoid) {}
                });
    }

    private void flashOn() {
        if (!mHasFlash) Toast.makeText(getContext(), "Flash not available", LENGTH_SHORT).show();
        else if (mFlash != null) mFlash.on();
    }

    private void flashOff() {
        if (!mHasFlash) Toast.makeText(getContext(), "Flash not available", LENGTH_SHORT).show();
        else if (mFlash != null) mFlash.off();
    }

    private void subscribeToCommands() {
        RelayrSdk.getWebSocketClient().subscribe(Storage.instance().getDevice())
                .subscribe(new Subscriber<Reading>() {
                    @Override public void onCompleted() {}

                    @Override public void onError(Throwable e) {}

                    @Override public void onNext(Reading reading) {}
                });
    }

    //NOT implemented
    @Override public void onProviderEnabled(String provider) {}

    //NOT implemented
    @Override public void onProviderDisabled(String provider) {}

    //NOT implemented
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    //NOT implemented
    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
}
