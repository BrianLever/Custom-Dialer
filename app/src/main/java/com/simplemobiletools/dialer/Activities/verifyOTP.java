package com.simplemobiletools.dialer.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsMessage;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.simplemobiletools.dialer.R;
import com.simplemobiletools.dialer.activities.IntroActivity;
import com.simplemobiletools.dialer.activities.MainActivity;
import com.simplemobiletools.dialer.utilities.Constants;
import com.simplemobiletools.dialer.utilities.PreferenceManager;
import com.simplemobiletools.dialer.utilities.otpVerifier;
import com.simplemobiletools.dialer.utilities.smsInterface;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;


public class verifyOTP extends AppCompatActivity implements smsInterface {

    private String verificationId, phoneNumber;
    private PreferenceManager preferenceManager;
    private static verifyOTP _instance;
    public static Boolean isStarted = false;
    ProgressBar progressBar;
    int counter;
    Boolean gotCode = false;
    Handler handler = new Handler();
    AppCompatTextView resendOTP;
    LinearLayout resendHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);
        ImageView imageBack = findViewById(R.id.imageBack);
        TextView textMobile = findViewById(R.id.inputMobile);
        preferenceManager = new PreferenceManager(getApplicationContext());
        _instance = new verifyOTP();

    /*    textMobile.setText(String.format(
            "+91 -%s", getIntent().getStringExtra("phoneNumber")
        )); */

        imageBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                otpVerifier.toggleActivityState(false);
                otpVerifier.getVerifierInstance(null);
                finish();
            }
        });

        progressBar = findViewById(R.id.progressBar);
        resendHolder = findViewById(R.id.resendHolder);
        resendHolder.setVisibility(View.GONE);
        resendOTP = findViewById(R.id.resendotp);
        resendOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               finish();
            }
        });
        verificationId = getIntent().getStringExtra("verificationId");
        phoneNumber = getIntent().getStringExtra("phoneNumber");
        otpVerifier.toggleActivityState(true);
        otpVerifier.getVerifierInstance(this);
        progressBar.setVisibility(View.VISIBLE);
        counter = 0;
        spinnerThread();
    }

    private void spinnerThread() {
        new Thread(new Runnable() {
            public void run() {
                while (counter < 800) {
                    counter ++;
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    handler.post(new Runnable() {
                        public void run() {
                           if(counter == 800) {
                               progressBar.setVisibility(View.GONE);
                               resendHolder.setVisibility(View.VISIBLE);
                               otpVerifier.toggleActivityState(false);
                               otpVerifier.getVerifierInstance(null);
                               return;
                           }if(gotCode==true){
                                progressBar.setVisibility(View.GONE);
                            }

                        }
                    });
                }
            }

        }).start();
    }

    public void getCode(String vCode){
        progressBar.setVisibility(View.GONE);
        gotCode=true;
        if(vCode!=null){
            if(verificationId!=null){
                Log.d("inyte123", verificationId);
            }
            PhoneAuthCredential phoneAuthCredential = PhoneAuthProvider.getCredential(
                verificationId,
                vCode
            );
            FirebaseAuth.getInstance().signInWithCredential(phoneAuthCredential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull @NotNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);

                        if(task.isSuccessful()) {
                            FirebaseFirestore database = FirebaseFirestore.getInstance();
                            HashMap<String, Object> user  = new HashMap<>();
                            user.put(Constants.KEY_MOBILE, phoneNumber);
                            database.collection(Constants.KEY_COLLECTION_USERS)
                                .add(user)
                                .addOnSuccessListener(documentReference -> {
                                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                                    preferenceManager.putBoolean(Constants.KEY_PROFILE_PICTURE_SELECTED, false);
                                    preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
                                    preferenceManager.putString(Constants.KEY_MOBILE, phoneNumber);
                                    Intent intent = new Intent(getApplicationContext(), IntroActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(verifyOTP.this, "Error: Network Connectivity Failure", Toast.LENGTH_SHORT ).show();
                                    progressBar.setVisibility(View.GONE);
                                    resendHolder.setVisibility(View.VISIBLE);
                                    otpVerifier.toggleActivityState(false);
                                    otpVerifier.getVerifierInstance(null);
                                });
                        }else {
                            Toast.makeText(verifyOTP.this, "The code entered is incorrect", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            resendHolder.setVisibility(View.VISIBLE);
                            otpVerifier.toggleActivityState(false);
                            otpVerifier.getVerifierInstance(null);
                        }
                    }
                });
        }else{
            Toast.makeText(verifyOTP.this, "Code is null", Toast.LENGTH_SHORT).show();
        }
    }

    private void lookupAndDelete(String code) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
            .whereEqualTo(Constants.KEY_MOBILE, phoneNumber)
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull @NotNull Task<QuerySnapshot> task) {
                    if(task.isSuccessful() && task.getResult()!=null && task.getResult().getDocuments().size()>0) {
                        List<DocumentSnapshot> snapshots = task.getResult().getDocuments();
                        for(int i=0;i<task.getResult().getDocuments().size();i++){
                            DocumentReference reference = task.getResult().getDocuments().get(i).getReference();
                            reference.delete();
                        }
                        getCode(code);
                    }else {
                        Log.d(Constants.TAG, "No documents to delete");
                        getCode(code);
                    }
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull @NotNull Exception e) {
                   Log.d(Constants.TAG, "Failed to check and potentially delete duplicates");
                   /* TODO */
                    getCode(code);
                }
            });

    }

    @Override
    public void sendData(String code) {
        lookupAndDelete(code);
    }
}
