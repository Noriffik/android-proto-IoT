package io.relayr.iotsmartphone.ui.utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.helper.DemandIntentReceiver;
import io.relayr.iotsmartphone.storage.Constants;
import io.relayr.iotsmartphone.ui.ActivityMain;

public class NotificationsUtil {

    public static void hideNotification(Context context, int id) {
        final NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
        managerCompat.cancel(id);
    }

    public static void showNotification(Context context, int extra) {
        switch (extra) {
            case Constants.NOTIF_VIB:
                showNotification(context, R.string.notif_vib, R.string.notif_vib_off, Constants.NOTIF_VIB);
                break;
            case Constants.NOTIF_SOUND:
                showNotification(context, R.string.notif_music, R.string.notif_music_off, Constants.NOTIF_SOUND);
                break;
            case Constants.NOTIF_FLASH:
                showNotification(context, R.string.notif_flash, R.string.notif_flash_off, Constants.NOTIF_FLASH);
                break;
        }
    }

    public static void showNotification(Context context, int titleId, int textId, int notificationId) {
        Intent demandIntent = new Intent(context, DemandIntentReceiver.class)
                .putExtra(DemandIntentReceiver.EXTRA_MESSAGE, notificationId)
                .setAction(DemandIntentReceiver.ACTION_DEMAND);

        PendingIntent demandPendingIntent = PendingIntent.getBroadcast(context, 0, demandIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.mipmap.logo,
                context.getString(textId), demandPendingIntent).build();

        showNotification(context, action, titleId, textId, notificationId);
    }

    public static void showNotification(Context context, NotificationCompat.Action action, int titleId, int textId, int notificationId) {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.notification)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentTitle(context.getString(titleId))
                .setContentText(context.getString(textId));

        if (UiUtil.isWearableConnected((ActivityMain) context)) {
            Bitmap bg = BitmapFactory.decodeResource(context.getResources(), R.color.primary);
            builder.extend(new NotificationCompat.WearableExtender().addAction(action).setBackground(bg));
        }
        builder.addAction(action);

        final NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
        managerCompat.notify(notificationId, builder.build());
    }
}
