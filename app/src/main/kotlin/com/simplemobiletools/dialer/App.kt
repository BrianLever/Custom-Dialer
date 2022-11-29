package com.simplemobiletools.dialer

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.multidex.MultiDex
import com.simplemobiletools.commons.extensions.checkUseEnglish

class App : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
    override fun onCreate() {
        super.onCreate()
        checkUseEnglish()
    }

}
