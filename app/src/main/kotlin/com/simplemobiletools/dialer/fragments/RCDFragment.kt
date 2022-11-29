package com.simplemobiletools.dialer.fragments

import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import android.util.AttributeSet
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import com.simplemobiletools.commons.dialogs.CallConfirmationDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.KEY_PHONE
import com.simplemobiletools.commons.helpers.MyContactsContentProvider
import com.simplemobiletools.commons.helpers.PERMISSION_READ_CALL_LOG
import com.simplemobiletools.commons.helpers.SimpleContactsHelper
import com.simplemobiletools.commons.models.SimpleContact
import com.simplemobiletools.dialer.R
import com.simplemobiletools.dialer.activities.SimpleActivity
import com.simplemobiletools.dialer.adapters.RCDCallAdapter
import com.simplemobiletools.dialer.adapters.RecentCallsAdapter
import com.simplemobiletools.dialer.extensions.config
import com.simplemobiletools.dialer.extensions.startContactDetailsIntent
import com.simplemobiletools.dialer.helpers.RecentsHelper
import com.simplemobiletools.dialer.interfaces.RefreshItemsListener
import com.simplemobiletools.dialer.models.RecentCall
import kotlinx.android.synthetic.main.fragment_rcds.view.*
import kotlinx.android.synthetic.main.fragment_recents.view.*

class RCDFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet), RefreshItemsListener {
    private var allRecentCalls = ArrayList<RecentCall>()

    override fun setupFragment() {
        val placeholderResId = if (context.hasPermission(PERMISSION_READ_CALL_LOG)) {
            R.string.no_previous_calls
        } else {
            R.string.could_not_access_the_call_history
        }

        rcd_placeholder.text = context.getString(placeholderResId)
        rcd_placeholder_2.apply {
            underlineText()
            setOnClickListener {
                requestCallLogPermission()
            }
        }
    }

    override fun setupColors(textColor: Int, primaryColor: Int, adjustedPrimaryColor: Int) {
        rcd_placeholder.setTextColor(textColor)
        rcd_placeholder_2.setTextColor(adjustedPrimaryColor)

        (rcd_list?.adapter as? RCDCallAdapter)?.apply {
            initDrawables()
            updateTextColor(textColor)
        }
    }

    override fun refreshItems() {
        val privateCursor = context?.getMyContactsCursor(false, true)?.loadInBackground()
        val groupSubsequentCalls = context?.config?.groupSubsequentCalls ?: false
        RecentsHelper(context).getRecentCalls(groupSubsequentCalls) { recents ->
            SimpleContactsHelper(context).getAvailableContacts(false) { contacts ->
                val privateContacts = MyContactsContentProvider.getSimpleContacts(context, privateCursor)
                recents.filter { it.phoneNumber == it.name }.forEach { recent ->
                    var wasNameFilled = false
                    if (privateContacts.isNotEmpty()) {
                        val privateContact = privateContacts.firstOrNull { it.doesContainPhoneNumber(recent.phoneNumber) }
                        if (privateContact != null) {
                            recent.name = privateContact.name
                            wasNameFilled = true
                        }
                    }

                    if (!wasNameFilled) {
                        val contact = contacts.firstOrNull { it.phoneNumbers.first() == recent.phoneNumber }
                        if (contact != null) {
                            recent.name = contact.name
                        }
                    }
                }

                allRecentCalls = recents
                activity?.runOnUiThread {
                    gotRecents(recents)
                }
            }
        }
    }

    private fun gotRecents(recents: ArrayList<RecentCall>) {
        if (recents.isEmpty()) {
            rcd_placeholder.beVisible()
            rcd_placeholder_2.beGoneIf(context.hasPermission(PERMISSION_READ_CALL_LOG))
            rcd_list.beGone()
        } else {
            rcd_placeholder.beGone()
            rcd_placeholder_2.beGone()
            rcd_list.beVisible()

            val currAdapter = rcd_list.adapter
            if (currAdapter == null) {
                RecentCallsAdapter(activity as SimpleActivity, recents, rcd_list, this) {
                    val recentCall = it as RecentCall
//                    if (context.config.showCallConfirmation) {
//                        CallConfirmationDialog(activity as SimpleActivity, recentCall.name) {
//                            activity?.launchCallIntent(recentCall.phoneNumber)
//                        }
//                    } else {
//                       activity?.launchCallIntent(recentCall.phoneNumber)
//
//                    }
                }.apply {
                   // rcd_list.adapter = this
                }
            } else {
                (currAdapter as RCDCallAdapter).updateItems(recents)
            }
        }
    }

    private fun requestCallLogPermission() {
        activity?.handlePermission(PERMISSION_READ_CALL_LOG) {
            if (it) {
                rcd_placeholder.text = context.getString(R.string.no_previous_calls)
                rcd_placeholder_2.beGone()

                val groupSubsequentCalls = context?.config?.groupSubsequentCalls ?: false
                RecentsHelper(context).getRecentCalls(groupSubsequentCalls) { recents ->
                    activity?.runOnUiThread {
                        gotRecents(recents)
                    }
                }
            }
        }
    }

    override fun onSearchClosed() {
        rcd_placeholder.beVisibleIf(allRecentCalls.isEmpty())
        (rcd_list.adapter as? RCDCallAdapter)?.updateItems(allRecentCalls)
    }

    override fun onSearchQueryChanged(text: String) {
        val recentCalls = allRecentCalls.filter {
            it.name.contains(text, true) || it.doesContainPhoneNumber(text)
        }.sortedByDescending {
            it.name.startsWith(text, true)
        }.toMutableList() as ArrayList<RecentCall>

        rcd_placeholder.beVisibleIf(recentCalls.isEmpty())
        (rcd_list.adapter as? RCDCallAdapter)?.updateItems(recentCalls, text)
    }
}
