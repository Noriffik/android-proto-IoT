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
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class RuleHandler {

    private static final String PROJECT_ID = "95758d69-2570-4add-a3a4-90a7414659db";
    private static final String TEMPLATE_ID = "6f214bdd-5f6c-4451-b9e3-2a489c9e472b";
    private static final String TEMPLATE_VERSION = "ea963600-15fd-46ff-87e0-436150da9ded";

    private static RuleBuilder sRule;
    private static AppliedTemplate sAppliedTemplate;
    private static String templateVersion;

    private static SimpleObserver<Boolean> mObserver;
    private static boolean sActive;

    public static void init(SimpleObserver<Boolean> observer) {
        mObserver = observer;
    }

    public static Observable<RuleBuilder> loadRule() {
        if (sRule != null) return Observable.just(sRule);

        if (templateVersion == null) return getLatestTemplate();
        else return fetchTemplateInstallation();
    }

    private static Observable<RuleBuilder> getLatestTemplate() {
        return RelayrSdk.getRuleTemplateApi()
                .getTemplate(PROJECT_ID, TEMPLATE_ID)
                .timeout(7, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .flatMap(new Func1<TemplateInfo, Observable<RuleBuilder>>() {
                    @Override public Observable<RuleBuilder> call(TemplateInfo templateInfo) {
                        if (templateInfo != null && templateInfo.getLatestVersion() != null)
                            templateVersion = templateInfo.getLatestVersion().getId();
                        else templateVersion = TEMPLATE_VERSION;
                        Crashlytics.log(Log.INFO, "RuleHandler", "Template version " + templateVersion);
                        return fetchTemplateInstallation();
                    }
                });
    }

    public static void setCondition(int position, Constants.DeviceType type, String meaning, String operation, int value) {
        if (sRule == null) sRule = new RuleBuilder();
        sRule.setCondition(position, Storage.instance().getDeviceId(type), meaning, operation, value);
        if (isValid()) sActive = true;
        update();
    }

    public static void removeCondition(int position) {
        sRule.removeCondition(position);
        update();
    }

    public static void setConditionOperator(String operator) {
        sRule.setConditionOperator(operator);
        update();
    }

    public static void setOutcome(int position, Constants.DeviceType type, String name, boolean value) {
        if (sRule == null) sRule = new RuleBuilder();
        sRule.setCommand(position, Storage.instance().getDeviceId(type), name, value);
        if (isValid()) sActive = true;
        update();
    }

    public static void removeOutcome(int position) {
        sRule.removeOutcome(position);
        update();
    }

    private static void update() {
        if (isValid()) saveRule();
        else setActivity(false);
    }

    public static boolean isValid() {return sRule != null && sRule.hasOutcome() && sRule.hasCondition();}

    public static void clearAfterLogOut() {
        sRule = null;
        mObserver = null;
        sAppliedTemplate = null;
    }

    public static boolean hasRule() {
        return sRule != null;
    }

    public static void setActivity(boolean active) {
        sActive = active;
        final boolean saved = saveRule();
        if (!saved && sAppliedTemplate != null)
            RelayrSdk.getRuleTemplateApi()
                    .updateAppliedTemplate(TemplateParameters.template(sActive, templateVersion),
                            DataStorage.getUserId(), sAppliedTemplate.getId())
                    .subscribeOn(Schedulers.io())
                    .subscribe(new SimpleObserver<AppliedTemplate>() {
                        @Override public void error(Throwable e) {
                            Crashlytics.log(Log.ERROR, "RuleHandler", "Template activity - error");
                            Crashlytics.logException(e);
                            if (mObserver != null) mObserver.onNext(false);
                        }

                        @Override
                        public void success(AppliedTemplate appliedTemplate) {
                            Crashlytics.log(Log.INFO, "RuleHandler", "Template activity - " + appliedTemplate.isActive());

                            sAppliedTemplate = appliedTemplate;
                            sActive = appliedTemplate.isActive();

                            if (mObserver != null) mObserver.onNext(true);
                        }
                    });
    }

    public static boolean isActive() {
        return sActive;
    }

    private static Observable<RuleBuilder> fetchTemplateInstallation() {
        if (!Strings.isNullOrEmpty(DataStorage.getUserId()) &&
                !Strings.isNullOrEmpty(Storage.instance().loadRule()))
            return RelayrSdk.getRuleTemplateApi()
                    .getAppliedTemplate(DataStorage.getUserId(), Storage.instance().loadRule())
                    .timeout(7, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())
                    .flatMap(new Func1<AppliedTemplate, Observable<RuleBuilder>>() {
                        @Override
                        public Observable<RuleBuilder> call(AppliedTemplate appliedTemplate) {
                            if (appliedTemplate == null) return Observable.just(null);
                            sAppliedTemplate = appliedTemplate;
                            if (!appliedTemplate.getTemplateVersionId().equals(templateVersion))
                                return updateTemplateVersion();
                            else return extractRule();
                        }
                    });
        return Observable.just(sRule);
    }

    private static Observable<RuleBuilder> updateTemplateVersion() {
        return RelayrSdk.getRuleTemplateApi()
                .updateAppliedTemplate(TemplateParameters.template(sActive, templateVersion),
                        DataStorage.getUserId(), sAppliedTemplate.getId())
                .timeout(7, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .flatMap(new Func1<AppliedTemplate, Observable<RuleBuilder>>() {
                    @Override public Observable<RuleBuilder> call(AppliedTemplate appliedTemplate) {
                        Crashlytics.log(Log.INFO, "RuleHandler", "Template version updated.");
                        sAppliedTemplate = appliedTemplate;
                        return extractRule();
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override public void call(Throwable e) {
                        Crashlytics.log(Log.ERROR, "RuleHandler", "Template version update failed.");
                        Crashlytics.logException(e);
                    }
                });
    }

    private static Observable<RuleBuilder> extractRule() {
        sActive = sAppliedTemplate.isActive();

        final Gson gson = new Gson();
        final String toJson = gson.toJson(sAppliedTemplate.getParameters());
        sRule = gson.fromJson(toJson, RuleBuilder.class);
        sRule.initialize();

        return Observable.just(sRule);
    }

    private static boolean saveRule() {
        if (sRule == null) return false;
        final RuleBuilder validatedRule = sRule.build();
        if (validatedRule == null) return false;

        final TemplateParameters params = new TemplateParameters("IoTSmartPhone", sActive, templateVersion, validatedRule);

        if (sAppliedTemplate == null)
            RelayrSdk.getRuleTemplateApi()
                    .applyTemplate(params, PROJECT_ID, TEMPLATE_ID, templateVersion)
                    .subscribeOn(Schedulers.io())
                    .subscribe(new SimpleObserver<AppliedTemplate>() {
                        @Override public void error(Throwable e) {
                            Crashlytics.log(Log.ERROR, "RuleHandler", "Template create - error");
                            Crashlytics.logException(e);
                            if (mObserver != null) mObserver.onNext(false);
                        }

                        @Override
                        public void success(AppliedTemplate appliedTemplate) {
                            Crashlytics.log(Log.INFO, "RuleHandler", "Template created");

                            sAppliedTemplate = appliedTemplate;
                            sActive = appliedTemplate.isActive();
                            Storage.instance().saveRule(appliedTemplate.getId());

                            if (mObserver != null) mObserver.onNext(true);
                        }
                    });
        else
            RelayrSdk.getRuleTemplateApi()
                    .updateAppliedTemplate(params, DataStorage.getUserId(), sAppliedTemplate.getId())
                    .subscribeOn(Schedulers.io())
                    .subscribe(new SimpleObserver<AppliedTemplate>() {
                        @Override public void error(Throwable e) {
                            Crashlytics.log(Log.ERROR, "RuleHandler", "Template update - error");
                            Crashlytics.logException(e);
                            if (mObserver != null) mObserver.onNext(false);
                        }

                        @Override
                        public void success(AppliedTemplate appliedTemplate) {
                            Crashlytics.log(Log.INFO, "RuleHandler", "Template updated");

                            sAppliedTemplate = appliedTemplate;
                            sActive = appliedTemplate.isActive();

                            if (mObserver != null) mObserver.onNext(true);
                        }
                    });
        return true;
    }
}
