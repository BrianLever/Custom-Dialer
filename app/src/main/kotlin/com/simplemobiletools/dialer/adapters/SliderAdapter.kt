package com.simplemobiletools.dialer.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import com.simplemobiletools.dialer.R
import com.simplemobiletools.dialer.models.SliderData

class SliderAdapter(
    val context: Context,
    private val sliderList: ArrayList<SliderData>
) : PagerAdapter() {

    override fun getCount(): Int {
        return sliderList.size
    }

    override fun isViewFromObject(mView: View, mObject: Any): Boolean {
        return mView === mObject
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {

        val layoutInflater: LayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = layoutInflater.inflate(R.layout.slider_item, container, false)
        val imageView: ImageView = view.findViewById(R.id.idIVSlider)
        val sliderHeadingTV: TextView = view.findViewById(R.id.idTVSliderTitle)
        val sliderDescTV: TextView = view.findViewById(R.id.idTVSliderDescription)
        val sliderData: SliderData = sliderList.get(position)
        imageView.setImageResource(sliderData.slideImage)
        sliderHeadingTV.text = sliderData.slideTitle
        sliderDescTV.text = sliderData.slideDescription
        container.addView(view)
        return view
    }

    override fun destroyItem(mContainer: ViewGroup, mPosition: Int, mObject: Any) {
        val v = mObject as View
        mContainer.removeView(v)
    }

}
