package com.simplemobiletools.dialer.helpers

import android.content.Context

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.telecom.Call

import android.telecom.VideoProfile
import com.simplemobiletools.commons.extensions.getMyContactsCursor
import com.simplemobiletools.commons.helpers.MyContactsContentProvider
import com.simplemobiletools.commons.helpers.SimpleContactsHelper
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.dialer.activities.CallActivity
import com.simplemobiletools.dialer.models.CallContact
import com.simplemobiletools.dialer.services.CallService


// inspired by https://github.com/Chooloo/call_manage
class CallManager {
    companion object {

         var isConferenceCall: Boolean = false
         var callMap:MutableMap<String, Call>? = null
         var callServiceMap:MutableMap<String, CallService>? = null
         var callIdentfierMap:MutableMap<Call, String>? = null
         var activeConferenceCall:Call? = null
         var activeConferenceIdentfier: String? = null
         var conferenceCallInstance: CallActivity? = null
         var foregroundCall:Call? = null
         var backgroundCall:Call? = null

        fun accept(telecomID: String) {
            var thisCall: Call? = null
            if(telecomID!=null && callMap!!.containsKey(telecomID)){
                thisCall = callMap!!.get(telecomID)
            }
            thisCall?.answer(VideoProfile.STATE_AUDIO_ONLY)

        }

        fun getChildren():List<Call>{
            var childList:List<Call> = mutableListOf<Call>()
            if(activeConferenceCall!=null){
                childList = CallManager.activeConferenceCall!!.children
                return childList!!
            }
            return childList!!
        }


        fun reject(telecomID: String) {
            var thisCall: Call? = null
            if(telecomID!=null && callMap!!.containsKey(telecomID)) {
                thisCall = callMap!!.get(telecomID)
            }
            if (thisCall != null) {
                if (thisCall!!.state == Call.STATE_RINGING) {
                    thisCall!!.reject(false, null)
                } else {
                    thisCall!!.disconnect()
                }
            }
        }

        fun rejectWithMessage(reason:String, telecomID:String){
            var callToReject:Call? = null
            if(telecomID!=null && reason!=null){
                callToReject = callMap!!.get(telecomID)
                if(callToReject!=null){
                    if(callToReject.state == Call.STATE_RINGING){
                        callToReject.reject(true, reason)
                    }else{
                        return
                    }
                }else{
                    return
                }
            }else{
                return
            }
        }


        fun rejectCall(call: Call) {
            var thisCall:Call = call
            if(thisCall!=null) {
                thisCall.disconnect()
            }
        }

        fun registerCallback(callback: Call.Callback, telecomID:String) {
            var thisCall:Call? = null
            if(callMap!!.containsKey(telecomID)) {
                thisCall = callMap!!.get(telecomID)
            }
            if (thisCall != null) {
                thisCall!!.registerCallback(callback)
            }
        }

        fun unregisterCallback(callback: Call.Callback, telecomID: String) {
            var thisCall:Call? = null
            if(callMap!!.containsKey(telecomID)) {
                thisCall = callMap!!.get(telecomID)
            }
            if(thisCall!=null) {
                thisCall!!.unregisterCallback(callback)
            }
        }


        fun getState(telecomID:String): Int{
            var thisCall:Call? = null
            if(callMap!!.containsKey(telecomID)) {
                thisCall = callMap!!.get(telecomID)
            }
            if(thisCall==null) return Call.STATE_DISCONNECTED
            else return thisCall.state
        }

        fun getIdentifier(call:Call): String {
            var thisCall:Call? = null
            var callIdentifier: String? = null
            if(call!=null) thisCall = call
            if(callIdentfierMap!!.containsKey(thisCall)) {
                callIdentifier = callIdentfierMap!!.get(thisCall)
            }
            return callIdentifier!!
        }

        fun keypad(c: Char, telecomID: String) {
            var thisCall:Call? = null
            if(telecomID!=null && callMap!!.containsKey(telecomID)) {
                thisCall = callMap!!.get(telecomID)
            }
            thisCall?.playDtmfTone(c)
            thisCall?.stopDtmfTone()
        }


        fun setRoute(value :Int, telecomID: String) {
            var callService:CallService? = null
            if(telecomID!=null && callServiceMap!!.containsKey(telecomID)){
                callService = callServiceMap!!.get(telecomID)
            }
            callService!!.setAudioRoute(value)
        }

        fun setMute(value : Boolean, telecomID: String) {
            var callService:CallService? = null
            if(telecomID!=null && callServiceMap!!.containsKey(telecomID)) {
                callService = callServiceMap!!.get(telecomID)
            }
            callService!!.setMuted(value)
        }

        fun getCall(telecomID: String?): Call? {

            var callList: MutableCollection<String> = CallManager.callIdentfierMap!!.values
            var thisCall:Call ? = null
            if(telecomID!=null && callMap!!.containsKey(telecomID)){
                thisCall = callMap!!.get(telecomID)
            }
            return thisCall
        }

        fun getCallContact(context: Context, telecomCall: Call, callback: (CallContact?) -> Unit) {
            var thisCall:Call = telecomCall

            ensureBackgroundThread {

                val callContact = CallContact("", "", "")
                if (thisCall == null || thisCall!!.details == null || thisCall!!.details!!.handle == null) {
                    callback(callContact)
                    return@ensureBackgroundThread
                }

                val uri = Uri.decode(thisCall!!.details.handle.toString())
                if (uri.startsWith("tel:")) {
                    val number = uri.substringAfter("tel:")
                    callContact.number = number
                    callContact.name = SimpleContactsHelper(context).getNameFromPhoneNumber(number)
                    callContact.photoUri = SimpleContactsHelper(context).getPhotoUriFromPhoneNumber(number)

                    if (callContact.name != callContact.number) {
                        callback(callContact)
                    } else {
                        Handler(Looper.getMainLooper()).post {
                            val privateCursor = context.getMyContactsCursor(false, true)?.loadInBackground()
                            ensureBackgroundThread {
                                val privateContacts = MyContactsContentProvider.getSimpleContacts(context, privateCursor)
                                val privateContact = privateContacts.firstOrNull { it.doesContainPhoneNumber(callContact.number) }
                                if (privateContact != null) {
                                    callContact.name = privateContact.name
                                }
                                callback(callContact)
                            }
                        }
                    }
                }
            }
        }

        fun getCallContactIP(context: Context, peerNumber: String, callback: (CallContact?) -> Unit) {


            ensureBackgroundThread {

                val callContact = CallContact("", "", "")
                if (peerNumber==null) {
                    callback(callContact)
                    return@ensureBackgroundThread
                }
                    val number = peerNumber
                    callContact.number = number
                    callContact.name = SimpleContactsHelper(context).getNameFromPhoneNumber(number)
                    callContact.photoUri = SimpleContactsHelper(context).getPhotoUriFromPhoneNumber(number)

                    if (callContact.name != callContact.number) {
                        callback(callContact)
                    } else {
                        Handler(Looper.getMainLooper()).post {
                            val privateCursor = context.getMyContactsCursor(false, true)?.loadInBackground()
                            ensureBackgroundThread {
                                val privateContacts = MyContactsContentProvider.getSimpleContacts(context, privateCursor)
                                val privateContact = privateContacts.firstOrNull { it.doesContainPhoneNumber(callContact.number) }
                                if (privateContact != null) {
                                    callContact.name = privateContact.name
                                }
                                callback(callContact)
                            }
                        }
                    }

            }
        }
    }
}
