package com.simplemobiletools.dialer.Firebase;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.service.notification.StatusBarNotification;

import android.util.Log;
import android.widget.RemoteViews;


import androidx.core.app.NotificationCompat;
import androidx.core.os.UserManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.simplemobiletools.dialer.Activities.outgoingCallActivity;
import com.simplemobiletools.dialer.R;
import com.simplemobiletools.dialer.helpers.CallManager;
import com.simplemobiletools.dialer.helpers.ConstantsKt;
import com.simplemobiletools.dialer.missedCalls.contactInfoHelper;
import com.simplemobiletools.dialer.network.ApiClient;
import com.simplemobiletools.dialer.network.ApiService;
import com.simplemobiletools.dialer.utilities.Constants;
import com.simplemobiletools.dialer.utilities.IPCallActionReceiver;
import com.simplemobiletools.dialer.utilities.PreferenceManager;
import com.simplemobiletools.dialer.utilities.notificationsTrackerDB;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class videoIdentifierMessagingService extends FirebaseMessagingService {

    private static int IP_CALL_NOTIFICATION_ID = 1990;
    private static int IP_INTERNET_CALL_NOTIFICATION_ID = 1988;

    private PreferenceManager preferenceManager = null;
    @Override
    public void onNewToken( @NotNull String token) {
        super.onNewToken(token);
    }

    @Override
    public void onMessageReceived(@NotNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        preferenceManager = new PreferenceManager(getApplicationContext());
        if(remoteMessage!=null) {
            String type = remoteMessage.getData().get(Constants.REMOTE_MSG_TYPE);
            if(type!=null) {
                if(type.equals(Constants.REMOTE_MSG_INVITE)) {
                    Long timeDiff;
                    Date timeNow = new Date();
                    Date remoteTime= null;
                    DateFormat sdf = new SimpleDateFormat("dd/MM/yyyy"+ " "+ " HH:mm:ss");
                    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                    try {
                        remoteTime = sdf.parse(remoteMessage.getData().get(Constants.KEY_TIMESTAMP));
                    }catch (Exception e){
                        Log.d(Constants.TAG, "FailedtoParsedateinvideoidentifier");
                        Log.d(Constants.TAG, e.getMessage());
                        Log.d(Constants.TAG, e.getMessage());
                    }
                    if(remoteTime!=null){
                        timeDiff = ((timeNow.getTime() - remoteTime.getTime())/1000)%60;
                        if(timeDiff>30){
                            return;
                            /* No point sending a cancel/decline here as this is likely a situation wherein */
                            /* Firebase has delayed message delivery */
                        }
                    }
                    android.telecom.Call call = CallManager.Companion.getForegroundCall();
                    if(call!=null){
                        if(!(call.getDetails().getHandle().toString()).contains(remoteMessage.getData().get(Constants.REMOTE_MSG_INITIATOR_NUMBER))){
                            return;
                        }
                    }
                    if(preferenceManager.getBoolean(Constants.KEY_VIDEO_ACTIVE)==true)
                    {
                        declineCall(remoteMessage.getData().get(Constants.REMOTE_MSG_INITIATOR_TOKEN));
                        return;
                    }

                    if(remoteMessage.getData().get(Constants.REMOTE_MSG_MEETING_TYPE).equals("video")) {
                        PowerManager powerManager = (PowerManager)getSystemService(getApplicationContext().POWER_SERVICE);
                        KeyguardManager keyguardManager = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
                        if(UserManagerCompat.isUserUnlocked(getApplicationContext()) && powerManager.isInteractive() && !keyguardManager.isKeyguardLocked()) {
                            setupNotification(remoteMessage.getData().get(Constants.REMOTE_MSG_INITIATOR_NUMBER),
                                remoteMessage.getData().get(Constants.REMOTE_MSG_MEETING_TYPE),
                                remoteMessage.getData().get(Constants.REMOTE_MSG_INITIATOR_TOKEN),
                                remoteMessage.getData().get(Constants.REMOTE_MSG_MEETING_IDENTIFIER));
                            if(preferenceManager.getBoolean(Constants.KEY_VIDEO_ACTIVE)==false){
                                preferenceManager.putBoolean(Constants.KEY_VIDEO_ACTIVE, true);
                            }
                            return;
                        }else{
                            Intent launchIncomingIntent = new Intent(getApplicationContext(),
                                com.simplemobiletools.dialer.Activities.incomingCallActivity.class);
                            launchIncomingIntent.putExtra(Constants.REMOTE_MSG_MEETING_TYPE,
                                remoteMessage.getData().get(Constants.REMOTE_MSG_MEETING_TYPE));
                            launchIncomingIntent.putExtra(Constants.REMOTE_MSG_INITIATOR_NUMBER,
                                remoteMessage.getData().get(Constants.REMOTE_MSG_INITIATOR_NUMBER));
                            launchIncomingIntent.putExtra(Constants.REMOTE_MSG_INITIATOR_TOKEN,
                                remoteMessage.getData().get(Constants.REMOTE_MSG_INITIATOR_TOKEN));
                            launchIncomingIntent.putExtra(Constants.REMOTE_MSG_MEETING_IDENTIFIER,
                                remoteMessage.getData().get(Constants.REMOTE_MSG_MEETING_IDENTIFIER));
                            launchIncomingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(launchIncomingIntent);
                            Log.d("inYTeFCM", "delivered");
                        }
                    }
                }else if(type.equals(Constants.REMOTE_MSG_INVITE_RESPONSE)) {
                    if((remoteMessage.getData().get(Constants.REMOTE_MSG_INVITE_RESPONSE).equalsIgnoreCase(Constants.REMOTE_MSG_INVITE_CANCELLED)) &&
                        (preferenceManager.getBoolean(Constants.KEY_IS_OUTGOING) == false))
                    {
                        NotificationManager notificationManager = getSystemService(NotificationManager.class);
                        for(StatusBarNotification notification : notificationManager.getActiveNotifications()){
                            if(notification.getId() == IP_CALL_NOTIFICATION_ID){
                                notificationManager.cancel(IP_CALL_NOTIFICATION_ID);
                            }
                        }
                        if(preferenceManager.getBoolean(Constants.KEY_VIDEO_ACTIVE)==true){
                            preferenceManager.putBoolean(Constants.KEY_VIDEO_ACTIVE, false);
                        }
                /*       if(preferenceManager.getStringSet(remoteMessage.getData().get(Constants.REMOTE_MSG_MEETING_IDENTIFIER))==null) {
                           Set<String> identifierSet = new HashSet<>();
                           String dummyValue = "dummy_value";
                           identifierSet.add(dummyValue);
                           preferenceManager.putStringSet(remoteMessage.getData().get(Constants.REMOTE_MSG_MEETING_IDENTIFIER), identifierSet);
                           displayInternetNotification(
                               remoteMessage.getData().get(Constants.REMOTE_MSG_INITIATOR_NUMBER)
                               ,remoteMessage.getData().get(Constants.REMOTE_MSG_INITIATOR_TOKEN)
                               ,remoteMessage.getData().get(Constants.REMOTE_MSG_MEETING_TYPE),
                               remoteMessage.getData().get(Constants.REMOTE_MSG_MEETING_IDENTIFIER));

                       } */

                        /* Test DB CODE HERE */
                        com.simplemobiletools.dialer.utilities.notificationsTrackerDB instance_ = new notificationsTrackerDB(getApplicationContext());
                        if(instance_!=null){

                            SQLiteDatabase db = instance_.getReadableDatabase();
                            Cursor cursor = db.query("TRACKER",
                                new String[] {"IDENTIFIER"},
                                "IDENTIFIER = ?",
                                new String[]{remoteMessage.getData().get(Constants.REMOTE_MSG_MEETING_IDENTIFIER)},
                                null, null, null);
                            if(cursor.moveToFirst()) {
                                String query = "DELETE FROM TRACKER WHERE TIME <= date('now', '-1 day')";
                                db.execSQL(query);
                                cursor.close();
                                db.close();
                            }else{
                                /* This represents a new identifier */
                                displayInternetNotification(
                                    remoteMessage.getData().get(Constants.REMOTE_MSG_INITIATOR_NUMBER)
                                    ,remoteMessage.getData().get(Constants.REMOTE_MSG_INITIATOR_TOKEN)
                                    ,remoteMessage.getData().get(Constants.REMOTE_MSG_MEETING_TYPE),
                                    remoteMessage.getData().get(Constants.REMOTE_MSG_MEETING_IDENTIFIER));
                                Date timeNow = new Date();
                                DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                                sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                                ContentValues values = new ContentValues();
                                values.put("IDENTIFIER",remoteMessage.getData().get(Constants.REMOTE_MSG_MEETING_IDENTIFIER));
                                values.put("TIME", sdf.format(timeNow));
                                db.insert("TRACKER", null, values);
                                String query = "DELETE FROM TRACKER WHERE TIME <= date('now', '-1 day')";
                                db.execSQL(query);
                                cursor.close();
                                db.close();
                            }
                        }

                    }
                    Intent callResponseIntent = new Intent(Constants.REMOTE_MSG_INVITE_RESPONSE);
                    callResponseIntent.putExtra(Constants.REMOTE_MSG_INVITE_RESPONSE,
                        remoteMessage.getData().get(Constants.REMOTE_MSG_INVITE_RESPONSE));
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(callResponseIntent);
                }else if(type.equals(Constants.REMOTE_MSG_PROVISIONAL_RESPONSE)) {

                    Intent provisionalIntent = new Intent(Constants.REMOTE_MSG_PROVISIONAL_RESPONSE);
                    provisionalIntent.putExtra(Constants.REMOTE_MSG_PROVISIONAL_RESPONSE,
                        remoteMessage.getData().get(Constants.REMOTE_MSG_PROVISIONAL_RESPONSE));
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(provisionalIntent);

                }else if(type.equals(Constants.REMOTE_MSG_BUSY)){

                    Intent busyIntent = new Intent(Constants.REMOTE_MSG_BUSY);
                    busyIntent.putExtra(Constants.REMOTE_MSG_BUSY,
                        remoteMessage.getData().get(Constants.REMOTE_MSG_BUSY));
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(busyIntent);

                } else if(type.equals(Constants.REMOTE_MSG_RECONNECT)) {
                    if(preferenceManager.getBoolean(Constants.KEY_VIDEO_ACTIVE)==true){
                        if(preferenceManager.getBoolean(Constants.KEY_IS_OUTGOING)==true){
                            if( remoteMessage.getData().get(Constants.REMOTE_MSG_INITIATOR_TOKEN).equals(
                                outgoingCallActivity.peerFCMToken
                            )){
                                Intent reconnectIntent = new Intent(Constants.REMOTE_MSG_RECONNECT);
                                reconnectIntent.putExtra("meetingIdentifier", remoteMessage.getData().get(
                                    Constants.REMOTE_MSG_MEETING_IDENTIFIER
                                ));
                                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(reconnectIntent);
                            }else{
                                declineReconnect(remoteMessage.getData().get(Constants.REMOTE_MSG_INITIATOR_TOKEN));
                                return;
                            }
                        }else{
                            if(remoteMessage.getData().get(Constants.REMOTE_MSG_INITIATOR_TOKEN).equals(
                                com.simplemobiletools.dialer.Activities.incomingCallActivity.peerFCMToken
                            )){
                                Intent reconnectIntent = new Intent(Constants.REMOTE_MSG_RECONNECT);
                                reconnectIntent.putExtra("meetingIdentifier", remoteMessage.getData().get(
                                    Constants.REMOTE_MSG_MEETING_IDENTIFIER
                                ));
                                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(reconnectIntent);
                            }else{
                                declineReconnect(remoteMessage.getData().get(Constants.REMOTE_MSG_INITIATOR_TOKEN));
                                return;
                            }

                        }
                    }else{
                        declineReconnect(remoteMessage.getData().get(Constants.REMOTE_MSG_INITIATOR_TOKEN));
                        return;
                    }
                } else if(type.equals(Constants.REMOTE_MSG_RECONNECT_RESPONSE)){
                    Intent callReconnectIntent = new Intent(Constants.REMOTE_MSG_RECONNECT_RESPONSE);
                    callReconnectIntent.putExtra(Constants.REMOTE_MSG_RECONNECT_RESPONSE,
                        remoteMessage.getData().get(Constants.REMOTE_MSG_RECONNECT_RESPONSE));
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(callReconnectIntent);
                }else if(type.equals(Constants.REMOTE_MSG_END_CALL)){

                    Intent endCall  = new Intent(Constants.REMOTE_MSG_END_CALL);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(endCall);
                }
            }else{
                return;
            }
        }
    }

    private void declineCall(String peerFCMToken) {
        try {
            JSONArray tokens = new JSONArray();
            tokens.put(peerFCMToken);
            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();
            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_BUSY);
            body.put(Constants.REMOTE_MSG_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);
            sendDeclineCall(body.toString(), peerFCMToken);
        }catch (Exception e) {

            Log.d(Constants.TAG, "declineCall");
            Log.d(Constants.TAG, e.getMessage());
        }

    }

    private void sendDeclineCall(String body, String token) {
        ApiClient.getClient().create(ApiService.class).sendRemoteMessage(
            Constants.getRemoteMessageHeaders(), body
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if(response.isSuccessful()) {}
                else{

                    Log.d(Constants.TAG, "sendDeclineCall");
                    Log.d(Constants.TAG, "Failure to send decline call");
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {

                Log.d(Constants.TAG, "onFailure");
                Log.d(Constants.TAG, t.getMessage());
            }
        });
    }

    private void declineReconnect(String peerFCMToken){
        try {
            JSONArray tokens = new JSONArray();
            tokens.put(peerFCMToken);
            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();
            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_RECONNECT_RESPONSE);
            data.put(Constants.REMOTE_MSG_RECONNECT_RESPONSE, Constants.REMOTE_MSG_RECONNECT_DECLINED);
            body.put(Constants.REMOTE_MSG_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);
            sendDeclineCall(body.toString(), peerFCMToken);
        }catch (Exception e) {

            Log.d(Constants.TAG, "declineReconnect");
            Log.d(Constants.TAG, "failure to send decline Reconnect");
        }
    }

    public void setupNotification(String remoteNumber, String meetingType, String peerToken, String meetingIdentifier){
        String contactNameNumber = null;
        com.simplemobiletools.dialer.missedCalls.contactInfoHelper contactInfoHelper = new contactInfoHelper(getApplicationContext());
        String IP_channel_id= "id_incoming_IP_call";
        if(com.simplemobiletools.commons.helpers.ConstantsKt.isOreoPlus()){
            String IP_channel_name = "name_incoming_IP_call";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(IP_channel_id, IP_channel_name, importance);
            Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            notificationChannel.setSound(ringtone, new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
            NotificationManager notificationManager = getApplicationContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        Intent openAppIntent = new Intent(Intent.ACTION_MAIN, null);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_ACTIVITY_NEW_TASK);
        openAppIntent.setClass(getApplicationContext(), com.simplemobiletools.dialer.Activities.incomingCallActivity.class);
        openAppIntent.putExtra(Constants.REMOTE_MSG_MEETING_TYPE, meetingType);
        openAppIntent.putExtra(Constants.REMOTE_MSG_MEETING_IDENTIFIER, meetingIdentifier);
        openAppIntent.putExtra(Constants.REMOTE_MSG_INITIATOR_TOKEN, peerToken);
        openAppIntent.putExtra(Constants.REMOTE_MSG_INITIATOR_NUMBER, remoteNumber);
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, openAppIntent, PendingIntent.FLAG_CANCEL_CURRENT);


        Intent acceptCallIntent = new Intent(getApplicationContext(), IPCallActionReceiver.class);
        acceptCallIntent.putExtra("remoteNumber", remoteNumber);
        acceptCallIntent.putExtra("meetingType", meetingType);
        acceptCallIntent.putExtra("peerToken", peerToken);
        acceptCallIntent.putExtra("meetingIdentifier", meetingIdentifier);
        acceptCallIntent.setAction(ConstantsKt.ACCEPT_IP_CALL);
        PendingIntent acceptCallPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, acceptCallIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent declineCallIntent = new Intent(getApplicationContext(), IPCallActionReceiver.class);
        declineCallIntent.putExtra("remoteNumber", remoteNumber);
        declineCallIntent.putExtra("meetingType", meetingType);
        declineCallIntent.putExtra("peerToken", peerToken);
        declineCallIntent.putExtra("meetingIdentifier", meetingIdentifier);
        declineCallIntent.setAction(ConstantsKt.DECLINE_IP_CALL);
        PendingIntent declineCallPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 1, declineCallIntent, PendingIntent.FLAG_CANCEL_CURRENT);


        com.simplemobiletools.dialer.missedCalls.contactInfo info = contactInfoHelper.getContactInfo(remoteNumber);
        if(info!=null){
            if(info.name!=null && !info.name.isEmpty()){
                contactNameNumber = info.name;
            }else{
                contactNameNumber = info.number;
            }
        }else{
            contactNameNumber = remoteNumber;
        }
        RemoteViews remoteView = new RemoteViews(getPackageName(), R.layout.call_notification);
        if(contactNameNumber!=null){
            remoteView.setTextViewText(R.id.notification_caller_name, contactNameNumber);
        }
        remoteView.setTextViewText(R.id.notification_call_status, "Incoming" + " " + meetingType + " "+ "call");
        remoteView.setOnClickPendingIntent(R.id.notification_decline_call, declineCallPendingIntent);
        remoteView.setOnClickPendingIntent(R.id.notification_accept_call, acceptCallPendingIntent);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), IP_channel_id)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setSmallIcon(R.drawable.ic_phone_vector)
            .setContentIntent(openAppPendingIntent)
            .setFullScreenIntent(openAppPendingIntent, true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCustomContentView(remoteView)
            .setOngoing(true)
            .setAutoCancel(true)
            .setChannelId(IP_channel_id)
            .setStyle(new NotificationCompat.DecoratedCustomViewStyle());

        Notification notification = builder.build();
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.notify(IP_CALL_NOTIFICATION_ID, notification);
        sendProvisionalResponse(peerToken);
    }

    private void sendProvisionalResponse(String peerFCMToken) {
        try{
            JSONArray tokens = new JSONArray();
            tokens.put(peerFCMToken);
            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_PROVISIONAL_RESPONSE);
            body.put(Constants.REMOTE_MSG_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);
            transmitProvisionalResponse(body.toString());
        }catch (Exception e){
            Log.d(Constants.TAG, e.getMessage());
        }
    }


    private void transmitProvisionalResponse(String body){
        ApiClient.getClient().create(ApiService.class).sendRemoteMessage(
            Constants.getRemoteMessageHeaders(), body
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if(response.isSuccessful()) {}
                else{
                    Log.d(Constants.TAG, "failure to send PRACK");
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.d(Constants.TAG, "failure to send PRACK");
            }
        });
    }

    private void displayInternetNotification(String peerNumber, String peerToken, String meetingType, String meetingIdentifier){
        com.simplemobiletools.dialer.missedCalls.contactInfoHelper helper = new contactInfoHelper(getApplicationContext());
        String channel_id = "id_missed_IP_call";
        String contactNameNumber = null;
        if(com.simplemobiletools.commons.helpers.ConstantsKt.isOreoPlus()){
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            String channel_name    = "name_missed_IP_call";
            NotificationChannel notificationChannel = new NotificationChannel(channel_id, channel_name, importance);
            NotificationManager notificationManager = getApplicationContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
        }


        Intent callBackIntent = new Intent(getApplicationContext(), IPCallActionReceiver.class);
        callBackIntent.putExtra("remoteNumber", peerNumber);
        callBackIntent.putExtra("meetingType", meetingType);
        callBackIntent.putExtra("peerToken", peerToken);
        callBackIntent.putExtra("meetingIdentifier", meetingIdentifier);
        callBackIntent.setAction(ConstantsKt.CALL_BACK);
        PendingIntent callBackPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 2, callBackIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent deleteItemIntent = new Intent(getApplicationContext(), com.simplemobiletools.dialer.missedCalls.internetMissedCall.class);
        deleteItemIntent.putExtra("notification", meetingIdentifier);
        deleteItemIntent.setAction(ConstantsKt.DELETE_ITEM);
        PendingIntent deleteItemPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 3, deleteItemIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        com.simplemobiletools.dialer.missedCalls.contactInfo info = helper.getContactInfo(peerNumber);
        if(info!=null){
            if(info.name!=null && !info.name.isEmpty()){
                contactNameNumber = info.name;
            }else{
                contactNameNumber = info.number;
            }
        }else{
            contactNameNumber = peerNumber;
        }
     /*   RemoteViews callBackView = new RemoteViews(getPackageName(), R.layout.internet_call_notification);
        if(contactNameNumber!=null){
            callBackView.setTextViewText(R.id.internet_notification_caller_name, contactNameNumber);
        }
        callBackView.setOnClickPendingIntent(R.id.call_back_button, callBackPendingIntent); */

        NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_callback, "Call back", callBackPendingIntent).build();


        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channel_id)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSmallIcon(R.drawable.missed_call_icon)
            .setAutoCancel(true)
            .setContentTitle("Missed" + " " + meetingType + " " + "call")
            .setContentText(contactNameNumber)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .addAction(action)
            .setWhen(System.currentTimeMillis())
            .setDeleteIntent(deleteItemPendingIntent)
            .setDefaults(Notification.DEFAULT_VIBRATE);

        Notification ip_call_notification = builder.build();
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.notify(meetingIdentifier, IP_INTERNET_CALL_NOTIFICATION_ID, ip_call_notification);
    }


}
