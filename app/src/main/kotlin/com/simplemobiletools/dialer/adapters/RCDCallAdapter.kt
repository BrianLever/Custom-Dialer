package com.simplemobiletools.dialer.adapters

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.provider.CallLog.Calls
import android.text.SpannableString
import android.text.TextUtils
import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.SimpleContact
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.dialer.R
import com.simplemobiletools.dialer.activities.SimpleActivity
import com.simplemobiletools.dialer.dialogs.ShowGroupedCallsDialog
import com.simplemobiletools.dialer.extensions.areMultipleSIMsAvailable
import com.simplemobiletools.dialer.extensions.callContactWithSim
import com.simplemobiletools.dialer.extensions.config
import com.simplemobiletools.dialer.extensions.startContactDetailsIntent
import com.simplemobiletools.dialer.helpers.RecentsHelper
import com.simplemobiletools.dialer.interfaces.RefreshItemsListener
import com.simplemobiletools.dialer.models.RecentCall
import com.simplemobiletools.dialer.utilities.Constants
import com.simplemobiletools.dialer.utilities.PreferenceManager
import kotlinx.android.synthetic.main.item_rcd_call.view.*
import kotlinx.android.synthetic.main.item_recent_call.view.*
import java.util.*


class RCDCallAdapter(
    activity: SimpleActivity, var recentCalls: ArrayList<RecentCall>, recyclerView: MyRecyclerView, private val refreshItemsListener: RefreshItemsListener?,
    itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activity, recyclerView, null, itemClick) {

    private lateinit var outgoingCallIcon: Drawable
    private lateinit var incomingCallIcon: Drawable
    private lateinit var incomingMissedCallIcon: Drawable
    private var fontSize = activity.getTextSize()
    private val areMultipleSIMsAvailable = activity.areMultipleSIMsAvailable()
    private val redColor = resources.getColor(R.color.md_red_700)
    private var textToHighlight = ""
    private var preferencesManager:PreferenceManager = PreferenceManager(activity.applicationContext)

    init {
        initDrawables()
        setupDragListener(true)
    }



    override fun getActionMenuId() = R.menu.cab_recent_calls

    override fun prepareActionMode(menu: Menu) {
//        val hasMultipleSIMs = activity.areMultipleSIMsAvailable()
//        val selectedItems = getSelectedItems()
//        val isOneItemSelected = selectedItems.size == 1
//        val selectedNumber = "tel:${getSelectedPhoneNumber()}"
//
//        menu.apply {
//
//            findItem(R.id.cab_call_sim_1).isVisible = hasMultipleSIMs && isOneItemSelected
//            findItem(R.id.cab_call_sim_2).isVisible = hasMultipleSIMs && isOneItemSelected
//            findItem(R.id.cab_richcall).isVisible=!hasMultipleSIMs && isOneItemSelected
//            findItem(R.id.cab_richcall_sim_1).isVisible=hasMultipleSIMs && isOneItemSelected
//            findItem(R.id.cab_richcall_sim_2).isVisible=hasMultipleSIMs && isOneItemSelected
//            findItem(R.id.cab_remove_default_sim).isVisible = isOneItemSelected && activity.config.getCustomSIM(selectedNumber) != ""
//
//            findItem(R.id.cab_block_number).isVisible = isNougatPlus()
//            findItem(R.id.cab_add_number).isVisible = isOneItemSelected
//            findItem(R.id.cab_copy_number).isVisible = isOneItemSelected
//            findItem(R.id.cab_show_grouped_calls).isVisible = isOneItemSelected && selectedItems.first().neighbourIDs.isNotEmpty()
//        }
    }

    override fun actionItemPressed(id: Int) {
//        if (selectedKeys.isEmpty()) {
//            return
//        }
//        when (id) {
//            R.id.cab_call_sim_1 -> callContact(true)
//            R.id.cab_call_sim_2 -> callContact(false)
//               R.id.cab_richcall -> lauchrichcallholder()
//                  R.id.cab_richcall_sim_1 -> launchrichcallholdersim(true)
//                  R.id.cab_richcall_sim_2 -> launchrichcallholdersim(false)
//            R.id.cab_remove_default_sim -> removeDefaultSIM()
//            R.id.cab_block_number -> askConfirmBlock()
//            R.id.cab_add_number -> addNumberToContact()
//            R.id.cab_send_sms -> sendSMS()
//            R.id.cab_show_grouped_calls -> showGroupedCalls()
//            R.id.cab_copy_number -> copyNumber()
//            R.id.cab_remove -> askConfirmRemove()
//            R.id.cab_select_all -> selectAll()
//        }
    }

    override fun getSelectableItemCount() = recentCalls.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = recentCalls.getOrNull(position)?.id

    override fun getItemKeyPosition(key: Int) = recentCalls.indexOfFirst { it.id == key }

    override fun onActionModeCreated() {
    }


    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(R.layout.item_rcd_call, parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recentCall = recentCalls[position]
        holder.bindView(recentCall, refreshItemsListener != null, refreshItemsListener != null) { itemView, layoutPosition ->
            setupView(itemView, recentCall,layoutPosition)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = recentCalls.size

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (!activity.isDestroyed && !activity.isFinishing) {
            Glide.with(activity).clear(holder.itemView.item_recents_image)
        }
    }

    fun initDrawables() {
        outgoingCallIcon = resources.getColoredDrawableWithColor(R.drawable.ic_outgoing_call_vector, baseConfig.textColor)
        incomingCallIcon = resources.getColoredDrawableWithColor(R.drawable.ic_incoming_call_vector, baseConfig.textColor)
        incomingMissedCallIcon = resources.getColoredDrawableWithColor(R.drawable.ic_incoming_call_vector, redColor)
    }

    private fun callContact(useSimOne: Boolean) {
        val phoneNumber = getSelectedPhoneNumber() ?: return
        activity.callContactWithSim(phoneNumber, useSimOne)
    }

    private fun removeDefaultSIM() {
        val phoneNumber = getSelectedPhoneNumber() ?: return
        activity.config.removeCustomSIM("tel:$phoneNumber")
        finishActMode()
    }

    private fun askConfirmBlock() {
        val numbers = TextUtils.join(", ", getSelectedItems().distinctBy { it.phoneNumber }.map { it.phoneNumber })
        val baseString = R.string.block_confirmation
        val question = String.format(resources.getString(baseString), numbers)

        ConfirmationDialog(activity, question) {
            blockNumbers()
        }
    }

    private fun blockNumbers() {
        if (selectedKeys.isEmpty()) {
            return
        }

        val callsToBlock = getSelectedItems()
        val positions = getSelectedItemPositions()
        recentCalls.removeAll(callsToBlock)

        ensureBackgroundThread {
            callsToBlock.map { it.phoneNumber }.forEach { number ->
                activity.addBlockedNumber(number)

            }

            activity.runOnUiThread {
                removeSelectedItems(positions)
                finishActMode()
            }
        }
    }

    private fun addNumberToContact() {
        val phoneNumber = getSelectedPhoneNumber() ?: return
        Intent().apply {
            action = Intent.ACTION_INSERT_OR_EDIT
            type = "vnd.android.cursor.item/contact"
            putExtra(KEY_PHONE, phoneNumber)
            activity.launchActivityIntent(this)
        }
    }

    private fun sendSMS() {
        val numbers = getSelectedItems().map { it.phoneNumber }
        val recipient = TextUtils.join(";", numbers)
        activity.launchSendSMSIntent(recipient)
    }

    private fun showGroupedCalls() {
        val recentCall = getSelectedItems().firstOrNull() ?: return
        val callIds = recentCall.neighbourIDs.map { it }.toMutableList() as ArrayList<Int>
        callIds.add(recentCall.id)
        ShowGroupedCallsDialog(activity, callIds)
    }

    private fun copyNumber() {
        val recentCall = getSelectedItems().firstOrNull() ?: return
        activity.copyToClipboard(recentCall.phoneNumber)
        finishActMode()
    }

    private fun askConfirmRemove() {
        ConfirmationDialog(activity, activity.getString(R.string.remove_confirmation)) {
            activity.handlePermission(PERMISSION_WRITE_CALL_LOG) {
                removeRecents()
            }
        }
    }

    private fun removeRecents() {
        if (selectedKeys.isEmpty()) {
            return
        }

        val callsToRemove = getSelectedItems()
        val positions = getSelectedItemPositions()
        val idsToRemove = ArrayList<Int>()
        callsToRemove.forEach {
            idsToRemove.add(it.id)
            it.neighbourIDs.mapTo(idsToRemove, { it })
        }

        RecentsHelper(activity).removeRecentCalls(idsToRemove) {
            recentCalls.removeAll(callsToRemove)
            activity.runOnUiThread {
                if (recentCalls.isEmpty()) {
                    refreshItemsListener?.refreshItems()
                    finishActMode()
                } else {
                    removeSelectedItems(positions)
                }
            }
        }
    }

    fun updateItems(newItems: ArrayList<RecentCall>, highlightText: String = "") {
        if (newItems.hashCode() != recentCalls.hashCode()) {
            recentCalls = newItems.clone() as ArrayList<RecentCall>
            textToHighlight = highlightText
            notifyDataSetChanged()
            finishActMode()
        } else if (textToHighlight != highlightText) {
            textToHighlight = highlightText
            notifyDataSetChanged()
        }
    }

    private fun getSelectedItems() = recentCalls.filter { selectedKeys.contains(it.id) } as ArrayList<RecentCall>

    private fun getSelectedPhoneNumber() = getSelectedItems().firstOrNull()?.phoneNumber

    private fun launchCall(type:String, peerNumber:String){
        /*TODO*/
        var peerFCM:String
        var database = FirebaseFirestore.getInstance()
        database.collection(Constants.KEY_COLLECTION_USERS)
            .whereEqualTo(Constants.KEY_MOBILE, peerNumber)
            .get()
            .addOnCompleteListener {
                if(it.isSuccessful && it.result!=null && it.result!!.documents.size >0 ){
                    var documentSnapshot = it.result!!.documents[0]
                    peerFCM = documentSnapshot.getString(Constants.KEY_FCM_TOKEN).toString()
                    /* TODO */
                    activity.let{
                        val outgoingCallIntent = Intent(it, com.simplemobiletools.dialer.Activities.outgoingCallActivity::class.java)
                        outgoingCallIntent.putExtra("Type", type)
                        outgoingCallIntent.putExtra("Remote-Token", peerFCM)
                        outgoingCallIntent.putExtra("Remote-Number", peerNumber)
                        it?.startActivity(outgoingCallIntent)
                    }

                }else{
                    /* Toast.makeText(activity.applicationContext, "Cannot Setup Call", Toast.LENGTH_SHORT).show() */
                    activity.let{
                        val outgoingCallIntent = Intent(it, com.simplemobiletools.dialer.Activities.outgoingCallActivity::class.java)
                        outgoingCallIntent.putExtra("Type", "videoSMS")
                        outgoingCallIntent.putExtra("Remote-Token", "NA")
                        outgoingCallIntent.putExtra("Remote-Number", peerNumber)
                        it?.startActivity(outgoingCallIntent)
                    }

                }
            }

    }

    private fun launchCall(type:String, peerNumber:String, remotePeer:String){
        /*TODO*/
        var peerFCM:String
        var database = FirebaseFirestore.getInstance()
        database.collection(Constants.KEY_COLLECTION_USERS)
            .whereEqualTo(Constants.KEY_MOBILE, peerNumber)
            .get()
            .addOnCompleteListener {
                if(it.isSuccessful && it.result!=null && it.result!!.documents.size >0 ){
                    var documentSnapshot = it.result!!.documents[0]
                    peerFCM = documentSnapshot.getString(Constants.KEY_FCM_TOKEN).toString()
                    /* TODO */
                    activity.let{
                        val outgoingCallIntent = Intent(it, com.simplemobiletools.dialer.Activities.outgoingCallActivity::class.java)
                        outgoingCallIntent.putExtra("Type", type)
                        outgoingCallIntent.putExtra("Remote-Token", peerFCM)
                        outgoingCallIntent.putExtra("Remote-Number", peerNumber)
                        outgoingCallIntent.putExtra("Remote-Peer", remotePeer)
                        it?.startActivity(outgoingCallIntent)
                    }

                }else{
                    /* Toast.makeText(activity.applicationContext, "Cannot Setup Call", Toast.LENGTH_SHORT).show() */
                    activity.let{
                        val outgoingCallIntent = Intent(it, com.simplemobiletools.dialer.Activities.outgoingCallActivity::class.java)
                        outgoingCallIntent.putExtra("Type", "videoSMS")
                        outgoingCallIntent.putExtra("Remote-Token", "NA")
                        outgoingCallIntent.putExtra("Remote-Number", peerNumber)
                        outgoingCallIntent.putExtra("Remote-Peer", remotePeer)
                        it?.startActivity(outgoingCallIntent)

                    }

                }
            }

    }

    private fun setupView(view: View, call: RecentCall,adapterPosition:Int) {
        view.apply {
            item_rcd_frame.isSelected = selectedKeys.contains(call.id)

            var nameToShow = SpannableString(call.name)
            var tray=this.findViewById<View>(R.id.item_recents_tray)
            var phoneNumber=this.findViewById<TextView>(R.id.item_recents_phoneNumber)
            var imageAddContact=this.findViewById<ImageView>(R.id.imageAddContact)
            if (call.neighbourIDs.isNotEmpty()) {
                nameToShow = SpannableString("$nameToShow (${call.neighbourIDs.size + 1})")
            }
            if (textToHighlight.isNotEmpty() && nameToShow.contains(textToHighlight, true)) {
                nameToShow = SpannableString(nameToShow.toString().highlightTextPart(textToHighlight, adjustedPrimaryColor))
            }
            item_recents_name.apply {
                text = nameToShow
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
                setOnClickListener{
                    recyclerView.post {
                        for(i in 0 until itemCount){
                            if(i!=(adapterPosition-positionOffset)){
                                val viewItem: View? =
                                    recyclerView.getLayoutManager()?.findViewByPosition(i)
                                val trayItem = viewItem?.findViewById<View>(R.id.item_recents_tray)
                                val phoneNumberItem=viewItem?.findViewById<TextView>(R.id.item_recents_phoneNumber)
                                val imageAddContactItem=viewItem?.findViewById<ImageView>(R.id.imageAddContact)
                                if (trayItem != null) {
                                    trayItem.visibility=View.GONE

                                }
                                if (phoneNumberItem != null) {
                                    phoneNumberItem.visibility=View.GONE
                                }
                                if (imageAddContactItem != null) {
                                    imageAddContactItem.visibility=View.GONE
                                }
                            }
                            else{
                                continue
                            }
                        }

                    }
                    if(tray.visibility==View.GONE) {
                        if (!actModeCallback.isSelectable) {
                            val value = call.name
                            if (value[0] == '+') {
                                imageAddContact.visibility = View.VISIBLE
                                phoneNumber.visibility = View.VISIBLE
                                phoneNumber.text = "Add Contacts"
                                phoneNumber.textSize = 15f
                                phoneNumber.setOnClickListener {
                                    Intent().apply {
                                        action = Intent.ACTION_INSERT_OR_EDIT
                                        type = "vnd.android.cursor.item/contact"
                                        putExtra(KEY_PHONE, call.phoneNumber)
                                        activity.launchActivityIntent(this)
                                    }
                                }
                                imageAddContact.setOnClickListener {
                                    Intent().apply {
                                        action = Intent.ACTION_INSERT_OR_EDIT
                                        type = "vnd.android.cursor.item/contact"
                                        putExtra(KEY_PHONE, call.phoneNumber)
                                        activity.launchActivityIntent(this)
                                    }
                                }
                            } else {
                                if(value[0].isDigit()) {
                                    imageAddContact.visibility = View.VISIBLE
                                    phoneNumber.visibility = View.VISIBLE
                                    phoneNumber.text = "Add Contacts"
                                    phoneNumber.textSize = 15f
                                    phoneNumber.setOnClickListener {
                                        Intent().apply {
                                            action = Intent.ACTION_INSERT_OR_EDIT
                                            type = "vnd.android.cursor.item/contact"
                                            putExtra(KEY_PHONE, call.phoneNumber)
                                            activity.launchActivityIntent(this)
                                        }
                                    }
                                    imageAddContact.setOnClickListener {
                                        Intent().apply {
                                            action = Intent.ACTION_INSERT_OR_EDIT
                                            type = "vnd.android.cursor.item/contact"
                                            putExtra(KEY_PHONE, call.phoneNumber)
                                            activity.launchActivityIntent(this)
                                        }
                                    }
                                }else{
                                    phoneNumber.visibility = View.VISIBLE
                                    phoneNumber.text = call.phoneNumber
                                }

                            }
                            tray.visibility = View.VISIBLE
                        } else {
                            val currentPosition = adapterPosition - positionOffset
                            val isSelected =
                                selectedKeys.contains(getItemSelectionKey(currentPosition))
                            toggleItemSelection(!isSelected, currentPosition, true)
                        }
                    }
                    else{
                        tray.visibility=View.GONE
                        phoneNumber.visibility=View.GONE
                        imageAddContact.visibility=View.GONE
                    }
                }
                setOnLongClickListener {
                    if(tray.visibility==View.GONE) {
                        val currentPosition = adapterPosition - positionOffset
                        if (!actModeCallback.isSelectable) {
                            activity.startSupportActionMode(actModeCallback)
                        }
                        toggleItemSelection(true, currentPosition, true)
                        itemLongClicked(currentPosition)
                    }
                    true

                }
            }
            item_recents_date_time.apply {
                text = nameToShow
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
                setOnClickListener{
                    recyclerView.post {
                        for (i in 0 until itemCount) {
                            if (i != (adapterPosition - positionOffset)) {
                                val viewItem: View? =
                                    recyclerView.getLayoutManager()?.findViewByPosition(i)
                                val trayItem = viewItem?.findViewById<View>(R.id.item_recents_tray)
                                val phoneNumberItem =
                                    viewItem?.findViewById<TextView>(R.id.item_recents_phoneNumber)
                                val imageAddContactItem =
                                    viewItem?.findViewById<ImageView>(R.id.imageAddContact)
                                if (trayItem != null) {
                                    trayItem.visibility = View.GONE

                                }
                                if (phoneNumberItem != null) {
                                    phoneNumberItem.visibility = View.GONE
                                }
                                if (imageAddContactItem != null) {
                                    imageAddContactItem.visibility = View.GONE
                                }
                            } else {
                                continue
                            }
                        }
                    }
                    if(tray.visibility==View.GONE) {
                        if (!actModeCallback.isSelectable) {
                            val value = call.name
                            if (value[0] == '+') {
                                imageAddContact.visibility = View.VISIBLE
                                phoneNumber.visibility = View.VISIBLE
                                phoneNumber.text = "Add Contacts"
                                phoneNumber.textSize = 15f
                                phoneNumber.setOnClickListener {
                                    Intent().apply {
                                        action = Intent.ACTION_INSERT_OR_EDIT
                                        type = "vnd.android.cursor.item/contact"
                                        putExtra(KEY_PHONE, call.phoneNumber)
                                        activity.launchActivityIntent(this)
                                    }
                                }
                                imageAddContact.setOnClickListener {
                                    Intent().apply {
                                        action = Intent.ACTION_INSERT_OR_EDIT
                                        type = "vnd.android.cursor.item/contact"
                                        putExtra(KEY_PHONE, call.phoneNumber)
                                        activity.launchActivityIntent(this)
                                    }
                                }
                            } else {
                                phoneNumber.visibility = View.VISIBLE
                                phoneNumber.text = call.phoneNumber

                            }
                            tray.visibility = View.VISIBLE
                        } else {
                            val currentPosition = adapterPosition - positionOffset
                            val isSelected =
                                selectedKeys.contains(getItemSelectionKey(currentPosition))
                            toggleItemSelection(!isSelected, currentPosition, true)
                        }
                    }
                    else{
                        tray.visibility=View.GONE
                        phoneNumber.visibility=View.GONE
                        imageAddContact.visibility=View.GONE
                    }
                }
                setOnLongClickListener {
                    if(tray.visibility==View.GONE) {
                        val currentPosition = adapterPosition - positionOffset
                        if (!actModeCallback.isSelectable) {
                            activity.startSupportActionMode(actModeCallback)
                        }
                        toggleItemSelection(true, currentPosition, true)
                        itemLongClicked(currentPosition)
                    }
                    true

                }
            }

            item_recents_image.apply {
                setOnClickListener{
                    if (!actModeCallback.isSelectable) {
                        SimpleContactsHelper(context).getAvailableContacts(false) { contacts ->
                            var contactInformation:SimpleContact?=null
                            for(contact in contacts){
                                if(contact.phoneNumbers.contains(call.phoneNumber)){
                                    contactInformation=contact
                                }
                                else{
                                    continue
                                }
                            }
                            if(contactInformation!=null){
                                activity.startContactDetailsIntent(contactInformation)
                            }
                            else{
                                Intent().apply {
                                    action = Intent.ACTION_INSERT_OR_EDIT
                                    type = "vnd.android.cursor.item/contact"
                                    putExtra(KEY_PHONE, call.phoneNumber)
                                    activity.launchActivityIntent(this)
                                }
                            }
                        }
                    }
                    else{
                        val currentPosition = adapterPosition - positionOffset
                        val isSelected = selectedKeys.contains(getItemSelectionKey(currentPosition))
                        toggleItemSelection(!isSelected, currentPosition, true)
                    }


                }
                setOnLongClickListener{
                    val currentPosition = adapterPosition - positionOffset
                    if (!actModeCallback.isSelectable) {
                        activity.startSupportActionMode(actModeCallback)
                    }
                    toggleItemSelection(true, currentPosition, true)
                    itemLongClicked(currentPosition)
                    true
                }
            }

            imageSIMCall.apply {
                val adjustedPrimaryColor = activity?.getAdjustedPrimaryColor()
                val callIcon = resources.getColoredDrawableWithColor(R.drawable.ic_phone_vector, adjustedPrimaryColor.getContrastColor())
                imageSIMCall.setImageDrawable(callIcon)
                imageSIMCall.background.applyColorFilter(adjustedPrimaryColor)
                setOnClickListener{
                    if (!actModeCallback.isSelectable) {
                        activity?.launchCallIntent(call.phoneNumber)
                    }
                    else{
                        val currentPosition = adapterPosition - positionOffset
                        val isSelected = selectedKeys.contains(getItemSelectionKey(currentPosition))
                        toggleItemSelection(!isSelected, currentPosition, true)
                    }

                }
                setOnLongClickListener{
                    val currentPosition = adapterPosition - positionOffset
                    if (!actModeCallback.isSelectable) {
                        activity.startSupportActionMode(actModeCallback)
                    }
                    toggleItemSelection(true, currentPosition, true)
                    itemLongClicked(currentPosition)
                    true
                }
            }
            imageSMSCall.apply {
                setOnClickListener{
                    val phoneNumber = call.phoneNumber
                    activity.launchSendSMSIntent(phoneNumber)
                }
            }

            item_recents_date_time.apply {
                text = call.startTS.formatDateOrTime(context, refreshItemsListener != null, false)
                setTextColor(if (call.type == Calls.MISSED_TYPE) redColor else textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.8f)

            }
            /*      item_recents_duration.apply {
                      text = call.duration.getFormattedDuration()
                      setTextColor(textColor)
                      beVisibleIf(call.type != Calls.MISSED_TYPE && call.type != Calls.REJECTED_TYPE && call.duration > 0)
                      setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.8f)
                  } */

            imageVideoCall.apply {
                /*TODO*/
                setOnClickListener {
                    var number= call.phoneNumber
                    var remotePeer:String?=null
                    if(call.name!=null){
                        remotePeer = call.name
                    }
                    if(number.startsWith("+91")){
                        number= number.substringAfter("+91")
                    }else {
                        number = number
                    }
                    if(remotePeer!=null) {
                        launchCall("video", number, remotePeer)
                    }else {
                        launchCall("video", number)
                    }
                }

            }
            imageAudioCall.apply {
                /*TODO*/
                setOnClickListener {
                    var number=call.phoneNumber
                    var remotePeer:String? = null
                    if(call.name!=null){
                        remotePeer = call.name
                    }
                    if(number.startsWith("+91")){
                        number= number.substringAfter("+91")
                    }else {
                        number = number
                    }
                    if(remotePeer!=null) {
                        launchCall("video", number, remotePeer!!)
                    }else {
                        launchCall("video", number)
                    }
                }

            }
            item_recents_sim_image.beVisibleIf(areMultipleSIMsAvailable)
            item_recents_sim_id.beVisibleIf(areMultipleSIMsAvailable)
            if (areMultipleSIMsAvailable) {
                item_recents_sim_image.applyColorFilter(textColor)
                item_recents_sim_id.setTextColor(textColor.getContrastColor())
                item_recents_sim_id.text = call.simID.toString()
            }

            SimpleContactsHelper(context).loadContactImage(call.photoUri, item_rcd_image, call.name)

            val drawable = when (call.type) {
                Calls.OUTGOING_TYPE -> outgoingCallIcon
                Calls.MISSED_TYPE -> incomingMissedCallIcon
                else -> incomingCallIcon
            }
            item_rcd_type.setImageDrawable(drawable)
        }
    }
}
