package io.relayr.iotsmartphone.ui.utils;

import android.content.Context;
import android.support.design.widget.TabLayout;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;

import io.relayr.iotsmartphone.IotApplication;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.handler.RuleHandler;
import io.relayr.iotsmartphone.storage.Constants;
import io.relayr.iotsmartphone.storage.Storage;
import io.relayr.iotsmartphone.ui.ActivityMain;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static io.relayr.iotsmartphone.storage.Storage.TUTORIAL;
import static io.relayr.iotsmartphone.storage.Storage.TUTORIAL_PLAY;
import static io.relayr.iotsmartphone.storage.Storage.TUTORIAL_LOG_IN;
import static io.relayr.iotsmartphone.storage.Storage.TUTORIAL_LOG_TO_PLAY;

public class TutorialUtil {

    private static int vectorControl;
    private static PopupWindow sPopupWindow;
    private static int mWidth;

    public static void showLogIn(Context context, TabLayout anchor, float x, float y, float z) {
        if (UiUtil.isCloudConnected()) return;
        if (!IotApplication.isVisible(Constants.DeviceType.PHONE)) return;
        if (!checkData(context, anchor, Storage.TUTORIAL_LOG_IN)) return;
        final double vector = UiUtil.calculateVector(x, y, z);
        if (vector > 12 && vectorControl++ > 3) {
            vectorControl = 0;
            showTutorial(context, anchor, TUTORIAL_LOG_IN, 1);
        }
    }

    public static void showPlay(Context context, View anchor) {
        if (RuleHandler.hasRule() || !UiUtil.isCloudConnected()) return;
        if (!checkData(context, anchor, Storage.TUTORIAL_PLAY)) return;
        showTutorial(context, anchor, TUTORIAL_PLAY, 2);
    }

    public static void showLogInToPlay(Context context, View anchor) {
        if (UiUtil.isCloudConnected()) return;
        if (!checkData(context, anchor, Storage.TUTORIAL_LOG_TO_PLAY)) return;
        showTutorial(context, anchor, TUTORIAL_LOG_TO_PLAY, 1);
    }

    public static void updateTutorial(String tutorial, boolean finished) {
        if (tutorial == null) return;
        Storage.instance().updateTutorial(tutorial, finished);
        if (tutorial.equals(TUTORIAL)) {
            Storage.instance().updateTutorial(TUTORIAL_LOG_IN, finished);
            Storage.instance().updateTutorial(TUTORIAL_PLAY, finished);
            Storage.instance().updateTutorial(TUTORIAL_LOG_TO_PLAY, finished);
        }
    }

    public static void dismiss() {
        if (sPopupWindow != null) sPopupWindow.dismiss();
        sPopupWindow = null;
        System.gc();
    }

    private static boolean checkData(Context context, View anchor, String step) {
        if (context == null || anchor == null || step == null) return false;
        return !(Storage.instance().tutorialFinished(Storage.TUTORIAL) ||
                Storage.instance().tutorialFinished(step));
    }

    private static void showTutorial(Context context, View anchor, final String tutorial, int tab) {
        dismiss();
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View sPopupView = layoutInflater.inflate(R.layout.popup, null);
        sPopupWindow = new PopupWindow(sPopupView, getScreenWidth(context) / 3, WRAP_CONTENT);

        int offset = tab * (getScreenWidth(context) / 3);
        sPopupWindow.showAsDropDown(anchor, offset, 0);

        final TextView text = (TextView) sPopupView.findViewById(R.id.popup_text);
        text.setText(getText(tutorial));
        final TextView btnDismiss = (TextView) sPopupView.findViewById(R.id.button_ok);
        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (sPopupWindow != null) sPopupWindow.dismiss();
                Storage.instance().updateTutorial(tutorial, true);
            }
        });
    }

    private static int getScreenWidth(Context context) {
        if (mWidth > 0) return mWidth;
        DisplayMetrics displaymetrics = new DisplayMetrics();
        ((ActivityMain) context).getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        mWidth = displaymetrics.widthPixels;
        return mWidth;
    }

    private static int getText(String tutorialId) {
        switch (tutorialId) {
            case TUTORIAL_LOG_IN:
                return R.string.tutorial_main;
            case TUTORIAL_PLAY:
                return R.string.tutorial_cloud;
            case TUTORIAL_LOG_TO_PLAY:
                return R.string.tutorial_rules;
            default:
                return R.string.app_name;
        }
    }
}
