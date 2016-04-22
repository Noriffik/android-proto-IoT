package io.relayr.iotsmartphone.ui.rules;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import java.util.concurrent.TimeoutException;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.handler.RuleBuilder;
import io.relayr.iotsmartphone.handler.RuleHandler;
import io.relayr.iotsmartphone.storage.Constants;
import io.relayr.iotsmartphone.utils.UiHelper;
import io.relayr.java.helper.observer.SimpleObserver;
import io.relayr.java.model.models.transport.DeviceCommand;
import io.relayr.java.model.models.transport.DeviceReading;
import rx.android.schedulers.AndroidSchedulers;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static io.relayr.iotsmartphone.storage.Constants.DeviceType.PHONE;

public class FragmentRules extends Fragment {

    interface ConditionListener {
        void conditionChanged(Constants.DeviceType type, DeviceReading reading, String operation, int value);

        void removeCondition();
    }

    interface OutcomeListener {
        void outcomeChanged(DeviceCommand command, boolean value);

        void removeOutcome();
    }

    @InjectView(R.id.rules_logged_in) View mLoggedInView;
    @InjectView(R.id.rules_not_logged_in) View mNotLoggedInView;

    @InjectView(R.id.condition_one) RuleCondition mConditionOne;
    @InjectView(R.id.condition_two) RuleCondition mConditionTwo;
    @InjectView(R.id.condition_operator) TextView mConditionOperator;

    @InjectView(R.id.outcome_one) RuleOutcome mOutcomeOne;
    @InjectView(R.id.outcome_two) RuleOutcome mOutcomeTwo;

    public FragmentRules() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        final View view = inflater.inflate(R.layout.activity_tab_fragment_rules, container, false);
        ButterKnife.inject(this, view);

        RuleHandler.init(new SimpleObserver<Boolean>() {
            @Override public void error(Throwable e) {
                UiHelper.showSnackBar(getActivity(), R.string.rule_update_problem);
            }

            @Override public void success(Boolean o) {
                UiHelper.showSnackBar(getActivity(), R.string.rule_updated);
            }
        });

        return view;
    }

    @Override public void onResume() {
        super.onResume();

        if (UiHelper.isCloudConnected()) loadRule();
        else setUpConditions(null);

        mLoggedInView.setVisibility(UiHelper.isCloudConnected() ? VISIBLE : GONE);
        mNotLoggedInView.setVisibility(UiHelper.isCloudConnected() ? GONE : VISIBLE);
    }

    private void loadRule() {
        RuleHandler.loadRule()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleObserver<RuleBuilder>() {
                    @Override public void error(Throwable e) {
                        Crashlytics.log(Log.WARN, "RuleHandler", "Failed to load current rule.");
                        if (e instanceof TimeoutException) loadRule();
                        else setUpConditions(null);
                    }

                    @Override public void success(RuleBuilder rule) {
                        setUpConditions(rule);
                    }
                });
    }

    @SuppressWarnings("unused") @OnClick(R.id.condition_operator)
    public void onOperatorClicked() {
        mConditionOperator.setVisibility(VISIBLE);
        if (mConditionOperator.getText().equals("&")) mConditionOperator.setText("||");
        else mConditionOperator.setText("&");
        RuleHandler.setConditionOperator(mConditionOperator.getText().toString());
    }

    private void setUpConditions(final RuleBuilder rule) {
        mConditionOne.setUp(R.color.graph_yellow, rule, 0,
                new ConditionListener() {
                    @Override
                    public void conditionChanged(Constants.DeviceType type, DeviceReading reading, String operation, int value) {
                        if (reading != null) {
                            mConditionOperator.setVisibility(VISIBLE);
                            mConditionTwo.setVisibility(VISIBLE);
                            RuleHandler.setCondition(0, type, reading.getMeaning(), operation, value);
                        }
                    }

                    @Override public void removeCondition() {
                        RuleHandler.removeCondition(0);
                        mConditionOperator.setVisibility(GONE);
                        mConditionTwo.setVisibility(GONE);
                    }
                });
        mConditionOperator.setVisibility(rule == null ? GONE : VISIBLE);
        mConditionTwo.setVisibility(rule == null ? GONE : VISIBLE);
        mConditionTwo.setUp(R.color.graph_blue, rule, 1, new ConditionListener() {
            @Override
            public void conditionChanged(Constants.DeviceType type, DeviceReading reading, String operation, int value) {
                mConditionOperator.setVisibility(VISIBLE);
                RuleHandler.setConditionOperator(mConditionOperator.getText().toString());
                RuleHandler.setCondition(1, type, reading.getMeaning(), operation, value);
            }

            @Override public void removeCondition() {
                RuleHandler.removeCondition(1);
            }
        });

        mOutcomeOne.setUp(R.color.graph_green, rule, 0, new OutcomeListener() {
            @Override public void outcomeChanged(DeviceCommand command, boolean value) {
                if (command != null) {
                    mOutcomeTwo.setVisibility(VISIBLE);
                    RuleHandler.setOutcome(0, PHONE, command.getName(), value);
                }
            }

            @Override public void removeOutcome() {
                mOutcomeTwo.setVisibility(GONE);
                RuleHandler.removeOutcome(0);
            }
        });
        mOutcomeTwo.setVisibility(rule == null ? GONE : VISIBLE);
        mOutcomeTwo.setUp(R.color.graph_red, rule, 1, new OutcomeListener() {
            @Override public void outcomeChanged(DeviceCommand command, boolean value) {
                RuleHandler.setOutcome(1, PHONE, command.getName(), value);
            }

            @Override public void removeOutcome() {
                RuleHandler.removeOutcome(1);
            }
        });
    }
}
