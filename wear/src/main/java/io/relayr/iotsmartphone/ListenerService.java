package io.relayr.iotsmartphone;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.WearableListenerService;

public class ListenerService extends WearableListenerService {

    private static final String TAG = "ListenerService";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            String path = event.getDataItem().getUri().getPath();
            if (Constants.ACTIVATE_PATH.equals(path)) {
                Intent startIntent = new Intent(this, MainActivity.class);
                startIntent.putExtra(Constants.ACTIVATE, true);
                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startIntent);
            }
        }
    }
    //
    //    private void sendLocalNotification(DataMap dataMap) {
    //        Intent startIntent = new Intent(this, MainActivity.class).setAction(Intent.ACTION_MAIN);
    //        startIntent.putExtra("extra", dataMap.getString("extra"));
    //        PendingIntent intent = PendingIntent.getActivity(this, 0, startIntent, FLAG_CANCEL_CURRENT);
    //
    //        Notification notify = new NotificationCompat.Builder(this)
    //                .setContentTitle(getString(R.string.app_name))
    //                .setContentText("Launch app to send data")
    //                .setSmallIcon(R.mipmap.logo)
    //                .setAutoCancel(true)
    //                .setContentIntent(intent)
    //                .build();
    //
    //        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
    //        notificationManager.notify(2376, notify);
    //    }
}
