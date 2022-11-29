package com.simplemobiletools.dialer.missedCalls;

import android.Manifest;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class CallLogNotificationsService extends IntentService {

     static final String ACTION_CANCEL_ALL_MISSED_CALLS =
        "com.android.dialer.calllog.ACTION_CANCEL_ALL_MISSED_CALLS";

     static final String ACTION_CANCEL_SINGLE_MISSED_CALL =
        "com.android.dialer.calllog.ACTION_CANCEL_SINGLE_MISSED_CALL";

    public CallLogNotificationsService() {
        super("calllognotificationservice");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }

      if((ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED
          && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED))
      {
          return;
      }

      String action = intent.getAction();
      switch (action) {
          case ACTION_CANCEL_ALL_MISSED_CALLS:
              cancelAllMissedCalls(this);
              break;
          case ACTION_CANCEL_SINGLE_MISSED_CALL:
              Uri callUri = intent.getData();
              callLogQueryHelper.markSingleMissedCallInCallLogAsRead(this, callUri);
              missedCallNotifier.cancelSingle(this, callUri);
              telecomManagerUtil.cancelMissedCallsNotifications(this);
              break;
          default:
              break;
      }
    }

    public static void cancelAllMissedCalls(Context context) {

                callLogQueryHelper.markAllMissedCallsInLogsAsRead(context);
                missedCallNotifier.cancelAllinGroup(context, "MissedCallGroup");
                telecomManagerUtil.cancelMissedCallsNotifications(context);
                NotificationReceiver.updateBadgeCount(context, 0);

    }

   public void cancelAllMissedCallss(Context context) {
        backgroundTaskExecutor executor = new backgroundTaskExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                callLogQueryHelper.markAllMissedCallsInLogsAsRead(context);
                missedCallNotifier.cancelAllinGroup(context, "MissedCallGroup");
                telecomManagerUtil.cancelMissedCallsNotifications(context);
            }
        });
   }

    public static PendingIntent createCancelAllMissedCallsPendingIntent(@NonNull Context context) {
        Intent intent = new Intent(context, CallLogNotificationsService.class);
        intent.setAction(ACTION_CANCEL_ALL_MISSED_CALLS);
        return PendingIntent.getService(context, 0, intent, 0);
    }

    public static PendingIntent createCancelSingleMissedCallPendingIntent(
        @NonNull Context context, @Nullable Uri callUri) {
        Intent intent = new Intent(context, CallLogNotificationsService.class);
        intent.setAction(ACTION_CANCEL_SINGLE_MISSED_CALL);
        intent.setData(callUri);
        return PendingIntent.getService(context, 0, intent, 0);
    }

}
