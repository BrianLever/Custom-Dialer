package com.simplemobiletools.commons.views

import android.content.ClipDescription
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.core.os.BuildCompat
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import com.simplemobiletools.commons.extensions.adjustAlpha
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.helpers.MEDIUM_ALPHA


class MyEditText : androidx.appcompat.widget.AppCompatEditText {
    private lateinit var imgTypeString: Array<String>
    private var keyBoardInputCallbackListener: KeyBoardInputCallbackListener? = null
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    fun setColors(textColor: Int, accentColor: Int, backgroundColor: Int) {
        background?.mutate()?.applyColorFilter(accentColor)
        // requires android:textCursorDrawable="@null" in xml to color the cursor too
        setTextColor(textColor)
        setHintTextColor(textColor.adjustAlpha(MEDIUM_ALPHA))
        setLinkTextColor(accentColor)
    }
    init {
        initView()
    }

    private fun initView() {
        imgTypeString = arrayOf(
            "image/png",
            "image/gif",
            "image/jpeg",
            "image/webp"
        )
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection? {
        val ic = super.onCreateInputConnection(outAttrs)
        if (outAttrs != null) {
            EditorInfoCompat.setContentMimeTypes(
                outAttrs,
                imgTypeString
            )
        }
        return outAttrs?.let { InputConnectionCompat.createWrapper(ic, it, callback) }
    }


    val callback: InputConnectionCompat.OnCommitContentListener =
        object : InputConnectionCompat.OnCommitContentListener {
            override fun onCommitContent(
                inputContentInfo: InputContentInfoCompat,
                flags: Int, opts: Bundle?
            ): Boolean {

                // read and display inputContentInfo asynchronously
                if (BuildCompat.isAtLeastNMR1() && flags and
                    InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION !== 0
                ) {
                    try {
                        inputContentInfo.requestPermission()
                    } catch (e: Exception) {
                        return false // return false if failed
                    }
                }
                var supported = false
                for (mimeType in imgTypeString) {
                    if (inputContentInfo.getDescription().hasMimeType(mimeType)) {
                        supported = true
                        break
                    }
                }
                if (!supported) {
                    return false
                }
                if (keyBoardInputCallbackListener != null) {
                    keyBoardInputCallbackListener!!.onCommitContent(inputContentInfo, flags, opts)
                }
                return true // return true if succeeded
            }
        }

    interface KeyBoardInputCallbackListener {
        fun onCommitContent(
            inputContentInfo: InputContentInfoCompat?,
            flags: Int, opts: Bundle?
        )
    }

    fun setKeyBoardInputCallbackListener(keyBoardInputCallbackListener: KeyBoardInputCallbackListener?) {
        this.keyBoardInputCallbackListener = keyBoardInputCallbackListener
    }

    fun getImgTypeString(): Array<String>? {
        return imgTypeString
    }

    fun setImgTypeString(imgTypeString: Array<String>) {
        this.imgTypeString = imgTypeString
    }
}

