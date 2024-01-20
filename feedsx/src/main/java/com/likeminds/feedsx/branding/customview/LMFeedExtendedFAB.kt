package com.likeminds.feedsx.branding.customview

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.likeminds.feedsx.R
import com.likeminds.feedsx.branding.model.LMFeedBranding
import com.likeminds.feedsx.branding.util.FeedBrandingUtil

internal class LMFeedExtendedFAB : ExtendedFloatingActionButton {
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
        val array = context.obtainStyledAttributes(attrs, R.styleable.LMFeedExtendedFAB)
        val fontStyle = array.getString(R.styleable.LMFeedExtendedFAB_lm_feed_font_Style)
        this.typeface = FeedBrandingUtil.getTypeFace(context, fontStyle)
        array.recycle()

        // color
        this.backgroundTintList = ColorStateList.valueOf(LMFeedBranding.getButtonsColor())
    }
}