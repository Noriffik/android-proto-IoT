package io.relayr.iotsmartphone.handler;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Strings;
import com.google.gson.Gson;

import java.util.concurrent.TimeUnit;

import io.relayr.android.RelayrSdk;
import io.relayr.android.storage.DataStorage;
import io.relayr.iotsmartphone.storage.Constants;
import io.relayr.iotsmartphone.storage.Storage;
import io.relayr.java.helper.observer.SimpleObserver;
import io.relayr.java.model.rules.AppliedTemplate;
import io.relayr.java.model.rules.TemplateInfo;
import io.relayr.java.model.rules.TemplateParameters;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class RuleHandler {

    private static final String PROJECT_ID = "95758d69-2570-4add-a3a4-90a7414659db";
    private static final String TEMPLATE_ID = "6f214bdd-5f6c-4451-b9e3-2a489c9e472b";
    private static final String TEMPLATE_VERSION = "640bb689-5dc4-4b09-8481-3b30fa6b518b";

    private static RuleBuilder sRule;
    private static AppliedTemplate sAppliedTemplate;
    private static String templateVersion = TEMPLATE_VERSION;

    private static SimpleObserver<Boolean> mObserver;

    public static void init(SimpleObserver<Boolean> observer) {
        mObserver = observer;
    }

    public static Observable<RuleBuilder> loadRule() {
        if (sRule != null) return Observable.just(sRule);

        if (templateVersion == null) return getLatestTemplate();
        else return fetchTemplateInstallation();
    }

    private static Observable<RuleBuilder> getLatestTemplate() {
        return RelayrSdk.getRuleTemplateApi().getTemplate(PROJECT_ID, TEMPLATE_ID)
                .timeout(7, TimeUnit.SECONDS)
                .flatMap(new Func1<TemplateInfo, Observable<RuleBuilder>>() {
                    @Override public Observable<RuleBuilder> call(TemplateInfo templateInfo) {
                        if (templateInfo != null && templateInfo.getLatestVersion() != null)
                            templateVersion = templateInfo.getLatestVersion().getId();
                        Crashlytics.log(Log.INFO, "RuleHandler", "Template version " + templateVersion);
                        return fetchTemplateInstallation();
                    }
                });
    }

    private static Observable<RuleBuilder> fetchTemplateInstallation() {
        if (!Strings.isNullOrEmpty(DataStorage.getUserId()) &&
                !Strings.isNullOrEmpty(Storage.instance().loadRule()))
            return RelayrSdk.getRuleTemplateApi()
                    .getAppliedTemplate(DataStorage.getUserId(), Storage.instance().loadRule())
                    .timeout(7, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap(new Func1<AppliedTemplate, Observable<RuleBuilder>>() {
                        @Override
                        public Observable<RuleBuilder> call(AppliedTemplate appliedTemplate) {
                            sAppliedTemplate = appliedTemplate;
                            final Gson gson = new Gson();
                            final String toJson = gson.toJson(appliedTemplate.getParameters());
                            sRule = gson.fromJson(toJson, RuleBuilder.class);
                            sRule.initialize();

                            return Observable.just(sRule);
                        }
                    });
        return Observable.just(sRule);
    }

    public static void setCondition(int position, Constants.DeviceType type, String meaning, String operation, int value) {
        if (sRule == null) sRule = new RuleBuilder();
        sRule.setCondition(position, Storage.instance().getDeviceId(type), meaning, operation, value);
        if (sRule.hasOutcome()) saveRule();
    }

    public static void removeCondition(int position) {
        sRule.removeCondition(position);
        if (sRule.hasOutcome()) saveRule();
    }

    public static void setConditionOperator(String operator) {
        if (sRule != null) sRule.setConditionOperator(operator);
    }

    public static void setOutcome(int position, Constants.DeviceType type, String name, boolean value) {
        if (sRule == null) sRule = new RuleBuilder();
        sRule.setCommand(position, Storage.instance().getDeviceId(type), name, value);
        if (sRule.hasCondition()) saveRule();
    }

    public static void removeOutcome(int position) {
        sRule.removeOutcome(position);
        if (sRule.hasOutcome() && sRule.hasCondition()) saveRule();
    }

    private static void saveRule() {
        final RuleBuilder validatedRule = sRule.build();
        if (validatedRule == null) return;

        final TemplateParameters params = new TemplateParameters("IoTSmartphone", null, validatedRule);

        if (sAppliedTemplate == null)
            RelayrSdk.getRuleTemplateApi()
                    .applyTemplate(params, PROJECT_ID, TEMPLATE_ID, templateVersion)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SimpleObserver<AppliedTemplate>() {
                        @Override public void error(Throwable e) {
                            Log.e("FRules", "Template failed");
                            e.printStackTrace();
                            if (mObserver != null) mObserver.onNext(false);
                        }

                        @Override
                        public void success(AppliedTemplate appliedTemplate) {
                            sAppliedTemplate = appliedTemplate;
                            Storage.instance().saveRule(appliedTemplate.getId());
                            Log.i("FRules - APPLIED", appliedTemplate.toString());
                            if (mObserver != null) mObserver.onNext(true);
                        }
                    });
        else
            RelayrSdk.getRuleTemplateApi()
                    .updateAppliedTemplate(params, DataStorage.getUserId(), sAppliedTemplate.getId())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SimpleObserver<AppliedTemplate>() {
                        @Override public void error(Throwable e) {
                            Log.e("FRules", "Template update failed");
                            e.printStackTrace();
                            if (mObserver != null) mObserver.onNext(false);
                        }

                        @Override
                        public void success(AppliedTemplate appliedTemplate) {
                            sAppliedTemplate = appliedTemplate;
                            Log.i("FRules - UPDATED", appliedTemplate.toString());
                            if (mObserver != null) mObserver.onNext(true);
                        }
                    });
    }

    public static void clearAfterLogOut() {
        sRule = null;
        sAppliedTemplate = null;
    }

    public static boolean hasRule() {
        return sRule != null;
    }
}
