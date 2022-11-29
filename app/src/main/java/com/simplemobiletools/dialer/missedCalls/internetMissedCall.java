package com.simplemobiletools.dialer.missedCalls;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.service.notification.StatusBarNotification;

import com.simplemobiletools.dialer.utilities.PreferenceManager;

public class internetMissedCall extends BroadcastReceiver {
    private static int IP_INTERNET_CALL_NOTIFICATION_ID = 1988;
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent!=null){
            String nNumber = intent.getStringExtra("notification");
            if(nNumber!=null){
                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                for(StatusBarNotification notification : notificationManager.getActiveNotifications()){
                    if(notification.getId() == IP_INTERNET_CALL_NOTIFICATION_ID){
                        notificationManager.cancel(nNumber, IP_INTERNET_CALL_NOTIFICATION_ID);
                    }
                }
              /*  PreferenceManager preferenceManager = new PreferenceManager(context);
                preferenceManager.removeStringKey(nNumber); */
            }
        }
    }
}

