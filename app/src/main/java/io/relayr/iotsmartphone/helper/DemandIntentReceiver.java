package io.relayr.iotsmartphone.helper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import io.relayr.iotsmartphone.storage.Constants;

public class DemandIntentReceiver extends BroadcastReceiver {

    public static final String ACTION_DEMAND = "io.relayr.iots.ACTION_DEMAND";
    public static final String EXTRA_MESSAGE = "io.relayr.iots.EXTRA_MESSAGE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_DEMAND)) {
            try {
                final int notificationId = intent.getIntExtra(EXTRA_MESSAGE, Constants.NOTIF_FLASH);
                Intent messageIntent = new Intent(Intent.ACTION_SEND);
                messageIntent.putExtra(EXTRA_MESSAGE, notificationId);
                LocalBroadcastManager.getInstance(context).sendBroadcast(messageIntent);

                final NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
                managerCompat.cancel(notificationId);
            } catch (Exception e) {
                Log.d("DemandIntentReceiver", "Failed to get extras");
            }
        }
    }
}
