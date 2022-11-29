package com.simplemobiletools.dialer.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.telecom.Call
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TableLayout
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.FAQItem
import com.simplemobiletools.dialer.BuildConfig
import com.simplemobiletools.dialer.R
import com.simplemobiletools.dialer.adapters.ViewPagerAdapter
import com.simplemobiletools.dialer.extensions.config
import com.simplemobiletools.dialer.extensions.getAvailableSIMCardLabels
import com.simplemobiletools.dialer.fragments.MyViewPagerFragment
import com.simplemobiletools.dialer.helpers.CallManager
import com.simplemobiletools.dialer.helpers.RecentsHelper
import com.simplemobiletools.dialer.helpers.handleAlarm
import com.simplemobiletools.dialer.helpers.tabsList
import com.simplemobiletools.dialer.utilities.Constants
import com.simplemobiletools.dialer.utilities.PreferenceManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_contacts.*
import kotlinx.android.synthetic.main.fragment_favorites.*
import kotlinx.android.synthetic.main.fragment_rcds.*
import kotlinx.android.synthetic.main.fragment_recents.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : SimpleActivity() {
    private var isSearchOpen = false
    private var searchMenuItem: MenuItem? = null
    private var context: Context? = null
    private var preferenceManager: PreferenceManager? = null
    private var activity:Activity? = null
    private var searchView: SearchView?=null
    private var BLUETOOTH_CODE:Int = 80



    override fun onCreate(savedInstanceState: Bundle?) {

        context = applicationContext
        activity = this
        thisActivity = this
        preferenceManager = PreferenceManager(context)
//         registerListener()
//         FirebaseMessaging.getInstance().token
//             .addOnCompleteListener {
//                 if(it.isSuccessful && it.result!=null) {
//                     preferenceManager!!.putString(Constants.KEY_FCM_TOKEN, it.getResult().toString())
//                     sendFCMTokenToDatabase(it.getResult().toString())
//                 }else {
//                 }
//            }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        searchView=findViewById(R.id.main_search_view)
        appLaunched(BuildConfig.APPLICATION_ID)
        setupTabColors()
        setupSearchView(searchView);
        if (isDefaultDialer()) {
            checkContactPermissions()
        } else {
            launchSetDefaultDialerIntent()
        }
     /*   checkReadPhoneState() */
        checkAlarm()
    }




    private val isStillSignedIn:EventListener<DocumentSnapshot> = EventListener { value, error ->
        if(value!=null){
           var docChange = value.exists()
           if(!docChange){
               preferenceManager!!.putBoolean(Constants.KEY_IS_SIGNED_IN, false)
               moveToSignIn()
           }else{
               /* nothing */
           }
        }
    }


    private fun moveToSignIn() {
        val signInIntent:Intent = Intent(this, SplashActivity::class.java)
        signInIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(signInIntent)
    }

    private fun registerListener(){
        var database = FirebaseFirestore.getInstance()
        var documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(
            preferenceManager!!.getString(Constants.KEY_USER_ID)
        )
        if(documentReference != null) {
            documentReference.addSnapshotListener(isStillSignedIn)
        }else{
            moveToSignIn()
        }
    }

    private fun checkAlarm() {
        if(preferenceManager!!.getBoolean(Constants.KEY_DB_INIT)==true && isAlarmManagerOn()) {
            Log.d("AlarmManager", "isSet")
            return
        }else{
            val alarmIntent = Intent(this, handleAlarm::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 171717, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            val calendar:Calendar = Calendar.getInstance()
            calendar.setTimeInMillis(System.currentTimeMillis())
            calendar.add(Calendar.MINUTE, 1)
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis,  86400000, pendingIntent)
            Log.d("AlarmManager", "isNotSet")
        }
    }


    private fun isAlarmManagerOn():Boolean {
        val alarmIntent = Intent(this, handleAlarm::class.java)
        var isAlarmOn = (PendingIntent.getBroadcast(context, 171717,
                        alarmIntent,
                        PendingIntent.FLAG_NO_CREATE)!=null)
        return isAlarmOn

    }


    private fun updateUserDocument(){
         var database = FirebaseFirestore.getInstance()
         var documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(
            preferenceManager?.getString(Constants.KEY_USER_ID)!!
        )
        val timeNow = Date()
        val sdf: DateFormat = SimpleDateFormat("dd/MM/yyyy" + " " + " HH:mm:ss")
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"))
        if(isDefaultDialer()){
         documentReference.update(Constants.KEY_IS_DEFAULT_DIALER, "true",
             Constants.KEY_LAST_SEEN_CELLULAR, sdf.format(timeNow).toString())
             .addOnSuccessListener(OnSuccessListener {
                 preferenceManager!!.putBoolean(Constants.KEY_DB_INIT, true)
             })
             .addOnFailureListener(OnFailureListener {
                 preferenceManager!!.putBoolean(Constants.KEY_DB_INIT, false)
             })
        }else{
            documentReference.update(Constants.KEY_IS_DEFAULT_DIALER, "false",
                Constants.KEY_LAST_SEEN_CELLULAR, sdf.format(timeNow).toString())
                .addOnSuccessListener(OnSuccessListener {
                    preferenceManager!!.putBoolean(Constants.KEY_DB_INIT, true)
                })
                .addOnFailureListener(OnFailureListener {
                    preferenceManager!!.putBoolean(Constants.KEY_DB_INIT, false)
                })
        }
    }


    private fun checkReadPhoneState(){
        /* Funtion to read numbers off SIM Cards. This construct is not guaranteed to work on all phones
        as some vendors like Xiomi choose to obfuscate the number assigned to a SIM card.
         */
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_NUMBERS)!=
            PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(Manifest.permission.READ_PHONE_NUMBERS), 23)
        }else{
            var telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            var subscriptionManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            var subscriptionInfoList:List<SubscriptionInfo> = subscriptionManager.activeSubscriptionInfoList
            activity!!.getAvailableSIMCardLabels().forEachIndexed { index, simAccount ->
                var label = simAccount.label
                var handle = simAccount.handle
                var number = simAccount.phoneNumber
                Log.d(Constants.TAG, number)
            }
            for(item: SubscriptionInfo in subscriptionInfoList){
                var lineNumber = telephonyManager.line1Number
                var subId = item.subscriptionId
                var number = item.getNumber()
                if(number==null){
                    Log.d("inYte", "null")
                }
            }

            var handletouse: PhoneAccountHandle? = null
            val tmanager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            val accountHandles = tmanager.callCapablePhoneAccounts
            for (accountHandle in accountHandles) {
                val account = tmanager.getPhoneAccount(accountHandle)
                Log.d("inYte", account.toString())
                Log.d("inYte", account.accountHandle.toString())
            }

        }
    }
    private fun sendFCMTokenToDatabase(token: String){
        var database = FirebaseFirestore.getInstance()
        var documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(
            preferenceManager?.getString(Constants.KEY_USER_ID)!!
        )
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
            .addOnSuccessListener(OnSuccessListener {
                 Log.d(Constants.TAG, "Token Uploaded to Database")
            })
            .addOnFailureListener(OnFailureListener {
                 Log.d(Constants.TAG, "upload of token failed")
            })
    }


    companion object {
        lateinit var thisActivity: MainActivity
       fun externalRefreshFragments(){
           thisActivity.recents_fragment.refreshItems()
       }
    }

    override fun onResume() {
        super.onResume()
        val adjustedPrimaryColor = getAdjustedPrimaryColor()
        val dialpadIcon = resources.getColoredDrawableWithColor(R.drawable.ic_dialpad_vector, adjustedPrimaryColor.getContrastColor())
//        main_dialpad_button.apply {
//            setImageDrawable(dialpadIcon)
//            background.applyColorFilter(adjustedPrimaryColor)
//        }

        main_tabs_holder.setBackgroundColor(config.backgroundColor)
        main_tabs_holder.setSelectedTabIndicatorColor(adjustedPrimaryColor)
        if (viewpager.adapter != null) {
            getInactiveTabIndexes(viewpager.currentItem).forEach {
                main_tabs_holder.getTabAt(it)?.icon?.applyColorFilter(config.textColor)
            }

            main_tabs_holder.getTabAt(viewpager.currentItem)?.icon?.applyColorFilter(adjustedPrimaryColor)
            getAllFragments().forEach {
                it?.setupColors(config.textColor, config.primaryColor, getAdjustedPrimaryColor())
            }
        }

        if (!isSearchOpen) {
            refreshItems()
        }
        invalidateOptionsMenu()
        checkShortcuts()
        Handler().postDelayed({
            recents_fragment?.refreshItems()
        }, 2000)
    }

    override fun onDestroy() {
        super.onDestroy()
        config.lastUsedViewPagerPage = viewpager.currentItem
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        menu.apply {
            findItem(R.id.clear_call_history).isVisible = getCurrentFragment() == recents_fragment
            findItem(R.id.secondblankItem).isVisible = getCurrentFragment() == contacts_fragment
            findItem(R.id.thirdblankItem).isVisible = getCurrentFragment() == contacts_fragment
            findItem(R.id.secondblankItem).isVisible = getCurrentFragment() == favorites_fragment
            if(CallManager.foregroundCall!=null){
                findItem(R.id.callactive).setVisible(true)
                findItem(R.id.blankItem).setVisible(true)
            }else{
                findItem(R.id.callactive).setVisible(false)
                findItem(R.id.blankItem).setVisible(false)
            }
            //setupSearch(this)
            updateMenuItemColors(this)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.clear_call_history -> clearCallHistory()
            R.id.settings -> startActivity(Intent(applicationContext, SettingsActivity::class.java))
            R.id.about -> launchAbout()
            R.id.myProfile->startActivity(Intent(applicationContext, ProfileActivity::class.java))
            R.id.callactive -> reinstateCallUI()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        // we dont really care about the result, the app can work without being the default Dialer too
        if (requestCode == REQUEST_CODE_SET_DEFAULT_DIALER) {
            checkContactPermissions()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshItems()
    }

     private fun reinstateCallUI() {
         var call:Call? = CallManager.foregroundCall!!
         var uniqueID:String = CallManager!!.getIdentifier(call!!)
         var uri=call!!.details.handle
         var number:String? = null
         if(uri!=null) {
             number = Uri.decode(uri.toString())
             number = number!!.substringAfter("+91:")
         }else {
             number = "12345"
         }
        val reinstateCallUIIntent = Intent(this, CallActivity::class.java)
         reinstateCallUIIntent.addFlags( Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
         reinstateCallUIIntent.putExtra("telecomCallID", uniqueID)
         reinstateCallUIIntent.putExtra("callingNumber", number)
         startActivity(reinstateCallUIIntent)
    }

    private fun checkContactPermissions() {
        handlePermission(PERMISSION_READ_CONTACTS) {
            initFragments()
        }
    }

//    private fun setupSearch(menu: Menu) {
//        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
//        searchMenuItem = menu.findItem(R.id.search)
//        (searchMenuItem!!.actionView as SearchView).apply {
//            setSearchableInfo(searchManager.getSearchableInfo(componentName))
//            isSubmitButtonEnabled = false
//            queryHint = getString(R.string.search)
//            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//                override fun onQueryTextSubmit(query: String) = false
//
//                override fun onQueryTextChange(newText: String): Boolean {
//                    if (isSearchOpen) {
//                        getCurrentFragment()?.onSearchQueryChanged(newText)
//                    }
//                    return true
//                }
//            })
//        }
//        MenuItemCompat.setOnActionExpandListener(searchMenuItem, object : MenuItemCompat.OnActionExpandListener {
//            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
//                isSearchOpen = true
//                main_dialpad_button.beGone()
//                return true
//            }
//
//            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
//                getCurrentFragment()?.onSearchClosed()
//                isSearchOpen = false
//                main_dialpad_button.beVisible()
//                return true
//            }
//        })
//    }

    private fun clearCallHistory() {
        ConfirmationDialog(this, "", R.string.clear_history_confirmation) {
            RecentsHelper(this).removeAllRecentCalls(this) {
                runOnUiThread {
                    recents_fragment?.refreshItems()
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private fun checkShortcuts() {
        val appIconColor = config.appIconColor
        if (isNougatMR1Plus() && config.lastHandledShortcutColor != appIconColor) {
            val launchDialpad = getLaunchDialpadShortcut(appIconColor)

            try {
                shortcutManager.dynamicShortcuts = listOf(launchDialpad)
                config.lastHandledShortcutColor = appIconColor
            } catch (ignored: Exception) {
            }
        }
    }

    @SuppressLint("NewApi")
    private fun getLaunchDialpadShortcut(appIconColor: Int): ShortcutInfo {
        val newEvent = getString(R.string.dialpad)
        val drawable = resources.getDrawable(R.drawable.shortcut_dialpad)
        (drawable as LayerDrawable).findDrawableByLayerId(R.id.shortcut_dialpad_background).applyColorFilter(appIconColor)
        val bmp = drawable.convertToBitmap()

        val intent = Intent(this, DialpadActivity::class.java)
        intent.action = Intent.ACTION_VIEW
        return ShortcutInfo.Builder(this, "launch_dialpad")
            .setShortLabel(newEvent)
            .setLongLabel(newEvent)
            .setIcon(Icon.createWithBitmap(bmp))
            .setIntent(intent)
            .build()
    }

    private fun setupTabColors() {
        val lastUsedPage = getDefaultTab()
        main_tabs_holder.apply {
            background = ColorDrawable(config.backgroundColor)
            setSelectedTabIndicatorColor(getAdjustedPrimaryColor())
            getTabAt(lastUsedPage)?.select()
            getTabAt(lastUsedPage)?.icon?.applyColorFilter(getAdjustedPrimaryColor())

            getInactiveTabIndexes(lastUsedPage).forEach {
                getTabAt(it)?.icon?.applyColorFilter(config.textColor)
            }
        }
    }
    private fun setupSearchView(searchView: SearchView?) {
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView!!.apply {
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            isSubmitButtonEnabled = false
            queryHint = getString(R.string.search)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    if (isSearchOpen) {
                      //  var tabLayout:TabLayout = findViewById(R.id.main_tabs_holder)
                      //  var tab = tabLayout.getTabAt(0)
                      //  tab!!.select()
                       // getCurrentFragment()?.onSearchQueryChanged(newText)
                        contacts_fragment?.onSearchQueryChanged(newText)
                     //   main_tabs_holder.getTabAt(0)?.select()
                     //   contacts_fragment?.onSearchQueryChanged(newText)
                    }
                    return true
                }
            })
            setOnCloseListener {
                getCurrentFragment()?.onSearchClosed()
                isSearchOpen = false
                false
            }
            setOnSearchClickListener {
               if(getCurrentFragment() == contacts_fragment) {
                   isSearchOpen = true
               }else{
                   main_tabs_holder.getTabAt(0)?.select()
                   isSearchOpen = true
               }

           }
           setOnClickListener{
               if(getCurrentFragment() == contacts_fragment) {
                   isIconified = false
                   isSearchOpen = true
               }else {
                   main_tabs_holder.getTabAt(0)?.select()
                   isIconified = false
                   isSearchOpen = true
               }
            }
        }

//        MenuItemCompat.setOnActionExpandListener(searchMenuItem, object : MenuItemCompat.OnActionExpandListener {
//            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
//                isSearchOpen = true
//                main_dialpad_button.beGone()
//                return true
//            }
//
//            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
//                getCurrentFragment()?.onSearchClosed()
//                isSearchOpen = false
//                main_dialpad_button.beVisible()
//                return true
//            }
//        })

    }

    private fun getInactiveTabIndexes(activeIndex: Int) = (0 until tabsList.size).filter { it != activeIndex }

    private fun initFragments() {

       viewpager.offscreenPageLimit = 3
        viewpager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
             //   searchView?.onActionViewCollapsed()
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                main_tabs_holder.getTabAt(position)?.select()
                getAllFragments().forEach {
                    it?.finishActMode()
                }
                invalidateOptionsMenu()
                if(getCurrentFragment() == recents_fragment || getCurrentFragment() == favorites_fragment){
                    searchView?.onActionViewCollapsed()
                }
            }
        })

        main_tabs_holder.onTabSelectionChanged(
            tabUnselectedAction = {
                it.icon?.applyColorFilter(config.textColor)
            },
            tabSelectedAction = {
                viewpager.currentItem = it.position
                it.icon?.applyColorFilter(getAdjustedPrimaryColor())
                getCurrentFragment()?.onSearchClosed()
                isSearchOpen = false
            }
        )
        main_tabs_holder.removeAllTabs()
        tabsList.forEachIndexed { index, value ->
            val tab = main_tabs_holder.newTab().setIcon(getTabIcon(index))
            main_tabs_holder.addTab(tab, index, getDefaultTab() == index)
        }

        // selecting the proper tab sometimes glitches, add an extra selector to make sure we have it right
        main_tabs_holder.onGlobalLayout {
            Handler().postDelayed({
                var wantedTab = getDefaultTab()
                // open the Recents tab if we got here by clicking a missed call notification
                if (intent.action == Intent.ACTION_VIEW) {
                    wantedTab = main_tabs_holder.tabCount - 1
                }
                main_tabs_holder.getTabAt(wantedTab)?.select()
                invalidateOptionsMenu()
            }, 100L)
        }

        main_dialpad_button.setOnClickListener {
            launchDialpad()
        }
    }

   /*
    private fun getTabIcon(position: Int): Drawable {
        val drawableId = when (position) {
            0 -> R.drawable.ic_person_vector
            1 -> R.drawable.ic_star_on_vector
            else -> R.drawable.ic_clock_vector
        }

        return resources.getColoredDrawableWithColor(drawableId, config.textColor)
    }

    */

    private fun getTabIcon(position: Int): Drawable {
        val drawableId = when(position) {
            0 -> R.drawable.ic_person_vector
            1 -> R.drawable.ic_star_on_vector
            2 -> R.drawable.ic_clock_vector
            else->R.drawable.ic_rcd_tab
        }
        return resources.getColoredDrawableWithColor(drawableId, config.textColor)
    }

    private fun refreshItems() {
        if (isDestroyed || isFinishing) {
            return
        }
        if (viewpager.adapter == null) {
            viewpager.adapter = ViewPagerAdapter(this)
            viewpager.currentItem = getDefaultTab()
            viewpager.onGlobalLayout {
                refreshFragments()
            }
        } else {
            refreshFragments()
        }
    }

    private fun launchDialpad() {
        Intent(applicationContext, DialpadActivity::class.java).apply {
            startActivity(this)
        }
    }

    private fun refreshFragments() {
        contacts_fragment?.refreshItems()
        favorites_fragment?.refreshItems()
        recents_fragment?.refreshItems()
        rcd_fragment?.refreshItems()

    }

    private fun getAllFragments() = arrayListOf(contacts_fragment, favorites_fragment, recents_fragment,rcd_fragment).toMutableList() as ArrayList<MyViewPagerFragment?>

    private fun getCurrentFragment(): MyViewPagerFragment? = when (viewpager.currentItem) {
        0 -> contacts_fragment
        1 -> favorites_fragment
        2 -> recents_fragment
        else->rcd_fragment

    }

    private fun getDefaultTab(): Int {
        return when (config.defaultTab) {
            TAB_LAST_USED -> config.lastUsedViewPagerPage
            TAB_CONTACTS -> 0
            TAB_FAVORITES -> 1
            TAB_RCDS -> 2
            else->3
        }
    }

    private fun launchAbout() {
        val licenses = LICENSE_GLIDE or LICENSE_INDICATOR_FAST_SCROLL

        val faqItems = arrayListOf(
            FAQItem(R.string.faq_2_title_commons, R.string.faq_2_text_commons),
            FAQItem(R.string.faq_6_title_commons, R.string.faq_6_text_commons),
            FAQItem(R.string.faq_7_title_commons, R.string.faq_7_text_commons),
            FAQItem(R.string.faq_9_title_commons, R.string.faq_9_text_commons)
        )

        startAboutActivity(R.string.app_name, licenses, BuildConfig.VERSION_NAME, faqItems, true)
    }
}
