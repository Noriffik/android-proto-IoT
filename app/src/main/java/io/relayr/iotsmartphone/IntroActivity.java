package io.relayr.iotsmartphone;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.animation.BounceInterpolator;
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

public class IntroActivity extends AppCompatActivity {

    @InjectView(R.id.intro_image) ImageView mImage;

    private Subscription mUserInfoSubscription = Subscriptions.empty();
    private SharedPreferences mPrefs;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        ButterKnife.inject(this);
        Log.e("IA", "onCreate");

        mPrefs = getSharedPreferences("io.relayr.iotsmartphone", Context.MODE_PRIVATE);

        if (RelayrSdk.isUserLoggedIn()) {
            showToast("Hello " + mPrefs.getString("io.relayr.username", ""));
            new Handler().postDelayed(new Runnable() {
                @Override public void run() {
                    startActivity(new Intent(IntroActivity.this, MainActivity.class));
                    finish();
                }
            }, 3000);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        Log.e("IA", "onResume");

        DisplayMetrics metrics = this.getResources().getDisplayMetrics();

        ObjectAnimator moveAnim = ObjectAnimator.ofFloat(mImage, "Y", metrics.heightPixels / 3);
        moveAnim.setInterpolator(new BounceInterpolator());
        moveAnim.setDuration(RelayrSdk.isUserLoggedIn() ? 3000 : 2000);
        moveAnim.start();

        if (!RelayrSdk.isUserLoggedIn()) {
            new Handler().postDelayed(new Runnable() {
                @Override public void run() {
                    logIn();
                }
            }, 2000);
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
                        mPrefs.edit().putString("io.relayr.username", user.getName()).apply();
                        mPrefs.edit().putString("io.relayr.userId", user.getId()).apply();

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
