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
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.Surface;
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
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.Storage;
import io.relayr.iotsmartphone.helper.FlashHelper;
import io.relayr.iotsmartphone.helper.SoundHelper;
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
import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.SensorManager.SENSOR_DELAY_NORMAL;
import static android.net.ConnectivityManager.TYPE_WIFI;
import static android.os.BatteryManager.EXTRA_LEVEL;
import static android.os.BatteryManager.EXTRA_SCALE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;

public class MainTabActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, SensorEventListener, LocationListener {

    @InjectView(R.id.toolbar) Toolbar mToolbar;
    @InjectView(R.id.drawer_layout) DrawerLayout mDrawer;
    @InjectView(R.id.nav_view) NavigationView mNavView;
    //    @InjectView(R.id.fab) FloatingActionButton mFab;
    @InjectView(R.id.viewpager) ViewPager mViewPager;
    @InjectView(R.id.tabs) TabLayout mTabView;

    private FlashHelper mFlash;
    private SoundHelper mSound;
    private WifiManager mWifiManager;
    private SensorManager mSensorManager;
    private LocationManager mLocationManager;
    private ConnectivityManager mConnectivityManager;

    // wifi, battery, location, acceleration, flash, sound
    private boolean[] mSettings = new boolean[]{false, false, false, false, false, false, false};

    private Subscription mCommandsSubscription = null;
    private int mSensorChange = 0;

    private int mAccIntensity = 0;

    private Subscription mRefreshSubs;
    private int mRotation;

    protected Snackbar mSnackBar;
    private final Fragment[] mFragments = new Fragment[3];

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_tab_main);
        ButterKnife.inject(this);

        getReadings();

        setSupportActionBar(mToolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();

        mNavView.setNavigationItemSelectedListener(this);
        setupViewPager(state);
        setUpTabs();

        int publishDelay = Storage.instance().loadDelay();
        mAccIntensity = 15 / (Storage.instance().loadIntensity() + 1);
        Crashlytics.log(Log.INFO, "SRV", "Delay " + publishDelay + " intensity " + mAccIntensity);

        mSettings = Storage.instance().loadSettings(mSettings.length);

        mRefreshSubs = Observable.interval(publishDelay, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                    @Override public void onCompleted() {}

                    @Override public void onError(Throwable e) {}

                    @Override public void onNext(Long aLong) {refreshData();}
                });

        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);

        //        if (SDK_INT >= JELLY_BEAN_MR1)
        //            mRotation = getDisplay().getRotation();
        //        else  //noinspection deprecation
        //            mRotation = getDisplay().getOrientation();

        switchWifi(true);
        switchBattery(true);
        switchAcceleration(true);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen(GravityCompat.START)) mDrawer.closeDrawer(GravityCompat.START);
        else super.onBackPressed();
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void onSensorChanged(SensorEvent e) {
        if (!mSettings[3]) return;
        if (e.sensor.getType() != TYPE_ACCELEROMETER) return;
        if (mSensorChange++ % mAccIntensity != 0) return;

        publishAcceleration(e);
    }

    private void getReadings() {
        RelayrSdk.getDeviceModelsApi().getDeviceModelById(Storage.MODEL_ID)
                .subscribe(new SimpleObserver<DeviceModel>() {
                    @Override public void error(Throwable e) {
                        Log.e("MODEL", "PROBLEM");
                        e.printStackTrace();
                    }

                    @Override public void success(DeviceModel deviceModel) {
                        try {
                            final Transport transport = deviceModel.getLatestFirmware().getDefaultTransport();
                            Storage.instance().setPhoneReadings(transport.getReadings());
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
        mViewPagerAdapter.addFrag(mFragments[0], "Readings");
        mViewPagerAdapter.addFrag(mFragments[1], "Cloud");
        mViewPagerAdapter.addFrag(mFragments[2], "Rules");

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
    }


    @Override public void onProviderEnabled(String provider) {
        initLocationManager();
    }

    @Override public void onProviderDisabled(String provider) {
        //        if (mLocSwitch != null) mLocSwitch.setChecked(false);
    }

    //NOT implemented
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    //NOT implemented
    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}

    //NOT implemented
    @Override public void onLocationChanged(Location location) {}

    public class MessageReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            toggleFlash(false);
        }
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

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

        public void addFrag(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    public void refreshData() {
        monitorWiFi();
        monitorBattery();
        monitorLocation();
    }

    private void initSensorManager() {
        if (mSensorManager == null)
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(TYPE_ACCELEROMETER), SENSOR_DELAY_NORMAL);
    }

    private void turnSensorOff() {
        if (mSensorManager != null)
            mSensorManager.unregisterListener(this);
    }

    private void publishAcceleration(SensorEvent e) {
        switch (mRotation) {
            case Surface.ROTATION_0:
                publishReading(createAccelReading(e.values[0], e.values[1], e.values[2]));
                break;
            case Surface.ROTATION_90:
                publishReading(createAccelReading(-e.values[1], e.values[0], e.values[2]));
                break;
            case Surface.ROTATION_180:
                publishReading(createAccelReading(-e.values[0], -e.values[1], e.values[2]));
                break;
            case Surface.ROTATION_270:
                publishReading(createAccelReading(e.values[1], -e.values[0], e.values[2]));
        }
    }

    private Reading createAccelReading(float x, float y, float z) {
        final AccelGyroscope.Acceleration acceleration = new AccelGyroscope.Acceleration();
        acceleration.x = x;
        acceleration.y = y;
        acceleration.z = z;
        return new Reading(0, System.currentTimeMillis(), "acceleration", "/", acceleration);
    }

    private void showAccelerometerWarning() {
        if (Storage.instance().isWarningShown())
            Toast.makeText(MainTabActivity.this, MainTabActivity.this.getString(R.string.sv_warning_toast), LENGTH_LONG).show();
        else
            new AlertDialog.Builder(MainTabActivity.this).setTitle(MainTabActivity.this.getString(R.string.sv_warning_dialog_title))
                    .setIcon(R.drawable.ic_warning)
                    .setMessage(MainTabActivity.this.getString(R.string.sv_warning_dialog_text))
                    .setPositiveButton(MainTabActivity.this.getString(R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int i) {
                            Storage.instance().warningShown();
                            dialog.dismiss();
                        }
                    }).show();
    }

    private void initWifiManager() {
        if (mWifiManager != null && mConnectivityManager != null) return;
        mWifiManager = (WifiManager) MainTabActivity.this.getSystemService(WIFI_SERVICE);
        mConnectivityManager = (ConnectivityManager) MainTabActivity.this.getSystemService(CONNECTIVITY_SERVICE);
    }

    private void monitorWiFi() {
        if (!mSettings[0] || mConnectivityManager == null || mWifiManager == null) return;

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
        if (!mSettings[1]) return;

        Intent batteryIntent = MainTabActivity.this.registerReceiver(null, new IntentFilter(ACTION_BATTERY_CHANGED));
        int level = batteryIntent != null ? batteryIntent.getIntExtra(EXTRA_LEVEL, -1) : 0;
        int scale = batteryIntent != null ? batteryIntent.getIntExtra(EXTRA_SCALE, -1) : 0;

        float bat;
        if (level == -1 || scale == -1) bat = 50.0f;
        else bat = ((float) level / (float) scale) * 100.0f;

        publishReading(new Reading(0, System.currentTimeMillis(), "batteryLevel", "/", bat));
    }

    private void initLocationManager() {
        if (Storage.instance().locationGranted()) {
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
        if (!mSettings[2] || mLocationManager == null) return;
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
        if (!mSettings[2]) return;

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

    private void turnOffLocation() {
        if (mLocationManager != null)
            if (ContextCompat.checkSelfPermission(MainTabActivity.this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(MainTabActivity.this, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED)
                mLocationManager.removeUpdates(this);
    }

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
        if (mSettings[4] && mFlash != null) return;

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
        if (mSettings[5] && mSound != null) return;
        if (mSound == null) mSound = new SoundHelper();
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
        if (!mSettings[4]) return;
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

    private void switchAcceleration(boolean isChecked) {
        mSettings[3] = isChecked;
        Storage.instance().saveSettings(mSettings);
        if (isChecked) initSensorManager();
        if (isChecked) showAccelerometerWarning();
        else turnSensorOff();
    }

    private void switchLocation(boolean isChecked) {
        mSettings[2] = isChecked;
        Storage.instance().saveSettings(mSettings);
        if (isChecked) initLocationManager();
        else turnOffLocation();
    }

    private void switchBattery(boolean isChecked) {
        mSettings[1] = isChecked;
        Storage.instance().saveSettings(mSettings);
        monitorBattery();
    }

    private void switchWifi(boolean isChecked) {
        mSettings[0] = isChecked;
        Storage.instance().saveSettings(mSettings);
        if (isChecked) {
            initWifiManager();
            monitorWiFi();
        }
    }

    void publishReading(Reading reading) {
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
