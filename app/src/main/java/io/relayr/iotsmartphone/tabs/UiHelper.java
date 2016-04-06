package io.relayr.iotsmartphone.tabs;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.design.widget.Snackbar;
import android.view.View;

import io.relayr.iotsmartphone.R;

import static android.support.design.widget.Snackbar.LENGTH_LONG;

public class UiHelper {

    public static boolean isWearableConnected(Activity activity) {
        try {
            activity.getPackageManager().getPackageInfo("com.google.android.wearable.app", PackageManager.GET_META_DATA);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            final View view = activity.findViewById(android.R.id.content);
            if (view == null) return false;
            Snackbar.make(view, activity.getString(R.string.srv_no_wearable), LENGTH_LONG).show();
            return false;
        }
    }
}
