package io.relayr.iotsmartphone.tabs;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
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
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import io.relayr.android.RelayrSdk;
import io.relayr.iotsmartphone.IotApplication;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.helper.FlashHelper;
import io.relayr.iotsmartphone.helper.SoundHelper;
import io.relayr.iotsmartphone.tabs.cloud.FragmentCloud;
import io.relayr.iotsmartphone.tabs.helper.Constants;
import io.relayr.iotsmartphone.tabs.helper.SettingsStorage;
import io.relayr.iotsmartphone.tabs.readings.FragmentReadings;
import io.relayr.iotsmartphone.tabs.rules.FragmentRules;
import io.relayr.java.helper.observer.SimpleObserver;
import io.relayr.java.model.AccelGyroscope;
import io.relayr.java.model.action.Command;
import io.relayr.java.model.action.Reading;
import io.relayr.java.model.models.DeviceModel;
import io.relayr.java.model.models.error.DeviceModelsException;
import io.relayr.java.model.models.transport.Transport;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.hardware.Sensor.TYPE_GYROSCOPE;
import static android.hardware.Sensor.TYPE_LIGHT;
import static android.hardware.Sensor.TYPE_LINEAR_ACCELERATION;
import static android.hardware.SensorManager.SENSOR_DELAY_NORMAL;
import static android.net.ConnectivityManager.TYPE_WIFI;
import static android.os.BatteryManager.EXTRA_LEVEL;
import static android.os.BatteryManager.EXTRA_SCALE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.widget.Toast.LENGTH_SHORT;
import static io.relayr.iotsmartphone.tabs.helper.SettingsStorage.FREQS;

public class MainTabActivity extends AppCompatActivity implements
        SensorEventListener, LocationListener {

    @InjectView(R.id.toolbar) Toolbar mToolbar;
    @InjectView(R.id.viewpager) ViewPager mViewPager;
    @InjectView(R.id.tabs) TabLayout mTabView;

    private FlashHelper mFlash;
    private SoundHelper mSound;
    private WifiManager mWifiManager;
    private SensorManager mSensorManager;
    private LocationManager mLocationManager;
    private ConnectivityManager mConnectivityManager;

    private Subscription mCommandsSubscription = null;
    private int mAccelerationChange = 0;
    private int mGyroscopeChange = 0;

    private Subscription mRefreshSubs;

    private final Fragment[] mFragments = new Fragment[3];
    //    private Map<String, Boolean> mUploadActions = new HashMap<>();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_tab_main);
        ButterKnife.inject(this);

        getReadings();

        setSupportActionBar(mToolbar);
        setupViewPager(state);
        setUpTabs();

        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);

        //        if (SDK_INT >= JELLY_BEAN_MR1)
        //            mRotation = getDisplay().getRotation();
        //        else  //noinspection deprecation
        //            mRotation = getDisplay().getOrientation();
    }

    @Override protected void onResume() {
        super.onResume();
        initReadings();
        if (mRefreshSubs == null)
            mRefreshSubs = Observable.interval(1, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Long>() {
                        @Override public void onCompleted() {}

                        @Override public void onError(Throwable e) {
                            Log.e("MTA", "Failed while refreshing");
                            e.printStackTrace();
                        }

                        @Override public void onNext(Long num) {
                            refreshTouch();
                            if (num % FREQS.get("batteryLevel") == 0) monitorBattery();
                            if (num % FREQS.get("location") == 0) monitorLocation();
                            if (num % FREQS.get("rssi") == 0) monitorWiFi();
                        }
                    });
    }

    @Override protected void onPause() {
        super.onPause();
        turnSensorOff();
        if (mRefreshSubs != null) mRefreshSubs.unsubscribe();
        mRefreshSubs = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 100: {
                final boolean granted = grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED;
                Crashlytics.log(Log.INFO, "MA", "User granted permission: " + granted);
                SettingsStorage.instance().locationPermission(granted);
                initLocationManager();
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN)
            publishReading(new Reading(0, System.currentTimeMillis(), "touch", "/", true));
        else if (ev.getAction() == MotionEvent.ACTION_UP)
            publishReading(new Reading(0, System.currentTimeMillis(), "touch", "/", false));
        return super.dispatchTouchEvent(ev);
    }

    private void getReadings() {
        RelayrSdk.getDeviceModelsApi().getDeviceModelById(SettingsStorage.MODEL_PHONE)
                .subscribe(new SimpleObserver<DeviceModel>() {
                    @Override public void error(Throwable e) {
                        Log.e("MTA", "PHONE model error");
                        e.printStackTrace();
                    }

                    @Override public void success(DeviceModel deviceModel) {
                        try {
                            final Transport transport = deviceModel.getLatestFirmware().getDefaultTransport();
                            SettingsStorage.instance().savePhoneReadings(transport.getReadings());
                            EventBus.getDefault().post(new Constants.DeviceModelEvent());
                        } catch (DeviceModelsException e) {
                            e.printStackTrace();
                        }
                    }
                });

        RelayrSdk.getDeviceModelsApi().getDeviceModelById(SettingsStorage.MODEL_WATCH)
                .subscribe(new SimpleObserver<DeviceModel>() {
                    @Override public void error(Throwable e) {
                        Log.e("MTA", "WATCH model error");
                        e.printStackTrace();
                    }

                    @Override public void success(DeviceModel deviceModel) {
                        try {
                            final Transport transport = deviceModel.getLatestFirmware().getDefaultTransport();
                            SettingsStorage.instance().saveWatchReadings(transport.getReadings());
                            EventBus.getDefault().post(new Constants.DeviceModelEvent());
                        } catch (DeviceModelsException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void setupViewPager(Bundle savedInstanceState) {
        if (savedInstanceState != null &&
                getSupportFragmentManager() != null &&
                getSupportFragmentManager().getFragments() != null) {
            for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                if (fragment instanceof FragmentReadings)
                    mFragments[0] = fragment;
                else if (fragment instanceof FragmentCloud)
                    mFragments[1] = fragment;
                else if (fragment instanceof FragmentRules)
                    mFragments[2] = fragment;
            }
        } else {
            mFragments[0] = new FragmentReadings();
            mFragments[1] = new FragmentCloud();
            mFragments[2] = new FragmentRules();
        }

        if (getSupportFragmentManager() == null) return;
        ViewPagerAdapter mViewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        mViewPagerAdapter.addFrag(mFragments[0]);
        mViewPagerAdapter.addFrag(mFragments[1]);
        mViewPagerAdapter.addFrag(mFragments[2]);

        mViewPager.setAdapter(mViewPagerAdapter);
    }

    private void setUpTabs() {
        mTabView.setupWithViewPager(mViewPager);
        mTabView.setSelectedTabIndicatorHeight(getResources().getDimensionPixelSize(R.dimen.default_padding));
        mTabView.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}

            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        setupTabIcons();
    }

    private void setupTabIcons() {
        if (mTabView == null) return;
        mTabView.getTabAt(0).setIcon(R.drawable.ic_tab_hardware);
        mTabView.getTabAt(1).setIcon(R.drawable.ic_tab_cloud);
        mTabView.getTabAt(2).setIcon(R.drawable.ic_tab_rule);
    }

    public class MessageReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            toggleFlash(false);
        }
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFrag(Fragment fragment) {
            mFragmentList.add(fragment);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return null;
        }
    }

    @Override public void onProviderEnabled(String provider) {
        initLocationManager();
    }

    @Override public void onProviderDisabled(String provider) {
        //        if (mLocSwitch != null) mLocSwitch.setChecked(false);
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override public void onLocationChanged(Location location) {}

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void onSensorChanged(SensorEvent e) {
        if (e.sensor.getType() == TYPE_LINEAR_ACCELERATION) {
            if (mAccelerationChange++ % FREQS.get("acceleration") == 0)
                publishReading(createAccelReading(e.values[0], e.values[1], e.values[2]));
        } else if (e.sensor.getType() == TYPE_GYROSCOPE) {
            if (mGyroscopeChange++ % FREQS.get("angularSpeed") == 0)
                publishReading(createGyroReading(e.values[0], e.values[1], e.values[2]));
        } else if (e.sensor.getType() == TYPE_LIGHT) {
            publishLight(e);
        }
    }

    private void initReadings() {
        initSensorManager();

        initWifiManager();

        monitorBattery();

        //        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED &&
        //                ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
        //            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}, 100);
        //        } else {
        //            initLocationManager();
        //        }
    }

    private void refreshTouch() {
        publishReading(new Reading(0, System.currentTimeMillis(), "touch", "/", false));
    }

    private void initSensorManager() {
        if (mSensorManager == null)
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        final Sensor acceleration = mSensorManager.getDefaultSensor(TYPE_LINEAR_ACCELERATION);
        if (acceleration != null)
            mSensorManager.registerListener(this, acceleration, SENSOR_DELAY_NORMAL);

        final Sensor gyroscope = mSensorManager.getDefaultSensor(TYPE_GYROSCOPE);
        if (gyroscope != null)
            mSensorManager.registerListener(this, gyroscope, SENSOR_DELAY_NORMAL);

        final Sensor light = mSensorManager.getDefaultSensor(TYPE_LIGHT);
        if (light != null)
            mSensorManager.registerListener(this, light, SENSOR_DELAY_NORMAL);
    }

    private void turnSensorOff() {
        if (mSensorManager != null)
            mSensorManager.unregisterListener(this);
    }

    private void publishLight(SensorEvent e) {
        publishReading(new Reading(0, System.currentTimeMillis(), "luminosity", "/", e.values[0]));
    }

    private Reading createAccelReading(float x, float y, float z) {
        final AccelGyroscope.Acceleration acceleration = new AccelGyroscope.Acceleration();
        acceleration.x = x;
        acceleration.y = y;
        acceleration.z = z;
        return new Reading(0, System.currentTimeMillis(), "acceleration", "/", acceleration);
    }

    private Reading createGyroReading(float x, float y, float z) {
        final AccelGyroscope.AngularSpeed angularSpeed = new AccelGyroscope.AngularSpeed();
        angularSpeed.x = x;
        angularSpeed.y = y;
        angularSpeed.z = z;
        return new Reading(0, System.currentTimeMillis(), "angularSpeed", "/", angularSpeed);
    }

    private void initWifiManager() {
        if (mWifiManager != null && mConnectivityManager != null) return;
        mWifiManager = (WifiManager) MainTabActivity.this.getSystemService(WIFI_SERVICE);
        mConnectivityManager = (ConnectivityManager) MainTabActivity.this.getSystemService(CONNECTIVITY_SERVICE);
        monitorWiFi();
    }

    private void monitorWiFi() {
        if (mConnectivityManager == null || mWifiManager == null) return;

        if (!checkWifi(mConnectivityManager))
            Toast.makeText(MainTabActivity.this, R.string.sv_no_wifi, LENGTH_SHORT).show();

        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo != null)
            publishReading(new Reading(0, System.currentTimeMillis(), "rssi", "wifi", wifiInfo.getRssi()));
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
        Intent batteryIntent = MainTabActivity.this.registerReceiver(null, new IntentFilter(ACTION_BATTERY_CHANGED));
        int level = batteryIntent != null ? batteryIntent.getIntExtra(EXTRA_LEVEL, -1) : 0;
        int scale = batteryIntent != null ? batteryIntent.getIntExtra(EXTRA_SCALE, -1) : 0;

        float bat;
        if (level == -1 || scale == -1) bat = 50.0f;
        else bat = ((float) level / (float) scale) * 100.0f;

        publishReading(new Reading(0, System.currentTimeMillis(), "batteryLevel", "/", bat));
    }

    private void initLocationManager() {
        if (SettingsStorage.instance().locationGranted()) {
            new Handler().postDelayed(new Runnable() {
                @Override public void run() {
                    if (mLocationManager == null)
                        mLocationManager = (LocationManager) MainTabActivity.this.getSystemService(LOCATION_SERVICE);
                    if (ContextCompat.checkSelfPermission(MainTabActivity.this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
                        try {
                            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, MainTabActivity.this);
                            monitorLocation();
                        } catch (Exception e) {
                            Crashlytics.log(Log.ERROR, "SRV", "GPS_PROVIDER doesn't exist.");
                            try {
                                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, MainTabActivity.this);
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
        if (mLocationManager == null) return;
        new Handler().post(new Runnable() {
            @Override public void run() {
                if (ContextCompat.checkSelfPermission(MainTabActivity.this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(MainTabActivity.this, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED) {
                    Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location == null)
                        location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (location != null)
                        publishLocation(location.getLatitude(), location.getLongitude());
                    else showLocationDialog();
                }
            }
        });
    }

    public void publishLocation(double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(MainTabActivity.this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses.isEmpty()) return;

            Address obj = addresses.get(0);
            String address = obj.getCountryName() + ", ";
            address += obj.getAddressLine(1) + ", ";
            address += obj.getAddressLine(0);

            publishReading(new Reading(0, System.currentTimeMillis(), "location", "/", address));
        } catch (IOException e) {
            Toast.makeText(MainTabActivity.this, R.string.sv_location_resolve_err, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    //    private void turnOffLocation() {
    //        if (mLocationManager != null)
    //            if (ContextCompat.checkSelfPermission(MainTabActivity.this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED &&
    //                    ContextCompat.checkSelfPermission(MainTabActivity.this, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED)
    //                mLocationManager.removeUpdates(this);
    //    }

    private void showLocationDialog() {
        new AlertDialog.Builder(MainTabActivity.this).setTitle(MainTabActivity.this.getString(R.string.sv_location_off_title))
                .setIcon(R.drawable.ic_warning)
                .setMessage(MainTabActivity.this.getString(R.string.sv_location_off_message))
                .setPositiveButton(MainTabActivity.this.getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        MainTabActivity.this.startActivity(myIntent);
                    }
                })
                .setNegativeButton(MainTabActivity.this.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void createFlashHelper() {
        if (mFlash != null) return;

        mFlash = new FlashHelper();
        try {
            mFlash.open(MainTabActivity.this.getApplicationContext());
        } catch (Exception e) {
            Crashlytics.log(Log.ERROR, "SRV", "Failed to create FlashHelper.");
            e.printStackTrace();

            Toast.makeText(MainTabActivity.this, R.string.sv_err_using_flash, Toast.LENGTH_SHORT).show();
            mFlash.close();
            mFlash = null;
        }
    }

    private void createSoundHelper() {
        if (mSound != null) return;
        mSound = new SoundHelper();
    }

    private void subscribeToCommands() {
        if (mCommandsSubscription == null)
            mCommandsSubscription = RelayrSdk.getWebSocketClient()
                    .subscribeToCommands(SettingsStorage.instance().getDevice().getId())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<Command>() {
                        @Override public void onCompleted() {}

                        @Override public void onError(Throwable e) {
                            Crashlytics.log(Log.ERROR, "SettingsView", "subscribeToCommands - error");
                            Crashlytics.logException(e);
                        }

                        @Override public void onNext(Command action) {
                            final String cmd = action.getName();
                            Crashlytics.log(Log.DEBUG, "SettingsView", "CMD - " + cmd);
                            if (cmd.equals("flashlight"))
                                toggleFlash(Boolean.parseBoolean(String.valueOf(action.getValue())));
                            if (cmd.equals("playSound")) playMusic((String) action.getValue());
                        }
                    });
    }

    private void toggleFlash(boolean on) {
        if (mFlash != null && !mFlash.hasFlash(MainTabActivity.this)) {
            Toast.makeText(MainTabActivity.this, R.string.sv_flashlight_not_available, LENGTH_SHORT).show();
        } else {
            //            showNotification(on, mSettings[6]);
            if (mFlash == null) return;
            if (on) mFlash.on();
            else mFlash.off();
        }
    }

    private void playMusic(String value) {
        if (value == null) return;
        if (mSound == null) createSoundHelper();

        mSound.playMusic(MainTabActivity.this, value);
    }

    void publishReading(Reading reading) {
        if (IotApplication.isVisible(Constants.DeviceType.PHONE))
            EventBus.getDefault().post(new Constants.ReadingEvent(reading));
        //        if (reading == null || reading.meaning == null) return;
        //        RelayrSdk.getWebSocketClient()
        //                .publish(Storage.instance().getDevice().getId(), reading)
        //                .subscribeOn(Schedulers.io())
        //                .subscribe(new Subscriber<Void>() {
        //                    @Override public void onCompleted() {}
        //
        //                    @Override public void onError(Throwable e) {
        //                        Crashlytics.log(Log.ERROR, "SettingsView", "publishReading - error");
        //                        e.printStackTrace();
        //                    }
        //
        //                    @Override public void onNext(Void aVoid) {}
        //                });
    }
}
