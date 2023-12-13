package com.likeminds.feedsx

import android.util.Log
import com.likeminds.feedsx.SDKApplication.Companion.LOG_TAG

object LMFeedAnalytics {
    /*
    * Event names variables
    * */
    object Events {
        const val POST_CREATION_STARTED = "Post creation started"
        const val CLICKED_ON_ATTACHMENT = "Clicked on Attachment"
        const val USER_TAGGED_IN_POST = "User tagged in a post"
        const val LINK_ATTACHED_IN_POST = "link attached in the post"
        const val IMAGE_ATTACHED_TO_POST = "Image attached to post"
        const val VIDEO_ATTACHED_TO_POST = "Video attached to post"
        const val DOCUMENT_ATTACHED_TO_POST = "Document attached in post"
        const val POST_CREATION_COMPLETED = "Post creation completed"
        const val POST_PINNED = "Post pinned"
        const val POST_UNPINNED = "Post unpinned"
        const val POST_REPORTED = "Post reported"
        const val POST_DELETED = "Post deleted"
        const val FEED_OPENED = "Feed opened"
        const val LIKE_LIST_OPEN = "Like list open"
        const val COMMENT_LIST_OPEN = "Comment list open"
        const val COMMENT_DELETED = "Comment deleted"
        const val COMMENT_REPORTED = "Comment reported"
        const val COMMENT_POSTED = "Comment posted"
        const val REPLY_POSTED = "Reply posted"
        const val REPLY_DELETED = "Reply deleted"
        const val REPLY_REPORTED = "Reply reported"
        const val POST_EDITED = "Post edited"
        const val POST_SHARED = "Post shared"
        const val POST_LIKED  = "Post Liked"
        const val POST_UNLIKED  = "Post Unliked"
        const val COMMENT_LIKED  = "Comment Liked"
        const val COMMENT_UNLIKED  = "Comment Unliked"

        const val NOTIFICATION_RECEIVED = "Notification Received"
        const val NOTIFICATION_CLICKED = "Notification Clicked"

        const val NOTIFICATION_PAGE_OPENED = "Notification page opened"
    }

    /*
    * Event keys variables
    * */
    object Keys {
        const val POST_ID = "post_id"
        const val USER_ID = "user_id"
        const val UUID = "uuid"
        const val COMMENT_ID = "comment_id"
        const val COMMENT_REPLY_ID = "comment_reply_id"
        const val POST_TYPE_TEXT = "text"
        const val POST_TYPE_IMAGE = "image"
        const val POST_TYPE_VIDEO = "video"
        const val POST_TYPE_IMAGE_VIDEO = "image,video"
        const val POST_TYPE_DOCUMENT = "document"
        const val POST_TYPE_LINK = "link"
    }

    /**
     * Source keys variables
     **/
    object Source {
        const val DEEP_LINK = "deep_link"
        const val NOTIFICATION = "notification"
        const val UNIVERSAL_FEED = "universal_feed"
        const val POST_DETAIL = "post_detail"
    }

    /**
     * called to trigger events
     * @param eventName - name of the event to trigger
     * @param eventProperties - {key: value} pair for properties related to event
     * */
    fun track(eventName: String, eventProperties: Map<String, String?> = mapOf()) {
        Log.d(
            LOG_TAG, """
            eventName: $eventName
            eventProperties: $eventProperties
        """.trimIndent()
        )
    }
}