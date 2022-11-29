package com.simplemobiletools.dialer.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.telecom.Call
import android.telecom.Call.Details.PROPERTY_CONFERENCE
import android.telecom.InCallService
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.UserManagerCompat
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder
import com.simplemobiletools.commons.extensions.notificationManager
import com.simplemobiletools.commons.extensions.setText
import com.simplemobiletools.commons.extensions.setVisibleIf
import com.simplemobiletools.commons.helpers.isOreoPlus
import com.simplemobiletools.dialer.R
import com.simplemobiletools.dialer.activities.CallActivity
import com.simplemobiletools.dialer.activities.MainActivity
import com.simplemobiletools.dialer.helpers.ACCEPT_CALL
import com.simplemobiletools.dialer.helpers.CallManager
import com.simplemobiletools.dialer.helpers.DECLINE_CALL
import com.simplemobiletools.dialer.missedCalls.contactInfoHelper
import com.simplemobiletools.dialer.receivers.CallActionReceiver
import kotlinx.coroutines.internal.synchronized
import java.lang.reflect.Method
import java.util.*
import kotlin.collections.HashMap


class CallService : InCallService() {
    private var normalizedNumber: String? = null
    private val CALL_NOTIFICATION_ID = 10

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("MissingPermission")
    override fun onCallAdded(call: Call) {
        val uniqueId: String = UUID.randomUUID().toString()
        super.onCallAdded(call)
        var uri = call!!.details.handle
        if (uri != null) {
            normalizedNumber = Uri.decode(uri.toString())
        } else {
            /* For instance when the calling number/name is masked, need a better way to handle this */
            normalizedNumber = "12345"
        }

        kotlin.synchronized(this) {
            if (CallManager.callMap == null) {
                CallManager.callMap = HashMap()
            }
            if (CallManager.callServiceMap == null) {
                CallManager.callServiceMap = HashMap()
            }
            if (CallManager.callIdentfierMap == null) {
                CallManager.callIdentfierMap = HashMap()
            }
            CallManager.callMap?.put(uniqueId, call)
            CallManager.callServiceMap?.put(uniqueId, this)
            CallManager.callIdentfierMap?.put(call, uniqueId)


            if (CallManager.isConferenceCall && call.details.hasProperty(PROPERTY_CONFERENCE)) {
                CallManager.activeConferenceCall = call
                CallManager.activeConferenceIdentfier = uniqueId
                CallActivity.handleConferenceAdd(call, uniqueId)
                val intent = Intent(this, CallActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra("telecomCallID", uniqueId)
                intent.putExtra("callingNumber", normalizedNumber)
                startActivity(intent)
            } else if (CallManager.isConferenceCall /* && !call.details.hasProperty(PROPERTY_CONFERENCE)*/) {
                if (call.parent == CallManager.activeConferenceCall && (call.state != Call.STATE_DIALING || call.state != Call.STATE_CONNECTING)) {
                    /*  don't do anything, as phone manufacturers disconnect existing individual calls and
                * re-instate all children of the conference call */

                } else if (CallManager.foregroundCall != null && CallManager.backgroundCall != null) {
                    CallManager.reject(uniqueId)
                } else if (CallManager.foregroundCall != null && call.state == Call.STATE_RINGING) {
                    CallActivity.handleIncomingActive(uniqueId, normalizedNumber!!)
                } else {
                    CallActivity.handleAddActive(uniqueId, normalizedNumber!!)
                    val intent = Intent(this, CallActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.putExtra("telecomCallID", uniqueId)
                    intent.putExtra("callingNumber", normalizedNumber)
                    startActivity(intent)
                }
            } else if (!CallManager.isConferenceCall && call.parent == null) {
                if (CallManager.foregroundCall != null && CallManager.backgroundCall != null) {
                    CallManager.reject(uniqueId)
                } else if (CallManager.foregroundCall != null && CallManager.foregroundCall!!.state != Call.STATE_ACTIVE && call.state == Call.STATE_RINGING) {
                    CallManager.reject(uniqueId)
                }else if(CallManager.foregroundCall!=null && (CallManager.foregroundCall!!.state!=Call.STATE_ACTIVE && CallManager.foregroundCall!!.state!=Call.STATE_HOLDING) && (call.state == Call.STATE_DIALING || call.state == Call.STATE_CONNECTING)){
                    CallManager.reject(uniqueId)
                } else if (CallManager.foregroundCall != null && call.state == Call.STATE_RINGING) {
                    CallActivity.handleIncomingActive(uniqueId, normalizedNumber!!)
                } else if (CallManager.foregroundCall != null && CallManager.backgroundCall == null) {
                    CallActivity.handleAddActive(uniqueId, normalizedNumber!!)
                    val intent = Intent(this, CallActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.putExtra("telecomCallID", uniqueId)
                    intent.putExtra("callingNumber", normalizedNumber)
                    startActivity(intent)
                } else {
                    var powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                    var keyguardManager =
                        getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                    if (call.state == Call.STATE_RINGING && UserManagerCompat.isUserUnlocked(this) && powerManager.isInteractive()
                        && !keyguardManager.isKeyguardLocked
                    ) {
                        CallManager.foregroundCall = call
                        CallManager.registerCallback(holdingCallCallback, uniqueId)
                        setupNotification(normalizedNumber!!, uniqueId)
                    } else {
                        CallManager.foregroundCall = call
                        val intent = Intent(this, CallActivity::class.java)
                        intent.putExtra("callingNumber", normalizedNumber)
                        intent.putExtra("telecomCallID", uniqueId)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                }
            }
        }
        /*  CallManager.call = call
        CallManager.inCallService = this */
    }


        @SuppressLint("NewApi", "WrongConstant")
        fun setupNotification(callingNumber: String, callIdentifier: String) {
            var callerNameNumber: String? = null
            var contactInfoHelper: com.simplemobiletools.dialer.missedCalls.contactInfoHelper =
                contactInfoHelper(applicationContext)
            val callState = CallManager.getState(callIdentifier!!)
            val channelId = "id_incoming_call_heads_up_notification"
            if (isOreoPlus()) {
                val importance = NotificationManager.IMPORTANCE_HIGH
                val name = "name_incoming_call_heads_up_notification"

                NotificationChannel(channelId, name, importance).apply {
                    setSound(null, null)
                    val notificationManager: NotificationManager =
                        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.createNotificationChannel(this)

                }
            }

            val openAppIntent = Intent(Intent.ACTION_MAIN, null)
            openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION or Intent.FLAG_ACTIVITY_NEW_TASK)
            openAppIntent.setClass(this, CallActivity::class.java)
            openAppIntent.putExtra("callingNumber", callingNumber)
            openAppIntent.putExtra("telecomCallID", callIdentifier)
            val openAppPendingIntent =
                PendingIntent.getActivity(this, 0, openAppIntent, PendingIntent.FLAG_CANCEL_CURRENT)

            val acceptCallIntent = Intent(this, CallActionReceiver::class.java)
            acceptCallIntent.putExtra("telecomID", callIdentifier!!)
            acceptCallIntent.putExtra("callingNumber", callingNumber!!)
            acceptCallIntent.action = ACCEPT_CALL

            val acceptPendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                acceptCallIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
            )

            val declineCallIntent = Intent(this, CallActionReceiver::class.java)
            declineCallIntent.putExtra("telecomID", callIdentifier!!)
            declineCallIntent.putExtra("callingNumber", callingNumber!!)
            declineCallIntent.action = DECLINE_CALL
            val declinePendingIntent = PendingIntent.getBroadcast(
                this,
                1,
                declineCallIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
            )
            var normalizedNumber:String? = null
            val callerDetails = contactInfoHelper.getContactInfo(callingNumber!!)
            if (callerDetails != null) {
                if (callerDetails.name != null && callerDetails.name.isNotEmpty()) {
                    var normalizedNumber = callerDetails.name
                    if(normalizedNumber!=null && normalizedNumber.startsWith("tel:")) {
                        normalizedNumber = normalizedNumber.substringAfter("tel:")
                    }
                    callerNameNumber = normalizedNumber
                } else {
                    var normalizedNumber = callerDetails.number
                    if(normalizedNumber!=null && normalizedNumber.startsWith("tel:")) {
                        normalizedNumber = normalizedNumber.substringAfter("tel:")
                    }
                    callerNameNumber = normalizedNumber
                }
            }else{
                callerNameNumber = callingNumber
            }

            val contentTextId = when (callState) {
                Call.STATE_RINGING -> R.string.is_calling
                Call.STATE_DIALING -> R.string.dialing
                Call.STATE_DISCONNECTED -> R.string.call_ended
                Call.STATE_DISCONNECTING -> R.string.call_ending
                else -> R.string.ongoing_call
            }

            val collapsedView = RemoteViews(packageName, R.layout.call_notification).apply {
                if (callerNameNumber != null) {
                    setText(R.id.notification_caller_name, callerNameNumber)
                }
                setText(R.id.notification_call_status, getString(contentTextId))
                setVisibleIf(R.id.notification_accept_call, callState == Call.STATE_RINGING)

                setOnClickPendingIntent(R.id.notification_decline_call, declinePendingIntent)
                setOnClickPendingIntent(R.id.notification_accept_call, acceptPendingIntent)

            }


            val remoteViews: RemoteViews =
                RemoteViews(packageName, R.layout.call_notification).apply {
                    if (callerNameNumber != null) {
                        setTextViewText(R.id.notification_caller_name, callerNameNumber)
                    }
                    setTextViewText(R.id.notification_call_status, getString(contentTextId))
                    setOnClickPendingIntent(R.id.notification_decline_call, declinePendingIntent)
                    setOnClickPendingIntent(R.id.notification_accept_call, acceptPendingIntent)
                }


           val builder = NotificationCompat.Builder(this, channelId)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setSmallIcon(R.drawable.ic_phone_vector)
                .setContentIntent(openAppPendingIntent)
                .setFullScreenIntent(openAppPendingIntent, true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCustomContentView(collapsedView)
                .setOngoing(true)
                .setSound(null)
                .setAutoCancel(true)
                .setChannelId(channelId)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())

            val notification = builder.build()
            with(NotificationManagerCompat.from(this)) {
                notificationManager.notify(CALL_NOTIFICATION_ID, notification)
            }
            /*   notificationManager.notify(CALL_NOTIFICATION_ID, notification)
        var notificationManager:NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(CALL_NOTIFICATION_ID, notification) */

        }


        override fun onCallRemoved(call: Call) {
            super.onCallRemoved(call)
            var destroyhandlestring: String? = null
            var telecomID:String ? = null
            var destroyedHandle = call.details!!.handle
            if (destroyedHandle != null) {
                destroyhandlestring = destroyedHandle.toString()
            }

            if(CallManager.foregroundCall!=null && CallManager.foregroundCall == call) {
                telecomID = CallManager.getIdentifier(call!!)
                if(telecomID!=null) {
                    CallManager.unregisterCallback(holdingCallCallback, telecomID)
                }
                CallManager.foregroundCall = null
            }
            var identifier: String? = null
            identifier = CallManager.callIdentfierMap!!.get(call)
            if (call != null && CallManager.callMap!!.containsKey(identifier)) CallManager.callMap!!.remove(
                identifier
            )
            if (call != null && CallManager.callServiceMap!!.containsKey(identifier)) CallManager.callServiceMap!!.remove(
                identifier
            )
            if (call != null && CallManager.callIdentfierMap!!.containsKey(call)) {
                CallManager.callIdentfierMap!!.remove(call)
            }

            val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val notifications = mNotificationManager.activeNotifications
            for (notification in notifications) {
                if (notification.id == 10) {
                    mNotificationManager.cancel(CALL_NOTIFICATION_ID)
                    MainActivity.externalRefreshFragments()
                }
            }
            /* CallManager.call = null
        CallManager.inCallService = null */
        }

      private val holdingCallCallback = object : Call.Callback() {
          override fun onStateChanged(call: Call?, state: Int) {
              super.onStateChanged(call, state)
          }
          override fun onCallDestroyed(call: Call?) {
              super.onCallDestroyed(call)
          }
      }

    }

