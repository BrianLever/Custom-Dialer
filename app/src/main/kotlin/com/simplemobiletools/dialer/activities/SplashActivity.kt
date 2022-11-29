package com.simplemobiletools.dialer.activities

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.simplemobiletools.commons.activities.BaseSplashActivity
import com.simplemobiletools.dialer.Activities.verifyOTP
import com.simplemobiletools.dialer.activities.MainActivity
import com.simplemobiletools.dialer.utilities.Constants
import com.simplemobiletools.dialer.utilities.PreferenceManager

class SplashActivity : BaseSplashActivity() {
    private var context: Context? = null
    private var preferenceManager:PreferenceManager? = null
    override fun initActivity() {
        context = applicationContext
        preferenceManager = PreferenceManager(context)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
//        if(preferenceManager?.getBoolean(Constants.KEY_IS_SIGNED_IN)!!) {
//            startActivity(Intent(this, MainActivity::class.java))
//            finish()
//        }
//        else {
//            startActivity(Intent(this, com.simplemobiletools.dialer.Activities.receiveOTP::class.java))
//            finish()
//        }
    }
}
