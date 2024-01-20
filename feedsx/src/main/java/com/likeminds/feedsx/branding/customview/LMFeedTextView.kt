package com.likeminds.feedsx.branding.customview

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.likeminds.feedsx.R
import com.likeminds.feedsx.branding.model.LMFeedBranding
import com.likeminds.feedsx.branding.util.FeedBrandingUtil

internal class LMFeedTextView : AppCompatTextView {

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
        val array = context.obtainStyledAttributes(attrs, R.styleable.LMFeedTextView)
        typeface = FeedBrandingUtil.getTypeFace(
            context,
            array.getString(R.styleable.LMFeedTextView_lm_feed_fontType)
        )

        //text color
        setTextColor(
            FeedBrandingUtil.getTextColor(
                this.currentTextColor,
                array.getString(R.styleable.LMFeedTextView_lm_feed_textType)
            )
        )

        // sets text link color
        setLinkTextColor(LMFeedBranding.getTextLinkColor())

        array.recycle()
    }
}