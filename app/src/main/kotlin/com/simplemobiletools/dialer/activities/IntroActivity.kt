package com.simplemobiletools.dialer.activities

import com.simplemobiletools.dialer.adapters.SliderAdapter
import com.simplemobiletools.dialer.models.SliderData
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.simplemobiletools.dialer.R

class IntroActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager
    lateinit var sliderAdapter: SliderAdapter
    lateinit var sliderList: ArrayList<SliderData>
    lateinit var skipBtn: Button
    lateinit var indicatorSlideOneTV: TextView
    lateinit var indicatorSlideTwoTV: TextView
    lateinit var indicatorSlideThreeTV: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)
        viewPager = findViewById(R.id.idViewPager)
        skipBtn = findViewById(R.id.idBtnSkip)
        indicatorSlideOneTV = findViewById(R.id.idTVSlideOne)
        indicatorSlideTwoTV = findViewById(R.id.idTVSlideTwo)
        indicatorSlideThreeTV = findViewById(R.id.idTVSlideThree)
        skipBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        sliderList = ArrayList()
        sliderList.add(
            SliderData(
                resources.getString(R.string.p2p_video_call),
                resources.getString(R.string.slider_one_substring),
                R.mipmap.p2p
            )
        )
        sliderList.add(
            SliderData(
                resources.getString(R.string.rich_call_data),
                resources.getString(R.string.slider_two_substring),
                R.mipmap.rcd
            )
        )
        sliderList.add(
            SliderData(
                resources.getString(R.string.conference_video_call),
                resources.getString(R.string.slider_three_substring),
                R.mipmap.conference
            )
        )
        sliderAdapter = SliderAdapter(this, sliderList)
        viewPager.adapter = sliderAdapter
        viewPager.addOnPageChangeListener(viewListener)
        addBottomDots()

    }
    private fun addBottomDots() {
        indicatorSlideTwoTV.setTextColor(resources.getColor(R.color.amp_gray))
        indicatorSlideThreeTV.setTextColor(resources.getColor(R.color.amp_gray))
        indicatorSlideOneTV.setTextColor(resources.getColor(R.color.md_blue))
    }
    private var viewListener: ViewPager.OnPageChangeListener = object : ViewPager.OnPageChangeListener {
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
        }
        override fun onPageSelected(position: Int) {
            when (position) {
                0 -> {
                    indicatorSlideTwoTV.setTextColor(resources.getColor(R.color.amp_gray))
                    indicatorSlideThreeTV.setTextColor(resources.getColor(R.color.amp_gray))
                    indicatorSlideOneTV.setTextColor(resources.getColor(R.color.md_blue))
                }
                1 ->{
                    indicatorSlideTwoTV.setTextColor(resources.getColor(R.color.md_blue))
                    indicatorSlideThreeTV.setTextColor(resources.getColor(R.color.amp_gray))
                    indicatorSlideOneTV.setTextColor(resources.getColor(R.color.amp_gray))
                }
                else ->{
                    indicatorSlideTwoTV.setTextColor(resources.getColor(R.color.amp_gray))
                    indicatorSlideThreeTV.setTextColor(resources.getColor(R.color.md_blue))
                    indicatorSlideOneTV.setTextColor(resources.getColor(R.color.amp_gray))
                }
            }
        }

        override fun onPageScrollStateChanged(state: Int) {}
    }
}
