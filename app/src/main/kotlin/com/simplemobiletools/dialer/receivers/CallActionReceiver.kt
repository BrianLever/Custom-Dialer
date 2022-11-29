package com.simplemobiletools.dialer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.simplemobiletools.dialer.activities.CallActivity
import com.simplemobiletools.dialer.helpers.ACCEPT_CALL
import com.simplemobiletools.dialer.helpers.CallManager
import com.simplemobiletools.dialer.helpers.DECLINE_CALL

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val bundle: Bundle? = intent.extras
        val telecomID = bundle!!.getString("telecomID")
        val callingNumber = bundle!!.getString("callingNumber")
        /*  when (intent.action) {
            ACCEPT_CALL -> CallManager.accept(telecomID!!)
            DECLINE_CALL -> CallManager.reject(telecomID!!)
        } */

          when(intent.action){
              ACCEPT_CALL -> prepareToAcceptCall(telecomID, callingNumber, context)
              DECLINE_CALL -> CallManager.reject(telecomID!!)
          }

    }

    private fun prepareToAcceptCall(telecomID: String?, callingNumber: String?, context: Context) {
        val intent = Intent()
        intent.putExtra("callingNumber", callingNumber)
        intent.putExtra("telecomCallID", telecomID)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setClass(context, CallActivity::class.java)
        CallManager.accept(telecomID!!)
        context.startActivity(intent)
    }
}
