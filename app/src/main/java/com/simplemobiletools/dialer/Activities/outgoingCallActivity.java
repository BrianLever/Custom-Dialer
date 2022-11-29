package com.simplemobiletools.dialer.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.renderscript.ScriptGroup;
import android.telephony.SmsManager;
import android.util.Log;
import android.util.Size;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.ShortDynamicLink;
import com.google.firebase.messaging.FirebaseMessaging;
import com.simplemobiletools.dialer.R;
import com.simplemobiletools.dialer.helpers.CallManager;
import com.simplemobiletools.dialer.models.CallContact;
import com.simplemobiletools.dialer.network.ApiClient;
import com.simplemobiletools.dialer.network.ApiService;
import com.simplemobiletools.dialer.utilities.Constants;
import com.simplemobiletools.dialer.utilities.PreferenceManager;
import com.simplemobiletools.dialer.utilities.networkMonitor;


import org.jetbrains.annotations.NotNull;
import org.jitsi.meet.sdk.BroadcastEvent;
import org.jitsi.meet.sdk.BroadcastIntentHelper;
import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileDescriptor;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class outgoingCallActivity extends AppCompatActivity implements com.simplemobiletools.dialer.utilities.networkMonitor.Observer {

    private TextView textOutgoingInvite, sendSMSDescription, startCallDescription, reconnecting, inytevideolabel,
    calleeNumberLabel, statusLabel, cancelCallLabel, SMSBanner;
    private ImageView cancelCall, sendSMS, startCall, calleeAvatar;
    public static String callType, peerNumber, peerFCMToken;
    private PreferenceManager preferenceManager;
    private  int MAX_LENGTH = 160;
    private String SENT = "SMS_SENT";
    private String localFCMToken;
    private CallContact callContactsend = new CallContact("", "", "");
    private static final int SMS_SEND_CODE = 200;
    private String remotePeer = null;
    private Bitmap bitmap;
    private int counter = 0;
    private int secondCounter = 0;
    private boolean terminatedAbnormally = false;
    private String conferenceURL = null;
    private String JWT = null;
    private com.simplemobiletools.dialer.utilities.networkMonitor monitor;
    Handler handler = new Handler();
    private Boolean isCallConnected = false;
    private String meetingIdentifier = null;
    private Boolean reconnectLock = false;
    private Boolean reconnectResponseLock = false;
    private MediaPlayer mediaPlayer = null;
    private AssetFileDescriptor fileDescriptor = null;
    String signature = "Keo6oAZ+9cseqG7uazWzOVS9jAPNyycxOk1uMHaB/Hs=";
    private AudioManager audioManager = null;
    private  GestureDetector gestureDetector;
    private LinearLayout linearLayout;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      /*  getSupportActionBar().hide(); */
        getSupportActionBar().setTitle("inYte");
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getApplicationContext().getResources().getColor(R.color.md_blue)));
        setContentView(R.layout.activity_outgoing_call);
        preferenceManager = new PreferenceManager(getApplicationContext());
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull @NotNull Task<String> task) {
                if(task.isSuccessful() && task.getResult()!=null){
                    localFCMToken=task.getResult();
                }
            }
        });
        if(preferenceManager.getBoolean(Constants.KEY_VIDEO_ACTIVE)!=true){
            preferenceManager.putBoolean(Constants.KEY_VIDEO_ACTIVE, true);
        }
        if(preferenceManager.getBoolean(Constants.KEY_IS_OUTGOING)!=true){
            preferenceManager.putBoolean(Constants.KEY_IS_OUTGOING, true);
        }
        inytevideolabel = findViewById(R.id.inytevideolabel);
        calleeNumberLabel = findViewById(R.id.callee_number_label);
        statusLabel = findViewById(R.id.status_label);
        calleeAvatar = findViewById(R.id.callee_avatar);
        reconnecting = findViewById(R.id.reconnecting);
        monitor = new networkMonitor(this, getApplicationContext());
        callType=getIntent().getStringExtra("Type");
        peerNumber= getIntent().getStringExtra("Remote-Number");
        peerFCMToken=getIntent().getStringExtra("Remote-Token");
        if(getIntent().hasExtra("MeetingURL")) {
            meetingIdentifier = getIntent().getStringExtra("MeetingURL");
        }
        if(getIntent().hasExtra("Remote-Peer")) {
            remotePeer = getIntent().getStringExtra("Remote-Peer");
        }
        gestureDetector = new GestureDetector(this, new GestureListener());
        cancelCall = findViewById(R.id.cancelCall);
        cancelCallLabel = findViewById(R.id.cancelCallLabel);
//        cancelCall.setOnClickListener(view -> {
//            callCancel(peerFCMToken);
//            onBackPressed();
//        });
        linearLayout=findViewById(R.id.outgoing_arrow_holder_cancel);
        cancelCall.setOnTouchListener((view, motionEvent) ->{
            cancelCall.animate()
                .translationY(-400f)
                .setDuration(800)
                .alpha(0f)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        cancelCall.setAlpha(1f);
                        cancelCall.setTranslationY(0f);
                        linearLayout.setVisibility(View.GONE);
                        super.onAnimationEnd(animation);
                    }
                    @Override
                    public void onAnimationStart(Animator animation) {
                        linearLayout.setVisibility(View.VISIBLE);
                    }
                });
            return gestureDetector.onTouchEvent(motionEvent);
        });
        startCall = findViewById(R.id.startCall);
        startCall.setOnClickListener(view -> onBackPressed());
        startCallDescription = findViewById(R.id.startCallDescription);
        sendSMS = findViewById(R.id.sendSMS);
        sendSMS.setOnClickListener(view -> {
            try {
                coverAndCreateLink();
            } catch (UnsupportedEncodingException e) {
                Log.d(Constants.TAG, e.getMessage());
            }
        });
        sendSMSDescription = findViewById(R.id.sendSMSDescription);
        audioManager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0);
        if(callType!=null && (callType.equals("video"))){
            startCall.setVisibility(View.GONE);
            startCallDescription.setVisibility(View.GONE);
            sendSMS.setVisibility(View.GONE);
            sendSMSDescription.setVisibility(View.GONE);
            initiateCall(callType, peerFCMToken);
            if(remotePeer!=null){
                getClearBitmap();
                calleeNumberLabel.setText(remotePeer);
                statusLabel.setText("Calling...");
                startCallTimer();
            }else{
                    calleeNumberLabel.setText(peerNumber);
                    statusLabel.setText("Calling...");
                    startCallTimer();
            }
        }else if(callType!=null && callType.equals("videoSMS"))
            setupSMSCall();
        else{
            /* Do nothing */
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
                        callCancel(peerFCMToken);
                        onBackPressed();
                    }
                    result = true;
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return result;
        }

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
        context.put("group", "meet.onecxn.info");
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
            return Jwt.compact();
    }


    private void checkSMSPermission() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)!=
            PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.SEND_SMS}, SMS_SEND_CODE);
        }else {
            /* NOTHING */
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
                                calleeAvatar.setImageBitmap(bitmap[0]);
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
                               calleeAvatar.setImageBitmap(bitmap);
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


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case SMS_SEND_CODE : {
                if(permissions[0].equalsIgnoreCase(Manifest.permission.SEND_SMS)
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                }else{
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if(shouldShowRequestPermissionRationale(Manifest.permission.SEND_SMS)){
                            showMessageOKCancel("SMS send access is required to send call URL:",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            requestPermissions(new String[]{Manifest.permission.SEND_SMS},
                                                SMS_SEND_CODE);
                                        }
                                    }
                                });
                            return;
                        }
                    }
                }
            }

        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(outgoingCallActivity.this)
            .setMessage(message)
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", null)
            .create()
            .show();
    }

    private void coverAndCreateLink() throws UnsupportedEncodingException {
        /* Function to obfuscate link when sending via SMS */
        checkSMSPermission();
        String meetURL = "https://inytecall1.onecxn.info/";
        Uri uri = Uri.parse("sms:"+peerNumber);
        String jwt = generateGuestToken();


        Task<ShortDynamicLink> shortLinkTask = FirebaseDynamicLinks.getInstance().createDynamicLink()
            .setLink(Uri.parse(meetURL+meetingIdentifier+"?jwt="+jwt))
            .setDomainUriPrefix("https://pawan.onecxn.info")
            .buildShortDynamicLink()
            .addOnCompleteListener(this, new OnCompleteListener<ShortDynamicLink>() {
                @Override
                public void onComplete(@NonNull @NotNull Task<ShortDynamicLink> task) {
                    if(task.isSuccessful()) {
                        Uri shortLink = task.getResult().getShortLink();
                     /*   Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
                        intent.putExtra("sms_body", "Click the below URL to join a video call:");
                        String SMSString = shortLink.toString();
                        intent.putExtra("sms_body", SMSString);
                        startActivity(intent); */
                        String messageToSend = "Click the following URL to join the call:";
                        messageToSend = messageToSend + "\n";
                        messageToSend = messageToSend + shortLink.toString();
                        sendMessage(peerNumber,messageToSend);
                    }else {
                        Log.d(Constants.TAG, "SMS Cover Creation Failure");
                    }
                }
            });

    }

    public void sendMessage(String phoneNumber, String messageToSend){
        Context context = getApplicationContext();
        SmsManager manager = SmsManager.getDefault();
        PendingIntent sendIntent = PendingIntent.getBroadcast(context, 0, new Intent(SENT), PendingIntent.FLAG_UPDATE_CURRENT);
        int length = messageToSend.length();
        if(length>0){
            manager.sendTextMessage(phoneNumber, null, messageToSend, sendIntent, null);
            startSMSCall();
        }else {
            return;
        }
    }


    private String generateGuestToken() throws UnsupportedEncodingException {
        Map<String, Object> payload = new HashMap<>();
        Map<String, String> user = new HashMap<>();
        Map<String, Object> context = new HashMap<>();
        user.put("avatar", "https://www.computerhope.com/jargon/g/guest-user.jpg");
        user.put("name", "Guest");
        user.put("email", "guest@nodomain.com");
        /*   user.put("id", preferenceManager.getString(Constants.KEY_EMAIL) + preferenceManager.getString(Constants.KEY_FIRST_NAME)); */
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

        return Jwt.compact();
    }

    private void setupSMSCall(){
        cancelCall.setVisibility(View.GONE);
        cancelCallLabel.setVisibility(View.GONE);
        SMSBanner = findViewById(R.id.SMSBanner);
        SMSBanner.setVisibility(View.VISIBLE);
        if(remotePeer!=null){
          getClearBitmap();
          SMSBanner.setText(remotePeer + "," + " "+"is not on inYte. Send them an SMS containing a link to the call and join?");
        }else{
            SMSBanner.setText(peerNumber + "," + " "+ "is not on inYte. Send them an SMS containing a link to the call and join?");
        }
        meetingIdentifier = preferenceManager.getString(Constants.KEY_USER_ID) + "_" +
            UUID.randomUUID().toString().substring(0,7);
    }

    private void launchSMS() throws UnsupportedEncodingException {
        String meetURL = "https://jitsimeet.onecxn.info/";
        Uri uri = Uri.parse("sms:"+peerNumber);
        String jwt = generateGuestToken();
        Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
        intent.putExtra("sms_body", "Click the below URL to join a video call:");
        intent.putExtra("sms_body", meetURL+meetingIdentifier+"?jwt="+jwt);
        startActivity(intent);
    }

    private void startSMSCall() {
            try {
                JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                    .setAudioMuted(false)
                    .setServerURL(new URL("https://jitsimeet.onecxn.info"))
                    .setRoom(meetingIdentifier)
                    .setToken(generateToken())
                    .setFeatureFlag("add-people.enabled", false)
                    .setFeatureFlag("audio-focus.disabled", true)
                    .setFeatureFlag("replace.participant", true)
                    .setFeatureFlag("calendar.enabled", false)
                    .setFeatureFlag("call-integration.enabled", true)
                    .setFeatureFlag("conference-time.enabled", false)
                    .setFeatureFlag("invite.enabled", false)
                    .setFeatureFlag("speakerstats.enabled", false)
                    .setFeatureFlag("kick-out.enabled", true)
                    .setFeatureFlag("lobby-mode.enabled", false)
                    .setFeatureFlag("meeting-name.enabled", false)
                    .setFeatureFlag("overflow-menu.enabled", false)
                    .setFeatureFlag("raise-hand.enabled", false)
                    .setFeatureFlag("welcomepage.enabled", false)
                    .build();
                JitsiMeetActivity.launch(getApplicationContext(), options);
                isCallConnected = true;
                startCall.setVisibility(View.GONE);
                startCallDescription.setVisibility(View.GONE);
                sendSMS.setVisibility(View.GONE);
                sendSMSDescription.setVisibility(View.GONE);
                cancelCall.setVisibility(View.INVISIBLE);
                cancelCallLabel.setVisibility(View.INVISIBLE);
            } catch (Exception e) {
                Log.d(Constants.TAG, e.getMessage());
                finish();
            }
    }

    private void initiateCall(String type, String receiverToken){

            Date timeNow = new Date();
            DateFormat sdf = new SimpleDateFormat("dd/MM/yyyy"+ " "+ " HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        try{
            JSONArray tokens = new JSONArray();
            tokens.put(receiverToken);

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();
            JSONObject urgency = new JSONObject();
            JSONObject ttl = new JSONObject();
            urgency.put("priority", "high");
            ttl.put("time_to_live", 1);
            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITE);
            data.put(Constants.REMOTE_MSG_MEETING_TYPE, type);
            data.put(Constants.REMOTE_MSG_INITIATOR_NUMBER,
                preferenceManager.getString(Constants.KEY_MOBILE));
            data.put(Constants.REMOTE_MSG_INITIATOR_TOKEN,
                preferenceManager.getString(Constants.KEY_FCM_TOKEN));
            meetingIdentifier = preferenceManager.getString(Constants.KEY_USER_ID) + "_" +
                UUID.randomUUID().toString().substring(0,7);
            data.put(Constants.KEY_TIMESTAMP, sdf.format(timeNow).toString());
            data.put(Constants.REMOTE_MSG_MEETING_IDENTIFIER, meetingIdentifier);
            body.put(Constants.REMOTE_MSG_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);
            body.put("priority", "high");
            body.put("time_to_live", 1);
            sendRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITE);
        }catch (Exception e){
            Log.d(Constants.TAG, e.getMessage());
            finish();
        }
    }

    private void callCancel(String receiverToken) {

        try{
            JSONArray tokens = new JSONArray();
            tokens.put(receiverToken);

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITE_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITE_RESPONSE, Constants.REMOTE_MSG_INVITE_CANCELLED);
            data.put(Constants.REMOTE_MSG_MEETING_TYPE, callType);
            data.put(Constants.REMOTE_MSG_INITIATOR_NUMBER, preferenceManager.getString(Constants.KEY_MOBILE));
            data.put(Constants.REMOTE_MSG_INITIATOR_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
            data.put(Constants.REMOTE_MSG_MEETING_IDENTIFIER, meetingIdentifier);
            body.put(Constants.REMOTE_MSG_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);
            body.put("priority", "high");
            body.put("time_to_live", 1);
            sendRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITE_CANCELLED);
            if(mediaPlayer!=null) {
                mediaPlayer.stop();
            }
            mediaPlayer = null;
            fileDescriptor = null;
            audioManager = null;
        }catch (Exception e){
            Log.d(Constants.TAG, e.getMessage());
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


    private void startCallTimer(){
        new Thread(new Runnable() {
            public void run() {
                while (counter < 500) {
                    counter ++;
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {

                    }
                    handler.post(new Runnable() {
                        public void run() {
                            if(counter == 500 && isCallConnected==false) {
                                callCancel(peerFCMToken);
                                finish();
                                return;
                            }

                        }
                    });
                }
            }

        }).start();
    }

    private BroadcastReceiver callBusy = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            statusLabel.setText("Peer busy!!!");
            preferenceManager.putBoolean(Constants.KEY_VIDEO_ACTIVE, false);
            if(mediaPlayer!=null) {
                mediaPlayer.stop();
            }
            mediaPlayer = null;
            fileDescriptor = null;
            audioManager = null;
            finish();
        }
    };

    private BroadcastReceiver provisionalResponse = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(mediaPlayer==null) {
                statusLabel.setText("Ringing...");
                try {
                    fileDescriptor = getAssets().openFd("ringback.wav");
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                    mediaPlayer.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(), fileDescriptor.getLength());
                    fileDescriptor.close();
                    mediaPlayer.prepare();
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mediaPlayer) {
                            mediaPlayer.start();
                            mediaPlayer.setLooping(true);
                        }
                    });
                } catch (Exception e) {
                    Log.d(Constants.TAG, "fileplayexception");
                }
            }
        }
    };


    private void sendReconnect (String remoteMessageBody, String type){
        ApiClient.getClient().create(ApiService.class).sendRemoteMessage(
            Constants.getRemoteMessageHeaders(), remoteMessageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if(response.isSuccessful()){
                   Log.d(Constants.TAG, "sendReconnect");
                   Log.d(Constants.TAG, "reconnect sent successfully");
                }else {
                    /* TODO Toast */
                    Log.d(Constants.TAG, "sendReconnect");
                    Log.d(Constants.TAG, "sendReconnectFailed");
                    Log.d(Constants.TAG, response.message().toString());
                    finish();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                /* TODO Toast */
                Log.d(Constants.TAG, "sendReconnect");
                Log.d(Constants.TAG, "sendReconnectFailed");
                finish();
            }
        });
    }

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


    private BroadcastReceiver inviteResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(Constants.REMOTE_MSG_INVITE_RESPONSE);
            if(type!=null){
                if(type.equals(Constants.REMOTE_MSG_INVITE_ACCEPTED)) {
                        if (mediaPlayer != null) {
                            mediaPlayer.stop();
                        }
                        mediaPlayer = null;
                        fileDescriptor = null;
                        audioManager = null;
                        try {
                            JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                                .setAudioMuted(false)
                                .setServerURL(new URL("https://inytecall1.onecxn.info"))
                                .setRoom(meetingIdentifier)
                                .setToken(generateToken())
                                .setFeatureFlag("replace.participant", true)
                                .setFeatureFlag("pip.enabled", false)
                                .setFeatureFlag("notifications.enabled", false)
                                .setFeatureFlag("audio-focus.disabled", true)
                                .setFeatureFlag("add-people.enabled", false)
                                .setFeatureFlag("calendar.enabled", false)
                                .setFeatureFlag("call-integration.enabled", true)
                                .setFeatureFlag("conference-time.enabled", false)
                                .setFeatureFlag("invite.enabled", false)
                                .setFeatureFlag("speakerstats.enabled", false)
                                .setFeatureFlag("kick-out.enabled", true)
                                .setFeatureFlag("lobby-mode.enabled", false)
                                .setFeatureFlag("meeting-name.enabled", false)
                                .setFeatureFlag("overflow-menu.enabled", false)
                                .setFeatureFlag("raise-hand.enabled", false)
                                .setFeatureFlag("welcomepage.enabled", false)
                                .build();
                            JitsiMeetActivity.launch(getApplicationContext(), options);
                            isCallConnected = true;
                            cancelCall.setVisibility(View.INVISIBLE);
                            cancelCallLabel.setVisibility(View.INVISIBLE);
                        } catch (Exception e) {
                            Log.d(Constants.TAG, e.getMessage());
                            finish();
                        }

                }else if(type.equals(Constants.REMOTE_MSG_INVITE_DELCINE)){
                    if(mediaPlayer!=null) {
                        mediaPlayer.stop();
                    }
                    mediaPlayer = null;
                    fileDescriptor = null;
                    audioManager = null;
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
                                callReconnectResponse(Constants.REMOTE_MSG_RECONNECT_ACCEPT, peerFCMToken );
                                return;
                            }
                        }
                    });
                }

            }
        }).start();
    }

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
                        try {
                            JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
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
                        } catch (Exception e) {
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


    private BroadcastReceiver reconnectResponse = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent!=null && reconnectLock){
                reconnectLock = false;
                String type = intent.getStringExtra(Constants.REMOTE_MSG_RECONNECT_RESPONSE);
                if(type.equals(Constants.REMOTE_MSG_RECONNECT_ACCEPT)){
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
                        } catch (Exception e) {
                            Log.d(Constants.TAG, "reconnectResponse");
                            Log.d(Constants.TAG, e.getMessage());
                            finish();
                        }
                }else{
                    finish();
                }
            }else{
                return;
            }
        }
    };

    private BroadcastReceiver terminated = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent!=null){
             /*   if(intent.hasExtra("error")==true){
                    String error = intent.getStringExtra("error");
                    Log.d("terminatedAbnormally", error);
                    if(error.contains("connection.droppedError") && terminatedAbnormally == false){
                        terminatedAbnormally = true;
                        Log.d("terminatedAbnormally", "inOnReceive");
                        isCallConnected = false;

                        if(sendSMS.getVisibility()==View.VISIBLE){
                            sendSMS.setVisibility(View.INVISIBLE);
                        }
                        if(sendSMSDescription.getVisibility()==View.VISIBLE){
                            sendSMSDescription.setVisibility(View.INVISIBLE);
                        }
                        if(startCall.getVisibility()==View.VISIBLE){
                            startCall.setVisibility(View.INVISIBLE);
                        }
                        if(startCallDescription.getVisibility()==View.VISIBLE){
                            startCallDescription.setVisibility(View.INVISIBLE);
                        }
                        reconnecting  = findViewById(R.id.reconnecting);
                        statusLabel.setVisibility(View.GONE);
                        reconnecting.setVisibility(View.VISIBLE);
                        if(cancelCall.getVisibility()==View.GONE || cancelCall.getVisibility()==View.INVISIBLE){
                            cancelCall.setVisibility(View.VISIBLE);
                            cancelCallLabel.setVisibility(View.VISIBLE);
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
                } */

                hangupCall();
                finish();
            }

        }
    };

    private BroadcastReceiver participantJoined = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };

    private BroadcastReceiver sentSMS = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (getResultCode()){
                case Activity.RESULT_OK:
                    startSMSCall();
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    Toast.makeText(getApplicationContext(), "Failed to send SMS", Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    Toast.makeText(getApplicationContext(), "Failed to send SMS", Toast.LENGTH_SHORT).show();
                    break;
                case  SmsManager.RESULT_ERROR_NULL_PDU:
                    Toast.makeText(getApplicationContext(), "Failed to send SMS", Toast.LENGTH_SHORT).show();
                    break;
                case   SmsManager.RESULT_ERROR_RADIO_OFF:
                    Toast.makeText(getApplicationContext(), "Failed to send SMS", Toast.LENGTH_SHORT).show();
                    break;

            }
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
            sentSMS,
            new IntentFilter(SENT)
        );

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
            provisionalResponse,
            new IntentFilter(Constants.REMOTE_MSG_PROVISIONAL_RESPONSE)
        );

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
            callBusy,
            new IntentFilter(Constants.REMOTE_MSG_BUSY)
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




    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("terminatedAbnormally", "inOnResume");
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

        if(preferenceManager.getBoolean(Constants.KEY_IS_OUTGOING)!=false){
            preferenceManager.putBoolean(Constants.KEY_IS_OUTGOING, false);
        }

        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
            inviteResponseReceiver
        );

        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
            sentSMS
        );

        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
            provisionalResponse
        );

        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
            callBusy
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

