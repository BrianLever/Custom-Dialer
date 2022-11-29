package com.simplemobiletools.dialer.missedCalls;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telecom.TelecomManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

public class telecomManagerUtil {

    /* Utility class for telecom Manager */

    public static TelecomManager getTelecomManager (Context context) {
       return (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
    }

    public static void cancelMissedCallsNotifications(Context context) {
        if((ActivityCompat.checkSelfPermission(context, Manifest.permission.MODIFY_PHONE_STATE) != PackageManager.PERMISSION_GRANTED))
        {
            try {
                getTelecomManager(context).cancelMissedCallsNotification();
            } catch (SecurityException e) {
                Log.d("inYTe", e.getMessage());
            }
        }else{
            Log.d("inYte", "modifyPhonestatepermissionmissing");
        }
    }
}
