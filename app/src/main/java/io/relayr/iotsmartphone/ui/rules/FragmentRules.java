package io.relayr.iotsmartphone.ui.rules;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.ButterKnife;
import io.relayr.iotsmartphone.R;

public class FragmentRules extends Fragment {

//    @InjectView(R.id.condition_one) View mConditionOne;
//    @InjectView(R.id.condition_two) View mConditionTwo;
//
//    @InjectView(R.id.outcome_one) View mOutcomeOne;
//    @InjectView(R.id.outcome_two) View mOutcomeTwo;

    public FragmentRules() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        final View view = inflater.inflate(R.layout.activity_tab_fragment_rules, container, false);
        ButterKnife.inject(this, view);

//        mConditionTwo.setVisibility(View.GONE);
//        mOutcomeTwo.setVisibility(View.GONE);

        return view;
    }
}
