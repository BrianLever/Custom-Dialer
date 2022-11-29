package com.simplemobiletools.dialer.helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.FirebaseFirestore
import com.simplemobiletools.commons.extensions.isDefaultDialer
import com.simplemobiletools.dialer.utilities.Constants
import com.simplemobiletools.dialer.utilities.PreferenceManager
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class handleAlarm: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            updateUserDocument(context)
        }
    }

    private fun updateUserDocument(context:Context){
        var preferenceManager = PreferenceManager(context)
        var database = FirebaseFirestore.getInstance()
        var documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(
            preferenceManager?.getString(Constants.KEY_USER_ID)!!
        )
        val timeNow = Date()
        val sdf: DateFormat = SimpleDateFormat("dd/MM/yyyy" + " " + " HH:mm:ss")
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"))
        if(context.isDefaultDialer()){
            Log.d("AlarmManager", "isSetting")
            documentReference.update(
                Constants.KEY_IS_DEFAULT_DIALER, "true",
                Constants.KEY_LAST_SEEN_CELLULAR, sdf.format(timeNow).toString())
                .addOnSuccessListener(OnSuccessListener {
                    preferenceManager!!.putBoolean(Constants.KEY_DB_INIT, true)
                })
                .addOnFailureListener(OnFailureListener {
                    preferenceManager!!.putBoolean(Constants.KEY_DB_INIT, false)
                })
        }else{
            documentReference.update(
                Constants.KEY_IS_DEFAULT_DIALER, "false",
                Constants.KEY_LAST_SEEN_CELLULAR, sdf.format(timeNow).toString())
                .addOnSuccessListener(OnSuccessListener {
                    preferenceManager!!.putBoolean(Constants.KEY_DB_INIT, true)
                })
                .addOnFailureListener(OnFailureListener {
                    preferenceManager!!.putBoolean(Constants.KEY_DB_INIT, false)
                })
        }
    }

}
