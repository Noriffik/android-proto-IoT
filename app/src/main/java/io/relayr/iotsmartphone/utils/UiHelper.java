package io.relayr.iotsmartphone.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

import io.relayr.android.RelayrSdk;
import io.relayr.iotsmartphone.R;

public class UiHelper {

    private static final String WEAR_APP = "com.google.android.wearable.app";
    private static final Map<String, String> sNameMap = new HashMap<>();

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


        //        try {
        //            Uri storeUri = Uri.parse("market://details?id=" + packageName);
        //            startStoreActivity(context, storeUri);
        //        } catch (ActivityNotFoundException anfe) {
        //            Uri webUri = Uri.parse("http://play.google.com/store/apps/details?id=" + packageName);
        //            startStoreActivity(context, webUri);
        //        }
    }

    private static void startStoreActivity(Context context, Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
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
}
