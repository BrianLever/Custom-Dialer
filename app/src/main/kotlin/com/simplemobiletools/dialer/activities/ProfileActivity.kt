package com.simplemobiletools.dialer.activities



import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.widget.*
import com.simplemobiletools.dialer.R
import com.simplemobiletools.dialer.utilities.Constants
import com.simplemobiletools.dialer.utilities.PreferenceManager


class ProfileActivity : SimpleActivity() {
    private var context: Context? = null
    private var preferenceManager: PreferenceManager? = null
    private var activity: Activity? = null
    private lateinit var profilePhoneNumberTextView: TextView
    private lateinit var nameEditText:EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneOneEditText: EditText
    private lateinit var phoneTwoEditText: EditText
    private lateinit var addressEditText: EditText
    private lateinit var twitterCheckBox: CheckBox
    private lateinit var twitterEditText: EditText
    private lateinit var linkedinCheckBox: CheckBox
    private lateinit var linkedinEditText: EditText
    private lateinit var facebookCheckBox: CheckBox
    private lateinit var facebookEditText: EditText
    private lateinit var instagramCheckBox: CheckBox
    private lateinit var instagramEditText: EditText
    private lateinit var websiteCheckBox: CheckBox
    private lateinit var websiteEditText: EditText
    private lateinit var updateButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile)
        context = applicationContext
        activity = this
        preferenceManager = PreferenceManager(context)
        profilePhoneNumberTextView=findViewById(R.id.profile_phoneNumber)
        nameEditText=findViewById(R.id.name_editText)
        emailEditText=findViewById(R.id.email_editText)
        phoneOneEditText=findViewById(R.id.phone1_editText)
        phoneTwoEditText=findViewById(R.id.phone2_editText)
        addressEditText=findViewById(R.id.address_editText)
        twitterCheckBox=findViewById(R.id.twitter_checkbox)
        twitterEditText=findViewById(R.id.twitter_editText)
        linkedinCheckBox=findViewById(R.id.linkedin_checkbox)
        linkedinEditText=findViewById(R.id.linkedin_editText)
        facebookCheckBox=findViewById(R.id.facebook_checkbox)
        facebookEditText=findViewById(R.id.facebook_editText)
        instagramCheckBox=findViewById(R.id.instagram_checkbox)
        instagramEditText=findViewById(R.id.instagram_editText)
        websiteCheckBox=findViewById(R.id.website_checkbox)
        websiteEditText=findViewById(R.id.website_editText)
        updateButton=findViewById(R.id.update_button)

        val otpMobileNumber:String?=preferenceManager?.getString(Constants.KEY_MOBILE)
        if(otpMobileNumber!=null){
            profilePhoneNumberTextView.text=otpMobileNumber
        }

        val twitterState:Boolean=preferenceManager!!.getBoolean(Constants.RCD_TWITTER_URL_FLAG)
            twitterCheckBox.isChecked = twitterState

        val linkedinState:Boolean=preferenceManager!!.getBoolean(Constants.RCD_LINKEDIN_URL_FLAG)
            linkedinCheckBox.isChecked = linkedinState

        val facebookState:Boolean=preferenceManager!!.getBoolean(Constants.RCD_FACEBOOK_URL_FLAG)
            facebookCheckBox.isChecked = facebookState

        val instagramState:Boolean=preferenceManager!!.getBoolean(Constants.RCD_INSTA_URL_FLAG)
            instagramCheckBox.isChecked = instagramState

        val websiteState:Boolean=preferenceManager!!.getBoolean(Constants.RCD_WEB_URL_FLAG)
             websiteCheckBox.isChecked = websiteState

        val twitterURL:String?=preferenceManager?.getString(Constants.RCD_TWITTER_URL)
            twitterEditText.setText(twitterURL, TextView.BufferType.EDITABLE)

        val linkedinURL:String?=preferenceManager?.getString(Constants.RCD_LINKEDIN_URL)
            linkedinEditText.setText(linkedinURL, TextView.BufferType.EDITABLE)

        val facebookURL:String?=preferenceManager?.getString(Constants.RCD_FACEBOOK_URL)
            facebookEditText.setText(facebookURL, TextView.BufferType.EDITABLE)

        val instagramURL:String?=preferenceManager?.getString(Constants.RCD_INSTA_URL)
            instagramEditText.setText(instagramURL, TextView.BufferType.EDITABLE)

        val websiteURL:String?=preferenceManager?.getString(Constants.RCD_WEB_URL)
            websiteEditText.setText(websiteURL, TextView.BufferType.EDITABLE)

        val name:String?=preferenceManager?.getString(Constants.RCD_NAME)
            nameEditText.setText(name, TextView.BufferType.EDITABLE)

        val email:String?=preferenceManager?.getString(Constants.RCD_EMAIL)
            emailEditText.setText(email, TextView.BufferType.EDITABLE)

        val phoneOne:String?=preferenceManager?.getString(Constants.RCD_PHONE_ONE)
            phoneOneEditText.setText(phoneOne, TextView.BufferType.EDITABLE)

        val phoneTwo:String?=preferenceManager?.getString(Constants.RCD_PHONE_TWO)
            phoneTwoEditText.setText(phoneTwo, TextView.BufferType.EDITABLE)

        val address:String?=preferenceManager?.getString(Constants.RCD_ADDRESS)
            addressEditText.setText(address, TextView.BufferType.EDITABLE)

        updateButton.setOnClickListener{
            val name:String=nameEditText.text.toString()
                preferenceManager!!.putString(Constants.RCD_NAME,name)

            val email:String=emailEditText.text.toString()
                preferenceManager!!.putString(Constants.RCD_EMAIL,email)

            val phoneOne:String=phoneOneEditText.text.toString()
                preferenceManager!!.putString(Constants.RCD_PHONE_ONE,phoneOne)

            val phoneTwo:String=phoneTwoEditText.text.toString()
                preferenceManager!!.putString(Constants.RCD_PHONE_TWO,phoneTwo)

            val address:String=addressEditText.text.toString()
                 preferenceManager!!.putString(Constants.RCD_ADDRESS,address)

            val twitter:String=twitterEditText.text.toString()
                preferenceManager!!.putString(Constants.RCD_TWITTER_URL,twitter)
            val twitterFlag:Boolean=twitterCheckBox.isChecked
                preferenceManager!!.putBoolean(Constants.RCD_TWITTER_URL_FLAG,twitterFlag)

            val linkedin:String=linkedinEditText.text.toString()
                preferenceManager!!.putString(Constants.RCD_LINKEDIN_URL,linkedin)
            val linkedinFlag:Boolean=linkedinCheckBox.isChecked
                preferenceManager!!.putBoolean(Constants.RCD_LINKEDIN_URL_FLAG,linkedinFlag)

            val facebook:String=facebookEditText.text.toString()
                preferenceManager!!.putString(Constants.RCD_FACEBOOK_URL,facebook)
            val facebookFlag:Boolean=facebookCheckBox.isChecked
                preferenceManager!!.putBoolean(Constants.RCD_FACEBOOK_URL_FLAG,facebookFlag)

            val instagram:String=instagramEditText.text.toString()
                preferenceManager!!.putString(Constants.RCD_INSTA_URL,instagram)
            val instagramFlag:Boolean=instagramCheckBox.isChecked
                preferenceManager!!.putBoolean(Constants.RCD_INSTA_URL_FLAG,instagramFlag)

            val website:String=websiteEditText.text.toString()
                preferenceManager!!.putString(Constants.RCD_WEB_URL,website)
            val websiteFlag:Boolean=websiteCheckBox.isChecked
                preferenceManager!!.putBoolean(Constants.RCD_WEB_URL_FLAG,websiteFlag)

            Toast.makeText(this,"A profile was successfully updated",Toast.LENGTH_LONG).show()
                finish()

        }
    }

    override fun onResume() {
        super.onResume()
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        updateMenuItemColors(menu)
        return super.onCreateOptionsMenu(menu)
    }

}
