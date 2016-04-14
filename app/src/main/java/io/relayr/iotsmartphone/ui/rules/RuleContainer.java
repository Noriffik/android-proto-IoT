package io.relayr.iotsmartphone.ui.rules;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import io.relayr.iotsmartphone.R;

public class RuleContainer extends LinearLayout {

    @InjectView(R.id.rule_widget_prefix) TextView mPrefixTv;
    @InjectView(R.id.rule_widget_color) View mColorView;

    @InjectView(R.id.rule_widget_icon) ImageView mIconImg;

    @InjectView(R.id.rule_widget_empty_text) TextView mEmptyTv;
    @InjectView(R.id.rule_widget_container) View mContainer;

    @InjectView(R.id.rule_widget_meaning) TextView mMeaning;
    @InjectView(R.id.rule_widget_info) TextView mInfoTv;
    @InjectView(R.id.rule_widget_condition) TextView mConditionTv;
    @InjectView(R.id.rule_widget_value) TextView mValueTv;

    protected int mColor;

    public RuleContainer(Context context) {
        this(context, null);
    }

    public RuleContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RuleContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ButterKnife.inject(this, this);

        mColorView.setBackgroundResource(mColor);
    }

    @OnClick(R.id.rule_widget_icon)
    public void onIconClicked() {
        mEmptyTv.setVisibility(GONE);
        mContainer.setVisibility(VISIBLE);
    }

    public void setUp(int color) {
        this.mColor = color;
    }
}
