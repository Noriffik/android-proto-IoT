package io.relayr.iotsmartphone.ui.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import java.util.HashMap;
import java.util.Map;

import io.relayr.android.RelayrSdk;
import io.relayr.iotsmartphone.R;
import io.relayr.java.model.AccelGyroscope;

public class UiUtil {

    private static final String WEAR_APP = "com.google.android.wearable.app";
    private static final Map<String, String> sNameMap = new HashMap<>();
    private static final Map<String, String> sUnitMap = new HashMap<>();

    public static boolean isWearableConnected(Activity activity) {
        try {
            activity.getPackageManager().getPackageInfo(WEAR_APP, PackageManager.GET_META_DATA);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isCloudConnected() {
        return RelayrSdk.isUserLoggedIn();
    }

    public static void showSnackBar(Activity activity, int stringId) {
        if (activity == null) return;
        final View view = activity.findViewById(android.R.id.content);
        if (view == null) return;
        Snackbar.make(view, activity.getString(stringId), Snackbar.LENGTH_SHORT).show();
    }

    public static void openDashboard(Context context) {
        String packageName = "io.relayr.wunderbar";
        PackageManager manager = context.getPackageManager();
        Intent startApp = manager.getLaunchIntentForPackage(packageName);
        if (startApp != null) {
            startApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startApp.addCategory(Intent.CATEGORY_LAUNCHER);

            context.startActivity(startApp);
            return;
        }

        String urlString = "https://developer.relayr.io/";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage("com.android.chrome");
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            intent.setPackage(null);
            context.startActivity(intent);
        }
    }

    public static String getNameForMeaning(Context context, String meaning) {
        if (sNameMap.isEmpty()) {
            sNameMap.put("acceleration", context.getString(R.string.reading_title_acceleration));
            sNameMap.put("angularSpeed", context.getString(R.string.reading_title_gyro));
            sNameMap.put("batteryLevel", context.getString(R.string.reading_title_battery));
            sNameMap.put("luminosity", context.getString(R.string.reading_title_light));
            sNameMap.put("location", context.getString(R.string.reading_title_location));
            sNameMap.put("rssi", context.getString(R.string.reading_title_rssi));
            sNameMap.put("touch", context.getString(R.string.reading_title_touch));
        }
        return sNameMap.get(meaning);
    }

    public static String getUnitForMeaning(Context context, String meaning) {
        if (sUnitMap.isEmpty()) {
            sUnitMap.put("acceleration", context.getString(R.string.reading_unit_acceleration));
            sUnitMap.put("angularSpeed", context.getString(R.string.reading_unit_acceleration));
            sUnitMap.put("batteryLevel", context.getString(R.string.reading_unit_battery));
            sUnitMap.put("luminosity", context.getString(R.string.reading_unit_light));
            sUnitMap.put("rssi", context.getString(R.string.reading_unit_rssi));
            sUnitMap.put("touch", context.getString(R.string.reading_unit_empty));
            sUnitMap.put("location", context.getString(R.string.reading_unit_empty));
        }
        return sUnitMap.get(meaning);
    }

    public static void hideKeyboard(Context context, View element) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(element.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
    }

    public static double calculateVector(AccelGyroscope.Acceleration acceleration) {
        return calculateVector(acceleration.x, acceleration.y, acceleration.z);
    }

    public static double calculateVector(AccelGyroscope.AngularSpeed angularSpeed) {
        return calculateVector(angularSpeed.x, angularSpeed.y, angularSpeed.z);
    }

    public static double calculateVector(float a, float b, float c) {
        return Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2) + Math.pow(c, 2));
    }
}
