package io.relayr.iotsmartphone;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.Toast;

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

    private Subscription mUserInfoSubscription = Subscriptions.empty();

    public static void start(MainActivity context) {
        context.startActivity(new Intent(context, IntroActivity.class));
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        ButterKnife.inject(this);
        Log.e("IA", "onCreate");

        if (RelayrSdk.isUserLoggedIn()) {
            showToast("Hello " + Storage.instance().getUsername());
            new Handler().postDelayed(new Runnable() {
                @Override public void run() {
                    startActivity(new Intent(IntroActivity.this, MainActivity.class));
                    finish();
                }
            }, 3000);
        }

        AnimationSet animSet = new AnimationSet(false);
        RotateAnimation rotate = new RotateAnimation(0f, 1440f, RELATIVE_TO_SELF, 0.5f, RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(1500);
        rotate.setFillAfter(true);

        ScaleAnimation zoom = new ScaleAnimation(0, 1.5f, 0, 1.5f, RELATIVE_TO_SELF, 0.5f, RELATIVE_TO_SELF, 1f);
        zoom.setDuration(2000);
        zoom.setFillAfter(true);

        animSet.addAnimation(zoom);
        animSet.addAnimation(rotate);
        animSet.setFillAfter(true);
        animSet.setInterpolator(new AccelerateDecelerateInterpolator());

        mImage.setAnimation(animSet);
    }

    @Override protected void onResume() {
        super.onResume();
        Log.e("IA", "onResume");

        if (mImage != null) mImage.animate();

        if (!RelayrSdk.isUserLoggedIn()) {
            new Handler().postDelayed(new Runnable() {
                @Override public void run() {
                    logIn();
                }
            }, 2500);
        }
    }

    @Override protected void onPause() {
        super.onPause();
        mUserInfoSubscription.unsubscribe();
    }

    private void logIn() {
        RelayrSdk.logIn(this)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<User>() {
                    @Override public void onCompleted() {}

                    @Override public void onError(Throwable e) {
                        showToast("Log in failed");
                    }

                    @Override public void onNext(User user) {
                        loadUserInfo();
                    }
                });
    }

    private void loadUserInfo() {
        mUserInfoSubscription = RelayrSdk.getUser()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<User>() {
                    @Override public void onCompleted() {}

                    @Override public void onError(Throwable e) {
                        showToast("Something went wrong.");
                        e.printStackTrace();

                        startActivity(new Intent(IntroActivity.this, MainActivity.class));
                        finish();
                    }

                    @Override public void onNext(User user) {
                        Storage.instance().saveUser(user);

                        showToast("Hello " + user.getName());

                        startActivity(new Intent(IntroActivity.this, MainActivity.class));
                        finish();
                    }
                });
    }

    private void showToast(String data) {
        Toast.makeText(this, data, Toast.LENGTH_SHORT).show();
    }
}
