package io.relayr.iotsmartphone;

import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import io.relayr.android.RelayrSdk;
import io.relayr.iotsmartphone.tabs.Constants;
import io.relayr.iotsmartphone.tabs.FragmentCloud;
import io.relayr.iotsmartphone.tabs.FragmentReadings;
import io.relayr.iotsmartphone.tabs.FragmentRules;
import io.relayr.java.helper.observer.SimpleObserver;
import io.relayr.java.model.models.DeviceModel;
import io.relayr.java.model.models.error.DeviceModelsException;
import io.relayr.java.model.models.transport.Transport;

public class MainTabActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    @InjectView(R.id.toolbar) Toolbar mToolbar;
    @InjectView(R.id.drawer_layout) DrawerLayout mDrawer;
    @InjectView(R.id.nav_view) NavigationView mNavView;
    @InjectView(R.id.fab) FloatingActionButton mFab;
    @InjectView(R.id.viewpager) ViewPager mViewPager;
    @InjectView(R.id.tabs) TabLayout mTabView;

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
}
