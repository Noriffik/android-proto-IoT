package io.relayr.iotsmartphone;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import io.relayr.android.RelayrSdk;
import io.relayr.iotsmartphone.widget.BasicView;
import io.relayr.iotsmartphone.widget.ControlListener;
import io.relayr.iotsmartphone.widget.SettingsView;
import io.relayr.java.model.Device;
import io.relayr.java.model.User;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.location.LocationManager.GPS_PROVIDER;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity implements OnNavigationItemSelectedListener, ControlListener {

    @InjectView(R.id.container) FrameLayout mContainer;
    @InjectView(R.id.fab) FloatingActionButton mFab;

    private int mPosition = 0;
    private ProgressDialog mLoadingProgress;
    private BasicView mCurrentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}, 100);
        } else {
            checkForDevice();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_main) switchView(1);
        else if (id == R.id.nav_user) switchView(2);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 100: {
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    checkForDevice();
                    Log.e("PERMISSION", "2376 Granted");
                } else {
                    Log.e("PERMISSION", "2376 Denied");
                }
            }
        }
    }

    @Override public void onDeviceCreated() {
        switchView(1);
    }

    @OnClick(R.id.fab)
    public void onFabClicked() {
        ((SettingsView) mCurrentView).refreshData();
    }

    private void checkForDevice() {
        if (Storage.instance().getDevice() != null) {
            switchView(1);
        } else {
            mLoadingProgress = ProgressDialog.show(this, "Initializing...", "Please wait", true);

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
                            Log.e("MA", "Loading devices failed.");
                            e.printStackTrace();
                            mLoadingProgress.dismiss();
                            checkForDevice();
                        }

                        @Override public void onNext(List<Device> devices) {
                            mLoadingProgress.dismiss();
                            for (Device device : devices)
                                if (device.getModelId() != null && device.getModelId().equals("86e0a7d7-5e18-449c-b7aa-f3b089c33b67"))
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
        mFab.setVisibility(mPosition == 1 ? VISIBLE : GONE);

        mContainer.removeAllViews();
        if (mPosition == 0)
            mCurrentView = (BasicView) View.inflate(this, R.layout.content_device, null);
        else if (mPosition == 1)
            mCurrentView = (BasicView) View.inflate(this, R.layout.content_settings, null);
        else if (mPosition == 2)
            mCurrentView = (BasicView) View.inflate(this, R.layout.content_user, null);

        mCurrentView.setListener(this);
        mContainer.addView(mCurrentView);
    }
}
