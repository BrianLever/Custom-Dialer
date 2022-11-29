package com.simplemobiletools.dialer.activities


import android.Manifest
import android.R.attr
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.View
import android.view.inputmethod.*
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.extensions.launchCallIntent
import com.simplemobiletools.commons.views.MyEditText
import com.simplemobiletools.commons.views.MyEditText.KeyBoardInputCallbackListener
import com.simplemobiletools.dialer.R
import com.simplemobiletools.dialer.utilities.Constants
import com.simplemobiletools.dialer.utilities.PreferenceManager
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit


class RCDActivity : SimpleActivity(), OnMapReadyCallback {
    private var context: Context? = null
    private var preferenceManager: PreferenceManager? = null
    private var activity: Activity? = null
    private lateinit var callNameTextView: TextView
    private lateinit var callPhoneTextView: TextView
    private lateinit var linkedinURLImageView: ImageView
    private lateinit var instagramURLImageView: ImageView
    private lateinit var facebookURLImageView: ImageView
    private lateinit var twitterURLImageView: ImageView
    private lateinit var websiteURLImageView: ImageView
    private lateinit var rcdDataEditText: MyEditText
    private lateinit var rcdDataEnter:ImageView
    private lateinit var rcdDataPhoto:ImageView
    private lateinit var rcdDataCamera:ImageView
    private var publicMessage=""
    private lateinit var rcdDataGIF:ImageView
    private lateinit var rcdDataLocation:ImageView
    private var mMap: GoogleMap? = null
    private lateinit var rcdDataImage:ImageView
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var currentLocation: Location? = null
    private var locationPermissionGranted : Boolean = false
    private lateinit var mapFragment:SupportMapFragment
    private lateinit var linkedinSwitch: SwitchCompat
    private lateinit var instagramSwitch:SwitchCompat
    private lateinit var facebookSwitch:SwitchCompat
    private lateinit var twitterSwitch:SwitchCompat
    private lateinit var websiteSwitch:SwitchCompat
    private var publicBitmap: String=""



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.rcd_data_import)
        context = applicationContext
        activity = this
        preferenceManager = PreferenceManager(context)
        MyEditText(applicationContext)
        val bundle = intent.extras
        var callName: String? = bundle!!.getString("callName", "")
        var callPhone: String? = bundle!!.getString("callPhone", "")
        mapFragment = supportFragmentManager
            .findFragmentById(R.id.rcd_data_map) as SupportMapFragment
        mapFragment.view?.visibility=View.GONE
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest().apply {
            interval = TimeUnit.SECONDS.toMillis(60)
            fastestInterval = TimeUnit.SECONDS.toMillis(30)
            maxWaitTime = TimeUnit.MINUTES.toMillis(2)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult?.lastLocation?.let {
                    currentLocation = it
                    Toast.makeText(this@RCDActivity,"Location: "+currentLocation!!.latitude+", "+currentLocation!!.longitude,Toast.LENGTH_SHORT).show()
                } ?: run {
                    Log.d("Error", "Location information isn't available.")
                }
            }
        }
        if (ActivityCompat.checkSelfPermission(
                this@RCDActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this@RCDActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            getLocationPermission()
        }
        mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
        callNameTextView=findViewById(R.id.rcd_data_name)
        callNameTextView.text=callName

        callPhoneTextView=findViewById(R.id.rcd_data_phoneNumber)
        callPhoneTextView.text=callPhone

        linkedinURLImageView=findViewById(R.id.rcd_data_linkedin)
        linkedinSwitch=findViewById(R.id.rcd_data_linkedin_switch)
            val linkedinState:Boolean=preferenceManager!!.getBoolean(Constants.RCD_LINKEDIN_URL_FLAG)
            if(linkedinState){
                linkedinURLImageView.alpha=1.0F
                linkedinSwitch.isChecked=true
            }
            else{
                linkedinURLImageView.alpha=0.3F
                linkedinSwitch.isChecked=false

            }
        linkedinSwitch.apply {
            setOnCheckedChangeListener{ buttonView, isChecked ->
               if(isChecked){
                   linkedinURLImageView.alpha=1.0F
               }
                else{
                   linkedinURLImageView.alpha=0.3F
               }
                preferenceManager!!.putBoolean(Constants.RCD_LINKEDIN_URL_FLAG,isChecked)
            }


        }

        instagramURLImageView=findViewById(R.id.rcd_data_instagram)
        instagramSwitch=findViewById(R.id.rcd_data_instagram_switch)
            val instagramState:Boolean=preferenceManager!!.getBoolean(Constants.RCD_INSTA_URL_FLAG)
            if(instagramState){
                instagramURLImageView.alpha=1.0F
                instagramSwitch.isChecked=true
            }
            else{
                instagramURLImageView.alpha=0.3F
                instagramSwitch.isChecked=false
            }
        instagramSwitch.apply {
            setOnCheckedChangeListener{ buttonView, isChecked ->
                if(isChecked){
                    instagramURLImageView.alpha=1.0F
                }
                else{
                    instagramURLImageView.alpha=0.3F
                }
                preferenceManager!!.putBoolean(Constants.RCD_INSTA_URL_FLAG,isChecked)
            }
        }
        facebookURLImageView=findViewById(R.id.rcd_data_facebook)
        facebookSwitch=findViewById(R.id.rcd_data_facebook_switch)
            val facebookState:Boolean=preferenceManager!!.getBoolean(Constants.RCD_FACEBOOK_URL_FLAG)
            if(facebookState){
                facebookURLImageView.alpha=1.0F
                facebookSwitch.isChecked=true
            }
            else{
                facebookURLImageView.alpha=0.3F
                facebookSwitch.isChecked=false
            }
        facebookSwitch.apply {
            setOnCheckedChangeListener{ buttonView, isChecked ->
                if(isChecked){
                    facebookURLImageView.alpha=1.0F
                }
                else{
                    facebookURLImageView.alpha=0.3F
                }
                preferenceManager!!.putBoolean(Constants.RCD_FACEBOOK_URL_FLAG,isChecked)
            }
        }
        twitterURLImageView=findViewById(R.id.rcd_data_twitter)
        twitterSwitch=findViewById(R.id.rcd_data_twitter_switch)
            val twitterState:Boolean=preferenceManager!!.getBoolean(Constants.RCD_TWITTER_URL_FLAG)
            if(twitterState){
                twitterURLImageView.alpha=1.0F
                twitterSwitch.isChecked=true
            }
            else {
                twitterURLImageView.alpha=0.3F
                twitterSwitch.isChecked=false
            }
        twitterSwitch.apply {
            setOnCheckedChangeListener{ buttonView, isChecked ->
                if(isChecked){
                    twitterURLImageView.alpha=1.0F
                }
                else{
                    twitterURLImageView.alpha=0.3F
                }
                preferenceManager!!.putBoolean(Constants.RCD_TWITTER_URL_FLAG,isChecked)
            }
        }
        websiteURLImageView=findViewById(R.id.rcd_data_website)
        websiteSwitch=findViewById(R.id.rcd_data_website_switch)
            val websiteState:Boolean=preferenceManager!!.getBoolean(Constants.RCD_WEB_URL_FLAG)
            if(websiteState){
                websiteURLImageView.alpha=1.0F
                websiteSwitch.isChecked=true
            }
            else {
                websiteURLImageView.alpha=0.3F
                websiteSwitch.isChecked=false
            }
        websiteSwitch.apply {
            setOnCheckedChangeListener{ buttonView, isChecked ->
                if(isChecked){
                    websiteURLImageView.alpha=1.0F
                }
                else{
                    websiteURLImageView.alpha=0.3F
                }
                preferenceManager!!.putBoolean(Constants.RCD_WEB_URL_FLAG,isChecked)
            }
        }

        rcdDataEditText=findViewById(R.id.rcd_data_editText)
            rcdDataEditText.apply {
                addTextChangedListener(object:TextWatcher{
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }
                    override fun onTextChanged(message: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        publicMessage=message.toString()
                    }
                    override fun afterTextChanged(p0: Editable?) {
                    }

                })
                setOnEditorActionListener{ v, actionId, event ->
                    if (event != null && event.keyCode === KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                        rcdDataEditText.setText("", TextView.BufferType.EDITABLE)
                        val imm: InputMethodManager =
                            getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.hideSoftInputFromWindow(rcdDataEditText.getWindowToken(), 0)
                    }
                    false
                }
                setKeyBoardInputCallbackListener(object : KeyBoardInputCallbackListener {
                    override fun onCommitContent(
                        inputContentInfo: InputContentInfoCompat?,
                        flags: Int, opts: Bundle?
                    ) {
                        val selectedImageUri: Uri? = inputContentInfo?.contentUri
                        if (null != selectedImageUri) {
                            var selectedImageBitmap: Bitmap?=null
                            try {
                                selectedImageBitmap = MediaStore.Images.Media.getBitmap(
                                    this@RCDActivity.contentResolver,
                                    selectedImageUri
                                )
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                            rcdDataImage.setImageBitmap(selectedImageBitmap)
                            if (selectedImageBitmap != null) {
                                imageToString(selectedImageBitmap)
                            }
                        }

                    }
                })
            }

        rcdDataEnter=findViewById(R.id.rcd_data_enter)
             rcdDataEnter.apply{
                setOnClickListener{
                    if (callPhone != null) {
                        preferenceManager!!.putBoolean(Constants.RCD_CALL_TYPE,true)
                        preferenceManager!!.putString(Constants.RCD_CALL_MESSAGE,publicMessage)
                        preferenceManager!!.putString(Constants.RCD_CALL_BITMAP_DATA,publicBitmap)
                        launchCallIntent(callPhone)
                    }

                }

            }
        rcdDataGIF=findViewById(R.id.rcd_data_GIF)
            rcdDataGIF.apply {
                setOnClickListener{
                    rcdDataEditText.requestFocus()
                    val imm: InputMethodManager =
                        getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(rcdDataEditText, 1)
                }
            }
        rcdDataImage=findViewById(R.id.rcd_data_image)
        rcdDataLocation=findViewById(R.id.rcd_data_location)
            rcdDataLocation.apply {
               setOnClickListener{
                   mapFragment.view?.visibility=View.VISIBLE
                   mapFragment.getMapAsync(this@RCDActivity)

               }
            }
        rcdDataPhoto=findViewById(R.id.rcd_data_photo)
            rcdDataPhoto.apply {
                setOnClickListener{
                    galleryImageChooser()
                }

            }
        rcdDataCamera=findViewById(R.id.rcd_data_camera)
            rcdDataCamera.apply {
                setOnClickListener {
                    cameraImageChooser()
                }
            }

    }
    private fun imageToString(bitmap: Bitmap){
        val stream = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.JPEG, 100, stream)
        val bytes = stream.toByteArray()
        publicBitmap = Base64.encodeToString(bytes, Base64.DEFAULT)

    }

    private fun galleryImageChooser() {
        val i = Intent()
        i.type = "image/*"
        i.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(i, "Select Picture"), 1)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                val selectedImageUri: Uri? = data?.data
                if (null != selectedImageUri) {
                    var selectedImageBitmap: Bitmap?=null
                    try {
                        selectedImageBitmap = MediaStore.Images.Media.getBitmap(
                            this.contentResolver,
                            selectedImageUri
                        )
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    rcdDataImage.setImageBitmap(selectedImageBitmap)
                    if (selectedImageBitmap != null) {
                        imageToString(selectedImageBitmap)
                    }
                }
            }
            else if(requestCode==2){
                val photo:Bitmap = data?.extras?.get("data") as Bitmap
                rcdDataImage.setImageBitmap(photo)
                imageToString(photo)
            }
        }
    }
    private fun cameraImageChooser() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(Intent.createChooser(cameraIntent, "Select Picture"), 2)
    }



    override fun onResume() {
        super.onResume()
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        updateMenuItemColors(menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (currentLocation != null) {
            val markerOptions = MarkerOptions()
            markerOptions.position(LatLng(currentLocation!!.latitude,
                currentLocation!!.longitude))
            markerOptions.title("Location: "+currentLocation!!.latitude+", "+currentLocation!!.longitude)
            markerOptions.icon(
                BitmapDescriptorFactory.defaultMarker(
                    BitmapDescriptorFactory.HUE_RED))
            mMap!!.addMarker(markerOptions)?.showInfoWindow()
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(currentLocation!!.latitude,
                    currentLocation!!.longitude), 13.0F
            ))
            mMap!!.setOnMapLoadedCallback {
                mMap!!.snapshot { bitmap ->
                    rcdDataImage.setImageBitmap(bitmap)
                    if (bitmap != null) {
                        imageToString(bitmap)
                    }
                }
            }
        }
        mapFragment.view?.beGone()
    }
    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1)
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        locationPermissionGranted = false
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

}
