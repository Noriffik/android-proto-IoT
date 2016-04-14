package io.relayr.iotsmartphone.ui.rules;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.relayr.iotsmartphone.R;

public class FragmentRules extends Fragment {

    @InjectView(R.id.condition_one) RuleCondition mConditionOne;
    @InjectView(R.id.condition_two) RuleCondition mConditionTwo;

    @InjectView(R.id.outcome_one) RuleOutcome mOutcomeOne;
    @InjectView(R.id.outcome_two) RuleOutcome mOutcomeTwo;

    public FragmentRules() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        final View view = inflater.inflate(R.layout.activity_tab_fragment_rules, container, false);
        ButterKnife.inject(this, view);

        setUpConditions();

        return view;
    }

    private void setUpConditions() {
        mConditionOne.setUp(R.color.graph_yellow);
        mConditionTwo.setUp(R.color.graph_blue);
        mOutcomeOne.setUp(R.color.graph_green);
        mOutcomeTwo.setUp(R.color.graph_red);

        mConditionTwo.setVisibility(View.GONE);
        mOutcomeTwo.setVisibility(View.GONE);
    }
}
