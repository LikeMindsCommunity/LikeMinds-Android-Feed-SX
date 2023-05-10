package com.likeminds.feedsampleapp

import android.app.Application
import com.likeminds.feedsx.LikeMindsFeedUI
import com.likeminds.feedsx.branding.model.LMFonts
import com.likeminds.feedsx.branding.model.SetBrandingRequest
import com.likeminds.likemindsfeed.LMCallback
import com.likeminds.likemindsfeed.LMFeedClient

class FeedSXApplication : Application(), LMCallback {

    companion object {
        const val LOG_TAG = "LikeMinds"
        private var feedSXApplication: FeedSXApplication? = null

        @JvmStatic
        fun getInstance(): FeedSXApplication {
            if (feedSXApplication == null) {
                feedSXApplication = FeedSXApplication()
            }
            return feedSXApplication!!
        }
    }

    override fun onCreate() {
        super.onCreate()

        // extras to instantiate LMFeedClient
        val extra = LMFeedClient.Builder(this)
            .lmCallback(this)
            .build()
        val brandingRequest = SetBrandingRequest.Builder()
            .headerColor("#02A8F3")
            .buttonsColor("#4BAE4F")
            .textLinkColor("#FE9700")
            .fonts(
                LMFonts.Builder()
                    .bold("fonts/montserrat-bold.ttf")
                    .medium("fonts/montserrat-medium.ttf")
                    .regular("fonts/montserrat-regular.ttf")
                    .build()
            )
            .build()

        LikeMindsFeedUI.initLikeMindsFeedUI(
            this,
            brandingRequest
        )
    }

    override fun login() {
        super.login()
    }
}