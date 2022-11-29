package com.simplemobiletools.dialer

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.telecom.Call
import android.util.Size
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.helpers.BaseConfig
import com.simplemobiletools.commons.helpers.isQPlus
import com.simplemobiletools.dialer.activities.CallActivity
import com.simplemobiletools.dialer.extensions.config
import com.simplemobiletools.dialer.helpers.CallManager
import com.simplemobiletools.dialer.models.CallContact
import kotlinx.android.synthetic.main.activity_call.*

import kotlinx.android.synthetic.main.fragment_conference_management.*


class conferenceManagement : Fragment() {

    companion object {
        var call: Call? = null
        var childCallList: List<Call>? = mutableListOf<Call>()
        var callContact:CallContact? = null
        var contactImage: Bitmap? = null
        lateinit var thisContext:Context
        lateinit var notifier: conferenceNotifier
        lateinit var thisAdapter:conferenceAdapter
        lateinit var thisActivity:Activity
    }

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            // Inflate the layout for this fragment
            return inflater.inflate(R.layout.fragment_conference_management, container, false)
        }

        override fun onStart() {
            super.onStart()
            thisActivity = requireActivity()
            thisContext = activity?.applicationContext!!
            if (CallManager.isConferenceCall) {
                call = CallManager.activeConferenceCall!!
                childCallList= CallManager.activeConferenceCall!!.children
            }
            var thisView: View? = view
            if (thisView != null) {
                exitcallmanagementfragement.setOnClickListener {
                    activity?.supportFragmentManager!!.beginTransaction().remove(this).commit()
                    activity?.container!!.beGone()
                }
                var lManager = LinearLayoutManager(activity?.applicationContext)
                callmanagementlist.background = ColorDrawable(activity?.config!!.backgroundColor)
                callmanagementlist.layoutManager = lManager
                thisAdapter = conferenceAdapter(childCallList!!.size)
                callmanagementlist.adapter = thisAdapter
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        notifier = context as conferenceNotifier
    }

        class conferenceAdapter(private var size: Int) :
            RecyclerView.Adapter<conferenceAdapter.ViewHolder>() {

            class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                var avatar: ImageView = itemView.findViewById(R.id.item_call_image)
                var removeCall: ImageView = itemView.findViewById(R.id.imageRemoveCall)
                var contactInformation: TextView = itemView.findViewById(R.id.item_call_name)
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                var view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_conference_call_list, parent, false)
                return ViewHolder(view)
            }

            override fun onBindViewHolder(holder: ViewHolder, position: Int) {

                 call = childCallList!!.get(position)
                 if(call!=null) {
                     CallManager.getCallContact(thisContext, call!!) { contact ->
                         var bitmap:Bitmap? = null
                         callContact = contact
                             if(callContact!!.name.isNotEmpty()) {
                                 holder.contactInformation.text = callContact!!.name
                             }else if(callContact!!.number.isNotEmpty()) {
                                 holder.contactInformation.text = callContact!!.number
                             }else{
                                 holder.contactInformation.text = "Unknown Number"
                             }
                         if(callContact!=null && callContact!!.photoUri!=null){
                             var photoUri = Uri.parse(callContact!!.photoUri)
                             if(isQPlus()){
                                 var size = 48
                                 bitmap = thisActivity.contentResolver.loadThumbnail(photoUri, Size(size, size), null)
                             }else{
                                 bitmap = MediaStore.Images.Media.getBitmap(thisActivity.contentResolver, photoUri)
                             }
                             bitmap = getCircularBitmap(bitmap!!)
                             thisActivity.runOnUiThread {
                                 holder.avatar.setImageBitmap(bitmap!!)
                             }
                         }

                     }

                     holder.removeCall.setOnClickListener {
                         notifier.callToRemove(childCallList!!.get(position))
                         childCallList = CallManager.activeConferenceCall!!.children
                         thisActivity?.container!!.beGone()
                     }

                 }
            }

            override fun getItemCount(): Int {
                return childCallList!!.size
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

        }
    interface conferenceNotifier {
        fun callToSwap(call: Call)
        fun callToRemove(call: Call)
    }

}
