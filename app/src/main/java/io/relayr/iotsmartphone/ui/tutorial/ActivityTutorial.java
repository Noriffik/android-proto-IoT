package io.relayr.iotsmartphone.ui.tutorial;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindDimen;
import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.storage.Storage;
import io.relayr.iotsmartphone.ui.ActivityMain;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class ActivityTutorial extends AppCompatActivity {

    @BindView(R.id.splash) View mSplash;
//
//    @BindView(R.id.btn_next) TextView btnNext;
//    @BindView(R.id.btn_skip) TextView btnSkip;
//    @BindView(R.id.tutorial_pager) ViewPager mPager;
//    @BindView(R.id.tutorial_indicator) View mPagerIndicator;
//    @BindView(R.id.tutorial_dots) LinearLayout mPagerDots;
//
//    @BindDrawable(R.drawable.tutorial_selected) Drawable dotSelectedRes;
//    @BindDrawable(R.drawable.tutorial_non_selected) Drawable dotNonSelectedRes;
//
//    @BindDimen(R.dimen.default_padding_half) int dotsPaddingRes;
//
//    private int mDotsCount;
//    private ImageView[] mDots;
//    private ViewPagerAdapter mAdapter;
//
//    private int[] mImageResources = {R.drawable.cover, R.drawable.cover, R.drawable.cover};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);
        ButterKnife.bind(this);

//        mSplash.setVisibility(Storage.instance().TutorialActivityFinished() ? View.VISIBLE : View.GONE);
//        mPager.setVisibility(Storage.instance().TutorialActivityFinished() ? View.GONE : View.VISIBLE);
//        mPagerIndicator.setVisibility(Storage.instance().TutorialActivityFinished() ? View.GONE : View.VISIBLE);

//        if (Storage.instance().TutorialActivityFinished())
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ActivityMain.start(ActivityTutorial.this);
                    finish();
                }
            }, 1000);
//        else
//            init();
    }

//    @SuppressWarnings("unused") @OnClick(R.id.btn_next)
//    public void onNextClicked() {
//        if (mPager.getCurrentItem() == mDotsCount - 1)
//            finishTutorial();
//        else
//            mPager.setCurrentItem((mPager.getCurrentItem() < mDotsCount) ? mPager.getCurrentItem() + 1 : 0);
//    }
//
//    @SuppressWarnings("unused") @OnClick(R.id.btn_skip)
//    public void onFinishClicked() {
//        finishTutorial();
//    }
//
//    private void init() {
//        mAdapter = new ViewPagerAdapter(this, mImageResources);
//        mPager.setAdapter(mAdapter);
//        mPager.setCurrentItem(0);
//        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
//            @Override public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
//
//            @Override public void onPageSelected(int position) {
//                for (int i = 0; i < mDotsCount; i++)
//                    mDots[i].setImageDrawable(dotNonSelectedRes);
//                mDots[position].setImageDrawable(dotSelectedRes);
//
//                btnSkip.setVisibility(position + 1 == mDotsCount ? View.GONE : View.VISIBLE);
//                btnNext.setText(position + 1 == mDotsCount ? R.string.t_lets_start : R.string.t_next);
//            }
//
//            @Override public void onPageScrollStateChanged(int state) {}
//        });
//
//        setUiPageViewController();
//    }
//
//    private void setUiPageViewController() {
//        mDotsCount = mAdapter.getCount();
//        mDots = new ImageView[mDotsCount];
//
//        for (int i = 0; i < mDotsCount; i++) {
//            mDots[i] = new ImageView(this);
//            mDots[i].setImageDrawable(dotNonSelectedRes);
//
//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
//            params.setMargins(dotsPaddingRes, 0, dotsPaddingRes, 0);
//
//            mPagerDots.addView(mDots[i], params);
//        }
//
//        mDots[0].setImageDrawable(dotSelectedRes);
//    }
//
//    private void finishTutorial() {
//        Storage.instance().tutorialActivity(true);
//        ActivityMain.start(ActivityTutorial.this);
//        finish();
//    }
}
