package io.relayr.iotsmartphone;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.relayr.android.RelayrSdk;
import io.relayr.java.model.User;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import static android.view.animation.Animation.RELATIVE_TO_SELF;

public class IntroActivity extends AppCompatActivity {

    @InjectView(R.id.intro_image) ImageView mImage;

    private final int ANIMATION_DURATION = 1500;
    private Subscription mUserInfoSubscription = Subscriptions.empty();

    public static void start(MainActivity context) {
        context.startActivity(new Intent(context, IntroActivity.class));
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        ButterKnife.inject(this);

        if (RelayrSdk.isUserLoggedIn()) {
            showToast(getString(R.string.ia_hello, Storage.instance().getUsername()));
            new Handler().postDelayed(new Runnable() {
                @Override public void run() {
                    startActivity(new Intent(IntroActivity.this, MainActivity.class));
                    finish();
                }
            }, ANIMATION_DURATION);
        }

        mImage.setAnimation(createAnimation());
    }

    @Override protected void onResume() {
        super.onResume();
        if (mImage != null) mImage.animate();

        if (!RelayrSdk.isUserLoggedIn()) {
            new Handler().postDelayed(new Runnable() {
                @Override public void run() {
                    logIn();
                }
            }, ANIMATION_DURATION);
        }
    }

    @Override protected void onPause() {
        super.onPause();
        mUserInfoSubscription.unsubscribe();
    }

    private Animation createAnimation() {
        AnimationSet animSet = new AnimationSet(false);
        RotateAnimation rotate = new RotateAnimation(0f, 1440f, RELATIVE_TO_SELF, 0.5f, RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration((long) (ANIMATION_DURATION * .7f));
        rotate.setFillAfter(true);

        ScaleAnimation zoom = new ScaleAnimation(0, 1.5f, 0, 1.5f, RELATIVE_TO_SELF, 0.5f, RELATIVE_TO_SELF, 1f);
        zoom.setDuration((long) (ANIMATION_DURATION * .9f));
        zoom.setFillAfter(true);

        animSet.addAnimation(zoom);
        animSet.addAnimation(rotate);
        animSet.setFillAfter(true);
        animSet.setInterpolator(new AccelerateDecelerateInterpolator());

        return animSet;
    }

    private void logIn() {
        RelayrSdk.logIn(this)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<User>() {
                    @Override public void onCompleted() {}

                    @Override public void onError(Throwable e) {
                        Crashlytics.log(Log.ERROR, "IA", "Login failed.");
                        e.printStackTrace();

                        showToast(getString(R.string.ia_log_in_failed));

                        if (e instanceof TimeoutException) logIn();
                        else finish();
                    }

                    @Override public void onNext(User user) {
                        loadUserInfo();
                    }
                });
    }

    private void loadUserInfo() {
        mUserInfoSubscription = RelayrSdk.getUser()
                .timeout(7, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<User>() {
                    @Override public void onCompleted() {}

                    @Override public void onError(Throwable e) {
                        Crashlytics.log(Log.ERROR, "IA", "Failed to load user info.");
                        e.printStackTrace();

                        showToast(getString(R.string.something_went_wrong));

                        if (e instanceof TimeoutException) loadUserInfo();
                        else finish();
                    }

                    @Override public void onNext(User user) {
                        Storage.instance().saveUser(user);

                        showToast(getString(R.string.ia_hello, user.getName()));

                        startActivity(new Intent(IntroActivity.this, MainActivity.class));
                        finish();
                    }
                });
    }

    private void showToast(String data) {
        Toast.makeText(this, data, Toast.LENGTH_SHORT).show();
    }
}
