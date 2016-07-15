package io.relayr.iotsmartphone.handler;

import android.app.Activity;
import android.util.Log;
import android.util.Pair;

import com.crashlytics.android.Crashlytics;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.relayr.android.RelayrSdk;
import io.relayr.android.storage.DataStorage;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.storage.Constants;
import io.relayr.iotsmartphone.storage.Storage;
import io.relayr.iotsmartphone.ui.utils.TutorialUtil;
import io.relayr.iotsmartphone.ui.utils.UiUtil;
import io.relayr.java.helper.observer.SimpleObserver;
import io.relayr.java.model.CreateDevice;
import io.relayr.java.model.Device;
import io.relayr.java.model.User;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static io.relayr.iotsmartphone.storage.Constants.DeviceType.PHONE;
import static io.relayr.iotsmartphone.storage.Constants.DeviceType.WATCH;

public class CloudHandler {

    public static Observable<Pair<Constants.DeviceType, Device>> logIn(final Activity activity) {
        return RelayrSdk.logIn(activity)
                .observeOn(Schedulers.io())
                .flatMap(new Func1<User, Observable<Pair<Constants.DeviceType, Device>>>() {
                    @Override
                    public Observable<Pair<Constants.DeviceType, Device>> call(User user) {
                        if (!user.getId().equals(Storage.instance().oldUserId()))
                            Storage.instance().loggedIn(user.getId());
                        Storage.instance().activate(PHONE);
                        Storage.instance().activate(WATCH);

                        TutorialUtil.updateTutorial(Storage.TUTORIAL_LOG_IN, true);
                        TutorialUtil.updateTutorial(Storage.TUTORIAL_LOG_TO_PLAY, true);

                        return loadDevices(activity);
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override public void call(Throwable e) {
                        UiUtil.showSnackBar(activity, R.string.cloud_log_in_failed);
                        Crashlytics.log(Log.DEBUG, "CloudHandler", "Login failed.");
                        Crashlytics.logException(e);
                    }
                });
    }

    public static Observable<Pair<Constants.DeviceType, Device>> loadDevices(final Activity activity) {
        return Observable.create(new Observable.OnSubscribe<Pair<Constants.DeviceType, Device>>() {
            @Override
            public void call(final Subscriber<? super Pair<Constants.DeviceType, Device>> subscriber) {
                if (Storage.instance().getDeviceId(PHONE) != null)
                    getDeviceFromCloud(PHONE)
                            .subscribe(new SimpleObserver<Device>() {
                                @Override public void error(Throwable e) {
                                    if (!(e instanceof TimeoutException)) {
                                        Storage.instance().clearDevicesData();
                                        createDevice(activity, PHONE, subscriber);
                                        loadWatchDevice(activity, subscriber);
                                    } else {
                                        subscriber.onError(e);
                                    }
                                }

                                @Override public void success(Device device) {
                                    Crashlytics.log(Log.DEBUG, "CloudHandler", "Loaded phone " + device.getId());
                                    subscriber.onNext(new Pair<>(PHONE, device));

                                    if (!UiUtil.isWearableConnected(activity)) return;
                                    loadWatchDevice(activity, subscriber);
                                }
                            });
                else {
                    createDevice(activity, PHONE, subscriber);
                    loadWatchDevice(activity, subscriber);
                }
            }
        }).subscribeOn(Schedulers.io());
    }

    private static void loadWatchDevice(final Activity activity, final Subscriber<? super Pair<Constants.DeviceType, Device>> subscriber) {
        if (Storage.instance().getDeviceId(WATCH) != null)
            getDeviceFromCloud(WATCH)
                    .subscribe(new SimpleObserver<Device>() {
                        @Override public void error(Throwable e) {
                            if (!(e instanceof TimeoutException)) createDevice(activity, WATCH, subscriber);
                            else subscriber.onError(e);
                        }

                        @Override public void success(Device device) {
                            Crashlytics.log(Log.DEBUG, "CloudHandler", "Loaded watch " + device.getId());
                            subscriber.onNext(new Pair<>(PHONE, device));
                        }
                    });
        else createDevice(activity, WATCH, subscriber);
    }

    private static Observable<Device> getDeviceFromCloud(final Constants.DeviceType type) {
        return RelayrSdk.getDeviceApi()
                .getDevice(Storage.instance().getDeviceId(type))
                .timeout(5, TimeUnit.SECONDS)
                .doOnError(new Action1<Throwable>() {
                    @Override public void call(Throwable e) {
                        Crashlytics.log(Log.ERROR, "CloudHandler", "Failed to load " + type.name() + " device.");
                        Crashlytics.logException(e);
                    }
                });
    }

    private static void createDevice(final Activity activity, final Constants.DeviceType type,
                                     final Subscriber<? super Pair<Constants.DeviceType, Device>> subscriber) {
        final String name = Storage.instance().getDeviceName(type);
        final String modelId = type == PHONE ? Storage.MODEL_PHONE : Storage.MODEL_WATCH;
        final String description = type == PHONE ? activity.getString(R.string.app_title_phone) :
                activity.getString(R.string.app_title_watch);

        final CreateDevice toCreate = new CreateDevice(name == null ? description : name,
                description, modelId, DataStorage.getUserId(), null, "1.0.0");

        RelayrSdk.getDeviceApi()
                .createDevice(toCreate)
                .timeout(5, TimeUnit.SECONDS)
                .subscribe(new SimpleObserver<Device>() {
                    @Override public void error(Throwable e) {
                        if (e instanceof TimeoutException) {
                            createDevice(activity, type, subscriber);
                        } else {
                            Crashlytics.log(Log.DEBUG, "CloudHandler", "Failed to create " + type.name() + " device.");
                            Crashlytics.logException(e);
                            subscriber.onError(e);
                        }
                    }

                    @Override public void success(Device device) {
                        Crashlytics.log(Log.DEBUG, "CloudHandler", "Created device " + device.getId());
                        Storage.instance().activate(type);
                        subscriber.onNext(new Pair<>(type, device));
                    }
                });
    }
}
