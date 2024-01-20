package com.likeminds.feedsx.branding.customview

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import com.likeminds.feedsx.R
import com.likeminds.feedsx.branding.model.LMFeedBranding
import com.likeminds.feedsx.branding.util.FeedBrandingUtil


internal class LMFeedEditText @JvmOverloads constructor(
    mContext: Context,
    attributeSet: AttributeSet? = null,
    defStyle: Int = android.R.attr.editTextStyle
) : AppCompatEditText(mContext, attributeSet, defStyle) {

    init {
        // fonts
        val array = context.obtainStyledAttributes(attributeSet, R.styleable.LMFeedEditText)
        this.typeface = FeedBrandingUtil.getTypeFace(
            context,
            array.getString(R.styleable.LMFeedEditText_lm_feed_font_style)
        )

        //edittext background color
        val etType = array.getString(R.styleable.LMFeedEditText_lm_feed_et_type)
        if (etType.equals("special"))
            this.backgroundTintList = ColorStateList.valueOf(LMFeedBranding.getButtonsColor())

        array.recycle()
    }

    private var showKeyboardDelayed = false

    @Override
    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        maybeShowKeyboard()
    }

    /**
     * Will request focus and try to show the keyboard.
     * It has into account if the containing window has focus or not yet.
     * And delays the call to show keyboard until it's gained.
     */
    fun focusAndShowKeyboard() {
        requestFocus()
        showKeyboardDelayed = true
        maybeShowKeyboard()
    }

    private fun maybeShowKeyboard() {
        if (hasWindowFocus() && showKeyboardDelayed) {
            if (isFocused) {
                post {
                    val imm =
                        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(this@LMFeedEditText, InputMethodManager.SHOW_IMPLICIT)
                }
            }
            showKeyboardDelayed = false
        }
    }
}