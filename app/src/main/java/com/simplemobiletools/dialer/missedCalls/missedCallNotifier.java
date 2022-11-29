package com.simplemobiletools.dialer.missedCalls;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.service.notification.StatusBarNotification;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.BuildCompat;
import androidx.core.os.UserManagerCompat;

import com.simplemobiletools.dialer.R;
import com.simplemobiletools.dialer.activities.MainActivity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class missedCallNotifier {

    Context context;
    callLogQueryHelper QueryHelper;
    private static final Set<StatusBarNotification> throttledNotificationSet = new HashSet<>();

    missedCallNotifier(Context context, callLogQueryHelper helper) {
        this.context = context;
        this.QueryHelper = helper;
    }

    public static missedCallNotifier getInstance(Context context) {
       return new missedCallNotifier(context, com.simplemobiletools.dialer.missedCalls.callLogQueryHelper.getInstance(context));
    }

    public static void cancelAllinGroup (Context context, String groupKey) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        for (StatusBarNotification notification : notificationManager.getActiveNotifications()) {
            if (groupKey.equals(notification.getNotification().getGroup())){
                notificationManager.cancel(notification.getTag(), notification.getId());
            }
        }
    }

    public static void cancelSingle(@NonNull Context context, @Nullable Uri callUri) {
        if (callUri == null) {
            return;
        }
        // This will also dismiss the group summary if there are no more missed call notifications.
        cancelEntry(
            context,
            getNotificationTagForCallUri(callUri),
            1);
    }

    public static void cancelEntry(Context context, String tag, int id) {

            Assert.isNotNull(context);
          /*  Assert.checkArgument(!tag.isEmpty()); */
             Log.d("inytechecking", tag);
            NotificationManager notificationManager = getNotificationManager(context);
            StatusBarNotification[] notifications = notificationManager.getActiveNotifications();

            String groupKey = findGroupKey(notifications, tag, id);
            Log.d("inytechecking", groupKey);
            if (!groupKey.isEmpty()) {
                Pair<StatusBarNotification, Integer> groupSummaryAndCount =
                    getGroupSummaryAndCount(notifications, groupKey);
                if (groupSummaryAndCount.first != null && groupSummaryAndCount.second <= 1) {

                    notificationManager.cancel(
                        groupSummaryAndCount.first.getTag(), groupSummaryAndCount.first.getId());
                }
            }

            notificationManager.cancel(tag, id);
        }


    private static String findGroupKey(
        @NonNull StatusBarNotification[] notifications, @NonNull String tag, int id) {
        for (StatusBarNotification notification : notifications) {
            if (tag.equals(notification.getTag()) && id == notification.getId()) {
                return notification.getNotification().getGroup();
            }
        }
        return null;
    }

    private static Pair<StatusBarNotification, Integer> getGroupSummaryAndCount(
        @NonNull StatusBarNotification[] notifications, @NonNull String groupKey) {
        StatusBarNotification groupSummaryNotification = null;
        int groupCount = 0;
        for (StatusBarNotification notification : notifications) {
            if (groupKey.equals(notification.getNotification().getGroup())) {
                if ((notification.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) != 0) {
                    groupSummaryNotification = notification;
                } else {
                    groupCount++;
                }
            }
        }
        return new Pair<>(groupSummaryNotification, groupCount);
    }

    public  void updateMissedCallNotifications(int count, String number) {
        boolean isCompleted = false;
        final int titleResId;
        CharSequence expandedText=null;
        List<com.simplemobiletools.dialer.missedCalls.callLogQueryHelper.NewCall> newCalls = QueryHelper.getNewMissedCalls();
        /* TODO add stuff about invalidating the missed call counts here */

        if(newCalls!=null) {
            if (count != -1 && count != newCalls.size()) {
                Log.d("inYte", "countmismatchmissedcallnotifier");
                count = newCalls.size();
            }
        }

        if((newCalls != null && newCalls.isEmpty()) || count == 0){
            callLogQueryHelper.markAllMissedCallsInLogsAsRead(context);
            cancelAllinGroup(context, "MissedCallGroup");
            return;
        }
            if(count == -1) {

                return;
            }

        Notification.Builder groupSummary = createNotificationBuilder();
        boolean useCallList = newCalls!=null;

        if(count == 1) {
            callLogQueryHelper.NewCall newCall =
                useCallList
                ? newCalls.get(0)
                    :new callLogQueryHelper.NewCall(
                        null,
                                number,
                                System.currentTimeMillis()
                );

            contactInfoHelper contactInfoHelper = new contactInfoHelper(context);
            contactInfo contactInfo = contactInfoHelper.getContactInfo(newCall.number);
            if(contactInfo.name!=null && contactInfo.name.equals(contactInfo.number)) {
                expandedText = contactInfo.number;
            }else {
                if(contactInfo.name!=null){
                    expandedText = contactInfo.name;
                }else{
                    expandedText = contactInfo.number;
                }
            }
        }else {
            expandedText = "Missed Call";
        }
        Notification.Builder publicBuilder = createNotificationBuilder();
        publicBuilder
            .setContentTitle("Missed Call")
            .setContentIntent(createCallLogPendingIntent())
            .setDeleteIntent(CallLogNotificationsService.createCancelAllMissedCallsPendingIntent(context));

        groupSummary
            .setContentTitle("Missed Call")
            .setContentText(expandedText)
            .setContentIntent(createCallLogPendingIntent())
            .setDeleteIntent(CallLogNotificationsService.createCancelAllMissedCallsPendingIntent(context))
            .setGroupSummary(useCallList)
            .setOnlyAlertOnce(useCallList)
            .setPublicVersion(publicBuilder.build());
            if(BuildCompat.isAtLeastO()) {
                groupSummary.setChannelId("phone_missed_call");
            }
            Notification notification = groupSummary.build();
            pushNotify(context, "GroupSummary_MissedCall", 1, notification);
             if(useCallList) {
                Set<String> activeAndThrottledTags = new ArraySet<>();
                for(StatusBarNotification activenotifications :
                getActiveNotifications(context)) {
                    activeAndThrottledTags.add(activenotifications.getTag());
                }

                for(StatusBarNotification throttledNotifications :
                getThrottledNotificationSet()) {
                    activeAndThrottledTags.add(throttledNotifications.getTag());
                }

                for(callLogQueryHelper.NewCall call : newCalls) {
                    String callTag = getNotificationTagForCall(call);
                    if (!activeAndThrottledTags.contains(callTag)) {
                        pushNotify(context, callTag, 1, getNotificationForCall(call, null));
                    }
                }
            }
        isCompleted = true;
        return;
    }

    private Notification getNotificationForCall(
        @NonNull callLogQueryHelper.NewCall call, @Nullable String postCallMessage) {
        contactInfoHelper contactInfoHelper = new contactInfoHelper(context);
        contactInfo contactInfo = contactInfoHelper.getContactInfo(call.number);


        // Create a public viewable version of the notification, suitable for display when sensitive
        // notification content is hidden.

        Notification.Builder publicBuilder =
            createNotificationBuilder(call).setContentTitle("");

        Notification.Builder builder = createNotificationBuilder(call);
        CharSequence expandedText;
        if(contactInfo.name!=null && contactInfo.name.equals(contactInfo.number)) {
            expandedText = contactInfo.number;
        }else {
            if(contactInfo.name!=null) {
                expandedText = contactInfo.name;
            }else{
                expandedText = contactInfo.number;
            }
        }



        // Create the notification suitable for display when sensitive information is showing.
        builder
            .setContentTitle("Missed Call")
            .setContentText(expandedText)
            // Include a public version of the notification to be shown when the missed call
            // notification is shown on the user's lock screen and they have chosen to hide
            // sensitive notification information.
            .setPublicVersion(publicBuilder.build());

        // Add additional actions when the user isn't locked
     /*   if (UserManagerCompat.isUserUnlocked(context)) {
            if (!call.number.isEmpty()
                && !call.number.equals("RESTRICTED")) {
                builder.addAction(
                    new Notification.Action.Builder(
                        Icon.createWithResource(context, R.drawable.m),
                        context.getString(R.string.notification_missedCall_call_back),
                        createCallBackPendingIntent(call.number, call.callsUri))
                        .build());

                if (!PhoneNumberHelper.isUriNumber(call.number)) {
                    builder.addAction(
                        new Notification.Action.Builder(
                            Icon.createWithResource(context, R.drawable.quantum_ic_message_white_24),
                            context.getString(R.string.notification_missedCall_message),
                            createSendSmsFromNotificationPendingIntent(call.number, call.callsUri))
                            .build());
                }
            }
        } */

        Notification notification = builder.build();
        return notification;
    }

    private static String getNotificationTagForCall(@NonNull callLogQueryHelper.NewCall call) {
        return getNotificationTagForCallUri(call.callsUri);
    }


    private static String getNotificationTagForCallUri(@NonNull Uri callUri) {
        return "MissedCall_" + callUri;
    }

    public static StatusBarNotification[] getActiveNotifications(@NonNull Context context) {
        Assert.isNotNull(context);
        return getNotificationManager(context).getActiveNotifications();
    }

    private static NotificationManager getNotificationManager(@NonNull Context context) {
        return context.getSystemService(NotificationManager.class);
    }




    private PendingIntent createCallLogPendingIntent() {
       Intent pendingIntent = new Intent(context, MainActivity.class);
       pendingIntent.setAction(Intent.ACTION_VIEW);
      /*  CallLogNotificationsService.cancelAllMissedCalls(context); */
       return  PendingIntent.getActivity(context, 10, pendingIntent, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    private PendingIntent createCallLogPendingIntent(@Nullable Uri callUri) {
        Intent contentIntent = new Intent(context, MainActivity.class);

        // TODO (a bug): scroll to call
        contentIntent.setData(callUri);
        return PendingIntent.getActivity(context, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    private void pushNotify(Context context, String tag, int id, Notification notification){

        Log.d("inYte", "inPushNotify");
        NotificationChannel channel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel = new NotificationChannel(
                "phone_missed_call",
                "Missed Call Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            );


            getNotificationManager(context).createNotificationChannel(channel);
            Assert.checkArgument(!(notification.getChannelId()).isEmpty());
        }
        getNotificationManager(context).notify(tag, id, notification);

        throttledNotificationSet.addAll(NotificationThrottler.throttle(context, notification));
    }

    private static Set<StatusBarNotification> getThrottledNotificationSet() {
        return throttledNotificationSet;
    }
    private Notification.Builder createNotificationBuilder() {
        return new Notification.Builder(context)
            .setGroup("MissedCallGroup")
            .setSmallIcon(R.drawable.missed_call_icon)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setDefaults(Notification.DEFAULT_VIBRATE);
    }

    private Notification.Builder createNotificationBuilder(callLogQueryHelper.NewCall newCall){
        Notification.Builder builder =
            createNotificationBuilder()
                .setWhen(newCall.dateMs)
                .setDeleteIntent(
                    CallLogNotificationsService.createCancelAllMissedCallsPendingIntent(context))
                .setContentIntent(createCallLogPendingIntent(newCall.callsUri));
        if (BuildCompat.isAtLeastO()) {
            builder.setChannelId("phone_missed_call");
        }

        return builder;

    }
}
