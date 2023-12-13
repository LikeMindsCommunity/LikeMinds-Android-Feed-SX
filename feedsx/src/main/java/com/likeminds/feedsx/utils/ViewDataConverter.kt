package com.likeminds.feedsx.utils

import android.net.Uri
import android.util.Base64
import com.likeminds.feedsx.db.models.*
import com.likeminds.feedsx.delete.model.ReasonChooseViewData
import com.likeminds.feedsx.likes.model.LikeViewData
import com.likeminds.feedsx.media.model.IMAGE
import com.likeminds.feedsx.media.model.PDF
import com.likeminds.feedsx.media.model.SingleUriData
import com.likeminds.feedsx.media.model.VIDEO
import com.likeminds.feedsx.notificationfeed.model.ActivityEntityViewData
import com.likeminds.feedsx.notificationfeed.model.ActivityViewData
import com.likeminds.feedsx.overflowmenu.model.OverflowMenuItemViewData
import com.likeminds.feedsx.post.detail.model.CommentsCountViewData
import com.likeminds.feedsx.posttypes.model.AttachmentMetaViewData
import com.likeminds.feedsx.posttypes.model.AttachmentViewData
import com.likeminds.feedsx.posttypes.model.CommentViewData
import com.likeminds.feedsx.posttypes.model.DOCUMENT
import com.likeminds.feedsx.posttypes.model.LINK
import com.likeminds.feedsx.posttypes.model.LinkOGTagsViewData
import com.likeminds.feedsx.posttypes.model.PostViewData
import com.likeminds.feedsx.posttypes.model.SDKClientInfoViewData
import com.likeminds.feedsx.posttypes.model.UserViewData
import com.likeminds.feedsx.report.model.ReportTagViewData
import com.likeminds.feedsx.topic.model.LMFeedTopicViewData
import com.likeminds.feedsx.utils.mediauploader.utils.AWSKeys
import com.likeminds.feedsx.utils.membertagging.model.UserTagViewData
import com.likeminds.feedsx.utils.model.*
import com.likeminds.likemindsfeed.comment.model.Comment
import com.likeminds.likemindsfeed.configuration.model.Configuration
import com.likeminds.likemindsfeed.initiateUser.model.ManagementRightPermissionData
import com.likeminds.likemindsfeed.moderation.model.ReportTag
import com.likeminds.likemindsfeed.notificationfeed.model.Activity
import com.likeminds.likemindsfeed.notificationfeed.model.ActivityEntityData
import com.likeminds.likemindsfeed.post.model.*
import com.likeminds.likemindsfeed.sdk.model.SDKClientInfo
import com.likeminds.likemindsfeed.sdk.model.User
import com.likeminds.likemindsfeed.topic.model.Topic
import org.json.JSONObject

object ViewDataConverter {

    /**--------------------------------
     * Media Model -> View Data Model
    --------------------------------*/

    // Converts the SingleUriData (contains the data of media) to AttachmentViewData
    fun convertSingleDataUri(singleUriData: SingleUriData): AttachmentViewData {
        val attachmentType: Int?
        val viewType = when (singleUriData.fileType) {
            IMAGE -> {
                attachmentType = com.likeminds.feedsx.posttypes.model.IMAGE
                ITEM_CREATE_POST_MULTIPLE_MEDIA_IMAGE
            }

            VIDEO -> {
                attachmentType = com.likeminds.feedsx.posttypes.model.VIDEO
                ITEM_CREATE_POST_MULTIPLE_MEDIA_VIDEO
            }

            else -> {
                attachmentType = DOCUMENT
                ITEM_CREATE_POST_DOCUMENTS_ITEM
            }
        }
        return AttachmentViewData.Builder()
            .dynamicViewType(viewType)
            .attachmentType(attachmentType)
            .attachmentMeta(
                AttachmentMetaViewData.Builder()
                    .name(singleUriData.mediaName)
                    .duration(singleUriData.duration)
                    .format(singleUriData.format)
                    .pageCount(singleUriData.pdfPageCount)
                    .width(singleUriData.width)
                    .height(singleUriData.height)
                    .size(singleUriData.size)
                    .uri(singleUriData.uri)
                    .build()
            )
            .build()
    }

    // converts internal media filetype to FileType
    fun convertFileType(fileType: String): Int {
        return when (fileType) {
            IMAGE -> com.likeminds.feedsx.utils.mediauploader.model.IMAGE
            PDF -> com.likeminds.feedsx.utils.mediauploader.model.PDF
            VIDEO -> com.likeminds.feedsx.utils.mediauploader.model.VIDEO
            else -> com.likeminds.feedsx.utils.mediauploader.model.IMAGE
        }
    }

    /**--------------------------------
     * View Data Model -> Network Model
    --------------------------------*/

    fun createAttachments(
        attachments: List<AttachmentViewData>
    ): List<Attachment> {
        return attachments.map {
            convertAttachment(it)
        }
    }

    fun convertAttachment(
        attachment: AttachmentViewData
    ): Attachment {
        return Attachment.Builder()
            .attachmentType(attachment.attachmentType)
            .attachmentMeta(convertAttachmentMeta(attachment.attachmentMeta))
            .build()
    }

    private fun convertAttachmentMeta(
        attachmentMeta: AttachmentMetaViewData
    ): AttachmentMeta {
        return AttachmentMeta.Builder().name(attachmentMeta.name)
            .ogTags(convertOGTags(attachmentMeta.ogTags))
            .url(attachmentMeta.url)
            .size(attachmentMeta.size)
            .duration(attachmentMeta.duration)
            .pageCount(attachmentMeta.pageCount)
            .format(attachmentMeta.format)
            .build()
    }

    // creates attachment list of Network Model for link attachment
    fun convertAttachments(
        linkOGTagsViewData: LinkOGTagsViewData
    ): List<Attachment> {
        return listOf(
            Attachment.Builder()
                .attachmentType(LINK)
                .attachmentMeta(convertAttachmentMeta(linkOGTagsViewData))
                .build()
        )
    }

    // creates AttachmentMeta Network Model for link attachment meta
    private fun convertAttachmentMeta(
        linkOGTagsViewData: LinkOGTagsViewData
    ): AttachmentMeta {
        return AttachmentMeta.Builder()
            .ogTags(convertOGTags(linkOGTagsViewData))
            .build()
    }

    // converts LinkOGTags view data model to network model
    private fun convertOGTags(
        linkOGTagsViewData: LinkOGTagsViewData
    ): LinkOGTags {
        return LinkOGTags.Builder()
            .title(linkOGTagsViewData.title)
            .image(linkOGTagsViewData.image)
            .description(linkOGTagsViewData.description)
            .url(linkOGTagsViewData.url)
            .build()
    }

    /**--------------------------------
     * Network Model -> View Data Model
    --------------------------------*/

    // converts User network model to view data model
    fun convertUser(
        user: User?
    ): UserViewData {
        if (user == null) return UserViewData.Builder().build()
        return UserViewData.Builder()
            .id(user.id)
            .name(user.name)
            .imageUrl(user.imageUrl)
            .userUniqueId(user.userUniqueId)
            .customTitle(user.customTitle)
            .isGuest(user.isGuest)
            .isDeleted(user.isDeleted)
            .uuid(user.uuid)
            .sdkClientInfoViewData(convertSDKClientInfo(user.sdkClientInfo))
            .build()
    }

    private fun convertSDKClientInfo(sdkClientInfo: SDKClientInfo): SDKClientInfoViewData {
        return SDKClientInfoViewData.Builder()
            .user(sdkClientInfo.user)
            .uuid(sdkClientInfo.uuid)
            .userUniqueId(sdkClientInfo.userUniqueId)
            .community(sdkClientInfo.community)
            .build()
    }

    //created a deleted user object
    private fun createDeletedUser(): UserViewData {
        val tempUserId = (System.currentTimeMillis() / 1000).toInt()
        return UserViewData.Builder()
            .id(tempUserId)
            .name("Deleted User")
            .imageUrl("")
            .userUniqueId("$tempUserId")
            .customTitle(null)
            .isGuest(false)
            .isDeleted(true)
            .build()
    }

    /**
     * convert list of [Post] and usersMap [Map] of String to User
     * to list of [PostViewData]
     *
     * @param posts: list of [Post]
     * @param usersMap: [Map] of String to User
     * */
    fun convertUniversalFeedPosts(
        posts: List<Post>,
        usersMap: Map<String, User>,
        topicsMap: Map<String, Topic>
    ): List<PostViewData> {
        return posts.map { post ->
            convertPost(post, usersMap, topicsMap)
        }
    }

    /**
     * converts [Post] and usersMap [Map] of String to User
     * to [PostViewData]
     *
     * @param post: a post [Post]
     * @param usersMap: [Map] of String to User
     * */
    fun convertPost(
        post: Post,
        usersMap: Map<String, User>,
        topicsMap: Map<String, Topic>
    ): PostViewData {
        val postCreator = post.uuid
        val user = usersMap[postCreator]
        val postId = post.id
        val replies = post.replies?.toMutableList()
        val topicsId = post.topicIds ?: emptyList()

        val userViewData = if (user == null) {
            createDeletedUser()
        } else {
            convertUser(user)
        }

        val topicsViewData = topicsId.mapNotNull { topicId ->
            topicsMap[topicId]
        }.map { topic ->
            convertTopic(topic)
        }

        return PostViewData.Builder()
            .id(postId)
            .text(post.text)
            .communityId(post.communityId)
            .isPinned(post.isPinned)
            .isSaved(post.isSaved)
            .isLiked(post.isLiked)
            .isEdited(post.isEdited)
            .menuItems(convertOverflowMenuItems(post.menuItems))
            .replies(
                convertComments(
                    replies,
                    usersMap,
                    postId
                )
            )
            .attachments(convertAttachments(post.attachments, postId))
            .userId(postCreator)
            .likesCount(post.likesCount)
            .commentsCount(post.commentsCount)
            .createdAt(post.createdAt)
            .updatedAt(post.updatedAt)
            .user(userViewData)
            .uuid(postCreator)
            .topics(topicsViewData)
            .build()
    }

    /**
     * convert list of [Comment] and usersMap [Map] of String to User
     * to list of [CommentViewData]
     *
     * @param comments: list of [Comment]
     * @param usersMap: [Map] of String to User
     * @param postId: postId of post
     * */
    private fun convertComments(
        comments: MutableList<Comment>?,
        usersMap: Map<String, User>,
        postId: String,
        parentCommentId: String? = null
    ): MutableList<CommentViewData> {
        if (comments == null) return mutableListOf()
        return comments.map { comment ->
            convertComment(
                comment,
                usersMap,
                postId,
                parentCommentId
            )
        }.toMutableList()
    }

    fun convertComment(
        comment: Comment,
        usersMap: Map<String, User>,
        postId: String,
        parentCommentId: String? = null
    ): CommentViewData {
        val commentCreator = comment.uuid
        val user = usersMap[commentCreator]
        val replies = comment.replies?.toMutableList()
        val parentId = parentCommentId ?: comment.parentComment?.id

        val userViewData = if (user == null) {
            createDeletedUser()
        } else {
            convertUser(user)
        }

        return CommentViewData.Builder()
            .id(comment.id)
            .postId(postId)
            .isLiked(comment.isLiked)
            .isEdited(comment.isEdited)
            .userId(commentCreator)
            .text(comment.text)
            .level(comment.level)
            .likesCount(comment.likesCount)
            .repliesCount(comment.commentsCount)
            .user(userViewData)
            .createdAt(comment.createdAt)
            .updatedAt(comment.updatedAt)
            .menuItems(convertOverflowMenuItems(comment.menuItems))
            .replies(
                convertComments(
                    replies,
                    usersMap,
                    postId,
                    comment.id
                )
            )
            .parentId(parentId)
            .parentComment(
                comment.parentComment?.let {
                    convertComment(
                        it,
                        usersMap,
                        postId
                    )
                }
            )
            .uuid(commentCreator)
            .tempId(comment.tempId)
            .build()
    }

    fun convertCommentsCount(commentsCount: Int): CommentsCountViewData {
        return CommentsCountViewData.Builder()
            .commentsCount(commentsCount)
            .build()
    }

    /**
     * convert list of [MenuItem] to [OverflowMenuItemViewData]
     * @param menuItems: list of [MenuItem]
     * */
    private fun convertOverflowMenuItems(
        menuItems: List<MenuItem>
    ): List<OverflowMenuItemViewData> {
        return menuItems.map { menuItem ->
            OverflowMenuItemViewData.Builder()
                .id(menuItem.id)
                .title(menuItem.title)
                .build()
        }
    }

    /**
     * convert list of [Attachment] to list of [AttachmentViewData]
     * @param attachments: list of [Attachment]
     **/
    private fun convertAttachments(
        attachments: List<Attachment>?,
        postId: String
    ): List<AttachmentViewData> {
        if (attachments == null) return emptyList()
        return attachments.map { attachment ->
            AttachmentViewData.Builder()
                .attachmentType(attachment.attachmentType)
                .attachmentMeta(convertAttachmentMeta(attachment.attachmentMeta))
                .postId(postId)
                .build()
        }
    }

    /**
     * convert [AttachmentMeta] to [AttachmentMetaViewData]
     * @param attachmentMeta: object of [AttachmentMeta]
     **/
    private fun convertAttachmentMeta(attachmentMeta: AttachmentMeta?): AttachmentMetaViewData {
        if (attachmentMeta == null) {
            return AttachmentMetaViewData.Builder().build()
        }
        return AttachmentMetaViewData.Builder()
            .name(attachmentMeta.name)
            .url(attachmentMeta.url)
            .format(attachmentMeta.format)
            .size(attachmentMeta.size)
            .duration(attachmentMeta.duration)
            .pageCount(attachmentMeta.pageCount)
            .ogTags(convertLinkOGTags(attachmentMeta.ogTags))
            .build()
    }

    /**
     * convert [LinkOGTags] to [LinkOGTagsViewData]
     * @param linkOGTags: object of [LinkOGTags]
     **/
    fun convertLinkOGTags(linkOGTags: LinkOGTags): LinkOGTagsViewData {
        return LinkOGTagsViewData.Builder()
            .url(linkOGTags.url)
            .description(linkOGTags.description)
            .title(linkOGTags.title)
            .image(linkOGTags.image)
            .build()
    }

    fun convertUserTag(tagMember: User): UserTagViewData {
        val nameDrawable = MemberImageUtil.getNameDrawable(
            MemberImageUtil.SIXTY_PX,
            tagMember.id.toString(),
            tagMember.name
        )
        return UserTagViewData.Builder()
            .id(tagMember.id)
            .imageUrl(tagMember.imageUrl)
            .isGuest(tagMember.isGuest)
            .name(tagMember.name)
            .userUniqueId(tagMember.userUniqueId)
            .placeHolder(nameDrawable.first)
            .uuid(tagMember.sdkClientInfo.uuid)
            .build()
    }

    /**
     * convert list of [Like] to list of [LikeViewData]
     * @param likes: list of [Like]
     * @param users: [Map] of String to User
     * */
    fun convertLikes(
        likes: List<Like>,
        users: Map<String, User>
    ): List<LikeViewData> {
        return likes.map { like ->
            //get user id
            val likedById = like.uuid

            //get user
            val likedBy = users[likedById]

            //convert view data
            val likedByViewData = if (likedBy == null) {
                createDeletedUser()
            } else {
                convertUser(likedBy)
            }

            //create like view data
            LikeViewData.Builder()
                .id(like.id)
                .userId(like.userId)
                .createdAt(like.createdAt)
                .updatedAt(like.updatedAt)
                .user(likedByViewData)
                .build()
        }
    }

    /**
     * convert list of [ReportTag] to list of [ReportTagViewData]
     * @param tags: list of [ReportTag]
     * */
    fun convertReportTag(
        tags: List<ReportTag>
    ): List<ReportTagViewData> {
        return tags.map { tag ->
            ReportTagViewData.Builder()
                .id(tag.id)
                .name(tag.name)
                .isSelected(false)
                .build()
        }
    }

    /**
     * convert list of [ReportTag] to list of [ReasonChooseViewData]
     * @param tags: list of [ReportTag]
     * */
    fun convertDeleteTag(
        tags: List<ReportTag>
    ): MutableList<ReasonChooseViewData> {
        return tags.map { tag ->
            ReasonChooseViewData.Builder()
                .value(tag.name)
                .build()
        }.toMutableList()
    }

    /**
     * convert list of [Activity] and usersMap [Map] of String to User
     * to list of [ActivityViewData]
     *
     * @param activities: list of [Activity]
     * @param usersMap: [Map] of String to User
     * */
    fun convertActivities(
        activities: List<Activity>,
        usersMap: Map<String, User>
    ): List<ActivityViewData> {
        return activities.map {
            convertActivity(it, usersMap)
        }
    }

    /**
     * converts [Activity] and usersMap [Map] of String to User
     * to [ActivityViewData]
     *
     * @param activity: an activity [ActivityViewData]
     * @param usersMap: [Map] of String to User
     * */
    private fun convertActivity(
        activity: Activity,
        usersMap: Map<String, User>
    ): ActivityViewData {
        val activityByUser = if (activity.actionBy.isNotEmpty()) {
            convertUser(usersMap[activity.actionBy.last()])
        } else {
            UserViewData.Builder().build()
        }

        return ActivityViewData.Builder()
            .id(activity.id)
            .isRead(activity.isRead)
            .actionOn(activity.actionOn)
            .actionBy(activity.actionBy)
            .entityType(activity.entityType)
            .entityId(activity.entityId)
            .entityOwnerId(activity.entityOwnerId)
            .action(activity.action)
            .cta(activity.cta)
            .activityText(activity.activityText)
            .activityEntityData(
                convertActivityEntityData(
                    activity.activityEntityData,
                    usersMap
                )
            )
            .activityByUser(activityByUser)
            .createdAt(activity.createdAt)
            .updatedAt(activity.updatedAt)
            .uuid(activity.uuid)
            .build()
    }

    private fun convertActivityEntityData(
        activityEntityData: ActivityEntityData?,
        usersMap: Map<String, User>
    ): ActivityEntityViewData? {
        if (activityEntityData == null) {
            return null
        }
        val entityCreator = activityEntityData.uuid
        val user = usersMap[entityCreator]
        val replies = activityEntityData.replies?.toMutableList()

        val userViewData = if (user == null) {
            createDeletedUser()
        } else {
            convertUser(user)
        }

        return ActivityEntityViewData.Builder()
            .id(activityEntityData.id)
            .text(activityEntityData.text)
            .deleteReason(activityEntityData.deleteReason)
            .deletedBy(activityEntityData.deletedBy)
            .heading(activityEntityData.heading)
            .attachments(
                convertAttachments(
                    activityEntityData.attachments,
                    activityEntityData.id
                )
            )
            .communityId(activityEntityData.communityId)
            .isEdited(activityEntityData.isEdited)
            .isPinned(activityEntityData.isPinned)
            .userId(activityEntityData.userId)
            .user(userViewData)
            .replies(
                convertComments(
                    replies,
                    usersMap,
                    activityEntityData.postId ?: activityEntityData.id
                )
            )
            .level(activityEntityData.level)
            .createdAt(activityEntityData.createdAt)
            .updatedAt(activityEntityData.updatedAt)
            .uuid(activityEntityData.uuid)
            .deletedByUUID(activityEntityData.deletedByUUID)
            .build()
    }

    /**
     * convert [Topic] to [LMFeedTopicViewData]
     * */
    fun convertTopic(topic: Topic): LMFeedTopicViewData {
        return LMFeedTopicViewData.Builder()
            .id(topic.id)
            .name(topic.name)
            .isEnabled(topic.isEnabled)
            .isSelected(false)
            .build()
    }


    /**--------------------------------
     * Network Model -> Db Model
    --------------------------------*/

    /**
     * converts [User] to [UserEntity]
     * @param user: object of [User]
     * @return object of [UserEntity]
     */
    fun createUserEntity(user: User): UserEntity {
        return UserEntity.Builder()
            .id(user.id)
            .imageUrl(user.imageUrl)
            .isGuest(user.isGuest)
            .name(user.name)
            .updatedAt(user.updatedAt)
            .customTitle(user.customTitle)
            .isDeleted(user.isDeleted)
            .userUniqueId(user.userUniqueId)
            .uuid(user.uuid)
            .sdkClientInfoEntity(createSDKClientInfoEntity(user.sdkClientInfo))
            .build()
    }

    /**
     * converts [SDKClientInfo] to [SDKClientInfoEntity]
     * @param sdkClientInfo: object of [SDKClientInfo]
     * @return object of [SDKClientInfoEntity]
     */
    private fun createSDKClientInfoEntity(sdkClientInfo: SDKClientInfo): SDKClientInfoEntity {
        return SDKClientInfoEntity.Builder()
            .user(sdkClientInfo.user)
            .community(sdkClientInfo.community)
            .userUniqueId(sdkClientInfo.userUniqueId)
            .uuid(sdkClientInfo.uuid)
            .build()
    }

    /**
     * converts list of [ManagementRightPermissionData] to list of [MemberRightsEntity]
     * @param userUniqueId: unique id of the user
     * @param memberRights: list of [ManagementRightPermissionData]
     * */
    fun createMemberRightsEntity(
        userUniqueId: String,
        memberRights: List<ManagementRightPermissionData>
    ): List<MemberRightsEntity> {
        return memberRights.map {
            createMemberRightEntity(
                userUniqueId,
                it
            )
        }
    }

    /**
     * converts [ManagementRightPermissionData] to [MemberRightsEntity]
     * @param userUniqueId: unique id of the user
     * @param memberRight: network model of member right [ManagementRightPermissionData]
     * */
    private fun createMemberRightEntity(
        userUniqueId: String,
        memberRight: ManagementRightPermissionData
    ): MemberRightsEntity {
        return MemberRightsEntity.Builder()
            .id(memberRight.id)
            .isLocked(memberRight.isLocked)
            .isSelected(memberRight.isSelected)
            .state(memberRight.state)
            .title(memberRight.title)
            .subtitle(memberRight.subtitle)
            .userUniqueId(userUniqueId)
            .build()
    }

    fun createConfigurationEntity(
        configuration: Configuration
    ): ConfigurationEntity {
        val valueJSONString = configuration.value.toString()
        return ConfigurationEntity.Builder()
            .type(configuration.type)
            .description(configuration.description)
            .value(valueJSONString)
            .build()
    }

    /**--------------------------------
     * Db Model -> View Data Model
    --------------------------------*/

    fun convertUser(
        user: UserEntity
    ): UserViewData {
        return UserViewData.Builder()
            .id(user.id)
            .imageUrl(user.imageUrl)
            .isGuest(user.isGuest)
            .name(user.name)
            .updatedAt(user.updatedAt)
            .customTitle(user.customTitle)
            .isDeleted(user.isDeleted)
            .userUniqueId(user.userUniqueId)
            .build()
    }

    fun convertPost(postWithAttachments: PostWithAttachments): PostViewData {
        val post = postWithAttachments.post
        val attachments = postWithAttachments.attachments
        val topics = postWithAttachments.topics
        return PostViewData.Builder()
            .text(post.text)
            .temporaryId(post.temporaryId)
            .thumbnail(post.thumbnail)
            .workerUUID(post.uuid)
            .isPosted(post.isPosted)
            .attachments(convertAttachmentsEntity(attachments))
            .topics(convertTopicsEntity(topics))
            .build()
    }

    private fun convertTopicsEntity(topics: List<TopicEntity>): List<LMFeedTopicViewData> {
        return topics.map { topic ->
            convertTopic(topic)
        }
    }

    private fun convertAttachmentsEntity(attachments: List<AttachmentEntity>): List<AttachmentViewData> {
        return attachments.map { attachment ->
            convertAttachment(attachment)
        }
    }

    fun convertAttachment(attachment: AttachmentEntity): AttachmentViewData {
        return AttachmentViewData.Builder()
            .attachmentType(attachment.attachmentType)
            .attachmentMeta(convertAttachmentMeta(attachment.attachmentMeta))
            .postId(attachment.postId)
            .build()
    }

    private fun convertAttachmentMeta(attachmentMeta: AttachmentMetaEntity): AttachmentMetaViewData {
        return AttachmentMetaViewData.Builder()
            .url(attachmentMeta.url)
            .name(attachmentMeta.name)
            .size(attachmentMeta.size)
            .duration(attachmentMeta.duration)
            .format(attachmentMeta.format)
            .pageCount(attachmentMeta.pageCount)
            .uri(Uri.parse(attachmentMeta.uri))
            .width(attachmentMeta.width)
            .height(attachmentMeta.height)
            .build()
    }

    /**
     * convert [TopicEntity] to [LMFeedTopicViewData]
     */
    private fun convertTopic(topicEntity: TopicEntity): LMFeedTopicViewData {
        return LMFeedTopicViewData.Builder()
            .id(topicEntity.id)
            .name(topicEntity.name)
            .isEnabled(topicEntity.isEnabled)
            .build()
    }

    /**--------------------------------
     * View Data -> Db Model
    --------------------------------*/

    fun convertPost(
        temporaryId: Long,
        uuid: String,
        thumbnail: String,
        text: String?
    ): PostEntity {
        return PostEntity.Builder()
            .temporaryId(temporaryId)
            .postId(temporaryId.toString())
            .uuid(uuid)
            .thumbnail(thumbnail)
            .text(text)
            .build()
    }

    fun convertAttachment(
        temporaryId: Long,
        singleUriData: SingleUriData
    ): AttachmentEntity {
        val attachmentType = when (singleUriData.fileType) {
            IMAGE -> {
                com.likeminds.feedsx.posttypes.model.IMAGE
            }

            VIDEO -> {
                com.likeminds.feedsx.posttypes.model.VIDEO
            }

            else -> {
                DOCUMENT
            }
        }
        return AttachmentEntity.Builder()
            .temporaryId(temporaryId)
            .postId(temporaryId.toString())
            .attachmentType(attachmentType)
            .attachmentMeta(convertAttachmentMeta(singleUriData))
            .build()
    }

    // converts singleUriData to attachmentMetaEntity
    private fun convertAttachmentMeta(
        singleUriData: SingleUriData
    ): AttachmentMetaEntity {
        val url = String(
            Base64.decode(
                AWSKeys.getBucketBaseUrl(),
                Base64.DEFAULT
            )
        ) + singleUriData.awsFolderPath
        return AttachmentMetaEntity.Builder().name(singleUriData.mediaName)
            .url(url)
            .uri(singleUriData.uri.toString())
            .pageCount(singleUriData.pdfPageCount)
            .size(singleUriData.size)
            .duration(singleUriData.duration)
            .format(singleUriData.format)
            .awsFolderPath(singleUriData.awsFolderPath)
            .localFilePath(singleUriData.localFilePath)
            .width(singleUriData.width)
            .height(singleUriData.height)
            .build()
    }

    /***
     * convert [LMFeedTopicViewData] to [TopicEntity]
     * @param temporaryId: Temporary id of the post
     */
    fun convertTopic(temporaryId: Long, topic: LMFeedTopicViewData): TopicEntity {
        return TopicEntity.Builder()
            .id(topic.id)
            .isEnabled(topic.isEnabled)
            .name(topic.name)
            .postId(temporaryId.toString())
            .build()
    }
}