package io.relayr.iotsmartphone.ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import io.relayr.android.RelayrSdk;
import io.relayr.iotsmartphone.IotApplication;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.handler.ReadingHandler;
import io.relayr.iotsmartphone.handler.RuleHandler;
import io.relayr.iotsmartphone.helper.DemandIntentReceiver;
import io.relayr.iotsmartphone.helper.FlashHelper;
import io.relayr.iotsmartphone.helper.SoundHelper;
import io.relayr.iotsmartphone.storage.Constants;
import io.relayr.iotsmartphone.storage.Storage;
import io.relayr.iotsmartphone.ui.cloud.FragmentCloud;
import io.relayr.iotsmartphone.ui.readings.FragmentReadings;
import io.relayr.iotsmartphone.ui.rules.FragmentRules;
import io.relayr.iotsmartphone.ui.utils.NotificationsUtil;
import io.relayr.iotsmartphone.ui.utils.TutorialUtil;
import io.relayr.iotsmartphone.ui.utils.UiUtil;
import io.relayr.java.helper.observer.SuccessObserver;
import io.relayr.java.model.action.Command;
import io.relayr.java.model.action.Reading;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.Sensor.TYPE_GYROSCOPE;
import static android.hardware.Sensor.TYPE_LIGHT;
import static android.hardware.SensorManager.SENSOR_DELAY_NORMAL;
import static android.location.LocationManager.GPS_PROVIDER;
import static android.location.LocationManager.NETWORK_PROVIDER;
import static android.net.ConnectivityManager.TYPE_WIFI;
import static android.os.BatteryManager.EXTRA_LEVEL;
import static android.os.BatteryManager.EXTRA_SCALE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.widget.Toast.LENGTH_SHORT;
import static io.relayr.iotsmartphone.storage.Constants.DeviceType.PHONE;
import static io.relayr.iotsmartphone.storage.Constants.DeviceType.WATCH;
import static io.relayr.iotsmartphone.storage.Storage.FREQS_PHONE;
import static io.relayr.iotsmartphone.storage.Storage.TUTORIAL;

public class MainActivity extends AppCompatActivity implements
        SensorEventListener, LocationListener, DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

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
    private long mAccelerationChange = 0;
    private long mGyroscopeChange = 0;
    private long mLightChange = 0;

    private Subscription mRefreshSubs;

    private GoogleApiClient mGoogleApiClient;

    private final Fragment[] mFragments = new Fragment[3];
    private boolean mResolvingError;

    private ProgressDialog mInitialiseDialog;
    private AlertDialog mProblemDialog;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_tab_main);
        ButterKnife.inject(this);

        setSupportActionBar(mToolbar);
        setupViewPager(state);
        setUpTabs();

        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);

        TutorialUtil.updateTutorial(TUTORIAL, false);
        initialise();
    }

    @Override protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        if (!Storage.instance().isActiveInBackground()) initialise();
    }

    @Override protected void onPause() {
        super.onPause();

        TutorialUtil.dismiss();

        if (mInitialiseDialog != null) mInitialiseDialog.dismiss();
        mInitialiseDialog = null;
        if (mProblemDialog != null) mProblemDialog.dismiss();
        mProblemDialog = null;

        if (mFlash != null) mFlash.off();
        if (mSound != null) {
            mSound.stopMusic();
            mSound.stopVibration();
        }

        EventBus.getDefault().unregister(this);
        if (!Storage.instance().isActiveInBackground()) stopReadings();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        stopReadings();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 100: {
                final boolean granted = grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED;
                Crashlytics.log(Log.INFO, "MA", "User granted permission: " + granted);
                Storage.instance().locationPermission(granted);
                initLocationManager();
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) ReadingHandler.publishTouch(true);
        else if (ev.getAction() == MotionEvent.ACTION_UP) ReadingHandler.publishTouch(false);
        return super.dispatchTouchEvent(ev);
    }

    @SuppressWarnings("unused") public void onEvent(Constants.DeviceChange event) {
        if (mTabView != null && mTabView.getSelectedTabPosition() == 0) setToolbarTitle(0);
        if (event.getType() == WATCH) sendToWearable();
    }

    @SuppressWarnings("unused") public void onEvent(Constants.CloudConnected event) {
        subscribeToCommands();
    }

    @SuppressWarnings("unused") public void onEvent(Constants.WatchSamplingUpdate event) {
        sendToWearable(event.getMeaning(), event.getSampling());
    }

    @SuppressWarnings("unused") public void onEvent(Constants.Tutorial event) {
        TutorialUtil.dismiss();
        if (event.getPosition() == 1)
            TutorialUtil.showPlay(MainActivity.this, mTabView);
        if (event.getPosition() == 2)
            TutorialUtil.showLogInToPlay(MainActivity.this, mTabView);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        mResolvingError = false;
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int cause) {}

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        if (mResolvingError) return;
        if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, Constants.REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                mGoogleApiClient.connect();
            }
        } else {
            mResolvingError = false;
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : FreezableUtils.freezeIterable(dataEvents))
            if (event.getType() == DataEvent.TYPE_CHANGED)
                ReadingHandler.publishWatch(event.getDataItem());
    }

    public class MessageReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            final int extra = intent.getIntExtra(DemandIntentReceiver.EXTRA_MESSAGE, Constants.NOTIF_FLASH);
            if (extra == Constants.NOTIF_FLASH) toggleFlash(false);
            else if (extra == Constants.NOTIF_SOUND) toggleSound(false);
            else if (extra == Constants.NOTIF_VIB) toggleVibration(false);
        }
    }

    @Override public void onProviderEnabled(String provider) {
        initLocationManager();
    }

    @Override public void onProviderDisabled(String provider) {
        Storage.instance().saveActivity("location", PHONE, false);
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override public void onLocationChanged(Location location) {
        if (Storage.instance().locationGranted()) ReadingHandler.publishLocation(location);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void onSensorChanged(SensorEvent e) {
        final long millis = System.currentTimeMillis();
        if (e.sensor.getType() == TYPE_ACCELEROMETER &&
                millis - FREQS_PHONE.get("acceleration") > mAccelerationChange) {
            mAccelerationChange = millis;
            TutorialUtil.showLogIn(this, mTabView, e.values[0], e.values[1], e.values[2]);
            ReadingHandler.publish(ReadingHandler.createAccelReading(e.values[0], e.values[1], e.values[2]));
        } else if (e.sensor.getType() == TYPE_GYROSCOPE &&
                millis - FREQS_PHONE.get("angularSpeed") > mGyroscopeChange) {
            ReadingHandler.publish(ReadingHandler.createGyroReading(e.values[0], e.values[1], e.values[2]));
            mGyroscopeChange = millis;
        } else if (e.sensor.getType() == TYPE_LIGHT &&
                millis - FREQS_PHONE.get("luminosity") > mLightChange) {
            ReadingHandler.publish(new Reading(0, System.currentTimeMillis(), "luminosity", "/", e.values[0]));
            mLightChange = millis;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.empty_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                new AlertDialog.Builder(this, R.style.AppTheme_DialogOverlay)
                        .setView(View.inflate(this, R.layout.dialog_settings, null))
                        .setTitle(getString(R.string.dialog_global_settings))
                        .setPositiveButton(getString(R.string.close), new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create()
                        .show();
                break;
        }
        return true;
    }

    private void initialise() {
        if (mInitialiseDialog == null)
            mInitialiseDialog = ProgressDialog.show(this, getString(R.string.initializing),
                    getString(R.string.preparing_cloud), true);

        ReadingHandler.getReadings()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SuccessObserver<Boolean>() {
                    @Override public void success(Boolean success) {
                        if (mInitialiseDialog != null) mInitialiseDialog.dismiss();
                        if (success) {
                            startReadings();
                        } else {
                            if (mProblemDialog != null) mProblemDialog.dismiss();
                            mProblemDialog = new AlertDialog.Builder(MainActivity.this, R.style.AppTheme_DialogOverlay)
                                    .setTitle(getString(R.string.something_went_wrong))
                                    .setNegativeButton(getString(R.string.close), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            finish();
                                        }
                                    })
                                    .setPositiveButton(getString(R.string.retry), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            initialise();
                                        }
                                    })
                                    .create();
                            mProblemDialog.show();
                        }
                    }
                });
    }

    private void startReadings() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        if (!mResolvingError) mGoogleApiClient.connect();

        if (mRefreshSubs == null)
            mRefreshSubs = Observable.interval(1, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Long>() {
                        @Override public void onCompleted() {}

                        @Override public void onError(Throwable e) {
                            Crashlytics.log(Log.ERROR, "MTA", "Failed while refreshing");
                            Crashlytics.logException(e);
                        }

                        @Override public void onNext(Long num) {
                            if (num % FREQS_PHONE.get("rssi") == 0) monitorWiFi();
                            if (num % FREQS_PHONE.get("location") == 0) monitorLocation();
                            if (num % FREQS_PHONE.get("batteryLevel") == 0) monitorBattery();
                            if (num % FREQS_PHONE.get("touch") == 0)
                                ReadingHandler.publishTouch(false);
                        }
                    });

        initReadings();
    }

    private void stopReadings() {
        mResolvingError = false;
        if (mGoogleApiClient != null) Wearable.DataApi.removeListener(mGoogleApiClient, this);

        if (mFlash != null) mFlash.close();
        if (mSound != null) mSound.close();

        if (mSensorManager != null) mSensorManager.unregisterListener(this);
        if (mLocationManager != null && checkPermission(ACCESS_FINE_LOCATION) && checkPermission(ACCESS_COARSE_LOCATION))
            mLocationManager.removeUpdates(this);

        if (mRefreshSubs != null) mRefreshSubs.unsubscribe();
        mRefreshSubs = null;
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
        }

        if (mFragments[0] == null) mFragments[0] = new FragmentReadings();
        if (mFragments[1] == null) mFragments[1] = new FragmentCloud();
        if (mFragments[2] == null) mFragments[2] = new FragmentRules();

        if (getSupportFragmentManager() == null) return;
        ViewPagerAdapter mViewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        mViewPagerAdapter.addFrag(mFragments[0]);
        mViewPagerAdapter.addFrag(mFragments[1]);
        mViewPagerAdapter.addFrag(mFragments[2]);

        mViewPager.setAdapter(mViewPagerAdapter);
    }

    private void setUpTabs() {
        mTabView.setupWithViewPager(mViewPager);
        mTabView.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                final int position = tab.getPosition();
                mViewPager.setCurrentItem(position);

                setVisibility(position);
                setToolbarTitle(position);

                onEvent(new Constants.Tutorial(position));
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}

            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        setupTabIcons();
    }

    private void setToolbarTitle(int position) {
        if (position == 0)
            setTitle(IotApplication.isVisible(PHONE) ? R.string.app_title_phone : R.string.app_title_watch);
        else if (position == 1) setTitle(R.string.cloud_title);
        else if (position == 2) setTitle(R.string.rules_title);
    }

    private void setVisibility(int position) {
        if (position == 0)
            IotApplication.visible(IotApplication.sCurrent == PHONE, IotApplication.sCurrent == WATCH);
        else if (position == 1) IotApplication.visible(false, false);
        else if (position == 2 && RuleHandler.hasRule()) IotApplication.visible(true, true);
        else IotApplication.visible(false, false);
    }

    private void setupTabIcons() {
        if (mTabView == null || mTabView.getTabCount() < 3) return;
        mTabView.getTabAt(0).setIcon(R.drawable.tab_device);
        mTabView.getTabAt(1).setIcon(R.drawable.tab_cloud);
        mTabView.getTabAt(2).setIcon(R.drawable.tab_rules);
    }

    private void initReadings() {
        initSensorManager();
        initWifiManager();
        monitorBattery();

        if (!Storage.instance().locationGranted() &&
                checkPermission(ACCESS_FINE_LOCATION) && checkPermission(ACCESS_COARSE_LOCATION)) {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}, 100);
        } else {
            initLocationManager();
        }

        if (UiUtil.isCloudConnected()) subscribeToCommands();
    }

    private void initSensorManager() {
        if (mSensorManager == null)
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        final Sensor acceleration = mSensorManager.getDefaultSensor(TYPE_ACCELEROMETER);
        if (acceleration != null)
            mSensorManager.registerListener(this, acceleration, SENSOR_DELAY_NORMAL);

        final Sensor gyroscope = mSensorManager.getDefaultSensor(TYPE_GYROSCOPE);
        if (gyroscope != null)
            mSensorManager.registerListener(this, gyroscope, SENSOR_DELAY_NORMAL);

        final Sensor light = mSensorManager.getDefaultSensor(TYPE_LIGHT);
        if (light != null)
            mSensorManager.registerListener(this, light, SENSOR_DELAY_NORMAL);
    }

    private void initWifiManager() {
        if (mWifiManager != null && mConnectivityManager != null) return;
        mWifiManager = (WifiManager) MainActivity.this.getSystemService(WIFI_SERVICE);
        mConnectivityManager = (ConnectivityManager) MainActivity.this.getSystemService(CONNECTIVITY_SERVICE);
        monitorWiFi();
    }

    private void monitorWiFi() {
        if (mConnectivityManager == null || mWifiManager == null) return;

        if (!checkWifi(mConnectivityManager))
            Toast.makeText(MainActivity.this, R.string.warning_no_wifi, LENGTH_SHORT).show();

        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo != null)
            ReadingHandler.publish(new Reading(0, System.currentTimeMillis(), "rssi", "wifi", wifiInfo.getRssi()));
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
        Intent batteryIntent = MainActivity.this.registerReceiver(null, new IntentFilter(ACTION_BATTERY_CHANGED));
        int level = batteryIntent != null ? batteryIntent.getIntExtra(EXTRA_LEVEL, -1) : 0;
        int scale = batteryIntent != null ? batteryIntent.getIntExtra(EXTRA_SCALE, -1) : 0;

        float bat;
        if (level == -1 || scale == -1) bat = 50.0f;
        else bat = ((float) level / (float) scale) * 100.0f;

        ReadingHandler.publish(new Reading(0, System.currentTimeMillis(), "batteryLevel", "/", bat));
    }

    private void initLocationManager() {
        if (!Storage.instance().locationGranted()) return;
        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                if (mLocationManager != null) return;
                if (checkPermission(ACCESS_FINE_LOCATION) && checkPermission(ACCESS_COARSE_LOCATION)) {
                    mLocationManager = (LocationManager) MainActivity.this.getSystemService(LOCATION_SERVICE);
                    try {
                        mLocationManager.requestLocationUpdates(GPS_PROVIDER, 0, 0, MainActivity.this);
                        monitorLocation();
                    } catch (Exception e) {
                        Crashlytics.log(Log.ERROR, "MTA", "GPS_PROVIDER doesn't exist.");
                        try {
                            mLocationManager.requestLocationUpdates(NETWORK_PROVIDER, 0, 0, MainActivity.this);
                            monitorLocation();
                        } catch (Exception e1) {
                            Crashlytics.log(Log.ERROR, "MTA", "NETWORK_PROVIDER doesn't exist.");
                        }
                    }
                }
            }
        }, 500);
    }

    private void monitorLocation() {
        if (mLocationManager == null || !Storage.instance().locationGranted()) return;
        new Handler().post(new Runnable() {
            @Override public void run() {
                if (checkPermission(ACCESS_FINE_LOCATION) && checkPermission(ACCESS_COARSE_LOCATION)) {
                    Location location = mLocationManager.getLastKnownLocation(GPS_PROVIDER);
                    if (location == null)
                        location = mLocationManager.getLastKnownLocation(NETWORK_PROVIDER);
                    if (location != null) ReadingHandler.publishLocation(location);

                    if (location == null && !mLocationManager.isProviderEnabled(GPS_PROVIDER) &&
                            !mLocationManager.isProviderEnabled(NETWORK_PROVIDER))
                        showLocationDialog();
                }
            }
        });
    }

    private boolean checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(MainActivity.this, permission) == PERMISSION_GRANTED;
    }

    private void showLocationDialog() {
        new AlertDialog.Builder(this, R.style.AppTheme_DialogOverlay)
                .setTitle(this.getString(R.string.warning_location_off_title))
                .setIcon(R.drawable.ic_warning)
                .setMessage(MainActivity.this.getString(R.string.warning_location_off_message))
                .setPositiveButton(MainActivity.this.getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        MainActivity.this.startActivity(myIntent);
                    }
                })
                .setNegativeButton(MainActivity.this.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void subscribeToCommands() {
        if (mCommandsSubscription == null)
            mCommandsSubscription = RelayrSdk.getWebSocketClient()
                    .subscribeToCommands(Storage.instance().getDeviceId(PHONE))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Command>() {
                        @Override public void onCompleted() {
                            mCommandsSubscription = null;
                            subscribeToCommands();
                        }

                        @Override public void onError(Throwable e) {
                            Crashlytics.log(Log.ERROR, "MTA", "subscribeToCommands - error");
                            Crashlytics.logException(e);
                            e.printStackTrace();
                            mCommandsSubscription = null;
                            subscribeToCommands();
                        }

                        @Override public void onNext(Command command) {
                            final String commandName = command.getName();
                            Crashlytics.log(Log.DEBUG, "MTA", "CMD - " + commandName);
                            try {
                                final boolean cmd = Boolean.parseBoolean(String.valueOf(command.getValue()));
                                switch (commandName) {
                                    case "flashlight":
                                        toggleFlash(cmd);
                                        break;
                                    case "playSound":
                                        toggleSound(cmd);
                                        break;
                                    case "vibration":
                                        toggleVibration(cmd);
                                        break;
                                }
                            } catch (Exception e) {
                                Crashlytics.log(Log.ERROR, "MTA", "CMD - parsing failed");
                            }
                        }
                    });
    }

    private void createFlashHelper() {
        if (mFlash == null) mFlash = new FlashHelper();
        try {
            mFlash.open(MainActivity.this.getApplicationContext());
            mFlash.on();
            NotificationsUtil.showNotification(this, Constants.NOTIF_FLASH);
        } catch (Exception e) {
            UiUtil.showSnackBar(this, R.string.error_using_flash);
            Crashlytics.log(Log.ERROR, "MTA", "Failed to create FlashHelper.");
            Crashlytics.logException(e);
            mFlash.close();
            mFlash = null;
        }
    }

    private void toggleFlash(boolean on) {
        if (mFlash != null && !mFlash.hasFlash(MainActivity.this)) {
            UiUtil.showSnackBar(this, R.string.warning_flashlight_not_available);
        } else {
            if (mFlash == null) createFlashHelper();
            else {
                if (on) {
                    mFlash.on();
                    NotificationsUtil.showNotification(this, Constants.NOTIF_FLASH);
                } else {
                    NotificationsUtil.hideNotification(this, Constants.NOTIF_FLASH);
                    mFlash.off();
                }
            }
        }
    }

    private void toggleSound(boolean start) {
        if (mSound == null) mSound = new SoundHelper();
        if (start) {
            NotificationsUtil.showNotification(this, Constants.NOTIF_SOUND);
            mSound.playMusic(MainActivity.this);
        } else {
            mSound.stopMusic();
            NotificationsUtil.hideNotification(this, Constants.NOTIF_SOUND);
        }
    }

    private void toggleVibration(boolean start) {
        if (mSound == null) mSound = new SoundHelper();
        if (start) {
            NotificationsUtil.showNotification(this, Constants.NOTIF_VIB);
            //Postpone vibration because of notification
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new TimerTask() {
                        @Override public void run() {
                            final boolean started = mSound.vibrate(MainActivity.this);
                            if (!started) {
                                UiUtil.showSnackBar(MainActivity.this, R.string.vibration_not_supported);
                                NotificationsUtil.hideNotification(MainActivity.this, Constants.NOTIF_VIB);
                            }
                        }
                    });
                }
            }, 1000);
        } else {
            NotificationsUtil.hideNotification(this, Constants.NOTIF_VIB);
            mSound.stopVibration();
        }
    }

    private void sendToWearable() {
        DataMap dataMap = new DataMap();
        dataMap.putLong(Constants.ACTIVATE, System.currentTimeMillis());
        new SendToDataLayerThread(Constants.ACTIVATE_PATH, dataMap).start();

        for (Map.Entry<String, Integer> entry : Storage.FREQS_WATCH.entrySet())
            sendToWearable(entry.getKey(), entry.getValue());
    }

    private void sendToWearable(String meaning, int sampling) {
        DataMap dataMap = new DataMap();
        dataMap.putString(Constants.SAMPLING_MEANING, meaning);
        dataMap.putInt(Constants.SAMPLING, sampling);
        new SendToDataLayerThread(Constants.SAMPLING_PATH, dataMap).start();
    }

    class SendToDataLayerThread extends Thread {
        String path;
        DataMap dataMap;

        SendToDataLayerThread(String p, DataMap data) {
            path = p;
            dataMap = data;
        }

        public void run() {
            if (mGoogleApiClient == null) return;
            PutDataMapRequest putDMR = PutDataMapRequest.create(path);
            putDMR.getDataMap().putAll(dataMap);
            PutDataRequest request = putDMR.asPutDataRequest();
            Wearable.DataApi.putDataItem(mGoogleApiClient, request).await();
        }
    }
}
