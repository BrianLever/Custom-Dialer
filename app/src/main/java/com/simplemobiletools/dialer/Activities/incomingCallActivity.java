package com.simplemobiletools.dialer.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.util.Size;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.InputStream;
import java.util.UUID;


import com.simplemobiletools.dialer.R;

import com.simplemobiletools.dialer.helpers.CallManager;
import com.simplemobiletools.dialer.missedCalls.contactInfo;
import com.simplemobiletools.dialer.missedCalls.contactInfoHelper;
import com.simplemobiletools.dialer.network.ApiClient;
import com.simplemobiletools.dialer.network.ApiService;
import com.simplemobiletools.dialer.utilities.Constants;
import com.simplemobiletools.dialer.utilities.PreferenceManager;
import com.simplemobiletools.dialer.utilities.networkMonitor;

import org.jitsi.meet.sdk.BroadcastEvent;
import org.jitsi.meet.sdk.BroadcastIntentHelper;

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

public class incomingCallActivity extends AppCompatActivity implements networkMonitor.Observer {

    private ImageView answerCall, declineCall, cancelCall, callerAvatar;
    private TextView textIncomingInvite, reconnecting;
    private ConstraintLayout incoming_call_holder;
    private Bitmap bitmap;
    public static String callType, peerFCMToken, peerNumber, meetingIdentifier;
    private PreferenceManager preferenceManager=null;
    private com.simplemobiletools.dialer.utilities.networkMonitor monitor;
    private MediaPlayer mediaPlayer = null;
    private AssetFileDescriptor fileDescriptor = null;
    private AudioManager audioManager = null;
    public static Boolean isCallConnected =false;
    private String JWT = null;
    Handler handler = new Handler();
    private Boolean terminatedAbnormally = false;
    private int secondCounter = 0;
    private Boolean reconnectLock = false;
    private Boolean reconnectResponseLock = false;
    private com.simplemobiletools.dialer.missedCalls.contactInfo contactInfo;
    private com.simplemobiletools.dialer.missedCalls.contactInfoHelper helper;
    String signature = "Keo6oAZ+9cseqG7uazWzOVS9jAPNyycxOk1uMHaB/Hs=";
    private GestureDetector gestureDetector;
    private LinearLayout linearLayoutCancel;
    private LinearLayout linearLayoutAccept;
    private View currentTouchedView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle("inYte");
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getApplicationContext().getResources().getColor(R.color.md_blue)));
        setContentView(R.layout.activity_incoming_call);
        if(com.simplemobiletools.commons.helpers.ConstantsKt.isOreoMr1Plus()) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }else{
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        incoming_call_holder = findViewById(R.id.incomingCallHolder);
        incoming_call_holder.setVisibility(View.VISIBLE);
        callType=getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_TYPE);
        gestureDetector = new GestureDetector(this, new GestureListener());
        if(callType!=null) {
            peerFCMToken = getIntent().getStringExtra(Constants.REMOTE_MSG_INITIATOR_TOKEN);
            peerNumber = getIntent().getStringExtra(Constants.REMOTE_MSG_INITIATOR_NUMBER);
            meetingIdentifier = getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_IDENTIFIER);
            preferenceManager = new PreferenceManager(getApplicationContext());
            if(preferenceManager.getBoolean(Constants.KEY_VIDEO_ACTIVE)!=true){
                preferenceManager.putBoolean(Constants.KEY_VIDEO_ACTIVE, true);
            }
            textIncomingInvite = findViewById(R.id.textIncomingInvite);
            helper = new contactInfoHelper(this);
             contactInfo = helper.getContactInfo(peerNumber);
            if(contactInfo!=null && contactInfo.name.equals(contactInfo.number)){
                textIncomingInvite.setText(peerNumber + " " + "calling...");
            }else if(contactInfo!=null && !contactInfo.name.equals(contactInfo.number)){
                getClearBitmap();
                textIncomingInvite.setText(contactInfo.name + " " + "calling...");
            }else{
                textIncomingInvite.setText(peerNumber + " " + "calling...");
            }

            callerAvatar = findViewById(R.id.caller_avatar);
            reconnecting = findViewById(R.id.reconnecting);
            monitor = new networkMonitor(this, getApplicationContext());
            answerCall=findViewById(R.id.answerCall);
            declineCall=findViewById(R.id.declineCall);
            cancelCall = findViewById(R.id.cancelCall);
            linearLayoutCancel=findViewById(R.id.incoming_arrow_holder_cancel);
            linearLayoutAccept=findViewById(R.id.incoming_ip_arrow_holder_accept);
            sendProvisionalResponse(peerFCMToken);
            audioManager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0);
            mediaPlayer = MediaPlayer.create(this, Settings.System.DEFAULT_RINGTONE_URI);
            mediaPlayer.start();
  /*          answerCall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(preferenceManager.getBoolean(Constants.KEY_VIDEO_ACTIVE)!=true){
                        preferenceManager.putBoolean(Constants.KEY_VIDEO_ACTIVE, true);
                    }
                    callResponse(Constants.REMOTE_MSG_INVITE_ACCEPTED,
                        peerFCMToken);
                    answerCall.setClickable(false);
                }
            }); */


            answerCall.setOnTouchListener((view, motionEvent) ->{
                currentTouchedView = answerCall;
                answerCall.animate()
                    .translationY(-400f)
                    .setDuration(800)
                    .alpha(0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            answerCall.setAlpha(1f);
                            answerCall.setTranslationY(0f);
                            linearLayoutAccept.setVisibility(View.GONE);
                            super.onAnimationEnd(animation);
                        }
                        @Override
                        public void onAnimationStart(Animator animation) {
                            linearLayoutCancel.setVisibility(View.GONE);
                            linearLayoutAccept.setVisibility(View.VISIBLE);
                        }
                    });
                return gestureDetector.onTouchEvent(motionEvent);
            });
//            declineCall.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    callResponse(Constants.REMOTE_MSG_INVITE_DELCINE,
//                        peerFCMToken);
//                    declineCall.setClickable(false);
//                }
//            });
            declineCall.setOnTouchListener((view, motionEvent) ->{
                currentTouchedView = declineCall;
                declineCall.animate()
                    .translationY(-400f)
                    .setDuration(800)
                    .alpha(0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            declineCall.setAlpha(1f);
                            declineCall.setTranslationY(0f);
                            linearLayoutCancel.setVisibility(View.GONE);
                            super.onAnimationEnd(animation);
                        }
                        @Override
                        public void onAnimationStart(Animator animation) {
                            linearLayoutAccept.setVisibility(View.GONE);
                            linearLayoutCancel.setVisibility(View.VISIBLE);
                        }
                    });
                return gestureDetector.onTouchEvent(motionEvent);
            });
        }

    }
    class GestureListener extends GestureDetector.SimpleOnGestureListener{
        private final int SWIPE_THRESHOLD = 100;
        private final int SWIPE_VELOCITY_THRESHOLD = 100;
        @Override
        public boolean onDown(MotionEvent event){
            return true;
        }
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
        {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {

                        } else {

                        }
                        result = true;
                    }
                } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {

                    } else {
                        if(currentTouchedView == answerCall){
                            if(preferenceManager.getBoolean(Constants.KEY_VIDEO_ACTIVE)!=true){
                                preferenceManager.putBoolean(Constants.KEY_VIDEO_ACTIVE, true);
                            }
                            callResponse(Constants.REMOTE_MSG_INVITE_ACCEPTED,
                                peerFCMToken);
                            answerCall.setClickable(false);
                        }
                        else{
                            callResponse(Constants.REMOTE_MSG_INVITE_DELCINE,
                                peerFCMToken);
                            declineCall.setClickable(false);
                        }
                    }
                    result = true;
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return result;
        }

    }

    private void getClearBitmap() {
        final String[] contactID = {null};
        final Bitmap[] bitmap = {null};
        String number = null;
        CallManager.Companion.getCallContactIP(getApplicationContext(), peerNumber, (callContact) -> {
            if(callContact!=null) {
                String peerContact = callContact.getNumber();
                Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(peerContact));
                String[] projection = new String[] {
                    ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.PhoneLookup._ID
                };
                try{
                    Cursor cursor = getContentResolver().query(
                        uri,
                        projection,
                        null,
                        null,
                        null
                    );
                    if(cursor!=null){
                        while(cursor.moveToNext()){
                            contactID[0] = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID));
                        }
                        cursor.close();
                    }
                    if(contactID!=null){
                        Uri tempURI = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, new Long(contactID[0]));
                        Uri contactURI = Uri.withAppendedPath(tempURI, ContactsContract.Contacts.Photo.DISPLAY_PHOTO);
                        AssetFileDescriptor fd = getContentResolver().openAssetFileDescriptor(contactURI, "r");
                        InputStream inputStream = null;
                        if(fd!=null){
                            inputStream = fd.createInputStream();
                        }
                        if(inputStream!=null){
                            bitmap[0] = BitmapFactory.decodeStream(inputStream);
                        }
                    }
                }catch (Exception e){
                    Log.d(Constants.TAG, "outgoingCallActivity, getBitmap");
                    Log.d(Constants.TAG, e.getMessage());
                }
                if(bitmap[0]!=null){
                    bitmap[0]=getCicularBitmap(bitmap[0]);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callerAvatar.setImageBitmap(bitmap[0]);
                        }
                    });
                }
            }
            return null;
        });
    }



    private void getBitmap(){
        CallManager.Companion.getCallContactIP(getApplicationContext(), peerNumber,  (callContact) -> {
            if(callContact!=null && callContact.getPhotoUri()!=null){
                Uri photoURI = Uri.parse(callContact.getPhotoUri());
                try{
                    if(com.simplemobiletools.commons.helpers.ConstantsKt.isQPlus()){
                        int size = ((int) getResources().getDimension(R.dimen.list_avatar_size));
                        bitmap =  getContentResolver().loadThumbnail(photoURI, new Size(size, size), null);
                    }else{
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoURI);
                    }
                    bitmap = getCicularBitmap(bitmap);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(bitmap!=null){
                                callerAvatar.setImageBitmap(bitmap);
                            }
                        }
                    });
                }catch (Exception e){
                    Log.d(Constants.TAG, "outgoingCallActivity");
                    Log.d(Constants.TAG, e.getMessage());
                }

            }
            return null;
        });
    }


    private Bitmap getCicularBitmap(Bitmap bitmap){
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        Rect rect = new Rect(0,0,bitmap.getWidth(),bitmap.getHeight());
        float radius = bitmap.getWidth()/(float)2;

        paint.isAntiAlias();
        canvas.drawARGB(0,0,0,0);
        canvas.drawCircle(radius, radius, radius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
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
            body.put("priority", "high");
            body.put("time_to_live", 1);
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

    private String generateToken() throws UnsupportedEncodingException {
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
            .setSubject("inytecall1.onecxn.info")
            .claim("room", meetingIdentifier)
            .signWith(SignatureAlgorithm.HS256, signature.getBytes("UTF-8"));
        JWT = Jwt.compact();
      /*  return Jwt.compact(); */
        if(callType.equals("video")){
            return Jwt.compact();
        }else {
            return Jwt.compact() + "#config.startSilent=true&config.startWithAudioMuted=true&config.disableInitialGUM=true";
        }
    }

    private void callResponse(String type, String receiverToken) {
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
            sendRemoteMessage(body.toString(), type);
        }catch (Exception e){
            Log.d(Constants.TAG, e.getMessage());
            if(mediaPlayer!=null) {
                mediaPlayer.stop();
            }
            mediaPlayer = null;
            finish();
        }
    }


    private void sendRemoteMessage (String remoteMessageBody, String type){
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
                                    .setServerURL(new URL("https://inytecall1.onecxn.info"))
                                    .setRoom(meetingIdentifier)
                                    .setToken(generateToken())
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
                                isCallConnected = true;
                                JitsiMeetActivity.launch(getApplicationContext(), options);
                                textIncomingInvite.setVisibility(View.INVISIBLE);
                                incoming_call_holder.setVisibility(View.INVISIBLE);
                                if (mediaPlayer != null) {
                                    mediaPlayer.stop();
                                }
                                mediaPlayer = null;
                            } catch (Exception e) {
                                Log.d(Constants.TAG, e.getMessage());
                                if (mediaPlayer != null) {
                                    mediaPlayer.stop();
                                }
                                mediaPlayer = null;
                                finish();
                            }
                    }else {
                        if(mediaPlayer!=null) {
                            mediaPlayer.stop();
                        }
                        mediaPlayer = null;
                        finish();
                    }
                }else {
                    Log.d(Constants.TAG, response.message().toString());
                    if(mediaPlayer!=null) {
                        mediaPlayer.stop();
                    }
                    mediaPlayer = null;
                    finish();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.d(Constants.TAG, t.getMessage());
                if(mediaPlayer!=null) {
                    mediaPlayer.stop();
                }
                mediaPlayer = null;
                finish();
            }
        });
    }

    private BroadcastReceiver inviteResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(Constants.REMOTE_MSG_INVITE_RESPONSE);
            if(type!=null){
                if(type.equals(Constants.REMOTE_MSG_INVITE_CANCELLED)) {
                    if(mediaPlayer!=null) {
                        mediaPlayer.stop();
                    }
                    mediaPlayer = null;
                    finish();
                }
            }
        }
    };

    private BroadcastReceiver reconnectRequest = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            /* check is already run to ensure the following:

            1. The same peer is sending a reconnect request - token & number
            2. VIDEO_ACTIVE flag is currently set to true
             */
            hangupCall();
            if(intent!=null && intent.hasExtra("meetingIdentifier")){
                meetingIdentifier = intent.getStringExtra("meetingIdentifier");
                handleReconnect();
            }else{

            }
        }
    };

    private void handleReconnect(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (secondCounter < 50){
                    secondCounter++;
                    try{
                        Thread.sleep(50);
                    }catch (Exception e){
                        Log.d(Constants.TAG, "reconnectRequest");
                        Log.d(Constants.TAG, e.getMessage());
                    }handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(secondCounter == 40){
                                if(secondCounter>40) return;
                                Log.d(Constants.TAG, "sendingCallReconnectResponse");
                                callReconnectResponse(Constants.REMOTE_MSG_RECONNECT_ACCEPT, peerFCMToken );
                                return;
                            }
                        }
                    });
                }

            }
        }).start();
    }


    private BroadcastReceiver reconnectResponse = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent!=null && reconnectLock){
                reconnectLock = false;
                String type = intent.getStringExtra(Constants.REMOTE_MSG_RECONNECT_RESPONSE);
                if(type.equals(Constants.REMOTE_MSG_RECONNECT_ACCEPT)){
                    try{
                        JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                            .setAudioMuted(false)
                            .setServerURL(new URL("https://inytecall1.onecxn.info"))
                            .setRoom(meetingIdentifier)
                            .setToken(generateToken())
                            .setFeatureFlag("add-people.enabled", false)
                            .setFeatureFlag("calendar.enabled", false)
                            .setFeatureFlag("call-integration.enabled", true)
                            .setFeatureFlag("conference-time.enabled", false)
                            .setFeatureFlag("invite.enabled", false)
                            .setFeatureFlag("speakerstats.enabled", false)
                            .setFeatureFlag("kick-out.enabled", false)
                            .setFeatureFlag("lobby-mode.enabled", false)
                            .setFeatureFlag("meeting-name.enabled", true)
                            .setFeatureFlag("overflow-menu.enabled", false)
                            .setFeatureFlag("raise-hand.enabled", false)
                            .setFeatureFlag("welcomepage.enabled", false)
                            .setFeatureFlag("replace.participant", true)
                            .build();
                        JitsiMeetActivity.launch(getApplicationContext(), options);
                    }catch (Exception e){
                        Log.d(Constants.TAG, e.getMessage());
                        finish();
                    }
                }else{
                    finish();
                }
            }else{
                finish();
            }
        }
    };

    private void callReconnectResponse(String type, String receiverToken) {
        if(!reconnectResponseLock) {
            reconnectResponseLock = true;
            try {
                JSONArray tokens = new JSONArray();
                tokens.put(receiverToken);

                JSONObject body = new JSONObject();
                JSONObject data = new JSONObject();

                data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_RECONNECT_RESPONSE);
                data.put(Constants.REMOTE_MSG_RECONNECT_RESPONSE, type);

                body.put(Constants.REMOTE_MSG_MSG_DATA, data);
                body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

                sendRemoteReconnectMessageResponse(body.toString(), type);
            } catch (Exception e) {
                Log.d(Constants.TAG, e.getMessage());
                finish();
            }
        }
    }

    private void sendRemoteReconnectMessageResponse(String remoteMessageBody, String type){
        ApiClient.getClient().create(ApiService.class).sendRemoteMessage(
            Constants.getRemoteMessageHeaders(), remoteMessageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if(response.isSuccessful() && type.equals(Constants.REMOTE_MSG_RECONNECT_ACCEPT)){
                    try{
                        JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                            .setAudioMuted(false)
                            .setServerURL(new URL("https://inytecall1.onecxn.info"))
                            .setRoom(meetingIdentifier)
                            .setToken(generateToken())
                            .setFeatureFlag("add-people.enabled", false)
                            .setFeatureFlag("calendar.enabled", false)
                            .setFeatureFlag("call-integration.enabled", true)
                            .setFeatureFlag("conference-time.enabled", false)
                            .setFeatureFlag("invite.enabled", false)
                            .setFeatureFlag("speakerstats.enabled", false)
                            .setFeatureFlag("kick-out.enabled", false)
                            .setFeatureFlag("lobby-mode.enabled", false)
                            .setFeatureFlag("meeting-name.enabled", true)
                            .setFeatureFlag("overflow-menu.enabled", false)
                            .setFeatureFlag("raise-hand.enabled", false)
                            .setFeatureFlag("welcomepage.enabled", false)
                            .setFeatureFlag("replace.participant", true)
                            .build();
                        JitsiMeetActivity.launch(getApplicationContext(), options);
                        isCallConnected = true;
                        terminatedAbnormally = false;
                        reconnectResponseLock = false;
                    }catch (Exception e){
                        Log.d(Constants.TAG, e.getMessage());
                        finish();
                    }
                }else {
                    Log.d(Constants.TAG, response.message().toString());
                    finish();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.d(Constants.TAG, t.getMessage());
                finish();
            }
        });
    }

    private BroadcastReceiver participantJoined = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };

    private BroadcastReceiver participantLeft = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            hangupCall();
            isCallConnected = false;
            terminatedAbnormally = false;
            preferenceManager.putBoolean(Constants.KEY_VIDEO_ACTIVE, false);
            preferenceManager.putBoolean(Constants.KEY_IS_OUTGOING, false);
            finish();
        }
    };

    private void hangupCall() {
        Intent intent = BroadcastIntentHelper.buildHangUpIntent();
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
            inviteResponseReceiver,
            new IntentFilter(Constants.REMOTE_MSG_INVITE_RESPONSE)
        );


        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
            terminated,
            new IntentFilter(BroadcastEvent.Type.CONFERENCE_TERMINATED.getAction())
        );

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
            participantJoined,
            new IntentFilter(BroadcastEvent.Type.PARTICIPANT_JOINED.getAction())
        );

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
            participantLeft,
            new IntentFilter(BroadcastEvent.Type.PARTICIPANT_LEFT.getAction())
        );

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
            reconnectResponse,
            new IntentFilter(Constants.REMOTE_MSG_RECONNECT_RESPONSE)
        );

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
            reconnectRequest,
            new IntentFilter(Constants.REMOTE_MSG_RECONNECT)
        );

    }

    private BroadcastReceiver terminated = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) { /*
                if(intent.hasExtra("error")==true){
                    String error = intent.getStringExtra("error");
                    if(error.contains("connection.droppedError") && terminatedAbnormally == false){
                        terminatedAbnormally = true;
                        isCallConnected = false;
                        textIncomingInvite.setVisibility(View.INVISIBLE);
                        reconnecting  = findViewById(R.id.reconnecting);
                        TextView firstDot = findViewById(R.id.firstDot);
                        TextView secondDot = findViewById(R.id.secondDot);
                        TextView thirdDot = findViewById(R.id.thirdDot);
                        reconnecting.setVisibility(View.VISIBLE);
                        firstDot.setVisibility(View.VISIBLE);
                        secondDot.setVisibility(View.VISIBLE);
                        thirdDot.setVisibility(View.VISIBLE);
                        if(cancelCall.getVisibility()==View.GONE || cancelCall.getVisibility()==View.INVISIBLE){
                            cancelCall.setVisibility(View.VISIBLE);
                        }
                        cancelCall.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                onBackPressed();
                            }
                        });
                        return;
                    }else{
                        return;
                    }
                }
            }
        } */
                hangupCall();
                finish();

            }
        }
    };



    private void reconnectVideo(){
        if(terminatedAbnormally && !isCallConnected){
            try{
                JSONArray tokens = new JSONArray();
                tokens.put(peerFCMToken);
                JSONObject data = new JSONObject();
                JSONObject body = new JSONObject();
                data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_RECONNECT);
                data.put(Constants.REMOTE_MSG_INITIATOR_NUMBER, preferenceManager.getString(Constants.KEY_MOBILE));
                data.put(Constants.REMOTE_MSG_INITIATOR_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                meetingIdentifier = preferenceManager.getString(Constants.KEY_USER_ID) + "_" + UUID.randomUUID().toString().subSequence(0,7);
                data.put(Constants.REMOTE_MSG_MEETING_IDENTIFIER, meetingIdentifier);
                body.put(Constants.REMOTE_MSG_MSG_DATA, data);
                body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);
                sendReconnect(body.toString(), Constants.REMOTE_MSG_RECONNECT);
                isCallConnected = true;
                terminatedAbnormally = false;
                reconnectLock = true;
            }catch (Exception e){
                Log.d(Constants.TAG, "reconnectVideoIP");
                Log.d(Constants.TAG, e.getMessage());
            }
        }else{
            return;
        }
    }


    private void sendReconnect (String remoteMessageBody, String type){
        ApiClient.getClient().create(ApiService.class).sendRemoteMessage(
            Constants.getRemoteMessageHeaders(), remoteMessageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if(response.isSuccessful()){
                  Log.d(Constants.TAG, "successfullysent");
                }else {
                    /* TODO Toast */
                    Log.d(Constants.TAG, response.message().toString());
                    finish();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                /* TODO Toast */
                Log.d(Constants.TAG, t.getMessage());
                finish();
            }
        });
    }



    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mediaPlayer!=null){
            mediaPlayer.stop();
        }
        if(preferenceManager.getBoolean(Constants.KEY_VIDEO_ACTIVE)!=false){
            preferenceManager.putBoolean(Constants.KEY_VIDEO_ACTIVE, false);
        }
        if(isCallConnected == true){
            isCallConnected = false;
        }

        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
            inviteResponseReceiver
        );


        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
            reconnectRequest
        );

        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
            reconnectResponse
        );
    }

    @Override
    public void onConnected(networkMonitor.connectionType connectionType) {
       if(reconnecting.getVisibility()==View.VISIBLE){
           reconnectVideo();
       }
    }

    @Override
    public void onDisconnected() {

    }
}
