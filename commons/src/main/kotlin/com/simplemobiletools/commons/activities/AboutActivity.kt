package com.simplemobiletools.commons.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.View
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.dialogs.ConfirmationAdvancedDialog
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.RateStarsDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.FAQItem
import kotlinx.android.synthetic.main.activity_about.*
import java.util.*

class AboutActivity : BaseSimpleActivity() {
    private var appName = ""
    private var linkColor = 0

    override fun getAppIconIDs() = intent.getIntegerArrayListExtra(APP_ICON_IDS) ?: ArrayList()

    override fun getAppLauncherName() = intent.getStringExtra(APP_LAUNCHER_NAME) ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        appName = intent.getStringExtra(APP_NAME) ?: ""
        linkColor = getAdjustedPrimaryColor()

       arrayOf(
           about_faq_icon,
           about_email_icon,
           about_rate_us_icon,
           about_invite_icon,
           about_website_icon,
           about_privacy_policy_icon,
           about_version_icon
       ).forEach {
           it.applyColorFilter(baseConfig.textColor)
       }
    }

    override fun onResume() {
        super.onResume()
      //  updateTextColors(about_holder)

        setupWebsite()
        setupEmail()
        setupFAQ()
      //  setupUpgradeToPro()
      //  setupMoreApps()
        setupRateUs()
        setupInvite()
       // setupLicense()
        //setupFacebook()
        //setupReddit()
        //setupCopyright()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        updateMenuItemColors(menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun setupWebsite() {
        val websiteText = String.format(getString(R.string.two_string_placeholder), getString(R.string.website_label), getString(R.string.my_website))
        val textwebsite = "www.inyte.com"
        about_website.text = textwebsite
    }

    private fun setupEmail() {
        val label = getString(R.string.email_label)
        val email = "help@inyte.com"

        val appVersion = String.format(getString(R.string.app_version, intent.getStringExtra(APP_VERSION_NAME)))
        val deviceOS = String.format(getString(R.string.device_os), Build.VERSION.RELEASE)
        val newline = "%0D%0A"
        val separator = "------------------------------"
        val body = "$appVersion$newline$deviceOS$newline$separator$newline$newline"
        val href = "$label<br><a href=\"mailto:$email?subject=$appName&body=$body\">$email</a>"
        about_email.text = Html.fromHtml(href)

        if (intent.getBooleanExtra(SHOW_FAQ_BEFORE_MAIL, false) && !baseConfig.wasBeforeAskingShown) {
            about_email.setOnClickListener {
                baseConfig.wasBeforeAskingShown = true
                about_email.movementMethod = LinkMovementMethod.getInstance()
                about_email.setOnClickListener(null)
                val msg = "${getString(R.string.before_asking_question_read_faq)}\n\n${getString(R.string.make_sure_latest)}"
                ConfirmationDialog(this, msg, 0, R.string.read_faq, R.string.skip) {
                   // about_faq_label.performClick()
                }
            }
        } else {
            about_email.movementMethod = LinkMovementMethod.getInstance()

        }
    }

    private fun setupFAQ() {
        val faqItems = intent.getSerializableExtra(APP_FAQ) as ArrayList<FAQItem>


        about_faq.beVisibleIf(faqItems.isNotEmpty())
        about_faq.setOnClickListener {
            openFAQ(faqItems)
        }

        about_faq.setTextColor(linkColor)
        about_faq.underlineText()
    }



    private fun openFAQ(faqItems: ArrayList<FAQItem>) {
        Intent(applicationContext, FAQActivity::class.java).apply {
            putExtra(APP_ICON_IDS, getAppIconIDs())
            putExtra(APP_LAUNCHER_NAME, getAppLauncherName())
            putExtra(APP_FAQ, faqItems)
            startActivity(this)
        }
    }



    private fun setupInvite() {
        about_invite.setOnClickListener {
            val text = String.format(getString(R.string.share_text), appName, getStoreUrl())
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_SUBJECT, appName)
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
                startActivity(Intent.createChooser(this, getString(R.string.invite_via)))
            }
        }
        about_invite.setTextColor(linkColor)
    }

    private fun setupRateUs() {
        if (baseConfig.appRunCount < 5) {
            about_rate_us.visibility = View.GONE
        } else {
            about_rate_us.setOnClickListener {
                if (baseConfig.wasBeforeRateShown) {
                    if (baseConfig.wasAppRated) {
                        redirectToRateUs()
                    } else {
                        RateStarsDialog(this)
                    }
                } else {
                    baseConfig.wasBeforeRateShown = true
                    val msg = "${getString(R.string.before_rate_read_faq)}\n\n${getString(R.string.make_sure_latest)}"
                    ConfirmationAdvancedDialog(this, msg, 0, R.string.read_faq, R.string.skip) {
                        if (it) {
                            about_rate_us.performClick()
                        } else {
                            about_rate_us.performClick()
                        }
                    }
                }
            }
        }
        about_rate_us.setTextColor(linkColor)
    }




}
