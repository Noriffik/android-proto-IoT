package io.relayr.iotsmartphone.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import io.relayr.android.RelayrSdk;
import io.relayr.iotsmartphone.MainActivity;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.Storage;
import io.relayr.iotsmartphone.helper.FlashHelper;
import io.relayr.iotsmartphone.helper.SoundHelper;
import io.relayr.java.model.AccelGyroscope;
import io.relayr.java.model.action.Command;
import io.relayr.java.model.action.Reading;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.INPUT_METHOD_SERVICE;
import static android.content.Context.LOCATION_SERVICE;
import static android.content.Context.SENSOR_SERVICE;
import static android.content.Context.WIFI_SERVICE;
import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.SensorManager.SENSOR_DELAY_NORMAL;
import static android.net.ConnectivityManager.TYPE_WIFI;
import static android.os.BatteryManager.EXTRA_LEVEL;
import static android.os.BatteryManager.EXTRA_SCALE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.support.v4.content.ContextCompat.checkSelfPermission;
import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;

public class SendReceiveView extends BasicView implements SensorEventListener, LocationListener {

    @InjectView(R.id.send_battery_switch) SwitchCompat mBatterySwitch;
    @InjectView(R.id.send_wifi_switch) SwitchCompat mWiFiSwitch;
    @InjectView(R.id.send_location_switch) SwitchCompat mLocSwitch;
    @InjectView(R.id.send_acceleration_switch) SwitchCompat mAccelSwitch;

    @InjectView(R.id.message_text) EditText mMessage;
    @InjectView(R.id.message_send) ImageView mIconSend;

    @InjectView(R.id.receive_flash_switch) SwitchCompat mFlashSwitch;
    @InjectView(R.id.receive_sound_switch) SwitchCompat mSoundSwitch;

    private FlashHelper mFlash;
    private SoundHelper mSound;
    private WifiManager mWifiManager;
    private SensorManager mSensorManager;
    private LocationManager mLocationManager;
    private ConnectivityManager mConnectivityManager;

    private long mNow = System.currentTimeMillis();

    // wifi, battery, location, acceleration, flash, sound
    private boolean[] mSettings = new boolean[]{false, false, false, false, false, false};

    private Subscription mCommandsSubscription = null;
    private int mSensorChange = 0;

    private int mPublishDelay = 10;
    private int mAccIntensity = 0;

    private Subscription mRefreshSubs;

    public SendReceiveView(Context context) {
        super(context);
    }

    public SendReceiveView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SendReceiveView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ButterKnife.inject(this);

        mPublishDelay = Storage.instance().loadDelay() + 1;
        mAccIntensity = 15 / (Storage.instance().loadIntensity());
        Crashlytics.log(Log.INFO, "SRV", "Delay " + mPublishDelay + " intensity " + mAccIntensity);

        mSettings = Storage.instance().loadSettings(mSettings.length);
        setUpSwitches();

        mMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                mIconSend.setImageResource(count > 0 ? R.drawable.action_send_active : R.drawable.action_send_inactive);
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        mMessage.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    onMessageSend();
                    return true;
                }
                return false;
            }
        });

        mRefreshSubs = Observable.interval(mPublishDelay, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                    @Override public void onCompleted() {}

                    @Override public void onError(Throwable e) {}

                    @Override public void onNext(Long aLong) {refreshData();}
                });
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mRefreshSubs != null) mRefreshSubs.unsubscribe();

        if (mFlash != null) mFlash.close();
        if (mSound != null) mSound.close();
        mFlash = null;
        mSound = null;

        ButterKnife.reset(this);

        if (mSensorManager != null) turnSensorOff();
        if (mLocationManager != null) turnOffLocation();
        if (mCommandsSubscription != null) mCommandsSubscription.unsubscribe();
    }

    @Override protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility != VISIBLE && (mSettings[0] || mSettings[1] || mSettings[2] || mSettings[3]))
            Toast.makeText(getContext(), R.string.sv_sending_in_bck, LENGTH_LONG).show();
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void onSensorChanged(SensorEvent e) {
        if (!mSettings[3]) return;
        if (e.sensor.getType() != TYPE_ACCELEROMETER) return;
        if (mSensorChange++ % mAccIntensity != 0) return;

        int rotation;
        if (SDK_INT >= JELLY_BEAN_MR1)
            rotation = getDisplay().getRotation();
        else  //noinspection deprecation
            rotation = getDisplay().getOrientation();

        publishAcceleration(e, rotation);
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.message_send)
    public void onMessageSend() {
        final String message = mMessage.getText().toString().trim();
        if (message.isEmpty()) return;

        hideKeyboard();

        mMessage.setText("");
        mIconSend.setImageResource(R.drawable.action_send_inactive);
        publishReading(new Reading(mNow, mNow, "message", "/", message));
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.publish_title)
    public void onSettingsClicked() {
        mListener.startSettings();
    }

    public void refreshData() {
        mNow = System.currentTimeMillis();
        monitorWiFi();
        monitorBattery();
        monitorLocation();
    }

    private void setUpSwitches() {
        mWiFiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSettings[0] = isChecked;
                Storage.instance().saveSettings(mSettings);
                if (isChecked) {
                    initWifiManager();
                    monitorWiFi();
                }
            }
        });
        mWiFiSwitch.setChecked(mSettings[0]);

        mBatterySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSettings[1] = isChecked;
                Storage.instance().saveSettings(mSettings);
                monitorBattery();
            }
        });
        mBatterySwitch.setChecked(mSettings[1]);

        mLocSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSettings[2] = isChecked;
                Storage.instance().saveSettings(mSettings);
                if (isChecked) initLocationManager();
                else turnOffLocation();
            }
        });
        mLocSwitch.setChecked(mSettings[2]);

        mAccelSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSettings[3] = isChecked;
                Storage.instance().saveSettings(mSettings);
                if (isChecked) initSensorManager();
                if (isChecked) showAccelerometerWarning();
                else turnSensorOff();
            }
        });
        mAccelSwitch.setChecked(mSettings[3]);

        mFlashSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSettings[4] = isChecked;
                Storage.instance().saveSettings(mSettings);
                if (isChecked) {
                    createFlashHelper();
                    subscribeToCommands();
                }
            }
        });
        mFlashSwitch.setChecked(mSettings[4]);

        mSoundSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSettings[5] = isChecked;
                Storage.instance().saveSettings(mSettings);
                if (isChecked) {
                    createSoundHelper();
                    subscribeToCommands();
                }
            }
        });
        mSoundSwitch.setChecked(mSettings[5]);
    }

    private void initSensorManager() {
        if (mSensorManager == null)
            mSensorManager = (SensorManager) getContext().getSystemService(SENSOR_SERVICE);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(TYPE_ACCELEROMETER), SENSOR_DELAY_NORMAL);
    }

    private void turnSensorOff() {
        if (mSensorManager != null)
            mSensorManager.unregisterListener(SendReceiveView.this);
    }

    private void publishAcceleration(SensorEvent e, int rotation) {
        Reading reading = null;
        switch (rotation) {
            case Surface.ROTATION_0:
                reading = createAccelReading(e.values[0], e.values[1], e.values[2]);
                break;
            case Surface.ROTATION_90:
                reading = createAccelReading(-e.values[1], e.values[0], e.values[2]);
                break;
            case Surface.ROTATION_180:
                reading = createAccelReading(-e.values[0], -e.values[1], e.values[2]);
                break;
            case Surface.ROTATION_270:
                reading = createAccelReading(e.values[1], -e.values[0], e.values[2]);
        }
        publishReading(reading);
    }

    private Reading createAccelReading(float x, float y, float z) {
        final AccelGyroscope.Acceleration acceleration = new AccelGyroscope.Acceleration();
        acceleration.x = x;
        acceleration.y = y;
        acceleration.z = z;
        mNow = System.currentTimeMillis();
        return new Reading(mNow, mNow, "acceleration", "/", acceleration);
    }

    private void showAccelerometerWarning() {
        if (Storage.instance().isWarningShown())
            Toast.makeText(getContext(), getContext().getString(R.string.sv_warning_toast), LENGTH_LONG).show();
        else
            new AlertDialog.Builder(getContext()).setTitle(getContext().getString(R.string.sv_warning_dialog_title))
                    .setIcon(R.drawable.ic_warning)
                    .setMessage(getContext().getString(R.string.sv_warning_dialog_text))
                    .setPositiveButton(getContext().getString(R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int i) {
                            Storage.instance().warningShown();
                            dialog.dismiss();
                        }
                    }).show();
    }

    private void initWifiManager() {
        if (mWifiManager != null && mConnectivityManager != null) return;
        mWifiManager = (WifiManager) getContext().getSystemService(WIFI_SERVICE);
        mConnectivityManager = (ConnectivityManager) getContext().getSystemService(CONNECTIVITY_SERVICE);
    }

    private void monitorWiFi() {
        if (!mSettings[0]) return;

        if (!checkWifi(mConnectivityManager))
            Toast.makeText(getContext(), R.string.sv_no_wifi, LENGTH_SHORT).show();

        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo != null)
            publishReading(new Reading(mNow, mNow, "rssi", "wifi", wifiInfo.getRssi()));
    }

    private boolean checkWifi(ConnectivityManager cm) {
        if (SDK_INT >= LOLLIPOP) {
            for (Network net : cm.getAllNetworks()) {
                NetworkInfo networkInfo = cm.getNetworkInfo(net);
                if (networkInfo != null && networkInfo.getType() == TYPE_WIFI)
                    return networkInfo.isConnected();
            }
        } else {
            //noinspection deprecation
            NetworkInfo networkInfo = cm.getNetworkInfo(TYPE_WIFI);
            return networkInfo != null && networkInfo.isConnected();
        }
        return false;
    }

    private void monitorBattery() {
        if (!mSettings[1]) return;

        Intent batteryIntent = getContext().registerReceiver(null, new IntentFilter(ACTION_BATTERY_CHANGED));
        int level = batteryIntent != null ? batteryIntent.getIntExtra(EXTRA_LEVEL, -1) : 0;
        int scale = batteryIntent != null ? batteryIntent.getIntExtra(EXTRA_SCALE, -1) : 0;

        float bat;
        if (level == -1 || scale == -1) bat = 50.0f;
        else bat = ((float) level / (float) scale) * 100.0f;

        publishReading(new Reading(mNow, mNow, "batteryLevel", "/", bat));
    }

    private void initLocationManager() {
        if (Storage.instance().locationGranted()) {
            new Handler().postDelayed(new Runnable() {
                @Override public void run() {
                    if (mLocationManager == null)
                        mLocationManager = (LocationManager) getContext().getSystemService(LOCATION_SERVICE);
                    if (checkSelfPermission(getContext(), ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
                        try {
                            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, SendReceiveView.this);
                            monitorLocation();
                        } catch (Exception e) {
                            Crashlytics.log(Log.ERROR, "SRV", "GPS_PROVIDER doesn't exist.");
                            try {
                                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, SendReceiveView.this);
                                monitorLocation();
                            } catch (Exception e1) {
                                Crashlytics.log(Log.ERROR, "SRV", "NETWORK_PROVIDER doesn't exist.");
                            }
                        }
                    }
                }
            }, 500);
        }
    }

    private void monitorLocation() {
        if (!mSettings[2]) return;
        new Handler().post(new Runnable() {
            @Override public void run() {
                if (checkSelfPermission(getContext(), ACCESS_FINE_LOCATION) == PERMISSION_GRANTED &&
                        checkSelfPermission(getContext(), ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED) {
                    Location location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (location != null)
                        publishLocation(location.getLatitude(), location.getLongitude());
                    else showLocationDialog();
                }
            }
        });
    }

    public void publishLocation(double lat, double lng) {
        if (!mSettings[2]) return;

        try {
            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses.isEmpty()) return;

            Address obj = addresses.get(0);
            String address = obj.getCountryName() + ", ";
            address += obj.getAddressLine(1) + ", ";
            address += obj.getAddressLine(0);

            publishReading(new Reading(mNow, mNow, "location", "/", address));
        } catch (IOException e) {
            Toast.makeText(getContext(), R.string.sv_location_resolve_err, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void turnOffLocation() {
        if (mLocationManager != null)
            if (checkSelfPermission(getContext(), ACCESS_FINE_LOCATION) == PERMISSION_GRANTED &&
                    checkSelfPermission(getContext(), ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED)
                mLocationManager.removeUpdates(this);
    }

    private void showLocationDialog() {
        new AlertDialog.Builder(getContext()).setTitle(getContext().getString(R.string.sv_location_off_title))
                .setIcon(R.drawable.ic_warning)
                .setMessage(getContext().getString(R.string.sv_location_off_message))
                .setPositiveButton(getContext().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        getContext().startActivity(myIntent);
                    }
                })
                .setNegativeButton(getContext().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void createFlashHelper() {
        if (mSettings[4] && mFlash != null) return;

        mFlash = new FlashHelper();
        try {
            mFlash.open(getContext().getApplicationContext());
        } catch (Exception e) {
            Crashlytics.log(Log.ERROR, "SRV", "Failed to create FlashHelper.");
            e.printStackTrace();

            Toast.makeText(getContext(), R.string.sv_err_using_flash, Toast.LENGTH_SHORT).show();
            mFlash.close();
            mFlash = null;
        }
    }

    private void createSoundHelper() {
        if (mSettings[5] && mSound != null) return;
        if (mSound == null) mSound = new SoundHelper();
    }

    private void publishReading(final Reading reading) {
        if (reading == null || reading.meaning == null) return;

        RelayrSdk.getWebSocketClient()
                .publish(Storage.instance().getDevice().getId(), reading)
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<Void>() {
                    @Override public void onCompleted() {}

                    @Override public void onError(Throwable e) {
                        Crashlytics.log(Log.ERROR, "SettingsView", "publishReading - error");
                        e.printStackTrace();
                    }

                    @Override public void onNext(Void aVoid) {}
                });
    }

    private void subscribeToCommands() {
        if (!mSettings[4] && !mSettings[5]) return;

        if (mCommandsSubscription == null)
            mCommandsSubscription = RelayrSdk.getWebSocketClient()
                    .subscribeToCommands(Storage.instance().getDevice().getId())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<Command>() {
                        @Override public void onCompleted() {}

                        @Override public void onError(Throwable e) {
                            Crashlytics.log(Log.ERROR, "SettingsView", "subscribeToCommands - error");
                            e.printStackTrace();
                        }

                        @Override public void onNext(Command action) {
                            final String cmd = action.getName();
                            Crashlytics.log(Log.INFO, "SettingsView", "CMD - " + cmd);
                            if (cmd.equals("flashlight"))
                                toggleFlash(Boolean.parseBoolean(String.valueOf(action.getValue())));
                            if (cmd.equals("playSound")) playMusic((String) action.getValue());
                        }
                    });
    }

    private void toggleFlash(boolean on) {
        if (!mSettings[4]) return;
        if (mFlash != null && !mFlash.hasFlash(getContext())) {
            Toast.makeText(getContext(), R.string.sv_flashlight_not_available, LENGTH_SHORT).show();
        } else {
            if (mFlash == null) return;
            if (on) mFlash.on();
            else mFlash.off();
        }
    }

    private void playMusic(String value) {
        if (value == null) return;
        if (mSound == null) createSoundHelper();

        mSound.playMusic(getContext(), value);
    }

    private void hideKeyboard() {
        if (mMessage != null) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mMessage.getWindowToken(), 0);
        }
    }

    @Override public void onProviderEnabled(String provider) {
        initLocationManager();
    }

    @Override public void onProviderDisabled(String provider) {
        if (mLocSwitch != null) mLocSwitch.setChecked(false);
    }

    //NOT implemented
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    //NOT implemented
    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}

    //NOT implemented
    @Override public void onLocationChanged(Location location) {
    }
}
