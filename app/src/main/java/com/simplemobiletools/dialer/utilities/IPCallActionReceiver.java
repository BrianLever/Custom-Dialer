package com.simplemobiletools.dialer.utilities;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.firestore.FirebaseFirestore;
import com.simplemobiletools.dialer.helpers.ConstantsKt;
import com.simplemobiletools.dialer.network.ApiClient;
import com.simplemobiletools.dialer.network.ApiService;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IPCallActionReceiver extends BroadcastReceiver {
    private static int IP_CALL_NOTIFICATION_ID = 1990;
    private static int IP_INTERNET_CALL_NOTIFICATION_ID = 1988;
    String signature = "Keo6oAZ+9cseqG7uazWzOVS9jAPNyycxOk1uMHaB/Hs=";
    private String JWT = null;
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent!=null){
            String meetingIdentifier = intent.getStringExtra("meetingIdentifier");
            String remoteNumber = intent.getStringExtra("remoteNumber");
            String meetingType = intent.getStringExtra("meetingType");
            String peerToken = intent.getStringExtra("peerToken");
            Log.d("actionreceive", "onReceive");
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            for(StatusBarNotification notification : notificationManager.getActiveNotifications()){
                if(notification.getId() == IP_CALL_NOTIFICATION_ID){
                    notificationManager.cancel(IP_CALL_NOTIFICATION_ID);
                }
            }

            if(intent.getAction().equalsIgnoreCase(ConstantsKt.ACCEPT_IP_CALL)){

                 acceptCall(meetingIdentifier, remoteNumber, meetingType, peerToken, context);
            }else if(intent.getAction().equalsIgnoreCase(ConstantsKt.CALL_BACK)){
                launchInternetCall(remoteNumber, peerToken, meetingType, context, meetingIdentifier);

            }else{
                declineCall(meetingIdentifier, remoteNumber, meetingType, peerToken, context);
            }
        }
    }



    private void launchInternetCall(String remotePeer, String remoteToken, String meetingType, Context context, String nNumber){
        /* Ideally a new lookup is not required as the missed call is logged only
        when a call is made from a peer app user.
         */
        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(it);
        Intent outgoingCallIntent = new Intent(context, com.simplemobiletools.dialer.Activities.outgoingCallActivity.class);
        outgoingCallIntent.putExtra("Type", meetingType);
        outgoingCallIntent.putExtra("Remote-Token", remoteToken);
        outgoingCallIntent.putExtra("Remote-Number", remotePeer);
        outgoingCallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(outgoingCallIntent);
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        for(StatusBarNotification notification : notificationManager.getActiveNotifications()){
            if(notification.getId() == IP_INTERNET_CALL_NOTIFICATION_ID){
                notificationManager.cancel(nNumber, IP_INTERNET_CALL_NOTIFICATION_ID);
            }
        }
      /*  PreferenceManager preferenceManager = new PreferenceManager(context);
        preferenceManager.removeStringKey(nNumber); */
    }

    private void acceptCall(String meetingIdentifier, String remoteNumber, String meetingType, String peerToken, Context context){
        callResponse(Constants.REMOTE_MSG_INVITE_ACCEPTED, peerToken, context, meetingIdentifier);
    }

    private void declineCall(String meetingIdentifier, String remoteNumber, String meetingType, String peerToken, Context context){
        callResponse(Constants.REMOTE_MSG_INVITE_DELCINE, peerToken, context, meetingIdentifier);
        PreferenceManager preferenceManager = new PreferenceManager(context);
        if(preferenceManager.getBoolean(Constants.KEY_VIDEO_ACTIVE)==true){
            preferenceManager.putBoolean(Constants.KEY_VIDEO_ACTIVE, false);
        }
    }

    private void callResponse(String type, String receiverToken, Context context, String meetingIdentifier) {

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.cancel(IP_CALL_NOTIFICATION_ID);
        try{
            JSONArray tokens = new JSONArray();
            tokens.put(receiverToken);
            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();
            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITE_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITE_RESPONSE, type);
            body.put(Constants.REMOTE_MSG_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);
            body.put("priority", "high");
            body.put("time_to_live", 1);
            sendRemoteMessage(body.toString(), type, meetingIdentifier, context);
        }catch (Exception e){
            Log.d(Constants.TAG, e.getMessage());
        }
    }

    private void sendRemoteMessage (String remoteMessageBody, String type, String meetingIdentifier, Context context){
        ApiClient.getClient().create(ApiService.class).sendRemoteMessage(
            Constants.getRemoteMessageHeaders(), remoteMessageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if(response.isSuccessful()){
                    if(type.equals(Constants.REMOTE_MSG_INVITE_ACCEPTED)){
                        try {
                            JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                                .setAudioMuted(false)
                                .setServerURL(new URL("https://jitsimeet.onecxn.info"))
                                .setRoom(meetingIdentifier)
                                .setToken(generateToken(meetingIdentifier, context))
                                .setFeatureFlag("add-people.enabled", false)
                                .setFeatureFlag("calendar.enabled", false)
                                .setFeatureFlag("call-integration.enabled", true)
                                .setFeatureFlag("conference-time.enabled", false)
                                .setFeatureFlag("invite.enabled", false)
                                .setFeatureFlag("pip.enabled", false)
                                .setFeatureFlag("notifications.enabled", false)
                                .setFeatureFlag("speakerstats.enabled", false)
                                .setFeatureFlag("kick-out.enabled", false)
                                .setFeatureFlag("lobby-mode.enabled", false)
                                .setFeatureFlag("meeting-name.enabled", false)
                                .setFeatureFlag("overflow-menu.enabled", false)
                                .setFeatureFlag("raise-hand.enabled", false)
                                .setFeatureFlag("welcomepage.enabled", false)
                                .setFeatureFlag("replace.participant", true)
                                .build();
                            com.simplemobiletools.dialer.Activities.incomingCallActivity.isCallConnected = true;
                            JitsiMeetActivity.launch(context, options);
                        } catch (Exception e) {
                            Log.d(Constants.TAG, e.getMessage());
                        }
                    }else {

                    }
                }else {
                    Log.d(Constants.TAG, response.message().toString());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.d(Constants.TAG, t.getMessage());
            }
        });
    }

    private String generateToken(String meetingIdentifier, Context contexts) throws UnsupportedEncodingException {
        PreferenceManager preferenceManager = new PreferenceManager(contexts);
        String name, id;
        Map<String, Object> payload = new HashMap<>();
        Map<String, String> user = new HashMap<>();
        Map<String, Object> context = new HashMap<>();
        if(preferenceManager.getString(Constants.KEY_FIRST_NAME)==null){
            name = preferenceManager.getString(Constants.KEY_MOBILE);
            id = preferenceManager.getString(Constants.KEY_MOBILE);
        }else{
            name =  preferenceManager.getString(Constants.KEY_FIRST_NAME);
            id   = preferenceManager.getString(Constants.KEY_FIRST_NAME);
        }
        user.put("avatar", "https://media-exp1.licdn.com/dms/image/C4E03AQE0XUdjM1yBGQ/profile-displayphoto-shrink_200_200/0/1517053167748?e=1652918400&v=beta&t=5HhvYLej__wzd0BKFq5wbPjJ-_m-Qmf4f3Y6btj1Uwg");
        user.put("name", name);
        user.put("id",  id);
        Map<String, String> group = new HashMap<>();
        context.put("user", user);
        context.put("group", "meet.onccxn.info");
        payload.put("context", context);


        JwtBuilder Jwt = Jwts.builder()
            .setHeaderParam("typ", "JWT")
            .setHeaderParam("alg", "HS256")
            .addClaims(payload)
            .setAudience("onecxninfra")
            .setIssuer("onecxninfra")
            .setSubject("jitsimeet.onecxn.info")
            .claim("room", meetingIdentifier)
            .signWith(SignatureAlgorithm.HS256, signature.getBytes("UTF-8"));
        JWT = Jwt.compact();
        return Jwt.compact();
    }


}
