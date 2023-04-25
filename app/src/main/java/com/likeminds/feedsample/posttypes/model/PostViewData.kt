package com.likeminds.feedsample.posttypes.model

import android.os.Parcelable
import com.likeminds.feedsample.overflowmenu.model.OverflowMenuItemViewData
import com.likeminds.feedsample.utils.model.*
import kotlinx.parcelize.Parcelize

@Parcelize
class PostViewData private constructor(
    var id: String,
    var text: String?,
    val alreadySeenFullContent: Boolean?,
    val isExpanded: Boolean,
    var attachments: List<AttachmentViewData>,
    var communityId: Int,
    var isPinned: Boolean,
    var isSaved: Boolean,
    var isLiked: Boolean,
    var isEdited: Boolean,
    var userId: String,
    var likesCount: Int,
    var commentsCount: Int,
    var menuItems: List<OverflowMenuItemViewData>,
    var replies: MutableList<CommentViewData>,
    var createdAt: Long,
    var updatedAt: Long,
    var user: UserViewData,
    val fromPostLiked: Boolean,
    val fromPostSaved: Boolean,
    val fromVideoAction: Boolean,
    val thumbnail: String?,
    val uuid: String,
    val isPosted: Boolean,
    val temporaryId: Long?
) : Parcelable, BaseViewType {

    //TODO: add post id while adding menu item

    override val viewType: Int
        get() = when {
            (attachments.size == 1 && attachments.first().attachmentType == IMAGE) -> {
                ITEM_POST_SINGLE_IMAGE
            }
            (attachments.size == 1 && attachments.first().attachmentType == VIDEO) -> {
                ITEM_POST_SINGLE_VIDEO
            }
            (attachments.isNotEmpty() && attachments.first().attachmentType == DOCUMENT) -> {
                ITEM_POST_DOCUMENTS
            }
            (attachments.size > 1 && (attachments.first().attachmentType == IMAGE || attachments.first().attachmentType == VIDEO)) -> {
                ITEM_POST_MULTIPLE_MEDIA
            }
            (attachments.size == 1 && attachments.first().attachmentType == LINK) -> {
                ITEM_POST_LINK
            }
            else -> {
                ITEM_POST_TEXT_ONLY
            }
        }

    class Builder {
        private var id: String = ""
        private var text: String? = null
        private var alreadySeenFullContent: Boolean? = null
        private var isExpanded: Boolean = false
        private var attachments: List<AttachmentViewData> = listOf()
        private var communityId: Int = 0
        private var isPinned: Boolean = false
        private var isSaved: Boolean = false
        private var isLiked: Boolean = false
        private var isEdited: Boolean = false
        private var userId: String = ""
        private var likesCount: Int = 0
        private var commentsCount: Int = 0
        private var menuItems: List<OverflowMenuItemViewData> = listOf()
        private var replies: MutableList<CommentViewData> = mutableListOf()
        private var createdAt: Long = 0
        private var updatedAt: Long = 0
        private var user: UserViewData = UserViewData.Builder().build()
        private var fromPostLiked: Boolean = false
        private var fromPostSaved: Boolean = false
        private var thumbnail: String? = null
        private var uuid: String = ""
        private var isPosted: Boolean = false
        private var temporaryId: Long? = null
        private var fromVideoAction: Boolean = false

        fun id(id: String) = apply { this.id = id }
        fun text(text: String?) = apply { this.text = text }

        fun alreadySeenFullContent(alreadySeenFullContent: Boolean?) =
            apply { this.alreadySeenFullContent = alreadySeenFullContent }

        fun isExpanded(isExpanded: Boolean) =
            apply { this.isExpanded = isExpanded }

        fun attachments(attachments: List<AttachmentViewData>) =
            apply { this.attachments = attachments }

        fun communityId(communityId: Int) = apply { this.communityId = communityId }
        fun isPinned(isPinned: Boolean) = apply { this.isPinned = isPinned }
        fun isSaved(isSaved: Boolean) = apply { this.isSaved = isSaved }
        fun isLiked(isLiked: Boolean) = apply { this.isLiked = isLiked }
        fun isEdited(isEdited: Boolean) = apply { this.isEdited = isEdited }
        fun userId(userId: String) = apply { this.userId = userId }
        fun likesCount(likesCount: Int) = apply { this.likesCount = likesCount }
        fun commentsCount(commentsCount: Int) = apply { this.commentsCount = commentsCount }
        fun menuItems(menuItems: List<OverflowMenuItemViewData>) =
            apply { this.menuItems = menuItems }

        fun replies(replies: MutableList<CommentViewData>) = apply { this.replies = replies }
        fun createdAt(createdAt: Long) = apply { this.createdAt = createdAt }
        fun updatedAt(updatedAt: Long) = apply { this.updatedAt = updatedAt }
        fun user(user: UserViewData) = apply { this.user = user }
        fun fromPostLiked(fromPostLiked: Boolean) = apply { this.fromPostLiked = fromPostLiked }
        fun fromPostSaved(fromPostSaved: Boolean) = apply { this.fromPostSaved = fromPostSaved }
        fun fromVideoAction(fromVideoAction: Boolean) =
            apply { this.fromVideoAction = fromVideoAction }

        fun thumbnail(thumbnail: String?) = apply { this.thumbnail = thumbnail }
        fun uuid(uuid: String) = apply { this.uuid = uuid }
        fun isPosted(isPosted: Boolean) = apply { this.isPosted = isPosted }
        fun temporaryId(temporaryId: Long?) = apply { this.temporaryId = temporaryId }

        fun build() = PostViewData(
            id,
            text,
            alreadySeenFullContent,
            isExpanded,
            attachments,
            communityId,
            isPinned,
            isSaved,
            isLiked,
            isEdited,
            userId,
            likesCount,
            commentsCount,
            menuItems,
            replies,
            createdAt,
            updatedAt,
            user,
            fromPostLiked,
            fromPostSaved,
            fromVideoAction,
            thumbnail,
            uuid,
            isPosted,
            temporaryId
        )
    }

    fun toBuilder(): Builder {
        return Builder().id(id)
            .text(text)
            .alreadySeenFullContent(alreadySeenFullContent)
            .isExpanded(isExpanded)
            .attachments(attachments)
            .communityId(communityId)
            .isPinned(isPinned)
            .isSaved(isSaved)
            .isLiked(isLiked)
            .isEdited(isEdited)
            .userId(userId)
            .likesCount(likesCount)
            .commentsCount(commentsCount)
            .menuItems(menuItems)
            .replies(replies)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .user(user)
            .fromPostLiked(fromPostLiked)
            .fromPostSaved(fromPostSaved)
            .fromVideoAction(fromVideoAction)
            .thumbnail(thumbnail)
            .uuid(uuid)
            .isPosted(isPosted)
            .temporaryId(temporaryId)
    }
}