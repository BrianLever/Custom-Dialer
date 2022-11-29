package com.simplemobiletools.dialer.utilities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.simplemobiletools.dialer.Activities.verifyOTP;

public class otpVerifier extends BroadcastReceiver {

    public static Boolean isVerifiyOTPStarted = false;
    public static smsInterface instance;
    public otpVerifier(){}
    public static void getVerifierInstance(smsInterface passedInstance){
        instance = passedInstance;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent!=null && intent.getAction()!= "android.provider.Telephony.SMS_RECEIVED"){
            return;
        }
       Bundle bundle = intent.getExtras();
        if(bundle==null){
            return;
        }
        String format = bundle.getString("format");
        Object[] pdus = (Object[])bundle.get("pdus");
        if(pdus.length == 0){
            return;
        }
        SmsMessage[] messages = new SmsMessage[pdus.length];
        StringBuilder stringBuilder = new StringBuilder();
        for(int i=0; i< pdus.length; i++) {
            messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
            stringBuilder.append(messages[i].getMessageBody());
        }
        String message = stringBuilder.toString();
        if(message.contains("is your verification code for")) {
            String verificationCode = message.substring(0,6);
            if(isVerifiyOTPStarted){
               instance.sendData(verificationCode);
            }
            return;
        }
    }

    public static void toggleActivityState(Boolean isStarted){
        isVerifiyOTPStarted = isStarted;
    }


}
