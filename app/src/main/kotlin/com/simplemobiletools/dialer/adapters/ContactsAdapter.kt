package com.simplemobiletools.dialer.adapters

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.SimpleContact
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.dialer.R
import com.simplemobiletools.dialer.activities.RCDActivity
import com.simplemobiletools.dialer.activities.SimpleActivity
import com.simplemobiletools.dialer.extensions.areMultipleSIMsAvailable
import com.simplemobiletools.dialer.extensions.callContactWithSim
import com.simplemobiletools.dialer.extensions.config
import com.simplemobiletools.dialer.extensions.startContactDetailsIntent
import com.simplemobiletools.dialer.interfaces.RefreshItemsListener
import com.simplemobiletools.dialer.utilities.Constants
import com.simplemobiletools.dialer.utilities.PreferenceManager
import kotlinx.android.synthetic.main.item_contact_call.view.*
import kotlinx.android.synthetic.main.item_recent_call.view.*

class ContactsAdapter(activity: SimpleActivity, var contacts: ArrayList<SimpleContact>, recyclerView: MyRecyclerView, val refreshItemsListener: RefreshItemsListener? = null,
                      highlightText: String = "", val showDeleteButton: Boolean = true, var tabFlag:Boolean,itemClick: (Any) -> Unit) :
        MyRecyclerViewAdapter(activity, recyclerView, null, itemClick) {

    private var textToHighlight = highlightText
    private var fontSize = activity.getTextSize()
    private var preferencesManager: PreferenceManager = PreferenceManager(activity.applicationContext)

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_contacts

    override fun prepareActionMode(menu: Menu) {
        val hasMultipleSIMs = activity.areMultipleSIMsAvailable()
        val isOneItemSelected = isOneItemSelected()
        val selectedNumber = "tel:${getSelectedPhoneNumber()}"

        menu.apply {
            findItem(R.id.cab_call_sim_1).isVisible = hasMultipleSIMs && isOneItemSelected
            findItem(R.id.cab_call_sim_2).isVisible = hasMultipleSIMs && isOneItemSelected
            findItem(R.id.cab_remove_default_sim).isVisible = isOneItemSelected && activity.config.getCustomSIM(selectedNumber) != ""
            findItem(R.id.cab_delete).isVisible = showDeleteButton
            findItem(R.id.cab_create_shortcut).isVisible = isOneItemSelected && isOreoPlus()
//            findItem(R.id.cab_view_details).isVisible = isOneItemSelected
        }
    }



    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_call_sim_1 -> callContact(true)
            R.id.cab_call_sim_2 -> callContact(false)
            R.id.cab_remove_default_sim -> removeDefaultSIM()
            R.id.cab_delete -> askConfirmDelete()
//            R.id.cab_send_sms -> sendSMS()
//            R.id.cab_view_details -> viewContactDetails()
            R.id.cab_create_shortcut -> createShortcut()
            R.id.cab_select_all -> selectAll()
        }
    }

    override fun getSelectableItemCount() = contacts.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = contacts.getOrNull(position)?.rawId

    override fun getItemKeyPosition(key: Int) = contacts.indexOfFirst { it.rawId == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(R.layout.item_contact_call, parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        holder.bindView(contact, true, true) { itemView, layoutPosition ->
            setupView(itemView, contact,layoutPosition)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = contacts.size

    fun updateItems(newItems: ArrayList<SimpleContact>, highlightText: String = "") {
        if (newItems.hashCode() != contacts.hashCode()) {
            contacts = newItems.clone() as ArrayList<SimpleContact>
            textToHighlight = highlightText
            notifyDataSetChanged()
            finishActMode()
        } else if (textToHighlight != highlightText) {
            textToHighlight = highlightText
            notifyDataSetChanged()
        }
    }

    @SuppressLint("MissingPermission")
    private fun callContact(useSimOne: Boolean) {
        val number = getSelectedPhoneNumber() ?: return
        activity.callContactWithSim(number, useSimOne)
    }

    private fun removeDefaultSIM() {
        val phoneNumber = getSelectedPhoneNumber() ?: return
        activity.config.removeCustomSIM("tel:$phoneNumber")
        finishActMode()
    }

    private fun sendSMS() {
        val numbers = getSelectedItems().map { it.phoneNumbers.first() }
        val recipient = TextUtils.join(";", numbers)
        activity.launchSendSMSIntent(recipient)
    }

    private fun viewContactDetails() {
        val contact = getSelectedItems().firstOrNull() ?: return
        activity.startContactDetailsIntent(contact)
    }

    private fun askConfirmDelete() {
        val itemsCnt = selectedKeys.size
        val firstItem = getSelectedItems().firstOrNull() ?: return
        val items = if (itemsCnt == 1) {
            "\"${firstItem.name}\""
        } else {
            resources.getQuantityString(R.plurals.delete_contacts, itemsCnt, itemsCnt)
        }
        val baseString = R.string.deletion_confirmation
        val question = String.format(resources.getString(baseString), items)

        ConfirmationDialog(activity, question) {
            activity.handlePermission(PERMISSION_WRITE_CONTACTS) {
                deleteContacts()
            }
        }
    }

    private fun deleteContacts() {
        if (selectedKeys.isEmpty()) {
            return
        }

        val contactsToRemove = getSelectedItems()
        val positions = getSelectedItemPositions()
        contacts.removeAll(contactsToRemove)
        val idsToRemove = contactsToRemove.map { it.rawId }.toMutableList() as ArrayList<Int>

        SimpleContactsHelper(activity).deleteContactRawIDs(idsToRemove) {
            activity.runOnUiThread {
                if (contacts.isEmpty()) {
                    refreshItemsListener?.refreshItems()
                    finishActMode()
                } else {
                    removeSelectedItems(positions)
                }
            }
        }
    }

    private fun getSelectedItems() = contacts.filter { selectedKeys.contains(it.rawId) } as ArrayList<SimpleContact>

    private fun getSelectedPhoneNumber() = getSelectedItems().firstOrNull()?.phoneNumbers?.firstOrNull()

    @SuppressLint("NewApi")
    private fun createShortcut() {
        val contact = contacts.firstOrNull { selectedKeys.contains(it.rawId) } ?: return
        val manager = activity.shortcutManager
        if (manager.isRequestPinShortcutSupported) {
            SimpleContactsHelper(activity).getShortcutImage(contact.photoUri, contact.name) { image ->
                activity.runOnUiThread {
                    activity.handlePermission(PERMISSION_CALL_PHONE) { hasPermission ->
                        val action = if (hasPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL
                        val intent = Intent(action).apply {
                            data = Uri.fromParts("tel", contact.phoneNumbers.first(), null)
                        }
                        val shortcut = ShortcutInfo.Builder(activity, contact.hashCode().toString())
                            .setShortLabel(contact.name)
                            .setIcon(Icon.createWithBitmap(image))
                            .setIntent(intent)
                            .build()

                        manager.requestPinShortcut(shortcut, null)
                    }
                }
            }
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (!activity.isDestroyed && !activity.isFinishing) {
            Glide.with(activity).clear(holder.itemView.findViewById<ImageView>(R.id.item_contact_image))
        }
    }
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

    private fun setupView(view: View, contact: SimpleContact,adapterPosition:Int) {
        view.apply {
            findViewById<FrameLayout>(R.id.item_contact_frame).isSelected = selectedKeys.contains(contact.rawId)
            var tray=this.findViewById<View>(R.id.item_contacts_tray)
            var inforAndSim=this.findViewById<ImageView>(R.id.imageInforAndSim)
            findViewById<TextView>(R.id.item_contact_name).apply {
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
                text = if (textToHighlight.isEmpty()) contact.name else {
                    if (contact.name.contains(textToHighlight, true)) {
                        contact.name.highlightTextPart(textToHighlight, adjustedPrimaryColor)
                    } else {
                        contact.name.highlightTextFromNumbers(textToHighlight, adjustedPrimaryColor)
                    }
                }
            }
            if (!activity.isDestroyed) {
                SimpleContactsHelper(context).loadContactImage(contact.photoUri, findViewById(R.id.item_contact_image), contact.name)
            }
            item_contact_name.apply {
                setOnClickListener{
                    recyclerView.post {
                        for (i in 0 until itemCount) {
                            if (i != (adapterPosition - positionOffset)) {
                                val viewItem: View? =
                                    recyclerView.getLayoutManager()?.findViewByPosition(i)
                                val trayItem = viewItem?.findViewById<View>(R.id.item_contacts_tray)
                                val inforAndSimItem =
                                    viewItem?.findViewById<ImageView>(R.id.imageInforAndSim)
                                if (trayItem != null) {
                                    trayItem.visibility = View.GONE
                                }
                                if (inforAndSimItem != null) {
                                    inforAndSimItem.visibility = View.GONE
                                }
                            } else {
                                continue
                            }
                        }
                    }
                    if(tray.visibility==View.GONE) {
                        if (!actModeCallback.isSelectable) {
                            if(tabFlag){
                                tray.visibility = View.VISIBLE
                                inforAndSim.setColorFilter(ContextCompat.getColor(context, R.color.colorWhite), android.graphics.PorterDuff.Mode.MULTIPLY)
                                inforAndSim.setImageResource(R.drawable.ic_phone_vector)
                                inforAndSim.visibility = View.VISIBLE
                                inforAndSim.setBackgroundResource(R.drawable.circle_background)
                                inforAndSim.setOnClickListener{
                                    val phoneNumber =contact.phoneNumbers.get(0)
                                    activity?.launchCallIntent(phoneNumber)
                                }
                            }
                            else{
                                tray.visibility = View.VISIBLE
                                inforAndSim.visibility = View.VISIBLE

                            }
                        } else {
                            val currentPosition = adapterPosition - positionOffset
                            val isSelected =
                                selectedKeys.contains(getItemSelectionKey(currentPosition))
                            toggleItemSelection(!isSelected, currentPosition, true)
                        }
                    }
                    else{
                        tray.visibility=View.GONE
                        inforAndSim.visibility=View.GONE
                    }
                }
                setOnLongClickListener {
                    val currentPosition = adapterPosition - positionOffset
                    if (!actModeCallback.isSelectable) {
                        activity.startSupportActionMode(actModeCallback)
                    }
                    toggleItemSelection(true, currentPosition, true)
                    itemLongClicked(currentPosition)
                    true

                }
            }
            item_contact_image.apply {
                setOnClickListener{
                    Intent().apply {
                        if (!actModeCallback.isSelectable) {
                            activity.startContactDetailsIntent(contact)
                        }
                        else{
                            val currentPosition = adapterPosition - positionOffset
                            val isSelected = selectedKeys.contains(getItemSelectionKey(currentPosition))
                            toggleItemSelection(!isSelected, currentPosition, true)
                        }

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

            inforAndSim.apply {
                setOnClickListener{
                    preferencesManager!!.putBoolean(Constants.RCD_CALL_TYPE,false)
                    activity.startContactDetailsIntent(contact)
                }
            }
            imageContactsSMSCall.apply {
                setOnClickListener{
                    val phoneNumber =contact.phoneNumbers.get(0)
                    activity.launchSendSMSIntent(phoneNumber)
                }
            }

            imageContactsVideoCall.apply {
                setOnClickListener {
                    var phoneNumber =contact.phoneNumbers.get(0)
                    var remotePeer:String?=null
                    if(contact.name!=null){
                        remotePeer = contact.name
                    }
                    if(phoneNumber.startsWith("+91")){
                        phoneNumber= phoneNumber.substringAfter("+91")
                    }else {
                        phoneNumber = phoneNumber
                    }
                    if(remotePeer!=null) {
                        launchCall("video", phoneNumber, remotePeer)
                    }else {
                        launchCall("video", phoneNumber)
                    }
                }
            }

            imageContactsAudioCall.apply {
                setOnClickListener {
                    var phoneNumber =contact.phoneNumbers.get(0)
                    var remotePeer:String? = null
                    if(contact.name!=null){
                        remotePeer = contact.name
                    }
                    if(phoneNumber.startsWith("+91")){
                        phoneNumber= phoneNumber.substringAfter("+91")
                    }else {
                        phoneNumber = phoneNumber
                    }
                    if(remotePeer!=null) {
                        launchCall("video", phoneNumber, remotePeer!!)
                    }else {
                        launchCall("video", phoneNumber)
                    }
                }
            }

            imageContactsRCDCall.apply{
                val bundle = Bundle()
                bundle.putString("callName", contact.name)
                bundle.putString("callPhone",contact.phoneNumbers.get(0))
                val intent = Intent(activity, RCDActivity::class.java)
                intent.putExtras(bundle)
                setOnClickListener{
                    activity.startActivity(intent)
                }
            }


        }
    }
}
