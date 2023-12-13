package com.likeminds.feedsx.post.detail.viewmodel

import androidx.lifecycle.*
import com.likeminds.feedsx.LMFeedAnalytics
import com.likeminds.feedsx.feed.UserWithRightsRepository
import com.likeminds.feedsx.posttypes.model.*
import com.likeminds.feedsx.utils.LMFeedUserPreferences
import com.likeminds.feedsx.utils.ViewDataConverter
import com.likeminds.feedsx.utils.coroutine.launchIO
import com.likeminds.feedsx.utils.memberrights.util.MemberRightUtil
import com.likeminds.feedsx.utils.membertagging.model.UserTagViewData
import com.likeminds.feedsx.utils.membertagging.util.MemberTaggingUtil
import com.likeminds.likemindsfeed.LMFeedClient
import com.likeminds.likemindsfeed.LMResponse
import com.likeminds.likemindsfeed.comment.model.*
import com.likeminds.likemindsfeed.helper.model.GetTaggingListRequest
import com.likeminds.likemindsfeed.helper.model.GetTaggingListResponse
import com.likeminds.likemindsfeed.post.model.GetPostRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

class PostDetailViewModel @Inject constructor(
    private val userWithRightsRepository: UserWithRightsRepository,
    private val userPreferences: LMFeedUserPreferences
) : ViewModel() {

    private val lmFeedClient = LMFeedClient.getInstance()

    private val _addCommentResponse = MutableLiveData<CommentViewData>()
    val addCommentResponse: LiveData<CommentViewData> = _addCommentResponse

    private val _editCommentResponse = MutableLiveData<CommentViewData>()
    val editCommentResponse: LiveData<CommentViewData> = _editCommentResponse

    // it holds pair of [parentCommentId] and [replyComment]
    private val _addReplyResponse = MutableLiveData<Pair<String, CommentViewData>>()
    val addReplyResponse: LiveData<Pair<String, CommentViewData>> = _addReplyResponse

    /**
     * it holds the Pair of [commentId] and [parentCommentId]
     * if comment level is 0 then [parentCommentId] is null
     * if comment level is 1 then [parentCommentId] is non null
     */
    private val _deleteCommentResponse = MutableLiveData<Pair<String, String?>>()
    val deleteCommentResponse: LiveData<Pair<String, String?>> = _deleteCommentResponse

    // it holds the Pair of [page] and [postViewData]
    private val _postResponse = MutableLiveData<Pair<Int, PostViewData>>()
    val postResponse: LiveData<Pair<Int, PostViewData>> = _postResponse

    // it holds the Pair of [page] and [commentViewData]
    private val _getCommentResponse = MutableLiveData<Pair<Int, CommentViewData>>()
    val getCommentResponse: LiveData<Pair<Int, CommentViewData>> = _getCommentResponse

    /**
     * [taggingData] contains first -> page called
     * second -> Community Members and Groups
     * */
    private val _taggingData = MutableLiveData<Pair<Int, ArrayList<UserTagViewData>>?>()
    val taggingData: LiveData<Pair<Int, ArrayList<UserTagViewData>>?> = _taggingData

    private val _hasCommentRights = MutableLiveData(true)
    val hasCommentRights: LiveData<Boolean> = _hasCommentRights

    sealed class ErrorMessageEvent {
        data class GetPost(val errorMessage: String?) : ErrorMessageEvent()
        data class GetTaggingList(val errorMessage: String?) : ErrorMessageEvent()
        data class LikeComment(
            val commentId: String,
            val errorMessage: String?
        ) : ErrorMessageEvent()

        data class AddComment(
            val tempId: String,
            val errorMessage: String?
        ) : ErrorMessageEvent()

        data class ReplyComment(
            val parentCommentId: String,
            val tempId: String,
            val errorMessage: String?
        ) : ErrorMessageEvent()

        data class EditComment(val errorMessage: String?) : ErrorMessageEvent()
        data class DeleteComment(val errorMessage: String?) : ErrorMessageEvent()
        data class GetComment(val errorMessage: String?) : ErrorMessageEvent()
    }

    private val errorMessageChannel = Channel<ErrorMessageEvent>(Channel.BUFFERED)
    val errorMessageEventFlow = errorMessageChannel.receiveAsFlow()

    companion object {
        const val PAGE_SIZE = 10
        const val REPLIES_PAGE_SIZE = 5
    }

    // to getPost and paginated comments
    fun getPost(postId: String, page: Int) {
        viewModelScope.launchIO {
            // builds api request
            val request = GetPostRequest.Builder()
                .postId(postId)
                .page(page)
                .pageSize(PAGE_SIZE)
                .build()

            // calls api
            val response = lmFeedClient.getPost(request)
            if (response.success) {
                val data = response.data ?: return@launchIO
                val post = data.post
                val users = data.users
                val topics = data.topics
                _postResponse.postValue(
                    Pair(
                        page,
                        ViewDataConverter.convertPost(post, users, topics)
                    )
                )
            } else {
                errorMessageChannel.send(ErrorMessageEvent.GetPost(response.errorMessage))
            }
        }
    }

    //for like/unlike a comment
    fun likeComment(postId: String, commentId: String, commentLiked: Boolean) {
        viewModelScope.launchIO {
            val request = LikeCommentRequest.Builder()
                .postId(postId)
                .commentId(commentId)
                .build()


            //call like post api
            val response = lmFeedClient.likeComment(request)

            //check for error
            if (response.success) {
                sendCommentLikedEvent(postId, commentId, commentLiked)
            } else {
                errorMessageChannel.send(
                    ErrorMessageEvent.LikeComment(
                        commentId,
                        response.errorMessage
                    )
                )
            }
        }
    }

    // for adding comment on post
    fun addComment(
        postId: String,
        tempId: String,
        text: String
    ) {
        viewModelScope.launchIO {
            // initializes temp id for local handling of comment

            // builds api request
            val request = AddCommentRequest.Builder()
                .postId(postId)
                .text(text)
                .tempId(tempId)
                .build()

            // calls api
            val response = lmFeedClient.addComment(request)
            if (response.success) {
                val data = response.data ?: return@launchIO
                val comment = data.comment
                val users = data.users
                sendCommentPostedEvent(postId, comment.id)

                _addCommentResponse.postValue(
                    ViewDataConverter.convertComment(
                        comment,
                        users,
                        postId
                    )
                )
            } else {
                errorMessageChannel.send(
                    ErrorMessageEvent.AddComment(
                        tempId,
                        response.errorMessage
                    )
                )
            }
        }
    }

    /**
     * Triggers when a comment is posted on a post
     **/
    private fun sendCommentPostedEvent(postId: String, commentId: String) {
        LMFeedAnalytics.track(
            LMFeedAnalytics.Events.COMMENT_POSTED,
            mapOf(
                LMFeedAnalytics.Keys.POST_ID to postId,
                LMFeedAnalytics.Keys.COMMENT_ID to commentId
            )
        )
    }

    // for editing comment on post
    fun editComment(
        postId: String,
        commentId: String,
        text: String
    ) {
        viewModelScope.launchIO {
            // builds api request
            val request = EditCommentRequest.Builder()
                .postId(postId)
                .commentId(commentId)
                .text(text)
                .build()

            // calls api
            val response = lmFeedClient.editComment(request)
            if (response.success) {
                val data = response.data ?: return@launchIO
                val comment = data.comment
                val users = data.users

                _editCommentResponse.postValue(
                    ViewDataConverter.convertComment(
                        comment,
                        users,
                        postId
                    )
                )
            } else {
                errorMessageChannel.send(ErrorMessageEvent.EditComment(response.errorMessage))
            }
        }
    }

    // for replying on a comment on the post
    fun replyComment(
        parentCommentCreatorUUID: String,
        postId: String,
        parentCommentId: String,
        text: String,
        tempId: String
    ) {
        viewModelScope.launchIO {
            // builds api request
            val request = ReplyCommentRequest.Builder()
                .postId(postId)
                .commentId(parentCommentId)
                .text(text)
                .tempId(tempId)
                .build()

            // calls api
            val response = lmFeedClient.replyComment(request)
            if (response.success) {
                val data = response.data ?: return@launchIO
                val comment = data.comment
                val users = data.users
                sendReplyPostedEvent(
                    parentCommentCreatorUUID,
                    postId,
                    parentCommentId,
                    comment.id
                )

                _addReplyResponse.postValue(
                    Pair(
                        parentCommentId,
                        ViewDataConverter.convertComment(
                            comment,
                            users,
                            postId,
                            parentCommentId
                        )
                    )
                )
            } else {
                errorMessageChannel.send(
                    ErrorMessageEvent.ReplyComment(
                        parentCommentId,
                        tempId,
                        response.errorMessage
                    )
                )
            }
        }
    }

    /**
     * Triggers when the reply is posted on a comment
     **/
    private fun sendReplyPostedEvent(
        parentCommentCreatorUUID: String,
        postId: String,
        parentCommentId: String,
        commentId: String,
    ) {
        LMFeedAnalytics.track(
            LMFeedAnalytics.Events.REPLY_POSTED,
            mapOf(
                LMFeedAnalytics.Keys.UUID to parentCommentCreatorUUID,
                LMFeedAnalytics.Keys.POST_ID to postId,
                LMFeedAnalytics.Keys.COMMENT_ID to parentCommentId,
                LMFeedAnalytics.Keys.COMMENT_REPLY_ID to commentId
            )
        )
    }

    // to get comment with paginated replies
    fun getComment(
        postId: String,
        commentId: String,
        page: Int
    ) {
        viewModelScope.launchIO {
            // builds api request
            val request = GetCommentRequest.Builder()
                .postId(postId)
                .commentId(commentId)
                .page(page)
                .pageSize(REPLIES_PAGE_SIZE)
                .build()

            // calls api
            val response = lmFeedClient.getComment(request)
            if (response.success) {
                val data = response.data ?: return@launchIO
                val comment = data.comment
                val users = data.users
                _getCommentResponse.postValue(
                    Pair(
                        page,
                        ViewDataConverter.convertComment(
                            comment,
                            users,
                            postId
                        )
                    )
                )
            } else {
                errorMessageChannel.send(ErrorMessageEvent.GetComment(response.errorMessage))
            }
        }
    }

    // for deleting comment/reply
    fun deleteComment(
        postId: String,
        commentId: String,
        parentCommentId: String? = null,
        reason: String? = null
    ) {
        viewModelScope.launchIO {
            val request = DeleteCommentRequest.Builder()
                .postId(postId)
                .commentId(commentId)
                .reason(reason)
                .build()

            //call delete comment api
            val response = lmFeedClient.deleteComment(request)

            if (response.success) {
                sendCommentReplyDeletedEvent(
                    postId,
                    commentId,
                    parentCommentId
                )
                _deleteCommentResponse.postValue(Pair(commentId, parentCommentId))
            } else {
                errorMessageChannel.send(ErrorMessageEvent.DeleteComment(response.errorMessage))
            }
        }
    }

    /**
     * Triggers when a comment/reply is deleted
     **/
    private fun sendCommentReplyDeletedEvent(
        postId: String,
        commentId: String,
        parentCommentId: String?
    ) {
        if (parentCommentId == null) {
            // comment deleted event
            LMFeedAnalytics.track(
                LMFeedAnalytics.Events.COMMENT_DELETED,
                mapOf(
                    LMFeedAnalytics.Keys.POST_ID to postId,
                    LMFeedAnalytics.Keys.COMMENT_ID to commentId
                )
            )
        } else {
            // reply deleted event
            LMFeedAnalytics.track(
                LMFeedAnalytics.Events.REPLY_DELETED,
                mapOf(
                    LMFeedAnalytics.Keys.POST_ID to postId,
                    LMFeedAnalytics.Keys.COMMENT_ID to parentCommentId,
                    LMFeedAnalytics.Keys.COMMENT_REPLY_ID to commentId,
                )
            )
        }
    }

    // calls api to get members for tagging
    fun getMembersForTagging(
        page: Int,
        searchName: String
    ) {
        viewModelScope.launchIO {
            // builds api request
            val request = GetTaggingListRequest.Builder()
                .page(page)
                .pageSize(MemberTaggingUtil.PAGE_SIZE)
                .searchName(searchName)
                .build()

            val response = lmFeedClient.getTaggingList(request)
            taggingResponseFetched(page, response)
        }
    }

    // processes tagging list response and sends response to the view
    private fun taggingResponseFetched(
        page: Int,
        response: LMResponse<GetTaggingListResponse>
    ) {
        viewModelScope.launchIO {
            if (response.success) {
                val data = response.data ?: return@launchIO
                _taggingData.postValue(
                    Pair(
                        page,
                        MemberTaggingUtil.getTaggingData(data.members)
                    )
                )
            } else {
                errorMessageChannel.send(ErrorMessageEvent.GetTaggingList(response.errorMessage))
            }
        }
    }

    // gets user from db and check if it has comment rights or not
    fun checkCommentRights() {
        viewModelScope.launchIO {
            val userId = userPreferences.getUserUniqueId()

            // fetches user with rights from DB with user.id
            val userWithRights = userWithRightsRepository.getUserWithRights(userId)
            val memberState = userWithRights.user.state
            val memberRights = userWithRights.memberRights

            _hasCommentRights.postValue(
                MemberRightUtil.hasCommentRight(
                    memberState,
                    memberRights
                )
            )
        }
    }

    // returns [CommentViewData] for local handling of comment
    fun getCommentViewDataForLocalHandling(
        postId: String,
        createdAt: Long,
        tempId: String,
        text: String,
        parentCommentId: String?,
        level: Int = 0
    ): CommentViewData {
        // adds comment locally
        return CommentViewData.Builder()
            .postId(postId)
            .user(
                UserViewData.Builder()
                    .name(userPreferences.getUserName())
                    .build()
            )
            .createdAt(createdAt)
            .id(tempId)
            .tempId(tempId)
            .text(text)
            .parentId(parentCommentId)
            .level(level)
            .build()
    }

    /**
     * Triggers when the current user likes/unlikes a comment
     */
    private fun sendCommentLikedEvent(
        postId: String,
        commentId: String,
        commentLiked: Boolean
    ) {
        val event = if (commentLiked) {
            LMFeedAnalytics.Events.COMMENT_LIKED
        } else {
            LMFeedAnalytics.Events.COMMENT_UNLIKED
        }

        LMFeedAnalytics.track(
            event,
            mapOf(
                LMFeedAnalytics.Keys.UUID to userPreferences.getUUID(),
                LMFeedAnalytics.Keys.POST_ID to postId,
                LMFeedAnalytics.Keys.COMMENT_ID to commentId,
            )
        )
    }
}