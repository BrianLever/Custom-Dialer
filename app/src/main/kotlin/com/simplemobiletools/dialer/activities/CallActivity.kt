
package com.simplemobiletools.dialer.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.*
import android.graphics.*
import android.media.AudioManager
import android.net.Uri
import android.os.*
import android.provider.ContactsContract
import android.provider.MediaStore
import android.telecom.Call
import android.telecom.Call.Details.*
import android.telecom.CallAudioState
import android.util.Base64
import android.util.Log
import android.util.Size
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.dialer.*
import com.simplemobiletools.dialer.extensions.*
import com.simplemobiletools.dialer.helpers.ACCEPT_CALL
import com.simplemobiletools.dialer.helpers.CallManager
import com.simplemobiletools.dialer.helpers.DECLINE_CALL
import com.simplemobiletools.dialer.helpers.bluetoothHelper
import com.simplemobiletools.dialer.interfaces.PositionInterface
import com.simplemobiletools.dialer.models.CallContact
import com.simplemobiletools.dialer.receivers.CallActionReceiver
import com.simplemobiletools.dialer.utilities.Constants
import com.simplemobiletools.dialer.utilities.PreferenceManager
import com.simplemobiletools.dialer.utilities.networkMonitor
import com.simplemobiletools.dialer.utilities.otpVerifier
import kotlinx.android.synthetic.main.activity_call.*
import kotlinx.android.synthetic.main.dialpad.*
import java.util.*


open class CallActivity : SimpleActivity(), conferenceManagement.conferenceNotifier, networkMonitor.Observer, bluetoothHelper.onBlueToothStateChange
   {

    private val CALL_NOTIFICATION_ID = 1
    private var conferenceURL:String? = null
    private var isSpeakerOn = false
    private var isBluetoothConnected = false
    private var isOnHold = false
    private var videoLaunched:Boolean = false
    private var isMicrophoneOn = true
    private var context: Context? = null
    private var isCallEnded = false
    private var callDuration = 0
    private var callContact: CallContact? = null
    private var callContactAvatar: Bitmap? = null
    private var proximityWakeLock: PowerManager.WakeLock? = null
    private var callTimer = Timer()
    private var numberOfPeer: String? = null
    private var telecomID: String? = null
    private var callState: Int = 0
    private var conferenceStarted: Boolean = false
    private var monitor:networkMonitor? = null
    private var bluetoothObserver:bluetoothHelper? = null
    private var preferenceManager:PreferenceManager? = null
    private var peerFCMToken:String? = null
    private var initCall: Call? = null
    var terminatedAbnormally = false
    private val CALL_INIT_NOTIFICATION_ID = 10
    private var reconnectLock:Boolean = false
    private var reconnectResponseLock:Boolean = false
    var contactCurrent:CallContact? = null
    var avatar:Bitmap? = null
    var reinstatedAvatar:Bitmap? = null
    var currentTouchedView:View?  = null
    private lateinit var gestureDetector: GestureDetector
    private  var callType:Boolean=false
    private  var message:String=""
    private lateinit var rcdImage:ImageView
    private  var imageData:String=""
    private lateinit var rcdLinkedin:ImageView
    private lateinit var rcdInstagram:ImageView
    private lateinit var rcdFacebook:ImageView
    private lateinit var rcdTwitter:ImageView
    private lateinit var rcdWebsite:ImageView
    private var linkedInURL:String=""
    private var instagramURL:String=""
    private var facebookURL:String=""
    private var twitterURL:String=""
    private var websiteURL:String=""







    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        val bundle= intent.extras
            numberOfPeer = bundle!!.getString("callingNumber","")
            telecomID = bundle!!.getString("telecomCallID","")
        thisContext=applicationContext
        thisActivity=this
        context = applicationContext
        preferenceManager = PreferenceManager(context)
        val rcdCallState:Boolean=preferenceManager!!.getBoolean(Constants.RCD_CALL_TYPE)
        if(rcdCallState){
            callType=true
            message=preferenceManager!!.getString(Constants.RCD_CALL_MESSAGE)
            imageData=preferenceManager!!.getString(Constants.RCD_CALL_BITMAP_DATA)
        }
        else{
            callType=false
        }
        setContentView(R.layout.activity_call)
        rcdImage=findViewById(R.id.rcd_image)
        rcdLinkedin=findViewById(R.id.rcd_linkedin)
        rcdInstagram=findViewById(R.id.rcd_instagram)
        rcdFacebook=findViewById(R.id.rcd_facebook)
        rcdTwitter=findViewById(R.id.rcd_twitter)
        rcdWebsite=findViewById(R.id.rcd_website)
        updateTextColors(call_holder)
        audioManager.mode = AudioManager.MODE_IN_CALL
        initCall = CallManager.getCall(telecomID)
        if (initCall == null) {
            finish()
        }
        if(CallManager.foregroundCall==null) {
            CallManager.foregroundCall = initCall
        }
        gestureDetector = GestureDetector(thisActivity, GestureListener())
        initButtons()
        CallManager.getCallContact(applicationContext, initCall!!) { contact ->
            callContact = contact
          //   callContactAvatar = getCallContactAvatar(callContact!!)
            callContactAvatar = getClearCallContactAvatar(callContact!!)
            runOnUiThread {
                setupNotification()
                updateOtherPersonsInfo()
                checkCalledSIMCard(telecomID!!)
                rcdControl()
            }
        }

        addLockScreenFlags()
        CallManager.registerCallback(callCallback, telecomID!!)
        callState = CallManager.getState(telecomID!!)
        checkBluetooth()
        updateCallState(callState)
        getConferenceReference()
        monitor = networkMonitor(this, context)
        preferenceManager = PreferenceManager(context!!)

    }

   private fun rcdControl(){
       if(callType){
           if(message!=""){
               rcd_message_label.beVisible()
               rcd_message_content.beVisible()
               rcd_message_label.text="Text Message :"
               rcd_message_content.text=message
           }
           else{
               rcd_message_label.beGone()
               rcd_message_content.beGone()
           }
           if(imageData!=""){
               stringToImage(imageData)
           }
           else{
               rcdImage.beGone()
           }
           socialURLControl()
           rcdDataUpload()

       }
       else{
           rcd_message_label.beGone()
           rcd_message_content.beGone()
           rcdImage.beGone()
           rcdLinkedin.beGone()
           rcdInstagram.beGone()
           rcdFacebook.beGone()
           rcdTwitter.beGone()
           rcdWebsite.beGone()

       }
   }

   private fun socialURLControl(){
       val linkedinState:Boolean=preferenceManager!!.getBoolean(Constants.RCD_LINKEDIN_URL_FLAG)
       linkedInURL = if(linkedinState){
           rcdLinkedin.beVisible()
           preferenceManager!!.getString(Constants.RCD_LINKEDIN_URL)
       } else{
           rcdLinkedin.beGone()
           ""
       }
       val instagramState:Boolean=preferenceManager!!.getBoolean(Constants.RCD_INSTA_URL_FLAG)
       instagramURL = if(instagramState){
           rcdInstagram.beVisible()
           preferenceManager!!.getString(Constants.RCD_INSTA_URL)
       } else{
           rcdInstagram.beGone()
           ""
       }
       val facebookState:Boolean=preferenceManager!!.getBoolean(Constants.RCD_FACEBOOK_URL_FLAG)
       facebookURL = if(facebookState){
           rcdFacebook.beVisible()
           preferenceManager!!.getString(Constants.RCD_FACEBOOK_URL)
       } else{
           rcdFacebook.beGone()
           ""
       }
       val twitterState:Boolean=preferenceManager!!.getBoolean(Constants.RCD_TWITTER_URL_FLAG)
       twitterURL = if(twitterState){
           rcdTwitter.beVisible()
           preferenceManager!!.getString(Constants.RCD_TWITTER_URL)
       } else {
           rcdTwitter.beGone()
           ""
       }
       val websiteState:Boolean=preferenceManager!!.getBoolean(Constants.RCD_WEB_URL_FLAG)
       websiteURL = if(websiteState){
           rcdWebsite.beVisible()
           preferenceManager!!.getString(Constants.RCD_WEB_URL)
       } else {
           rcdWebsite.beGone()
           ""
       }
   }

  private fun rcdDataUpload(){
      val database = FirebaseFirestore.getInstance()
      val receiver = HashMap<String, Any>()
      receiver[Constants.RCD_CALL_REASON] = message
      receiver[Constants.RCD_IMAGE] =imageData
      receiver[Constants.RCD_LINKEDIN_URL]=linkedInURL
      receiver[Constants.RCD_INSTA_URL]=instagramURL
      receiver[Constants.RCD_FACEBOOK_URL]=facebookURL
      receiver[Constants.RCD_TWITTER_URL]=twitterURL
      receiver[Constants.RCD_WEB_URL]=websiteURL
      database.collection(Constants.KEY_COLLECTION_RCD)
          .add(receiver)
          .addOnSuccessListener { documentReference: DocumentReference ->
              Toast.makeText(
                  this,
                  "RCD Data Upload Success",
                  Toast.LENGTH_SHORT
              ).show()
          }
          .addOnFailureListener { e: java.lang.Exception? ->
              Toast.makeText(
                  this,
                  "Error: RCD Data Upload Failure",
                  Toast.LENGTH_SHORT
              ).show()

          }
  }
   private fun stringToImage(string: String){
       if(string!=""){
           val bytes: ByteArray = Base64.decode(string, Base64.DEFAULT)
           val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
           rcdImage.setImageBitmap(bitmap)
       }

   }

    class GestureListener : GestureDetector.SimpleOnGestureListener(),PositionInterface{
        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100
        override fun onDown(e: MotionEvent?): Boolean {
            return true
        }
        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            var result = false
            try {
                var diffY = e2.y - e1.y
                var diffX = e2.x - e1.x
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight()
                        } else {
                            onSwipeLeft()

                        }
                        result = true
                    }
                } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeBottom()
                    } else {
                        onSwipeTop()
                    }
                    result = true
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
            return result
        }

        override fun onSwipeRight() {
        }
        override fun onSwipeLeft() {
        }
        override fun onSwipeTop() {
            if(thisActivity.currentTouchedView== thisActivity.call_decline) {
                thisActivity.terminateCall()
            }else{
                thisActivity.acceptCall()
            }
        }
        override fun onSwipeBottom() {
        }

    }
    override fun onStart() {
        super.onStart()
        val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notifications = mNotificationManager.activeNotifications
        for (notification in notifications) {
            if (notification.id == 10) {
                mNotificationManager.cancel(CALL_INIT_NOTIFICATION_ID)
            }
        }

    }


    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        checkBluetooth()
        if(CallManager.foregroundCall==null && CallManager.backgroundCall==null) {
            finish()
        }else if(CallManager.foregroundCall!=null && (CallManager.foregroundCall!!.state == Call.STATE_ACTIVE || CallManager.foregroundCall!!.state == Call.STATE_HOLDING)){
            try {
                callTimer.cancel()
                callTimer = Timer()
                updateCallduration()
                callTimer.scheduleAtFixedRate(getCallTimerUpdateTask(), 1000, 1000)
            } catch (ignored: Exception) {
            }
            if(CallManager.foregroundCall!!.state == Call.STATE_HOLDING){
                CallManager.foregroundCall!!.unhold()
            }
        }else {
            /* Nothing */
        }
    }



    override fun onBackPressed() {
        var callIdentifier:String = CallManager.getIdentifier(CallManager.foregroundCall!!)
        if (dialpad_wrapper.isVisible()) {
            dialpad_wrapper.beGone()
            return
        } else {

        }
        if (CallManager.getState(callIdentifier!!) == Call.STATE_DIALING) {
            terminateCall()
        }
    }

    companion object {
        var newCall:Call? = null
        var newCallIdentifier:String? = null
        var newCallHandle: String? = null
        var newCallState: Int = 0
        var newCallContact:CallContact? = null
        var newCallContactAvatar: Bitmap? = null
        @SuppressLint("StaticFieldLeak")
        lateinit var thisContext : Context
        @SuppressLint("StaticFieldLeak")
        lateinit var thisActivity: CallActivity
        fun handleIncomingActive(callID:String, handle:String) {
            val incomingActiveCall: Call? = CallManager.getCall(callID)
            if(incomingActiveCall!=null) {
                /* Here, CallManager.foregroundCall must never be equal to null */
                CallManager.backgroundCall = CallManager.foregroundCall
                CallManager.foregroundCall = incomingActiveCall
                thisActivity.ongoing_call_holder.beGone()
             /*   thisActivity.swap_merge_holder.beGone()
                thisActivity.merge_holder.beGone()
                thisActivity.add_call_holder.beGone() */
                newCall = incomingActiveCall
                newCallIdentifier = callID
                newCallHandle = handle
                CallManager.getCallContact(thisContext!!, incomingActiveCall!!) { contact ->
                    newCallContact = contact
                    newCallContactAvatar= thisActivity.getClearCallContactAvatar(newCallContact!!)
                    thisActivity.runOnUiThread {
                        thisActivity.updateOtherPersonsInfo()
                        thisActivity.checkCalledSIMCard(callID!!)
                        thisActivity.callTimer.cancel()
                    }
                }
                thisActivity.addLockScreenFlags()
                CallManager.registerCallback(thisActivity.callCallback, callID!!)
                newCallState = CallManager.getState(callID!!)
                thisActivity.updateCallState(newCallState)
                thisActivity.updatenotificationview()
            }
        }

        fun handleAddActive(callID: String, handle: String) {
            val callToAdd: Call? = CallManager.getCall(callID)
            if(callToAdd!=null) {
                /* relaunch the GUI */
                CallManager.backgroundCall = CallManager.foregroundCall
                CallManager.foregroundCall = callToAdd
                thisActivity.ongoing_call_holder.beGone()
           /*   thisActivity.swap_merge_holder.beGone()
                thisActivity.add_call_holder.beGone()
                thisActivity.merge_holder.beGone()
                thisActivity.conference_management_holder.beGone() */
                newCall = callToAdd
                newCallHandle = handle
                newCallIdentifier = callID
                CallManager.getCallContact(thisContext!!, callToAdd!!) { contact ->
                    newCallContact = contact
                    newCallContactAvatar= thisActivity.getClearCallContactAvatar(newCallContact!!)

                    thisActivity.runOnUiThread {
                        thisActivity.updateOtherPersonsInfo()
                        thisActivity.checkCalledSIMCard(callID!!)
                    }
                }
                thisActivity.addLockScreenFlags()
                CallManager.registerCallback(thisActivity.callCallback, callID!!)
                newCallState = CallManager.getState(callID!!)
                thisActivity.updatenotificationview()
                thisActivity.updateCallState(newCallState)
            }

        }

        fun handleConferenceAdd(call: Call, callID:String) {
            var conferenceCall:Call? = null
            if(call!=null){
                conferenceCall = call
                /* There should be no background call now. Also, the foreground call should be the conference
                call, until and unless a new call is added
                 */
                CallManager.backgroundCall = null
                CallManager.foregroundCall = conferenceCall
                thisActivity.ongoing_call_holder.beGone()
             /*   thisActivity.swap_merge_holder.beGone()
                thisActivity.add_call_holder.beGone() */
                newCall = null
                newCallHandle = null
                newCallIdentifier = null
                newCallContactAvatar = null
                newCallContact = null
                CallManager.getCallContact(thisContext!!, call!!) { contact ->
                    thisActivity.contactCurrent = contact
                    thisActivity.avatar = thisActivity.getClearCallContactAvatar(thisActivity.contactCurrent!!)
                    thisActivity.runOnUiThread {
                        thisActivity.updateOtherPersonsInfo()
                        thisActivity.checkCalledSIMCard(callID!!)
                    }
                }
                thisActivity.addLockScreenFlags()
                CallManager.registerCallback(thisActivity.callCallback, callID!!)
                newCallState = CallManager.getState(callID!!)
                thisActivity.updateCallState(newCallState)
                thisActivity.updatenotificationview()
                var conferenceCallList: List<Call> = CallManager.activeConferenceCall!!.children
                if(!conferenceCallList.isEmpty()) {
                    conferenceCallList.forEachIndexed {index, call ->
                        call.unhold()
                    }
                }
            }
        }

    }

    private fun terminateCall() {
        var foregroundCallIdentifier:String? = null
        if(CallManager.activeConferenceCall!=null && CallManager.activeConferenceCall == CallManager.foregroundCall){
            var childCallList: List<Call> = mutableListOf<Call>()
            childCallList = CallManager.activeConferenceCall!!.children
            if(!childCallList.isEmpty()) {
                childCallList.forEachIndexed {index, call ->
                    if(call!=null){
                        var identifier = CallManager.getIdentifier(call)
                        CallManager.reject(identifier)
                    }
                }
            }
            if(CallManager.activeConferenceCall!= null) {
                if(CallManager.activeConferenceCall!!.state == Call.STATE_HOLDING || CallManager.activeConferenceCall!!.state == Call.STATE_ACTIVE){
                    CallManager.reject(CallManager.activeConferenceIdentfier!!)
                    CallManager.activeConferenceCall = null
                    CallManager.activeConferenceIdentfier = null
                    CallManager.isConferenceCall = false
                }
            }
            return
        }else if(CallManager.foregroundCall!=null && CallManager.backgroundCall!=null) {
            foregroundCallIdentifier = CallManager.getIdentifier(CallManager.foregroundCall!!)
            CallManager.reject(foregroundCallIdentifier)
        }else if(CallManager.backgroundCall==null) {
            foregroundCallIdentifier = CallManager.getIdentifier(CallManager.foregroundCall!!)
            CallManager.reject(foregroundCallIdentifier)
        }else {
            Log.d(Constants.TAG, "onTerminated")
            finish()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.N)
    private fun initButtons() {
//        call_decline.setOnClickListener {
//            terminateCall()
//        }
        call_decline.setOnTouchListener { p0, p1 ->
            currentTouchedView = call_decline
            call_decline.animate()
                .translationY(-400f)
                .setDuration(500)
                .alpha(0f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        call_decline.setAlpha(1f)
                        call_decline.setTranslationY(0f)
                        incoming_arrow_holder_decline.beInvisible()
                        incoming_arrow_holder_accept.beInvisible()
                    }

                    override fun onAnimationStart(animator: Animator){
                        incoming_arrow_holder_accept.beInvisible()
                        incoming_arrow_holder_decline.beVisible()
                    }

                })
            gestureDetector.onTouchEvent(p1)
        }
//        call_accept.setOnClickListener {
//            acceptCall()
//        }
        call_accept.setOnTouchListener { p0, p1 ->
            currentTouchedView = call_accept
            call_accept.animate()
                .translationY(-400f)
                .setDuration(500)
                .alpha(0f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        call_accept.setAlpha(1f)
                        call_accept.setTranslationY(0f)
                        incoming_arrow_holder_decline.beInvisible()
                        incoming_arrow_holder_accept.beInvisible()
                    }

                    override fun onAnimationStart(animation:Animator) {
                        incoming_arrow_holder_decline.beInvisible()
                        incoming_arrow_holder_accept.beVisible()
                    }
                })
            gestureDetector.onTouchEvent(p1)
        }

        headset.setOnClickListener {
            onHeadSet()
        }

        bluetooth.setOnClickListener {
            onBluetooth()
        }

        speaker.setOnClickListener {
            onSpeaker()
        }

        exitoutputHolder.setOnClickListener {
            outputContainer.beGone()
        }

       callBack.setOnClickListener {
           selectMessage(it as TextView)
       }

       callLater.setOnClickListener {
           selectMessage(it as TextView)
       }

       cantTalkNow.setOnClickListener {
           selectMessage(it as TextView)
       }

       customMessage.setOnClickListener {
           selectMessage(it as TextView)
       }


        call_toggle_microphone.setOnClickListener {
            toggleMicrophone()
        }

        call_toggle_speaker.setOnClickListener {
            toggleSpeaker()
        }

        call_dialpad.setOnClickListener {
            toggleDialpadVisibility()
        }

        dialpad_close.setOnClickListener {
            dialpad_wrapper.beGone()
        }

        add_call_button.setOnClickListener {
            handleAddCall()
        }

        videocallbutton.setOnClickListener {
            launchvideocallactivity()
        }

        callHoldButton.setOnClickListener {
            toggleHold()
        }

        call_end.setOnClickListener {
            terminateCall()
        }
        swap_call_button.setOnClickListener {
            swapCurrentCall()
        }

        merge_call_button.setOnClickListener {
            mergeCall()
        }

        manage_conference.setOnClickListener {
            launchConferenceManagement()
        }

        accept_and_end_end.setOnClickListener {
            acceptAndEnd()
        }

        accept_and_end_accept.setOnClickListener {
            acceptAndEnd()
        }

        call_decline_second.setOnClickListener {
            declineSecondCall()
        }

        hold_and_accept_accept.setOnClickListener {
            holdAndAccept()
        }

        hold_and_accept_hold.setOnClickListener {
            holdAndAccept()
        }

        sendMessage.setOnClickListener {
            sendMessageClicked()
        }

        cancelSendMessage.setOnClickListener {
            cancelSendMessageClicked()
        }

        declineMessage.setOnClickListener {
            clickDeclineMessage()
        }

        call_holder.setOnClickListener {
            if(rejectWithSMS.visibility == View.VISIBLE){
                rejectWithSMS.beGone()
            }
        }

        dialpad_0_holder.setOnClickListener { dialpadPressed('0') }
        dialpad_1_holder.setOnClickListener { dialpadPressed('1') }
        dialpad_2_holder.setOnClickListener { dialpadPressed('2') }
        dialpad_3_holder.setOnClickListener { dialpadPressed('3') }
        dialpad_4_holder.setOnClickListener { dialpadPressed('4') }
        dialpad_5_holder.setOnClickListener { dialpadPressed('5') }
        dialpad_6_holder.setOnClickListener { dialpadPressed('6') }
        dialpad_7_holder.setOnClickListener { dialpadPressed('7') }
        dialpad_8_holder.setOnClickListener { dialpadPressed('8') }
        dialpad_9_holder.setOnClickListener { dialpadPressed('9') }

        dialpad_0_holder.setOnLongClickListener { dialpadPressed('+'); true }
        dialpad_asterisk_holder.setOnClickListener { dialpadPressed('*') }
        dialpad_hashtag_holder.setOnClickListener { dialpadPressed('#') }

        dialpad_wrapper.setBackgroundColor(config.backgroundColor)
        customMessageCardView.setBackgroundColor(config.cardViewColor)
        rejectWithSMS.setBackgroundColor(config.backgroundColor)
        arrayOf(
            call_toggle_microphone,
            call_toggle_speaker,
            add_call_button,
            swap_call_button,
            merge_call_button,
            callHoldButton,
            manage_conference,
            videocallbutton,
            declineMessage,
            call_dialpad,
            dialpad_close,
            call_sim_image
        ).forEach {
            it.applyColorFilter(config.textColor)
        }

        call_sim_id.setTextColor(config.textColor.getContrastColor())
    }

    private fun clickDeclineMessage() {
        rejectWithSMS.beVisible()
    }

    private fun selectMessage(view: TextView) {
        /* The call to be rejected with message should always be the foreground call */
        var foregroundCallIdentifier:String = CallManager.getIdentifier(CallManager.foregroundCall!!)
        if(view == callBack){
            CallManager.rejectWithMessage("I'll call you right back.", foregroundCallIdentifier)
        }else if(view == callLater){
            CallManager.rejectWithMessage("I'll call you later.", foregroundCallIdentifier)
        }else if(view == cantTalkNow){
            CallManager.rejectWithMessage("I can't talk now.", foregroundCallIdentifier)
        }else if(view == customMessage){
            customMessageCardView.beVisible()
            customMessageHolder.requestFocus()
        }else{
            return
        }
    }


    private fun cancelSendMessageClicked(){
        customMessageCardView.beGone()
        if(rejectWithSMS.visibility == View.GONE || rejectWithSMS.visibility == View.VISIBLE){
            rejectWithSMS.beVisible()
        }
    }

    private fun sendMessageClicked(){
        var foregroundCallIdentifier:String = CallManager.getIdentifier(CallManager.foregroundCall!!)
        var text = customMessageHolder.text.toString()
        if(text!=null){
            CallManager.rejectWithMessage(text, foregroundCallIdentifier)
        }else{
            CallManager.rejectWithMessage("I'll call you later", foregroundCallIdentifier)
        }

        customMessageCardView.beGone()
        rejectWithSMS.beGone()
    }



    private fun acceptAndEnd() {
        var backgroundCallIdentifier:String = CallManager.getIdentifier(CallManager.backgroundCall!!)
        var foregroundCallIdentifier:String = CallManager.getIdentifier(CallManager.foregroundCall!!)
        if(foregroundCallIdentifier!=null){
            CallManager.accept(foregroundCallIdentifier)
        }
        if(backgroundCallIdentifier!=null){
            CallManager.reject(backgroundCallIdentifier)
        }
    }

    private fun holdAndAccept() {
        var foregroundCallIdentifier:String = CallManager.getIdentifier(CallManager.foregroundCall!!)
        if(CallManager.backgroundCall!=null) CallManager.backgroundCall!!.hold()
        if(foregroundCallIdentifier!=null) {
            CallManager.accept(foregroundCallIdentifier)
        }
    }

    private fun declineSecondCall() {
        var foregroundCallIdentifier: String = CallManager.getIdentifier(CallManager.foregroundCall!!)
        CallManager.reject(foregroundCallIdentifier)
    }

    private fun launchConferenceManagement() {
        val fragment = conferenceManagement()
        var transaction = supportFragmentManager.beginTransaction()
        container.beVisible()
        transaction.replace(R.id.container, fragment)
        transaction.commit()
    }

    private fun dialpadPressed(char: Char) {
        var callIdentifier = CallManager.getIdentifier(CallManager.foregroundCall!!)
        CallManager.keypad(char, callIdentifier!!)
        dialpad_input.addCharacter(char)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun launchvideocallactivity() {
        /* place holder for service provider video, will be added later */
    }


    private fun getConferenceReference() {
        var currentCall: Call? = null
        currentCall = CallManager.getCall(telecomID)
        if (currentCall != null && currentCall.details.hasProperty(PROPERTY_CONFERENCE)) {
            CallManager.conferenceCallInstance = this
        } else if (currentCall!!.parent == CallManager.activeConferenceCall && CallManager.isConferenceCall) {
            conferenceStarted = true
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkBluetooth(){
        var bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if(bluetoothManager!=null){
            var adapter = bluetoothManager.adapter
            if(adapter!=null && adapter.isEnabled){
               var state =  adapter.getProfileConnectionState(BluetoothProfile.HEADSET)
                if(state == BluetoothProfile.STATE_CONNECTED){
                    isBluetoothConnected = true
                    setCorrectOutput()
                }else{
                    isBluetoothConnected = false
                    tholder2.setText("Speaker")
                    onHeadSet()
                }

            }
        }
    }


    private fun toggleSpeaker() {
        if(isBluetoothConnected){
            if(outputContainer.visibility == View.GONE || outputContainer.visibility == View.INVISIBLE){
                outputContainer.beVisible()
                return
            }
        }

        var callIdentifier = CallManager.getIdentifier(CallManager.foregroundCall!!)
        isSpeakerOn = !isSpeakerOn
        val drawable =
            if (isSpeakerOn) R.drawable.ic_speaker_on_grey_test else R.drawable.ic_speaker_on_vector
        call_toggle_speaker.setImageDrawable(getDrawable(drawable))
        if(isSpeakerOn){
            var factor  = call_toggle_speaker.resources.displayMetrics.density
            call_toggle_speaker.layoutParams.height = (70 * factor).toInt()
            call_toggle_speaker.layoutParams.width =  (70 * factor).toInt()
            call_toggle_speaker.requestLayout()
        }else{
            var factor  = call_toggle_speaker.resources.displayMetrics.density
            call_toggle_speaker.layoutParams.height = (50 * factor).toInt()
            call_toggle_speaker.layoutParams.width =  (50 * factor).toInt()
            call_toggle_speaker.requestLayout()
        }
        audioManager.isSpeakerphoneOn = isSpeakerOn

        val newRoute =
            if (isSpeakerOn) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
        /*  CallManager.inCallService?.setAudioRoute(newRoute) */

        CallManager.setRoute(newRoute, callIdentifier!!)
    }

    private fun onBluetooth(){
        outputContainer.beGone()
        if(isBluetoothConnected) setCorrectOutput()
        else return
    }

    private fun onSpeaker() {
        outputContainer.beGone()
        var callIdentifier = CallManager.getIdentifier(CallManager.foregroundCall!!)
        isSpeakerOn = true
        val drawable =
            if (isSpeakerOn) R.drawable.ic_speaker_on_grey_test else R.drawable.ic_speaker_on_vector
        call_toggle_speaker.setImageDrawable(getDrawable(drawable))
        audioManager.isSpeakerphoneOn = isSpeakerOn

        val newRoute =
            if (isSpeakerOn) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
        /*  CallManager.inCallService?.setAudioRoute(newRoute) */

        CallManager.setRoute(newRoute, callIdentifier!!)
    }

    private fun onHeadSet() {
        outputContainer.beGone()
        var callIdentifier = CallManager.getIdentifier(CallManager.foregroundCall!!)
        isSpeakerOn = false
        val drawable =
            if (isSpeakerOn) R.drawable.ic_speaker_on_grey_test else R.drawable.ic_speaker_on_vector
        call_toggle_speaker.setImageDrawable(getDrawable(drawable))
        audioManager.isSpeakerphoneOn = isSpeakerOn

        val newRoute =
            if (isSpeakerOn) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
        /*  CallManager.inCallService?.setAudioRoute(newRoute) */

        CallManager.setRoute(newRoute, callIdentifier!!)

    }

    private fun setCorrectOutput() {
        var callIdentifier = CallManager.getIdentifier(CallManager.foregroundCall!!)
        if(isBluetoothConnected){
            call_toggle_speaker.setImageDrawable(getDrawable(R.drawable.ic_bluetooth))
            val newRoute =
                CallAudioState.ROUTE_BLUETOOTH
                CallManager.setRoute(newRoute, callIdentifier!!)
                audioManager.isBluetoothScoOn
        }else return
    }

    private fun toggleHold() {
        var callIdentifier = CallManager.getIdentifier(CallManager.foregroundCall!!)
        isOnHold = !isOnHold
        val drawable =
            if(isOnHold) R.drawable.ic_call_hold_grey else R.drawable.ic_call_hold
        callHoldButton.setImageDrawable(getDrawable(drawable))
        if(isOnHold){
            var factor  = callHoldButton.resources.displayMetrics.density
            callHoldButton.layoutParams.height = (70 * factor).toInt()
            callHoldButton.layoutParams.width =  (70 * factor).toInt()
            callHoldButton.requestLayout()
        }else{
            var factor  = callHoldButton.resources.displayMetrics.density
            callHoldButton.layoutParams.height = (50 * factor).toInt()
            callHoldButton.layoutParams.width =  (50 * factor).toInt()
            callHoldButton.requestLayout()
        }
        if(isOnHold)CallManager.foregroundCall!!.hold() else CallManager.foregroundCall!!.unhold()
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

    }

    private fun toggleMicrophone() {
        var callIdentifier = CallManager.getIdentifier(CallManager.foregroundCall!!)
        isMicrophoneOn = !isMicrophoneOn
        val drawable =
            if (isMicrophoneOn){
                R.drawable.ic_microphone_vector
            } else R.drawable.ic_microphone_grey
        call_toggle_microphone.setImageDrawable(getDrawable(drawable))
        if(isMicrophoneOn){
            var factor  = call_toggle_microphone.resources.displayMetrics.density
            call_toggle_microphone.layoutParams.height = (50 * factor).toInt()
            call_toggle_microphone.layoutParams.width =  (50 * factor).toInt()
            call_toggle_microphone.requestLayout()
        }else{
            var factor  = call_toggle_microphone.resources.displayMetrics.density
            call_toggle_microphone.layoutParams.height = (70 * factor).toInt()
            call_toggle_microphone.layoutParams.width =  (70 * factor).toInt()
            call_toggle_microphone.requestLayout()
        }
        audioManager.isMicrophoneMute = !isMicrophoneOn
        /*  CallManager.inCallService?.setMuted(!isMicrophoneOn) */
        CallManager.setMute(!isMicrophoneOn, callIdentifier!!)
    }

    private fun toggleDialpadVisibility() {
        if (dialpad_wrapper.isVisible()) {
            dialpad_wrapper.beGone()
        } else {
            dialpad_wrapper.beVisible()
        }
    }


    private fun handleAddCall() {
        var callIdentifier = CallManager.getIdentifier(CallManager.foregroundCall!!)
        /* care has to be take to ensure that the AddCall button isn't visible
        if there are more than two calls - one held and one active
         */
        if(CallManager.activeConferenceCall!=null && newCall!=null) {
            /* This is not a correct call state to be in, as
            you cannot have an active conference call, and a
            call that is add or remove pending with an option for
            adding another call
             */
            add_call_button.beGone()
        }else if(CallManager.activeConferenceCall!=null) {
            /* If there is an active conference, place the entire conference on hold */
            var conferenceCallList:List<Call> = CallManager.activeConferenceCall!!.children
            if(!conferenceCallList.isEmpty()) {
                conferenceCallList.forEachIndexed {index, call ->
                    conferenceCallList.get(index)!!.hold()
                }
            }
            var thisCall:Call = CallManager.activeConferenceCall!!
            if(thisCall!=null)thisCall.hold()
            callTimer.cancel()
            val addCallIntent = Intent(this, MainActivity::class.java)
            startActivity(addCallIntent)
        }else {
            var currentCall:Call? = null
            if(CallManager.callMap!!.containsKey(callIdentifier)) {
                currentCall = CallManager.getCall(callIdentifier)
            }
            currentCall!!.hold()
            callTimer.cancel()
            val addCallIntent = Intent(this, MainActivity::class.java)
            /*addCallIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) */
            startActivity(addCallIntent)
        }
    }






    private fun swapCurrentCall() {
        if(CallManager.foregroundCall!=null && CallManager.backgroundCall!=null){
          /* Foreground Call will always present the plane on which the swap button is pressed */
            val call = CallManager.foregroundCall!!
            CallManager.foregroundCall!!.hold()
            CallManager.backgroundCall!!.unhold()
            CallManager.foregroundCall = CallManager.backgroundCall
            CallManager.backgroundCall = call
            reinstateCallUI(CallManager.foregroundCall!!)
        }else{
            return
        }
    }

    private fun reinstateCallUI(call:Call) {

        var callToRevive: Call = call
        var callIdentifier = CallManager.getIdentifier(callToRevive!!)
        if(callToRevive!=null) {
            if(callToRevive == newCall) {
                CallManager.getCallContact(applicationContext, callToRevive!!) { contact ->
                    newCallContact = contact
                    newCallContactAvatar = getClearCallContactAvatar(newCallContact!!)
                    contactCurrent = newCallContact
                    runOnUiThread {
                        updateOtherPersonsInfo()
                        checkCalledSIMCard(callIdentifier!!)
                    }
                }
            }else if(callToRevive == initCall) {
                CallManager.getCallContact(applicationContext, callToRevive!!) { contact ->
                    callContact = contact
                    avatar = getClearCallContactAvatar(callContact!!)
                    contactCurrent = callContact
                    runOnUiThread {
                        updateOtherPersonsInfo()
                        checkCalledSIMCard(callIdentifier!!)
                    }
                }
            }else {
                CallManager.getCallContact(applicationContext, callToRevive!!) { contact ->
                    contactCurrent = contact
                    avatar = getClearCallContactAvatar(contactCurrent!!)
                    runOnUiThread {
                        updateOtherPersonsInfo()
                        checkCalledSIMCard(callIdentifier!!)
                    }
                }
            }
            addLockScreenFlags()
            /* CallManager.registerCallback(callCallback, telecomID!!) */
            callState = CallManager.getState(callIdentifier!!)
            updateCallState(callState)
            try {
                callTimer.cancel()
                callTimer = Timer()
                updateCallduration()
                callTimer.scheduleAtFixedRate(getCallTimerUpdateTask(), 1000, 1000)
            } catch (ignored: Exception) {
            }
            updateNotification()
        }else{
            finish()
        }
    }



    private fun mergeCall() {
        if(CallManager.activeConferenceCall!=null && CallManager.activeConferenceCall == CallManager.backgroundCall){
            var conferenceCallList: List<Call> = CallManager.foregroundCall!!.conferenceableCalls
            /*    if (!conferenceCallList.isEmpty()) {
                    conferenceCallList.forEachIndexed {index, call ->
                        CallManager.foregroundCall!!.conference(conferenceCallList.get(index))!!
                    }
                    CallManager.isConferenceCall = true
                }else{
                    Log.d(Constants.TAG, "mergefailed!!!")
                } */
            CallManager.activeConferenceCall!!.conference(CallManager.foregroundCall)
            CallManager.isConferenceCall=true
            handleConferenceAdd(CallManager.activeConferenceCall!!, CallManager.activeConferenceIdentfier!!)
        }
        else if(CallManager.foregroundCall!=null && CallManager.backgroundCall!=null) {
            var conferenceCallList: List<Call> = CallManager.foregroundCall!!.conferenceableCalls
            if (!conferenceCallList.isEmpty()) {
                conferenceCallList.forEachIndexed {index, call ->
                    CallManager.foregroundCall!!.conference(conferenceCallList.get(index))!!
                }
                CallManager.isConferenceCall = true
            }else{
                Log.d(Constants.TAG, "mergefailed!!!")
            }
        }else{
            Log.d(Constants.TAG, "mergefailed")
        }
    }



    private fun updateOtherPersonsInfo() {
        var regionString:String? = null
        if (callContact == null) {
            return
        }
        if(newCall!=null && newCall == CallManager.foregroundCall!!) {
            /* This is the first check that ought to be run; basically, to check if there is
            a new call over an already existing one
             */
            caller_name_label.text =
                if(newCallContact!!.name.isNotEmpty()) newCallContact!!.name else getString(R.string.unknown_caller)
                if (newCallContact!!.number.isNotEmpty() && newCallContact!!.number != newCallContact!!.name) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    regionString =  parseNumber(newCallContact!!.number)
                    if(!regionString.equals("invalidNumber")) {
                        caller_number_label.beVisible()
                        caller_number_label.text =
                            newCallContact!!.number + "," + " " + regionString
                    }else{
                        caller_number_label.beVisible()
                        caller_number_label.text = newCallContact!!.number
                    }
                }
            } else {
                if(!caller_name_label.text.equals(getString(R.string.unknown_caller)) && newCallContact!!.number.isNotEmpty()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        regionString = parseNumber(newCallContact!!.number)
                        if(!regionString.equals("invalidNumber")) {
                            caller_number_label.beVisible()
                            caller_number_label.text =
                                 regionString
                        }else{
                            caller_number_label.beGone()
                        }
                    }
                }

            }

            if (newCallContactAvatar != null) {

                if(caller_avatar.visibility==View.GONE || caller_avatar.visibility==View.INVISIBLE){
                    caller_avatar.beVisible()
                }
                caller_avatar.setImageBitmap(newCallContactAvatar)
            }else{
                if(caller_avatar.visibility == View.VISIBLE){
                    caller_avatar.beGone()
                }
            }
            return
        }

        if(CallManager.activeConferenceCall!=null && CallManager.activeConferenceCall == CallManager.foregroundCall) {
            caller_number_label.beGone()
            caller_name_label.text = "On Conference"
            if(caller_avatar.visibility == View.VISIBLE) {
                caller_avatar.beGone()
            }
            return
        }

        caller_name_label.text =
            if (callContact!!.name.isNotEmpty()) callContact!!.name else getString(R.string.unknown_caller)
        if (callContact!!.number.isNotEmpty() && callContact!!.number != callContact!!.name) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                regionString =  parseNumber(callContact!!.number)
                if(!regionString.equals("invalidNumber")) {
                    caller_number_label.beVisible()
                    caller_number_label.text = callContact!!.number + "," + " " + regionString
                }else{
                    caller_number_label.beVisible()
                    caller_number_label.text = callContact!!.number
                }
            }
        } else {
            if(!caller_name_label.text.equals(getString(R.string.unknown_caller)) && callContact!!.number.isNotEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    regionString = parseNumber(callContact!!.number)
                    if(!regionString.equals("invalidNumber")) {
                        caller_number_label.beVisible()
                        caller_number_label.text =
                             regionString
                    }else{
                        caller_number_label.beVisible()
                        caller_number_label.beGone()
                    }
                }
            }
          /*  caller_number_label.beGone() */
        }

        if (callContactAvatar != null) {
            if(caller_avatar.visibility==View.GONE || caller_avatar.visibility==View.INVISIBLE){
                caller_avatar.beVisible()
            }
            caller_avatar.setImageBitmap(callContactAvatar)
        }
        else {
            if (caller_avatar.visibility == View.VISIBLE) {
                 caller_avatar.beGone()
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun checkCalledSIMCard(telecomID:String) {
        var currentCall: Call = CallManager.foregroundCall!!
        if(currentCall!=null){
            try{
                val accounts = telecomManager.callCapablePhoneAccounts
                if (accounts.size > 1) {
                    accounts.forEachIndexed { index, account ->
                        if (account == currentCall?.details?.accountHandle) {
                            call_sim_id.text = "${index + 1}"
                            call_sim_id.beVisible()
                            call_sim_image.beVisible()
                        }
                    }
                }
            }catch (ignored:java.lang.Exception) {

            }
        }
    }

    private fun updateCallState(state: Int) {
        var callIdentifier:String? = null
        if(CallManager.foregroundCall!=null) {
            callIdentifier = CallManager.getIdentifier(CallManager.foregroundCall!!)
        }
            when (state) {
            Call.STATE_RINGING -> callRinging()
            Call.STATE_ACTIVE -> callStarted()
            /*  Call.STATE_DISCONNECTED -> endCall() */
            Call.STATE_CONNECTING, Call.STATE_DIALING -> initOutgoingCallUI()
            Call.STATE_SELECT_PHONE_ACCOUNT -> showPhoneAccountPicker(callIdentifier!!)
        }

        if (state == Call.STATE_DISCONNECTED || state == Call.STATE_DISCONNECTING) {
            callTimer.cancel()
        }

        val statusTextId = when (state) {
            Call.STATE_RINGING -> R.string.is_calling
            Call.STATE_DIALING -> R.string.dialing
            else -> 0
        }

        if (statusTextId != 0) {
            call_status_label.text = getString(statusTextId)
        }
        if(newCall==null) {
         /*   setupNotification() */
        }
    }

    private fun acceptCall() {
        var foregroundCallIdentifer: String? = null
        var backgroundCallIdentifier: String? = null
        if(newCall!=null) {
            /* Foreground call always will be the new incoming call */
            if(CallManager.backgroundCall!!.state != Call.STATE_HOLDING) CallManager.backgroundCall!!.hold()
            foregroundCallIdentifer = CallManager.getIdentifier(CallManager.foregroundCall!!)
            CallManager.accept(foregroundCallIdentifer!!)
            return
        }/*else if(CallManager.foregroundCall!=null && CallManager.backgroundCall!=null) {
               foregroundCallIdentifer = CallManager.getIdentifier(CallManager.foregroundCall!!)
               if(CallManager.backgroundCall!!.state != Call.STATE_HOLDING) CallManager.backgroundCall!!.hold()
               CallManager.accept(foregroundCallIdentifer!!)
           } */else {
            foregroundCallIdentifer = CallManager.getIdentifier(CallManager.foregroundCall!!)
            CallManager.accept(foregroundCallIdentifer!!)
        }
    }

    private fun initOutgoingCallUI() {
        initProximitySensor()
        incoming_call_holder.beGone()
        ongoing_call_holder.beVisible()
        add_call_button.beVisible()
        swap_call_button.beGone()
        manage_conference.beGone()
        merge_call_button.beGone()
        videocallbutton.beVisible()
        tholder1.setText("Mute")
        tholder2.setText("Speaker")
        tholder3.setText("Keypad")
        tholder4.setText("Add Call")
        tholder5.setText("Hold")
        tholder6.setText("Video")
    }


    private fun callRinging() {
        ongoing_call_holder.beGone()
        incoming_arrow_holder_accept.beGone()
        incoming_arrow_holder_decline.beGone()
        if(newCall==null) {
            incoming_call_holder.beVisible()
        }else{
            incoming_second_call_holder.beVisible()
        }
    }

    fun convertToBitmap(imagestring: String?): Bitmap? {
        var bitmap: Bitmap? = null
        try {
            val bytes = Base64.decode(imagestring, Base64.DEFAULT)
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: java.lang.Exception) {
            Log.d(Constants.TAG, e.message!!)
        }
        return bitmap
    }


    private fun callStarted(){
        incoming_call_holder.beGone()
        incoming_second_call_holder.beGone()
        try {
            callTimer.cancel()
            callTimer = Timer()
            updateCallduration()
            callTimer.scheduleAtFixedRate(getCallTimerUpdateTask(), 1000, 1000)
        } catch (ignored: Exception) {
        }
        ongoing_call_holder.beVisible()
        if(CallManager.foregroundCall!=null && CallManager.backgroundCall!=null){
            if(isBluetoothConnected){
                tholder2.setText("Audio")
            }
            add_call_button.beGone()
            tholder4.setText("Swap")
            swap_call_button.beVisible()
            videocallbutton.beGone()
            tholder6.setText("Merge")
            merge_call_button.beVisible()
            updatenotificationview()
        }else if(CallManager.foregroundCall!=null && CallManager.foregroundCall == CallManager.activeConferenceCall && CallManager.backgroundCall!=null){
            if(isBluetoothConnected){
                tholder2.setText("Audio")
            }
            add_call_button.beGone()
            tholder4.setText("Swap")
            swap_call_button.beVisible()
            videocallbutton.beGone()
            tholder6.setText("Merge")
            merge_call_button.beVisible()
            updatenotificationview()
        }else if(CallManager.foregroundCall!=null && CallManager.foregroundCall == CallManager.activeConferenceCall && CallManager.backgroundCall == null) {
            if(isBluetoothConnected){
                tholder2.setText("Audio")
            }
            swap_call_button.beGone()
            tholder4.setText("Add Call")
            add_call_button.beVisible()
            videocallbutton.beGone()
            merge_call_button.beGone()
            tholder6.setText("Manage")
            manage_conference.beVisible()
            updatenotificationview()
        }else{
            swap_call_button.beGone()
            add_call_button.beVisible()
            merge_call_button.beGone()
            manage_conference.beGone()
            videocallbutton.beVisible()
            tholder1.setText("Mute")
            if(isBluetoothConnected){
                tholder2.setText("Audio")
            }else{
                tholder2.setText("Speaker")
            }
            tholder3.setText("Keypad")
            tholder4.setText("Add Call")
            tholder5.setText("Hold")
            tholder6.setText("Video")
            updatenotificationview()
        }

    }

    private fun updateCallduration(){
        var callStartTime:Long = CallManager.foregroundCall!!.details.connectTimeMillis
        var currentTime:Long = System.currentTimeMillis()
        var timeLapse:Long = (currentTime - callStartTime)/1000L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            callDuration = Math.toIntExact(timeLapse)
        }else{
            callDuration = timeLapse.toInt()
        }
    }


    private fun showPhoneAccountPicker(telecomID: String) {
        var callIdentifier = CallManager.getIdentifier(CallManager.foregroundCall!!)
        var currentCall: Call? = null
        if (callIdentifier != null && CallManager.callMap!!.containsKey(callIdentifier)) {
            currentCall = CallManager.callMap!!.get(callIdentifier)
        }
        if (callContact != null) {
            getHandleToUse(intent, callContact!!.number) { handle ->
                currentCall?.phoneAccountSelected(handle, false)
            }
        }
    }


    private fun getCallTimerUpdateTask() = object : TimerTask() {
        override fun run() {
            callDuration ++
            runOnUiThread {
                if (!isCallEnded) {
                    call_status_label.text = callDuration.getFormattedDuration()
                }
            }
        }
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            updateCallState(state)
        }

        override fun onCallDestroyed(call: Call?) {
            super.onCallDestroyed(call)
            endCall(call!!)
        }
    }


    private fun endCall(call:Call) {
        var telecomID:String? = null
        /*case 1: foreground call ends, with a background call*/
        if(CallManager.foregroundCall == call && CallManager.backgroundCall!=null){
            if(CallManager.foregroundCall == newCall){
                newCall = null
                newCallHandle = null
                newCallIdentifier= null
                newCallState = 0
                newCallContact = null
                newCallContactAvatar = null
            }
            telecomID = CallManager.getIdentifier(call!!)
            if(telecomID!=null) {
               CallManager.unregisterCallback(callCallback, telecomID!!)
            }
            CallManager.foregroundCall = CallManager.backgroundCall
            CallManager.backgroundCall = null
            if(CallManager.foregroundCall!!.state == Call.STATE_HOLDING)CallManager.foregroundCall!!.unhold()
            reinstateCallUI(CallManager.foregroundCall!!)
            return
        }else if(CallManager.foregroundCall == call && CallManager.backgroundCall==null){
            if(call == CallManager.activeConferenceCall) {
                var callList: MutableCollection<Call> = CallManager.callMap!!.values
                callList.forEachIndexed { index, callCurrent ->
                    if(callCurrent!=null && (callCurrent.state == Call.STATE_HOLDING || callCurrent.state == Call.STATE_ACTIVE)) {
                        CallManager.foregroundCall = callCurrent
                        CallManager.activeConferenceCall = null
                        CallManager.isConferenceCall = false
                        reinstateCallUI(callCurrent)
                        return
                    }
                }
                if(CallManager.isConferenceCall) {
                    CallManager.isConferenceCall = false;
                    CallManager.activeConferenceCall = null;
                }
            }
            if (proximityWakeLock?.isHeld == true) {
                proximityWakeLock!!.release()
            }
            try {
                audioManager.mode = AudioManager.MODE_NORMAL
            } catch (ignored: Exception) {
            }
            if (callDuration > 0) {
                runOnUiThread {
                    call_status_label.text =
                        "${callDuration.getFormattedDuration()} (${getString(R.string.call_ended)})"
                    Handler().postDelayed({
                        finish()
                    }, 3000)
                }
                telecomID = CallManager.getIdentifier(call!!)
                if(telecomID!=null) {
                    CallManager.unregisterCallback(callCallback, telecomID)
                }
                if(CallManager.foregroundCall!=null) CallManager.foregroundCall = null
                cancelNotification()
            }else{
                runOnUiThread {
                    call_status_label.text =
                        "(${getString(R.string.call_ended)})"
                    Handler().postDelayed({
                        finish()
                    }, 3000)
                }
                telecomID = CallManager.getIdentifier(call!!)
                if(telecomID!=null) {
                    CallManager.unregisterCallback(callCallback, telecomID)
                }
                if(CallManager.foregroundCall!=null) CallManager.foregroundCall = null
                 cancelNotification()
            }
            /* CallManager.foregroundCall = null */
        }else if(CallManager.backgroundCall == call){
            if(CallManager.backgroundCall == newCall){
                newCall = null
                newCallHandle = null
                newCallIdentifier= null
                newCallState = 0
                newCallContact = null
                newCallContactAvatar = null
            }
            telecomID = CallManager.getIdentifier(call!!)
            if(telecomID!=null) {
                CallManager.unregisterCallback(callCallback, telecomID)
            }
            CallManager.backgroundCall=null
            return
        }else{
            if(CallManager.foregroundCall!=null){
                if(CallManager.foregroundCall!!.state == Call.STATE_HOLDING)CallManager.foregroundCall!!.unhold()
                reinstateCallUI(CallManager.foregroundCall!!)
                return
            }
        }
    }

    @SuppressLint("NewApi")
    private fun addLockScreenFlags() {
        if (isOreoMr1Plus()) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }

        if (isOreoPlus()) {
            (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).requestDismissKeyguard(
                this,
                null
            )
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        }
    }

    private fun initProximitySensor() {
        if (proximityWakeLock == null || proximityWakeLock?.isHeld == false) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            proximityWakeLock = powerManager.newWakeLock(
                PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                "com.simplemobiletools.dialer.pro:wake_lock"
            )
            proximityWakeLock!!.acquire(10 * MINUTE_SECONDS * 1000L)
        }
    }

    private fun cancelNotification() {
        notificationManager.cancel(CALL_NOTIFICATION_ID)
    }

    @SuppressLint("NewApi")
    private fun updateNotification() {

        cancelNotification()
        val identifier = CallManager.getIdentifier(CallManager.foregroundCall!!)
        val callState = CallManager.getState(identifier)
        val channelId = "simple_dialer_call"
        if (isOreoPlus()) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val name = "call_notification_channel"

            NotificationChannel(channelId, name, importance).apply {
                setSound(null, null)
                notificationManager.createNotificationChannel(this)
            }
        }

        val openAppIntent = Intent(this, CallActivity::class.java)
        openAppIntent.flags = Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
        val openAppPendingIntent = PendingIntent.getActivity(this, 0, openAppIntent, 0)

        var callerName:String?= null
        if(CallManager.activeConferenceCall!=null && CallManager.foregroundCall == CallManager.activeConferenceCall) {
            callerName = "Conference Call"
        }else {
         callerName =
            if (contactCurrent != null && contactCurrent!!.name.isNotEmpty()) contactCurrent!!.name else getString(
                R.string.unknown_caller
            )
            }
        val contentTextId = when (callState) {
            Call.STATE_RINGING -> R.string.is_calling
            Call.STATE_DIALING -> R.string.dialing
            Call.STATE_DISCONNECTED -> R.string.call_ended
            Call.STATE_DISCONNECTING -> R.string.call_ending
            else -> R.string.ongoing_call

        }
        val collapsedView = RemoteViews(packageName, R.layout.call_notification).apply {
            setText(R.id.notification_caller_name, callerName)
            setText(R.id.notification_call_status, getString(contentTextId))

            if (avatar != null) {
                setImageViewBitmap(
                    R.id.notification_thumbnail,
                    getCircularBitmap(avatar!!)
                )
            }
        }
        var callStartTime:Long = CallManager.foregroundCall!!.details.connectTimeMillis
      /*  var currentTime:Long = System.currentTimeMillis()
        var timeLapse:Long = (currentTime - callStartTime)/1000L */

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_phone_vector)
            .setContentIntent(openAppPendingIntent)
            .setCustomContentView(collapsedView)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(Notification.CATEGORY_CALL)
            .setOngoing(true)
            .setSound(null)
            .setWhen(callStartTime)
            .setUsesChronometer(callState == Call.STATE_ACTIVE)
            .setChannelId(channelId)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
         val notification = builder.build()
        notificationManager.notify(CALL_NOTIFICATION_ID, notification)

    }

    fun updatenotificationview() {
        if(CallManager.foregroundCall!=null) {
            CallManager.getCallContact(applicationContext, CallManager.foregroundCall!!) { contact ->
                contactCurrent = contact
                avatar = getClearCallContactAvatar(contactCurrent!!)
                runOnUiThread {
                    updateNotification()
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private fun setupNotification() {
        val callState = CallManager.getState(telecomID!!)
        val channelId = "simple_dialer_call"
        if (isOreoPlus()) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val name = "call_notification_channel"

            NotificationChannel(channelId, name, importance).apply {
                setSound(null, null)
                notificationManager.createNotificationChannel(this)
            }
        }

        val openAppIntent = Intent(this, CallActivity::class.java)
        openAppIntent.flags = Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
        val openAppPendingIntent = PendingIntent.getActivity(this, 0, openAppIntent, 0)

        val acceptCallIntent = Intent(this, CallActionReceiver::class.java)
        acceptCallIntent.putExtra("telecomID", telecomID!!)
        acceptCallIntent.action = ACCEPT_CALL
        val acceptPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            acceptCallIntent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )

        val declineCallIntent = Intent(this, CallActionReceiver::class.java)
        declineCallIntent.putExtra("telecomID", telecomID!!)
        declineCallIntent.action = DECLINE_CALL
        val declinePendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            declineCallIntent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )

        val callerName =
            if (callContact != null && callContact!!.name.isNotEmpty()) callContact!!.name else getString(
                R.string.unknown_caller
            )
        val contentTextId = when (callState) {
            Call.STATE_RINGING -> R.string.is_calling
            Call.STATE_DIALING -> R.string.dialing
            Call.STATE_DISCONNECTED -> R.string.call_ended
            Call.STATE_DISCONNECTING -> R.string.call_ending
            else -> R.string.ongoing_call
        }

        val collapsedView = RemoteViews(packageName, R.layout.call_notification).apply {
            setText(R.id.notification_caller_name, callerName)
            setText(R.id.notification_call_status, getString(contentTextId))
            setVisibleIf(R.id.notification_accept_call, callState == Call.STATE_RINGING)

            setOnClickPendingIntent(R.id.notification_decline_call, declinePendingIntent)
            setOnClickPendingIntent(R.id.notification_accept_call, acceptPendingIntent)

            if (callContactAvatar != null) {
                setImageViewBitmap(
                    R.id.notification_thumbnail,
                    getCircularBitmap(callContactAvatar!!)
                )
            }
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_phone_vector)
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(Notification.CATEGORY_CALL)
            .setCustomContentView(collapsedView)
            .setOngoing(true)
            .setSound(null)
            .setUsesChronometer(callState == Call.STATE_ACTIVE)
            .setChannelId(channelId)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())

        val notification = builder.build()
        notificationManager.notify(CALL_NOTIFICATION_ID, notification)

    }

    @SuppressLint("NewApi")
    private fun getCallContactAvatar(contact:CallContact): Bitmap? {
        var number = contact!!.number
        var thisContact = contact
        var bitmap: Bitmap? = null
        if (thisContact?.photoUri?.isNotEmpty() == true) {
            val photoUri = Uri.parse(thisContact!!.photoUri)
            try {
                bitmap = if (isQPlus()) {
                    val tmbSize = resources.getDimension(R.dimen.list_avatar_size).toInt()
                    contentResolver.loadThumbnail(photoUri, Size(tmbSize, tmbSize), null)
                } else {
                    MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
                }
                bitmap = getCircularBitmap(bitmap!!)
            } catch (ignored: Exception) {
                return null
            }
        }
        return bitmap
    }

    private fun getClearCallContactAvatar(contact:CallContact):Bitmap? {
        var contactId:String? = null
        var bitmap:Bitmap? = null
        var number = contact!!.number
        var uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        val projection = arrayOf(
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup._ID
        )
        try{
          val cursor = contentResolver.query(
              uri,
              projection,
              null,
              null,
              null
          )
          if(cursor!=null){
              while(cursor.moveToNext()){
                  contactId  = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID))
              }
              cursor.close()
          }
            if(contactId!=null){
               // val inputStream = ContactsContract.Contacts.openContactPhotoInputStream(contentResolver, ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId.toLong() ) )
                val tempURI = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId.toLong())
                var contactURI = Uri.withAppendedPath(tempURI, ContactsContract.Contacts.Photo.DISPLAY_PHOTO)
                var fd = contentResolver.openAssetFileDescriptor(contactURI, "r")
                var inputStream = fd!!.createInputStream()
                if(inputStream!=null) {
                    bitmap = BitmapFactory.decodeStream(inputStream)
                }
                if(inputStream!=null){
                    inputStream.close()
                }
            }
        }catch (ignored:Exception){

        }
        if(bitmap!=null){
            bitmap = getCircularBitmap(bitmap)
        }
        return bitmap
    }


    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.width, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val radius = bitmap.width / 2.toFloat()
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(radius, radius, radius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output

    }

    override fun callToSwap(call: Call) {
        var currentCall:Call = call
        var childList:List<Call>  = mutableListOf<Call>()
        childList = CallManager.activeConferenceCall!!.children
        if(!childList.isEmpty()) {
            childList.forEachIndexed {index, call ->
                if(call!=currentCall)call.hold()
            }
            CallManager.activeConferenceCall!!.hold()
            currentCall.unhold()
        }
        swapConferenceCall(currentCall)
    }
    private fun swapConferenceCall(call:Call) {
        if(call!=null && CallManager.activeConferenceCall!=null) {
            CallManager.foregroundCall = call
            CallManager.backgroundCall = CallManager.activeConferenceCall!!
            reinstateCallUI(call)
        }
    }

    override fun callToRemove(call: Call) {
        var currentCall: Call = call
        if(currentCall!=null) {
            var identifier: String = CallManager.getIdentifier(currentCall)
            CallManager.reject(identifier)
        }
    }



    @RequiresApi(Build.VERSION_CODES.N)
    private fun parseNumber(numberToParse:String):String{
        var countryDetector:com.simplemobiletools.dialer.utilities.countryDetector = com.simplemobiletools.dialer.utilities.countryDetector.getInstance(this)
        var phoneNumberUtil:PhoneNumberUtil = PhoneNumberUtil.getInstance()
        var geoRegion:String?=null
        var isValid:Boolean = false
        try{
            var initPhonenumber:Phonenumber.PhoneNumber = phoneNumberUtil.parse(numberToParse, countryDetector.countryIso)
            isValid = phoneNumberUtil.isValidNumber(initPhonenumber)
            if(!isValid){
               return "invalidNumber"
            }
        }catch (e:Exception){
            Log.d(Constants.TAG, "countryDetectorException")
            Log.d(Constants.TAG, e.message.toString())
        }
        try {
            var phoneNumber: Phonenumber.PhoneNumber = phoneNumberUtil.parse(numberToParse, countryDetector.countryIso)
            var formattedNumber: String? = phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
            var region = phoneNumberUtil.getRegionCodeForNumber(phoneNumber)
            var geocoder: PhoneNumberOfflineGeocoder = PhoneNumberOfflineGeocoder.getInstance()
            geoRegion =  geocoder.getDescriptionForNumber(phoneNumber, Locale.ENGLISH)
            Log.d(Constants.TAG, "geoRegion")
            Log.d(Constants.TAG, geoRegion.toString())
        }catch (e: Exception){
            Log.d(Constants.TAG, "countryDetectorException")
            Log.d(Constants.TAG, e.message.toString())
            return "invalidNumber"
        }
        return geoRegion!!
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onConnected(connectionType: networkMonitor.connectionType?) {
    }

    override fun onDisconnected() {
    }

    override fun onStateChanged(state: Boolean, device: String) {
        if(state){
            isBluetoothConnected = true
            setCorrectOutput()
        }else{
            isBluetoothConnected = false
            tholder2.setText("Speaker")
            onHeadSet()
        }
    }

}


