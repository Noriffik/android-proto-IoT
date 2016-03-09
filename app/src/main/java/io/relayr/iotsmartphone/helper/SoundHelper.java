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

import static android.content.Context.VIBRATOR_SERVICE;
import static android.media.RingtoneManager.TYPE_ALARM;

public class SoundHelper {

    private boolean mIsPlaying;
    private Ringtone mRingManager;
    private Vibrator mVibrator;

    public void playMusic(Context context, String seconds) {
        if (mIsPlaying) return;
        mIsPlaying = true;

        int sec;
        try {
            sec = Integer.parseInt(seconds);
            if (sec > 10) sec = sec % 10;
        } catch (Exception e) {
            Crashlytics.log(Log.ERROR, "SoundH", "Seconds can't be parsed: " + seconds);
            e.printStackTrace();
            return;
        }

        Uri alarm = RingtoneManager.getDefaultUri(TYPE_ALARM);
        if (mRingManager == null) mRingManager = RingtoneManager.getRingtone(context, alarm);
        vibrate(context, sec * 1000);
        mRingManager.play();

        Observable
                .create(new Observable.OnSubscribe<Object>() {
                    @Override public void call(Subscriber<? super Object> subscriber) {
                        mIsPlaying = false;
                        if (mRingManager != null) mRingManager.stop();
                        if (mVibrator != null) mVibrator.cancel();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .delaySubscription(sec, TimeUnit.SECONDS)
                .subscribe();
    }

    public void close() {
        if (mVibrator != null && mVibrator.hasVibrator()) mVibrator.cancel();
        if (mRingManager != null && mRingManager.isPlaying()) mRingManager.stop();
        mVibrator = null;
        mRingManager = null;
    }

    //time in milliseconds
    private void vibrate(Context context, int time) {
        if (mVibrator == null) mVibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
        if (mVibrator.hasVibrator())
            mVibrator.vibrate(time);
    }
}
