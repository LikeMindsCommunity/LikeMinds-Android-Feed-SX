package com.likeminds.feedsx

import android.app.Application
import android.content.Context
import com.amazonaws.mobile.client.*
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.likeminds.feedsx.branding.model.LMFeedBranding
import com.likeminds.feedsx.branding.model.SetFeedBrandingRequest
import com.likeminds.feedsx.di.DaggerLikeMindsFeedComponent
import com.likeminds.feedsx.di.LikeMindsFeedComponent
import com.likeminds.feedsx.di.feed.FeedComponent
import com.likeminds.feedsx.di.likes.LikesComponent
import com.likeminds.feedsx.di.media.MediaComponent
import com.likeminds.feedsx.di.moderation.reasonchoose.ReasonChooseComponent
import com.likeminds.feedsx.di.moderation.report.ReportComponent
import com.likeminds.feedsx.di.notificationfeed.NotificationFeedComponent
import com.likeminds.feedsx.di.post.create.CreatePostComponent
import com.likeminds.feedsx.di.post.detail.PostDetailComponent
import com.likeminds.feedsx.di.post.edit.EditPostComponent
import com.likeminds.feedsx.di.topic.LMFeedTopicComponent
import com.likeminds.feedsx.post.PostWithAttachmentsRepository
import com.likeminds.likemindsfeed.LMCallback
import com.likeminds.likemindsfeed.LMFeedClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SDKApplication : LMCallback {

    @Inject
    lateinit var transferUtility: TransferUtility

    @Inject
    lateinit var postWithAttachmentsRepository: PostWithAttachmentsRepository

    private var likeMindsFeedComponent: LikeMindsFeedComponent? = null
    private var lmFeedComponent: FeedComponent? = null
    private var likesComponent: LikesComponent? = null
    private var mediaComponent: MediaComponent? = null
    private var notificationFeedComponent: NotificationFeedComponent? = null
    private var createPostComponent: CreatePostComponent? = null
    private var postDetailComponent: PostDetailComponent? = null
    private var editPostComponent: EditPostComponent? = null
    private var reportComponent: ReportComponent? = null
    private var reasonChooseComponent: ReasonChooseComponent? = null
    private var lmFeedTopicComponent: LMFeedTopicComponent? = null

    companion object {
        const val LOG_TAG = "LikeMindsFeed"
        private var sdkApplicationInstance: SDKApplication? = null
        private var lmUICallback: LMFeedUICallback? = null

        /**
         * @return Singleton Instance of SDK Application class, which used for injecting dagger in fragments.
         * */
        @JvmStatic
        fun getInstance(): SDKApplication {
            if (sdkApplicationInstance == null) {
                sdkApplicationInstance = SDKApplication()
            }
            return sdkApplicationInstance!!
        }
    }

    fun initSDKApplication(
        application: Application,
        lmUICallback: LMFeedUICallback?,
        brandingRequest: SetFeedBrandingRequest?
    ) {
        LMFeedClient.Builder(application)
            .lmCallback(this)
            .build()
        SDKApplication.lmUICallback = lmUICallback
        setupBranding(brandingRequest)
        initAppComponent(application)
        initAWSMobileClient(application)
    }

    // sets branding to the app
    fun setupBranding(setFeedBrandingRequest: SetFeedBrandingRequest?) {
        val updatedBrandingRequest = setFeedBrandingRequest ?: SetFeedBrandingRequest.Builder().build()
        LMFeedBranding.setBranding(updatedBrandingRequest)
    }

    private fun initAWSMobileClient(applicationContext: Context) {
        AWSMobileClient.getInstance()
            .initialize(applicationContext, object : Callback<UserStateDetails> {
                override fun onResult(result: UserStateDetails?) {
                }

                override fun onError(e: java.lang.Exception?) {
                }
            })
    }

    /**
     * initiate dagger for the sdk
     *
     * @param application : The client will pass instance application to the function
     * */
    private fun initAppComponent(application: Application) {
        if (likeMindsFeedComponent == null) {
            likeMindsFeedComponent = DaggerLikeMindsFeedComponent.builder()
                .application(application)
                .build()
        }
        likeMindsFeedComponent!!.inject(this)
    }

    /**
     * initiate and return FeedComponent: All dependencies required for feed package
     * */
    fun feedComponent(): FeedComponent? {
        if (lmFeedComponent == null) {
            lmFeedComponent = likeMindsFeedComponent?.feedComponent()?.create()
        }
        return lmFeedComponent
    }

    /**
     * initiate and return LikesComponent: All dependencies required for likes package
     * */
    fun likesComponent(): LikesComponent? {
        if (likesComponent == null) {
            likesComponent = likeMindsFeedComponent?.likesComponent()?.create()
        }
        return likesComponent
    }

    /**
     * initiate and return MediaComponent: All dependencies required for media package
     * */
    fun mediaComponent(): MediaComponent? {
        if (mediaComponent == null) {
            mediaComponent = likeMindsFeedComponent?.mediaComponent()?.create()
        }
        return mediaComponent
    }

    /**
     * initiate and return NotificationFeedComponent: All dependencies required for notificationfeed package
     * */
    fun notificationFeedComponent(): NotificationFeedComponent? {
        if (notificationFeedComponent == null) {
            notificationFeedComponent =
                likeMindsFeedComponent?.notificationFeedComponent()?.create()
        }
        return notificationFeedComponent
    }

    /**
     * initiate and return CreatePostComponent: All dependencies required for createpost package
     * */
    fun createPostComponent(): CreatePostComponent? {
        if (createPostComponent == null) {
            createPostComponent = likeMindsFeedComponent?.createPostComponent()?.create()
        }
        return createPostComponent
    }

    /**
     * initiate and return PostDetailComponent: All dependencies required for postdetail package
     * */
    fun postDetailComponent(): PostDetailComponent? {
        if (postDetailComponent == null) {
            postDetailComponent = likeMindsFeedComponent?.postDetailComponent()?.create()
        }
        return postDetailComponent
    }

    /**
     * initiate and return EditPostComponent: All dependencies required for editpost package
     * */
    fun editPostComponent(): EditPostComponent? {
        if (editPostComponent == null) {
            editPostComponent = likeMindsFeedComponent?.editPostComponent()?.create()
        }
        return editPostComponent
    }

    /**
     * initiate and return ReportComponent: All dependencies required for report package
     * */
    fun reportComponent(): ReportComponent? {
        if (reportComponent == null) {
            reportComponent = likeMindsFeedComponent?.reportComponent()?.create()
        }
        return reportComponent
    }

    /**
     * initiate and return ReasonChooseComponent: All dependencies required for reason choose fragment
     * */
    fun reasonChooseComponent(): ReasonChooseComponent? {
        if (reasonChooseComponent == null) {
            reasonChooseComponent = likeMindsFeedComponent?.reasonChooseComponent()?.create()
        }
        return reasonChooseComponent
    }

    fun lmFeedTopicComponent(): LMFeedTopicComponent? {
        if (lmFeedTopicComponent == null) {
            lmFeedTopicComponent = likeMindsFeedComponent?.topicComponent()?.create()
        }

        return lmFeedTopicComponent
    }

    override fun login() {
        super.login()
        lmUICallback?.login()
    }
}