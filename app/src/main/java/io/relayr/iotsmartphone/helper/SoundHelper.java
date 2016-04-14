package io.relayr.iotsmartphone.helper;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import static android.content.Context.VIBRATOR_SERVICE;
import static android.media.RingtoneManager.TYPE_ALARM;

public class SoundHelper {

    private static final int DURATION = 3000;

    private static boolean mIsPlaying;
    private Ringtone mRingManager;
    private Vibrator mVibrator;

    public void playMusic(Context context) {
        if (mIsPlaying) return;
        mIsPlaying = true;

        Uri alarm = RingtoneManager.getDefaultUri(TYPE_ALARM);
        if (mRingManager == null) mRingManager = RingtoneManager.getRingtone(context, alarm);
        mRingManager.play();

        Observable
                .create(new Observable.OnSubscribe<Void>() {
                    @Override public void call(Subscriber<? super Void> subscriber) {
                        mIsPlaying = false;
                        if (mRingManager != null) mRingManager.stop();
                        if (mVibrator != null) mVibrator.cancel();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .delaySubscription(DURATION, TimeUnit.MILLISECONDS)
                .subscribe(new Action1<Void>() {
                    @Override public void call(Void aVoid) {
                        mIsPlaying = false;
                    }
                }, new Action1<Throwable>() {
                    @Override public void call(Throwable throwable) {
                        mIsPlaying = false;
                    }
                });
    }


    public void close() {
        if (mVibrator != null && mVibrator.hasVibrator()) mVibrator.cancel();
        if (mRingManager != null && mRingManager.isPlaying()) mRingManager.stop();
        mVibrator = null;
        mRingManager = null;
    }

    public void vibrate(Context context) {
        if (mVibrator == null) mVibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
        if (mVibrator.hasVibrator())
            mVibrator.vibrate(DURATION);
    }
}
