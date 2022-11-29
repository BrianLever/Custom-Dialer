package com.simplemobiletools.dialer.missedCalls;

import android.content.Context;
import android.util.Log;

import java.util.concurrent.Callable;

public class missedCallNotificationTask implements Callable<Boolean> {
    missedCallNotifier notifier;
    int count;
    String number;
    Boolean result;

    missedCallNotificationTask(Context context, int count, String number) {
        this.notifier = missedCallNotifier.getInstance(context);
        this.count = count;
        this.number = number;
    }
    @Override
    public Boolean call() throws Exception {
       try{
           notifier.updateMissedCallNotifications(count, number);
       }catch (Exception e){
           Log.d("inYte", "errorinMissedCallNotificationTask");
           result=false;
           return result;
       }
       result = true;
       return result;
    }
}
