package com.simplemobiletools.dialer.utilities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SIMStateChecker extends BroadcastReceiver {

    private static final String SIM_STATE =
        "android.intent.action.SIM_STATE_CHANGED";
    private String simState = null;
    private String phoneName = null;
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("inYte", "inonreceive");
        Log.d("inYte", "simStateChecker");
        if(intent.getAction().equals(SIM_STATE)) {
            simState = intent.getStringExtra("ss");
            phoneName = intent.getStringExtra("phoneName");
            if(simState!=null){
                switch (simState){
                        case "ABSENT":
                        Log.d("inYte", "simStateAbsent");
                         break;
                        case "ISMI":
                        Log.d("inYte", "simISMI");
                        break;
                        case "LOCKED":
                        Log.d("inYte", "simLocked");
                        break;
                        case "READY":
                        Log.d("inYte", "simReady");
                        break;
                        case "LOADED":
                        Log.d("inYte", "simLoaded");
                         default: return;
                }
            }
        }else{
            return;
        }
    }
}
