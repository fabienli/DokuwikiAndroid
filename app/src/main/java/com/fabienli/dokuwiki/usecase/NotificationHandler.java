package com.fabienli.dokuwiki.usecase;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.fabienli.dokuwiki.MainActivity;
import com.fabienli.dokuwiki.R;

public class NotificationHandler {
    static String TAG = "NotificationHandler";
    NotificationCompat.Builder _notificationBuilder;
    NotificationManager _notificationManager;
    Context _context;

    public NotificationHandler(Context context){
        _context = context;
    }

    public void createNotification(String contentText) {
        // ensure notificaiton channel exists
        createNotificationChannel(_context);

        _notificationBuilder = new NotificationCompat.Builder(_context, "CHANNEL1")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Dokuwiki Synchro")
                .setContentText(contentText)
                .setContentInfo("sync'ing...")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // when clicking, open main activity
        PendingIntent contentIntent = PendingIntent.getActivity(_context, 0,
                new Intent(_context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        _notificationBuilder.setContentIntent(contentIntent);

        _notificationManager = (NotificationManager) _context.getSystemService(Context.NOTIFICATION_SERVICE);
        _notificationManager.notify(0, _notificationBuilder.build());

    }

    public void updateNotification(String contentText){
        if(_notificationBuilder == null) {
            createNotification(contentText);
        }

        if(_notificationManager == null)
            _notificationManager = (NotificationManager) _context.getSystemService(Context.NOTIFICATION_SERVICE);

        _notificationBuilder.setContentText(contentText);
        _notificationManager.notify(0, _notificationBuilder.build());
    }

    public void removeNotification(){
        _notificationManager.cancel(0);
    }

    public static void createNotificationChannel(Context context) {
        Log.d(TAG, "create channel");
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Dokuwiki Android";
            String description = "Ongoing Dokuwiki synchronisation";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel("CHANNEL1", name, importance);
            channel.setDescription(description);
            channel.enableVibration(false);
            channel.enableLights(false);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "channel created");
        }
    }
}
