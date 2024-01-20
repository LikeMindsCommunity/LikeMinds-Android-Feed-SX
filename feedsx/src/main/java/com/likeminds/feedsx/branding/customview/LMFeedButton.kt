package com.likeminds.feedsx.branding.customview

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
import com.likeminds.feedsx.R
import com.likeminds.feedsx.branding.model.LMFeedBranding
import com.likeminds.feedsx.branding.util.FeedBrandingUtil

internal class LMFeedButton : MaterialButton {
    constructor(context: Context) : super(context) {
        initiate(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initiate(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        initiate(attrs)
    }

    private fun initiate(attrs: AttributeSet?) {
        // fonts
        val array = context.obtainStyledAttributes(attrs, R.styleable.LMFeedButton)
        val fontStyle = array.getString(R.styleable.LMFeedButton_lm_feed_font_type)
        val buttonType = array.getString(R.styleable.LMFeedButton_lm_feed_button_type)
        val textType = array.getString(R.styleable.LMFeedButton_lm_feed_text_type)
        typeface = FeedBrandingUtil.getTypeFace(context, fontStyle)
        array.recycle()

        // applies button color to button drawables
        compoundDrawables.forEach {
            it?.setTintList(ColorStateList.valueOf(LMFeedBranding.getButtonsColor()))
        }

        // bg color
        if (!buttonType.equals("normal")) {
            backgroundTintList = ColorStateList.valueOf(LMFeedBranding.getButtonsColor())
        }

        // text color
        if (!textType.equals("normal")) {
            setTextColor(LMFeedBranding.getButtonsColor())
        }
    }
}