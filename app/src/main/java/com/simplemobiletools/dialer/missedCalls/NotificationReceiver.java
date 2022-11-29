package com.simplemobiletools.dialer.missedCalls;



import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import me.leolin.shortcutbadger.ShortcutBadger;

/**
 * Receives broadcasts that should trigger a refresh of the missed call notification. This includes
 * both an explicit broadcast from Telecom and a reboot.
 */
public class NotificationReceiver extends BroadcastReceiver {



    public static final String ACTION_SHOW_MISSED_CALLS_NOTIFICATION =
        "android.telecom.action.SHOW_MISSED_CALLS_NOTIFICATION";

    public static final String EXTRA_NOTIFICATION_COUNT = "android.telecom.extra.NOTIFICATION_COUNT";

    public static final String EXTRA_NOTIFICATION_PHONE_NUMBER =
        "android.telecom.extra.NOTIFICATION_PHONE_NUMBER";

    @Override
    public void onReceive(Context context, Intent intent) {
        Boolean returnedResult;
        String action = intent.getAction();
        if (!ACTION_SHOW_MISSED_CALLS_NOTIFICATION.equals(action)) {
            return;
        }


        int count =
            intent.getIntExtra(
                EXTRA_NOTIFICATION_COUNT, -1);
        Log.d("inYteonReceive", String.valueOf(count));
        String phoneNumber = intent.getStringExtra(EXTRA_NOTIFICATION_PHONE_NUMBER);
        ExecutorService executor = (ExecutorService) Executors.newFixedThreadPool(1);
        missedCallNotificationTask missedCallNotificationTask = new missedCallNotificationTask(context, count, phoneNumber);
        Future<Boolean> taskResult = null;
        PendingResult pendingResult = goAsync();

        try{
            taskResult=executor.submit(missedCallNotificationTask);
            Log.d("inYTE", "returnedTaskResultWithoutError");
        }catch (Exception e){
            Log.d("inYTE", "Failedtosubmitnotificaioncallable");
        }

        try{
           returnedResult = taskResult.get();
           Log.d("inYTE", returnedResult.toString());
           if(returnedResult){
            updateBadgeCount(context, count);
              pendingResult.finish();
           }else {
               Log.d("inYte", "failed to handle missed call broadcast");
               updateBadgeCount(context, count);
               pendingResult.finish();
           }
        }catch (Exception e){
            Log.d("inYte", "couldn't get TaskResult value");
        }

    }

    public static void updateBadgeCount(Context context, int count) {
        boolean success = ShortcutBadger.applyCount(context, count);
        Log.d("inYteBadger", String.valueOf(success));
    }
}

