package io.relayr.iotsmartphone.helper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class DemandIntentReceiver extends BroadcastReceiver {

    public static final String ACTION_DEMAND = "io.relayr.iots.ACTION_DEMAND";
    public static final String EXTRA_MESSAGE = "io.relayr.iots.EXTRA_MESSAGE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_DEMAND)) {
            try {
                boolean message = intent.getBooleanExtra(EXTRA_MESSAGE, false);
                Intent messageIntent = new Intent(Intent.ACTION_SEND);
                messageIntent.putExtra(EXTRA_MESSAGE, message);
                LocalBroadcastManager.getInstance(context).sendBroadcast(messageIntent);
            } catch (Exception e) {
                Log.v("DemandIntentReceiver", "Failed to get extras");
            }
        }
    }
}
