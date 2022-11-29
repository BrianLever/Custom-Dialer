package com.simplemobiletools.dialer.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.blongho.country_data.World;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.simplemobiletools.dialer.R;
import com.simplemobiletools.dialer.activities.IntroActivity;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class receiveOTP extends AppCompatActivity {
    Button cancelOTPAction;
    private static final int SMS_RECEIVE_CODE = 100;
    final Boolean[] isCancelClicked = {false};
    private String countryCode;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_otp);
        World.init(getApplicationContext());
        final AppCompatTextView appCompatTextView = findViewById(R.id.countryCode);
        final AppCompatImageView countryFlag=findViewById(R.id.countryFlag);
        countryCode ="+"+getCountryCode();
        appCompatTextView.setText("("+countryCode+")");
        countryFlag.setImageResource(World.getFlagOf(getCountryName()));
        final EditText inputMobile = findViewById(R.id.inputMobileNumber);
        Button generateOTP = findViewById(R.id.getOTP);
        cancelOTPAction = findViewById(R.id.cancelOTPAction);
        final Boolean[] isCancelClicked = {false};
        final ProgressBar progressBar = findViewById(R.id.progressBar);
        if (cancelOTPAction.getVisibility() == View.VISIBLE) {
            cancelOTPAction.setVisibility(View.GONE);
        }

        cancelOTPAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (progressBar.getVisibility() == View.VISIBLE) {
                    progressBar.setVisibility(View.GONE);
                }
                if (generateOTP.getVisibility() == View.INVISIBLE || generateOTP.getVisibility() == View.GONE) {
                    generateOTP.setVisibility(View.VISIBLE);
                }
                cancelOTPAction.setVisibility(View.GONE);
                isCancelClicked[0] = true;
            }
        });

        generateOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (inputMobile.getText().toString().trim().isEmpty()) {
                    Toast.makeText(receiveOTP.this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
                    return;
                }
                progressBar.setVisibility(View.VISIBLE);
                isCancelClicked[0] = false;
                cancelOTPAction.setVisibility(View.VISIBLE);
                generateOTP.setVisibility(View.INVISIBLE);
                /* TODO, the below constructs are deprecated, does require updating */

                PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    countryCode + inputMobile.getText().toString(),
                    35,
                    TimeUnit.SECONDS,
                    receiveOTP.this,
                    new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                        @Override
                        public void onVerificationCompleted(@NonNull @NotNull PhoneAuthCredential phoneAuthCredential) {
                            progressBar.setVisibility(View.GONE);
                            generateOTP.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onVerificationFailed(@NonNull @NotNull FirebaseException e) {

                            Toast.makeText(receiveOTP.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onCodeSent(@NonNull @NotNull String s, @NonNull @NotNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                            super.onCodeSent(s, forceResendingToken);
                            if (isCancelClicked[0] != true) {
                                progressBar.setVisibility(View.GONE);
                                generateOTP.setVisibility(View.VISIBLE);
                                if (cancelOTPAction.getVisibility() == View.VISIBLE) {
                                    cancelOTPAction.setVisibility(View.GONE);
                                }
                                Intent verifyIntent = new Intent(getApplicationContext(), verifyOTP.class);
                                verifyIntent.putExtra("phoneNumber", inputMobile.getText().toString());
                                verifyIntent.putExtra("verificationId", s);
                                startActivity(verifyIntent);
                            }
                        }
                    }
                );

            }
        });
        checkSMSPermission();
    }

    private void checkSMSPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) !=
            PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, SMS_RECEIVE_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case SMS_RECEIVE_CODE: {
                if (permissions[0].equalsIgnoreCase(Manifest.permission.RECEIVE_SMS)
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("inYTE", "receiveSMSPermissioninPLace");
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(Manifest.permission.RECEIVE_SMS)) {
                            showMessageOKCancel("SMS Receive access is required for application security",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS},
                                                SMS_RECEIVE_CODE);
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
        new AlertDialog.Builder(receiveOTP.this)
            .setMessage(message)
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", null)
            .create()
            .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cancelOTPAction.getVisibility() == View.VISIBLE) {
            cancelOTPAction.setVisibility(View.GONE);
        }
        isCancelClicked[0] = false;

    }
    public String getCountryCode() {
        String CountryID = "";
        String CountryZipCode = "";
        TelephonyManager manager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        CountryID = manager.getSimCountryIso().toUpperCase();
        String[] rl = this.getResources().getStringArray(R.array.CountryCodes);
        for (int i = 0; i < rl.length; i++) {
            String[] g = rl[i].split(",");
            if (g[1].trim().equals(CountryID.trim())) {
                CountryZipCode = g[0];
                break;
            }
        }
        return CountryZipCode;
    }
    public String getCountryName() {
        String CountryID = "";
        TelephonyManager manager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        CountryID = manager.getSimCountryIso();
        return CountryID.trim();
    }

}
