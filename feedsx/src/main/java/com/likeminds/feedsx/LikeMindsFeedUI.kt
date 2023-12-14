package com.likeminds.feedsx

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.likeminds.feedsx.branding.model.SetFeedBrandingRequest
import com.likeminds.feedsx.feed.model.LMFeedExtras
import com.likeminds.feedsx.feed.view.LMFeedFragment

object LikeMindsFeedUI {

    /**
     * Call this function to configure SDK in client's app
     *
     * @param application: application instance of client's app
     * @param brandingRequest: branding request from client
     **/
    fun initLikeMindsFeedUI(
        application: Application,
        lmUICallback: LMFeedUICallback?,
        brandingRequest: SetFeedBrandingRequest?
    ) {
        Log.d(SDKApplication.LOG_TAG, "initiate LikeMindsFeedUI called")

        //create object of SDKApplication
        val sdk = SDKApplication.getInstance()

        //call initSDKApplication to initialise sdk
        sdk.initSDKApplication(
            application,
            lmUICallback,
            brandingRequest
        )
    }

    /**
     * Call this function to initiate feed in the client's app
     * this function will show home screen of the sdk
     *
     * @param activity: instance of the activity
     * @param containerViewId: Id of the container (FrameLayout or FragmentContainerView) to show the home feed
     * @param apiKey : API key of the community
     * @param userName : Name of the user
     * @param uuid : uuid of the user
     * @param isGuest | nullable: is user a guest user of not
     **/
    fun initFeed(
        activity: AppCompatActivity,
        containerViewId: Int,
        apiKey: String,
        userName: String,
        uuid: String,
        isGuest: Boolean
    ) {
        Log.d(SDKApplication.LOG_TAG, "initiate feed called")
        Log.d(
            SDKApplication.LOG_TAG, """
            container id: $containerViewId
            user_name: $userName
            uuid: $uuid
            isGuest: $isGuest
        """.trimIndent()
        )

        val extras = LMFeedExtras.Builder()
            .apiKey(apiKey)
            .uuid(uuid)
            .userName(userName)
            .isGuest(isGuest)
            .build()

        val fragment = LMFeedFragment.getInstance(extras)

        val transaction = activity.supportFragmentManager.beginTransaction()
        transaction.replace(containerViewId, fragment, containerViewId.toString())
        transaction.setReorderingAllowed(true)
        Log.d(SDKApplication.LOG_TAG, "showing feed")
        transaction.commitNowAllowingStateLoss()
    }

    fun setBranding(brandingRequest: SetFeedBrandingRequest) {
        val sdk = SDKApplication.getInstance()
        sdk.setupBranding(brandingRequest)
    }
}