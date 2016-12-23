package io.relayr.iotsmartphone.ui.rules;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import java.util.concurrent.TimeoutException;

import butterknife.ButterKnife;
import butterknife.BindView;
import butterknife.OnClick;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.handler.RuleBuilder;
import io.relayr.iotsmartphone.handler.RuleHandler;
import io.relayr.iotsmartphone.storage.Constants;
import io.relayr.iotsmartphone.ui.utils.TutorialUtil;
import io.relayr.iotsmartphone.ui.utils.UiUtil;
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

    @BindView(R.id.rules_logged_in) View mLoggedInView;
    @BindView(R.id.rules_not_logged_in) View mNotLoggedInView;

    @BindView(R.id.condition_one) RuleCondition mConditionOne;
    @BindView(R.id.condition_two) RuleCondition mConditionTwo;
    @BindView(R.id.condition_operator) TextView mConditionOperator;

    @BindView(R.id.rule_activity) Switch mStateSwitch;
    @BindView(R.id.outcome_one) RuleOutcome mOutcomeOne;
    @BindView(R.id.outcome_two) RuleOutcome mOutcomeTwo;

    public FragmentRules() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        final View view = inflater.inflate(R.layout.activity_tab_fragment_rules, container, false);
        ButterKnife.bind(this, view);

        RuleHandler.init(new SimpleObserver<Boolean>() {
            @Override public void error(Throwable e) {
                UiUtil.showSnackBar(getActivity(), R.string.rule_update_problem);
            }

            @Override public void success(Boolean o) {
                UiUtil.showSnackBar(getActivity(), R.string.rule_updated);
            }
        });

        return view;
    }

    @Override public void onResume() {
        super.onResume();

        if (UiUtil.isCloudConnected()) loadRule();
        else setUpConditions(null);

        mLoggedInView.setVisibility(UiUtil.isCloudConnected() ? VISIBLE : GONE);
        mNotLoggedInView.setVisibility(UiUtil.isCloudConnected() ? GONE : VISIBLE);
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

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && mStateSwitch != null) updateActivity();
    }

    @SuppressWarnings("unused") @OnClick(R.id.condition_operator)
    public void onOperatorClicked() {
        mConditionOperator.setVisibility(VISIBLE);
        if (mConditionOperator.getText().equals("&")) mConditionOperator.setText("||");
        else mConditionOperator.setText("&");
        RuleHandler.setConditionOperator(mConditionOperator.getText().toString());
    }

    private void setUpConditions(final RuleBuilder rule) {
        mConditionOne.setUp(R.color.accent, rule, 0,
                new ConditionListener() {
                    @Override
                    public void conditionChanged(Constants.DeviceType type, DeviceReading reading, String operation, int value) {
                        if (reading != null) {
                            mConditionOperator.setVisibility(VISIBLE);
                            mConditionTwo.setVisibility(VISIBLE);
                            RuleHandler.setCondition(0, type, reading.getMeaning(), operation, value);
                            updateActivity();
                        }
                    }

                    @Override public void removeCondition() {
                        RuleHandler.removeCondition(0);
                        updateActivity();
                        if (!RuleHandler.isValid()) {
                            mConditionOperator.setVisibility(GONE);
                            mConditionTwo.setVisibility(GONE);
                        }
                    }
                });

        mConditionOperator.setVisibility(rule == null ? GONE : VISIBLE);
        if (rule != null) mConditionOperator.setText(rule.getConditionOperator());

        mConditionTwo.setVisibility(rule == null ? GONE : VISIBLE);
        mConditionTwo.setUp(R.color.accent, rule, 1, new ConditionListener() {
            @Override
            public void conditionChanged(Constants.DeviceType type, DeviceReading reading, String operation, int value) {
                mConditionOperator.setVisibility(VISIBLE);
                RuleHandler.setConditionOperator(mConditionOperator.getText().toString());
                RuleHandler.setCondition(1, type, reading.getMeaning(), operation, value);
                updateActivity();
            }

            @Override public void removeCondition() {
                RuleHandler.removeCondition(1);
                updateActivity();
            }
        });

        mOutcomeOne.setUp(R.color.accent, rule, 0, new OutcomeListener() {
            @Override public void outcomeChanged(DeviceCommand command, boolean value) {
                if (command != null) {
                    mOutcomeTwo.setVisibility(VISIBLE);
                    RuleHandler.setOutcome(0, PHONE, command.getName(), value);
                    updateActivity();
                }
            }

            @Override public void removeOutcome() {
                RuleHandler.removeOutcome(0);
                updateActivity();
                if (!RuleHandler.isValid()) mOutcomeTwo.setVisibility(GONE);
            }
        });
        mOutcomeTwo.setVisibility(rule == null ? GONE : VISIBLE);
        mOutcomeTwo.setUp(R.color.accent, rule, 1, new OutcomeListener() {
            @Override public void outcomeChanged(DeviceCommand command, boolean value) {
                RuleHandler.setOutcome(1, PHONE, command.getName(), value);
                updateActivity();
            }

            @Override public void removeOutcome() {
                RuleHandler.removeOutcome(1);
                updateActivity();
            }
        });

        updateActivity();
    }

    private void updateActivity() {
        if (RuleHandler.isValid()) {
            mStateSwitch.setOnCheckedChangeListener(null);
            mStateSwitch.setChecked(RuleHandler.isActive());
            if (mStateSwitch.getVisibility() == GONE) {
                mStateSwitch.setVisibility(VISIBLE);
                UiUtil.showSnackBar(getActivity(), RuleHandler.isActive() ? R.string.rule_enabled : R.string.rule_disabled);
            }
        } else {
            mStateSwitch.setVisibility(GONE);
        }
        mStateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                RuleHandler.setActivity(isChecked);
                UiUtil.showSnackBar(getActivity(), isChecked ? R.string.rule_enabled : R.string.rule_disabled);
            }
        });
    }
}
