package com.naji_astier.lecrousclub;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        // For our recurring task, we'll just display a message
        Toast.makeText(context, "Alarm fired", Toast.LENGTH_SHORT).show();
        Intent myIntent = new Intent(context, MainActivity.class);
        myIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, myIntent, 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_stat_action_account_child)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setLights(0xFF0000FF, 500, 500)
                        .setContentTitle("Vas-tu manger au Crous ce midi?")
                //.addAction(R.drawable.ic_done_white_24dp, "Oui", pendingIntent)
                //.addAction(R.drawable.ic_clear_white_24dp, "Non", pendingIntent)
                ;

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, mBuilder.build());
    }
}
