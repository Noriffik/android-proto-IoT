package io.relayr.iotsmartphone;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.relayr.android.RelayrSdk;
import io.relayr.iotsmartphone.helper.ControlListener;
import io.relayr.iotsmartphone.helper.DemandIntentReceiver;
import io.relayr.iotsmartphone.widget.BasicView;
import io.relayr.java.model.Device;
import io.relayr.java.model.User;
import io.relayr.java.model.action.Reading;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.support.design.widget.Snackbar.LENGTH_LONG;

public class MainActivity extends AppCompatActivity implements OnNavigationItemSelectedListener, ControlListener,
        DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final int REQUEST_RESOLVE_ERROR = 1000;
    private static final String ACTIVATE_PATH = "/activate";
    private static final String ACTIVATE = "activate";
    private static final String SENSOR_PATH = "/sensor";
    private static final String SENSOR = "sensor";

    @InjectView(R.id.container) FrameLayout mContainer;

    private int mPosition = 0;
    private ProgressDialog mLoadingProgress;
    private BasicView mCurrentView;
    private AlertDialog mLogOutDialog;

    private boolean mResolvingError = false;
    private GoogleApiClient mGoogleApiClient;
    private boolean mActivateWearable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        if (!mResolvingError) mGoogleApiClient.connect();

        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}, 100);
        } else {
            checkForDevice();
        }
    }

    @Override protected void onPause() {
        super.onPause();
        if (mLogOutDialog != null) mLogOutDialog.dismiss();
        mLogOutDialog = null;
        if (mLoadingProgress != null) mLoadingProgress.dismiss();
        mLoadingProgress = null;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (mPosition == 2 || mPosition == 3) {
            switchView(1);
        } else {
            super.onBackPressed();
        }
    }

    @Override protected void onDestroy() {
        if (!mResolvingError) {
            sendToWearable(false);
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onDestroy();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_send_receive) switchView(1);
        else if (id == R.id.nav_main) switchView(2);
        else if (id == R.id.nav_user) switchView(3);
        else if (id == R.id.nav_log_out) logOut();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 100: {
                final boolean granted = grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED;
                Crashlytics.log(Log.INFO, "MA", "User granted permission: " + granted);
                Storage.instance().locationPermission(granted);
                checkForDevice();
            }
        }
    }

    @Override public void onDeviceCreated() {
        switchView(1);
    }

    @Override public void startSettings() {
        switchView(2);
    }

    @Override public void activateWearable(boolean active) {
        try {
            getPackageManager().getPackageInfo("com.google.android.wearable.app", PackageManager.GET_META_DATA);
            mActivateWearable = active;
            if (mGoogleApiClient.isConnected()) sendToWearable(active);
        } catch (PackageManager.NameNotFoundException e) {
            //android wear app is not installed
            final View view = findViewById(android.R.id.content);
            if (view == null || !active) return;
            Snackbar.make(view, getString(R.string.srv_no_wearable), LENGTH_LONG).show();
        }
    }

    @Override public void publishReading(Reading reading) {
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

    @Override public void showNotification(boolean show, boolean wearEnabled) {
        if (!show) return;
        Intent demandIntent = new Intent(this, DemandIntentReceiver.class)
                .putExtra(DemandIntentReceiver.EXTRA_MESSAGE, false)
                .setAction(DemandIntentReceiver.ACTION_DEMAND);
        PendingIntent demandPendingIntent = PendingIntent.getBroadcast(this, 0, demandIntent, 0);
        NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.mipmap.logo,
                this.getString(R.string.srv_turn_off_flash), demandPendingIntent).build();
        showNotification(action, wearEnabled);
    }

    private void showNotification(NotificationCompat.Action action, boolean wearEnabled) {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.notification)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentTitle(this.getString(R.string.app_name))
                .setContentText(this.getString(R.string.srv_flash_status, "ON"));

        if (wearEnabled) {
            Bitmap bg = BitmapFactory.decodeResource(getResources(), R.color.colorPrimary);
            builder.extend(new NotificationCompat.WearableExtender().addAction(action).setBackground(bg));
        }
        builder.addAction(action);


        final NotificationManagerCompat managerCompat = NotificationManagerCompat.from(this);
        managerCompat.notify(2376, builder.build());
    }

    private void logOut() {
        mLogOutDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle(getString(R.string.ma_log_out_dialog_title))
                .setMessage(getString(R.string.ma_log_out_dialog_message))
                .setNegativeButton(getString(R.string.ma_log_out_dialog_negative), new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton(getString(R.string.ma_log_out_dialog_positive), new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        Crashlytics.log(Log.INFO, "MA", "User logged out.");
                        RelayrSdk.logOut();
                        Storage.instance().clear();
                        dialog.dismiss();
                        finish();
                        IntroActivity.start(MainActivity.this);
                    }
                }).show();
    }

    private void checkForDevice() {
        if (Storage.instance().getDevice() != null) {
            switchView(1);
        } else {
            mLoadingProgress = ProgressDialog.show(this, getString(R.string.initializing), getString(R.string.please_wait), true);

            RelayrSdk.getUser()
                    .flatMap(new Func1<User, Observable<List<Device>>>() {
                        @Override public Observable<List<Device>> call(User user) {
                            return user.getDevices();
                        }
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<List<Device>>() {
                        @Override public void onCompleted() {}

                        @Override public void onError(Throwable e) {
                            Crashlytics.log(Log.ERROR, "MA", "Loading devices failed.");
                            e.printStackTrace();
                            if (mLoadingProgress != null) mLoadingProgress.dismiss();
                            checkForDevice();
                        }

                        @Override public void onNext(List<Device> devices) {
                            if (mLoadingProgress != null) mLoadingProgress.dismiss();
                            for (Device device : devices)
                                if (device.getModelId() != null && device.getModelId().equals(Storage.MODEL_ID))
                                    Storage.instance().saveDevice(device);

                            switchView(Storage.instance().getDevice() != null ? 1 : 0);
                        }
                    });
        }
    }

    private void switchView(int position) {
        mPosition = position;
        switchView();
    }

    private void switchView() {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                mContainer.removeAllViews();
                if (mPosition == 0)
                    mCurrentView = (BasicView) View.inflate(MainActivity.this, R.layout.content_device, null);
                else if (mPosition == 1)
                    mCurrentView = (BasicView) View.inflate(MainActivity.this, R.layout.content_send_receive, null);
                else if (mPosition == 2)
                    mCurrentView = (BasicView) View.inflate(MainActivity.this, R.layout.content_settings, null);
                else if (mPosition == 3)
                    mCurrentView = (BasicView) View.inflate(MainActivity.this, R.layout.content_user, null);

                mCurrentView.setListener(MainActivity.this);
                mContainer.addView(mCurrentView);
            }
        });
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.e("APP", "Google API Client was connected");
        mResolvingError = false;
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        sendToWearable(mActivateWearable);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.e("APP", "Connection to Google API client was suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        if (mResolvingError) return;
        if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
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
        for (DataEvent event : FreezableUtils.freezeIterable(dataEvents)) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                if (SENSOR_PATH.equals(event.getDataItem().getUri().getPath())) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    int val = dataMapItem.getDataMap().getInt(SENSOR);
                    final long timeMillis = System.currentTimeMillis();
                    publishReading(new Reading(timeMillis, timeMillis, "message", "/", val + " lux"));
                }
            }
        }
    }

    private void sendToWearable(boolean active) {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(ACTIVATE_PATH);
        putDataMapRequest.getDataMap().putBoolean(ACTIVATE, active);
        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request);
    }
}
