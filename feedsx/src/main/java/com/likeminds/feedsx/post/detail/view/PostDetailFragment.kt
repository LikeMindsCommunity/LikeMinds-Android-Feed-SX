package com.likeminds.feedsx.post.detail.view

import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.*
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.likeminds.feedsx.*
import com.likeminds.feedsx.branding.model.LMFeedBranding
import com.likeminds.feedsx.databinding.LmFeedFragmentPostDetailBinding
import com.likeminds.feedsx.delete.model.*
import com.likeminds.feedsx.delete.view.LMFeedAdminDeleteDialogFragment
import com.likeminds.feedsx.delete.view.LMFeedSelfDeleteDialogFragment
import com.likeminds.feedsx.feed.util.PostEvent
import com.likeminds.feedsx.likes.model.*
import com.likeminds.feedsx.likes.view.LMFeedLikesActivity
import com.likeminds.feedsx.media.util.PostVideoAutoPlayHelper
import com.likeminds.feedsx.overflowmenu.model.*
import com.likeminds.feedsx.post.detail.model.*
import com.likeminds.feedsx.post.detail.view.PostDetailActivity.Companion.POST_DETAIL_EXTRAS
import com.likeminds.feedsx.post.detail.view.adapter.PostDetailAdapter
import com.likeminds.feedsx.post.detail.view.adapter.PostDetailAdapter.PostDetailAdapterListener
import com.likeminds.feedsx.post.detail.view.adapter.PostDetailReplyAdapter.PostDetailReplyAdapterListener
import com.likeminds.feedsx.post.detail.viewmodel.PostDetailViewModel
import com.likeminds.feedsx.post.edit.model.LMFeedEditPostExtras
import com.likeminds.feedsx.post.edit.view.LMFeedEditPostActivity
import com.likeminds.feedsx.post.edit.viewmodel.LMFeedHelperViewModel
import com.likeminds.feedsx.post.viewmodel.PostActionsViewModel
import com.likeminds.feedsx.posttypes.model.*
import com.likeminds.feedsx.posttypes.view.adapter.PostAdapterListener
import com.likeminds.feedsx.report.model.*
import com.likeminds.feedsx.report.view.*
import com.likeminds.feedsx.utils.*
import com.likeminds.feedsx.utils.ValueUtils.pluralizeOrCapitalize
import com.likeminds.feedsx.utils.ViewUtils.hide
import com.likeminds.feedsx.utils.ViewUtils.show
import com.likeminds.feedsx.utils.customview.BaseFragment
import com.likeminds.feedsx.utils.membertagging.model.MemberTaggingExtras
import com.likeminds.feedsx.utils.membertagging.util.*
import com.likeminds.feedsx.utils.membertagging.view.LMFeedMemberTaggingView
import com.likeminds.feedsx.utils.model.BaseViewType
import com.likeminds.feedsx.utils.pluralize.model.WordAction
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class PostDetailFragment :
    BaseFragment<LmFeedFragmentPostDetailBinding, PostDetailViewModel>(),
    PostAdapterListener,
    PostDetailAdapterListener,
    PostDetailReplyAdapterListener,
    LMFeedSelfDeleteDialogFragment.DeleteAlertDialogListener,
    LMFeedAdminDeleteDialogFragment.DeleteDialogListener {

    // shared viewModel between [FeedFragment] and [PostDetailFragment] for postActions
    @Inject
    lateinit var postActionsViewModel: PostActionsViewModel

    @Inject
    lateinit var initiateViewModel: InitiateViewModel

    @Inject
    lateinit var userPreferences: LMFeedUserPreferences

    @Inject
    lateinit var lmFeedHelperViewModel: LMFeedHelperViewModel

    private lateinit var postDetailExtras: PostDetailExtras

    private lateinit var mPostDetailAdapter: PostDetailAdapter
    private lateinit var mScrollListener: EndlessRecyclerScrollListener
    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout

    private var parentCommentIdToReply: String? = null
    private var toFindComment: Boolean = false

    // variables to handle comment/reply edit action
    private var editCommentId: String? = null
    private var parentId: String? = null

    private lateinit var memberTagging: LMFeedMemberTaggingView

    private lateinit var postVideoAutoPlayHelper: PostVideoAutoPlayHelper

    // fixed position of viewTypes in adapter
    private val postDataPosition = 0
    private val commentsCountPosition = 1
    private val commentsStartPosition = 2

    // [postPublisher] to publish changes in the post
    private val postEvent by lazy {
        PostEvent.getPublisher()
    }

    companion object {
        const val TAG = "PostDetailFragment"
        const val REPLIES_THRESHOLD = 5
    }

    override val useSharedViewModel: Boolean
        get() = true

    override fun getViewModelClass(): Class<PostDetailViewModel> {
        return PostDetailViewModel::class.java
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().postDetailComponent()?.inject(this)
    }

    override fun getViewBinding(): LmFeedFragmentPostDetailBinding {
        return LmFeedFragmentPostDetailBinding.inflate(layoutInflater)
    }

    override fun receiveExtras() {
        super.receiveExtras()
        if (arguments == null || arguments?.containsKey(POST_DETAIL_EXTRAS) == false) {
            requireActivity().supportFragmentManager.popBackStack()
            return
        }
        postDetailExtras = ExtrasUtil.getParcelable(
            arguments,
            POST_DETAIL_EXTRAS,
            PostDetailExtras::class.java
        ) ?: throw emptyExtrasException(TAG)
    }

    override fun onResume() {
        super.onResume()
        initiateAutoPlayer()
    }

    override fun onPause() {
        super.onPause()
        destroyAutoPlayer()
    }

    override fun setUpViews() {
        super.setUpViews()

        binding.buttonColor = LMFeedBranding.getButtonsColor()
        fetchPostData()
        checkCommentsRight()
        initRecyclerView()
        initMemberTaggingView()
        initSwipeRefreshLayout()
        initCommentEditText()
        initListeners()
    }

    override fun setPostVariable() {
        super.setPostVariable()
        val postAsVariable = lmFeedHelperViewModel.getPostVariable()

        //post header
        (requireActivity() as PostDetailActivity).binding.tvToolbarTitle.text =
            postAsVariable.pluralizeOrCapitalize(WordAction.FIRST_LETTER_CAPITAL_SINGULAR)
    }

    // fetches post data to set initial data
    private fun fetchPostData(fromRefresh: Boolean = false) {
        if (!fromRefresh) {
            // show progress bar
            ProgressHelper.showProgress(binding.progressBar)
        }
        //if source is notification/deep link, then call initiate first and then other apis
        if (postDetailExtras.source == LMFeedAnalytics.Source.NOTIFICATION ||
            postDetailExtras.source == LMFeedAnalytics.Source.DEEP_LINK
        ) {
            initiateViewModel.initiateUser(
                requireContext(),
                userPreferences.getApiKey(),
                userPreferences.getUserName(),
                userPreferences.getUserUniqueId(),
                userPreferences.getIsGuest()
            )
        } else {
            viewModel.getPost(postDetailExtras.postId, 1)
        }
    }

    // check if user has comment rights or not
    private fun checkCommentsRight() {
        viewModel.checkCommentRights()
    }

    // initializes the post detail screen recycler view
    private fun initRecyclerView() {
        val linearLayoutManager = LinearLayoutManager(context)
        mPostDetailAdapter = PostDetailAdapter(this, this, this)
        binding.rvPostDetails.apply {
            layoutManager = linearLayoutManager
            adapter = mPostDetailAdapter
            if (itemAnimator is SimpleItemAnimator)
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
            show()

            attachScrollListener(
                this,
                linearLayoutManager
            )
        }
    }

    /**
     * initializes the [memberTaggingView] with the edit text
     * also sets listener to the [memberTaggingView]
     */
    private fun initMemberTaggingView() {
        memberTagging = binding.memberTaggingView
        memberTagging.initialize(
            MemberTaggingExtras.Builder()
                .editText(binding.etComment)
                .maxHeightInPercentage(0.4f)
                .color(
                    LMFeedBranding.getTextLinkColor()
                )
                .build()
        )
        memberTagging.addListener(object : MemberTaggingViewListener {
            override fun callApi(page: Int, searchName: String) {
                viewModel.getMembersForTagging(page, searchName)
            }
        })
    }

    // initializes swipe refresh layout and sets refresh listener
    private fun initSwipeRefreshLayout() {
        mSwipeRefreshLayout = binding.swipeRefreshLayout
        mSwipeRefreshLayout.setColorSchemeColors(
            LMFeedBranding.getButtonsColor(),
        )

        mSwipeRefreshLayout.setOnRefreshListener {
            refreshPostData()
        }
    }

    // initializes comment edittext with TextWatcher and focuses the keyboard
    private fun initCommentEditText() {
        binding.apply {
            if (postDetailExtras.isEditTextFocused) etComment.focusAndShowKeyboard()

            etComment.doAfterTextChanged {
                if (it?.trim().isNullOrEmpty()) {
                    ivCommentSend.isClickable = false
                    ivCommentSend.setImageResource(R.drawable.ic_comment_send_disable)
                } else {
                    ivCommentSend.isClickable = true
                    ivCommentSend.setImageResource(R.drawable.ic_comment_send_enable)
                }
            }
        }
    }

    // initializes text-watcher and click listeners
    private fun initListeners() {
        binding.apply {
            ivCommentSend.setOnClickListener {
                val text = etComment.text
                val updatedText = memberTagging.replaceSelectedMembers(text).trim()
                val postId = postDetailExtras.postId
                when {
                    parentCommentIdToReply != null -> {
                        addReply(updatedText)
                    }

                    editCommentId != null -> {
                        editCommentLocally(updatedText)
                    }

                    else -> {
                        // input text is a comment
                        addComment(postId, updatedText)
                    }
                }
                ViewUtils.hideKeyboard(this.root)
                etComment.text = null
            }
            ivCommentSend.isClickable = false
            ivCommentSend.setImageResource(R.drawable.ic_comment_send_disable)

            ivRemoveReplyingTo.setOnClickListener {
                hideReplyingToView()
            }
        }
    }

    override fun observeData() {
        super.observeData()
        observeInitiateResponse()
        observePostData()
        observeCommentData()
        observeCommentsRightData()
        observeMembersTaggingList()
        observeErrors()
    }

    // observes hasCommentRights live data
    private fun observeCommentsRightData() {
        viewModel.hasCommentRights.observe(viewLifecycleOwner) {
            //if source is notification/deep link, don't update comments right from here
            if (postDetailExtras.source != LMFeedAnalytics.Source.NOTIFICATION &&
                postDetailExtras.source != LMFeedAnalytics.Source.DEEP_LINK
            ) {
                handleCommentRights(it)
            }
        }
    }

    private fun observeInitiateResponse() {
        initiateViewModel.userResponse.observe(viewLifecycleOwner) {
            //get post detail
            viewModel.getPost(postDetailExtras.postId, 1)
        }

        initiateViewModel.logoutResponse.observe(viewLifecycleOwner) {
            binding.apply {
                mainLayout.hide()
                invalidAccessLayout.show()
            }
        }

        initiateViewModel.initiateErrorMessage.observe(viewLifecycleOwner) {
            ProgressHelper.hideProgress(binding.progressBar)
            ViewUtils.showErrorMessageToast(requireContext(), it)
        }

        initiateViewModel.hasCommentRights.observe(viewLifecycleOwner) {
            //if source is notification/deep link, update comments right from Initiate call
            if (postDetailExtras.source == LMFeedAnalytics.Source.NOTIFICATION ||
                postDetailExtras.source == LMFeedAnalytics.Source.DEEP_LINK
            ) {
                handleCommentRights(it)
            }
        }
    }

    // shows restricted text view or comment edit text as per comment rights
    private fun handleCommentRights(hasCommentRights: Boolean) {
        binding.apply {
            if (hasCommentRights) {
                etComment.show()
                ivCommentSend.show()
                tvRestricted.hide()
            } else {
                etComment.hide()
                ivCommentSend.hide()
                tvRestricted.show()
            }
        }
    }

    // observes live data related to post
    private fun observePostData() {
        // observes postResponse live data
        viewModel.postResponse.observe(viewLifecycleOwner) { pair ->
            //hide progress bar
            ProgressHelper.hideProgress(binding.progressBar)
            binding.apply {
                mainLayout.show()
                invalidAccessLayout.hide()
            }
            //page in sent in api
            val page = pair.first

            // post data
            val post = pair.second

            // notifies the subscribers about the change in post data
            postEvent.notify(Pair(post.id, post))

            // update the comments count
            updateCommentsCount(post.commentsCount)

            //if pull to refresh is called
            if (mSwipeRefreshLayout.isRefreshing) {
                setPostDataAndScrollToTop(post)
                mSwipeRefreshLayout.isRefreshing = false
                return@observe
            }

            //normal adding
            if (page == 1) {
                setPostDataAndScrollToTop(post)
            } else {
                updatePostAndAddComments(post)
            }
        }

        // observes deletePostResponse LiveData
        postActionsViewModel.deletePostResponse.observe(viewLifecycleOwner) {
            // notifies the subscribers about the deletion of post
            postEvent.notify(Pair(postDetailExtras.postId, null))

            ViewUtils.showShortToast(
                requireContext(),
                getString(
                    R.string.s_deleted,
                    lmFeedHelperViewModel.getPostVariable()
                        .pluralizeOrCapitalize(WordAction.FIRST_LETTER_CAPITAL_SINGULAR)
                )
            )
            requireActivity().finish()
        }

        // observes pinPostResponse LiveData
        postActionsViewModel.pinPostResponse.observe(viewLifecycleOwner) {
            val post = mPostDetailAdapter[postDataPosition] as PostViewData

            val postAsVariable = lmFeedHelperViewModel.getPostVariable()
            if (post.isPinned) {
                ViewUtils.showShortToast(
                    requireContext(), getString(
                        R.string.s_pinned_to_top,
                        postAsVariable.pluralizeOrCapitalize(WordAction.FIRST_LETTER_CAPITAL_SINGULAR)
                    )
                )
            } else {
                ViewUtils.showShortToast(
                    requireContext(), getString(
                        R.string.s_unpinned,
                        postAsVariable.pluralizeOrCapitalize(WordAction.FIRST_LETTER_CAPITAL_SINGULAR)
                    )
                )
            }
        }
    }

    private fun observeCommentData() {
        // observes addCommentResponse LiveData
        viewModel.addCommentResponse.observe(viewLifecycleOwner) { comment ->
            val index =
                getIndexAndCommentFromAdapterUsingTempId(comment.tempId)?.first ?: return@observe

            if (mPostDetailAdapter[index] is CommentViewData) {
                mPostDetailAdapter.update(index, comment)
            }
        }

        // observes editCommentResponse LiveData
        viewModel.editCommentResponse.observe(viewLifecycleOwner) { comment ->
            editCommentInAdapter(comment)
        }

        // observes addReplyResponse LiveData
        viewModel.addReplyResponse.observe(viewLifecycleOwner) { pair ->
            // [parentCommentId] for the reply
            val parentCommentId = pair.first

            // view data of comment with level-1
            val replyViewData = pair.second

            // gets the parentComment from adapter
            val parentComment = getIndexAndCommentFromAdapter(parentCommentId) ?: return@observe
            val parentIndex = parentComment.first
            val parentCommentViewData = parentComment.second

            val replyIndex = parentCommentViewData.replies.indexOfFirst {
                it.tempId == replyViewData.tempId
            }

            if (replyIndex != -1) {
                parentCommentViewData.replies[replyIndex] = replyViewData
                val newCommentViewData = parentCommentViewData.toBuilder()
                    .fromCommentLiked(false)
                    .fromCommentEdited(false)
                    .build()
                mPostDetailAdapter.update(parentIndex, newCommentViewData)
            }
        }

        // observes deleteCommentResponse LiveData
        viewModel.deleteCommentResponse.observe(viewLifecycleOwner) { pair ->
            val commentId = pair.first
            val parentCommentId = pair.second

            // level-0 comment
            if (parentCommentId == null) {
                removeCommentFromAdapter(commentId)
            } else {
                // level-1 comment
                removeReplyFromAdapter(parentCommentId, commentId)
            }
        }

        viewModel.getCommentResponse.observe(viewLifecycleOwner) { pair ->
            //page in api send
            val page = pair.first

            //comment data
            val comment = pair.second

            // adds paginated replies to adapter
            addReplies(comment, page)
        }
    }

    // finds and edits comment/reply in the adapter
    private fun editCommentInAdapter(comment: CommentViewData) {
        val parentComment = comment.parentComment
        if (parentComment == null) {
            // edited comment is of level-0

            val commentPosition = getIndexAndCommentFromAdapter(comment.id)?.first ?: return

            //update comment view data
            val updatedComment = comment.toBuilder()
                .fromCommentLiked(false)
                .fromCommentEdited(true)
                .build()

            mPostDetailAdapter.update(commentPosition, updatedComment)
        } else {
            // edited comment is of level-1 (reply)

            val parentCommentId = parentComment.id
            val pair = getIndexAndCommentFromAdapter(parentCommentId) ?: return
            val parentIndex = pair.first
            val parentCommentInAdapter = pair.second

            // finds index of the reply inside the comment
            val index =
                getIndexAndReplyFromComment(parentCommentInAdapter, comment.id)?.first ?: return

            if (index == -1) return

            parentCommentInAdapter.replies[index] = comment

            val newViewData = parentCommentInAdapter.toBuilder()
                .fromCommentLiked(false)
                .fromCommentEdited(false)
                .build()

            // updates the parentComment with edited reply
            mPostDetailAdapter.update(parentIndex, newViewData)
        }
    }

    private fun removeCommentFromAdapter(commentId: String, isLocal: Boolean = false) {
        // gets old [CommentsCountViewData] from adapter
        val oldCommentsCountViewData =
            (mPostDetailAdapter[commentsCountPosition] as CommentsCountViewData)

        // creates new [CommentsCountViewData] by adding to [commentsCount]
        val newCommentsCountViewData = oldCommentsCountViewData.toBuilder()
            .commentsCount(oldCommentsCountViewData.commentsCount - 1)
            .build()

        // updates [CommentsCountViewData]
        mPostDetailAdapter.update(commentsCountPosition, newCommentsCountViewData)

        // get the deleted comment from the adapter
        val indexToRemove =
            getIndexAndCommentFromAdapter(commentId)?.first ?: return

        // removes the deleted comment from the adapter
        mPostDetailAdapter.removeIndex(indexToRemove)

        if (!isLocal) {
            ViewUtils.showShortToast(
                requireContext(),
                getString(R.string.comment_deleted)
            )
        }

        if (newCommentsCountViewData.commentsCount == 0) {
            mPostDetailAdapter.removeIndex(commentsCountPosition)
            // adds no comments view data to adapter
            val noCommentViewData = NoCommentsViewData.Builder().build()
            mPostDetailAdapter.add(noCommentViewData)
        }
    }

    /**
     * Observes for member tagging list, This is a live observer which will update itself on addition of new members
     * [taggingData] contains first -> page called in api
     * second -> Community Members and Groups
     */
    private fun observeMembersTaggingList() {
        viewModel.taggingData.observe(viewLifecycleOwner) { result ->
            MemberTaggingUtil.setMembersInView(memberTagging, result)
        }
    }

    // observes error events
    private fun observeErrors() {
        viewModel.errorMessageEventFlow.onEach { response ->
            when (response) {
                is PostDetailViewModel.ErrorMessageEvent.GetTaggingList -> {
                    ViewUtils.showErrorMessageToast(requireContext(), response.errorMessage)
                }

                is PostDetailViewModel.ErrorMessageEvent.GetPost -> {
                    mSwipeRefreshLayout.isRefreshing = false
                    ProgressHelper.hideProgress(binding.progressBar)
                    ViewUtils.showErrorMessageToast(requireContext(), response.errorMessage)
                }

                is PostDetailViewModel.ErrorMessageEvent.LikeComment -> {
                    val commentId = response.commentId

                    //get comment and index
                    val pair = getIndexAndCommentFromAdapter(commentId) ?: return@onEach
                    val comment = pair.second
                    val index = pair.first

                    //update comment view data
                    val updatedComment = comment.toBuilder()
                        .isLiked(false)
                        .fromCommentLiked(true)
                        .fromCommentEdited(false)
                        .likesCount(comment.likesCount - 1)
                        .build()

                    //update recycler view
                    mPostDetailAdapter.update(index, updatedComment)

                    //show error message
                    ViewUtils.showErrorMessageToast(requireContext(), response.errorMessage)
                }

                is PostDetailViewModel.ErrorMessageEvent.AddComment -> {
                    removeCommentFromAdapter(response.tempId, isLocal = true)
                    ViewUtils.showErrorMessageToast(requireContext(), response.errorMessage)
                }

                is PostDetailViewModel.ErrorMessageEvent.ReplyComment -> {
                    removeReplyFromAdapter(response.parentCommentId, response.tempId)
                    ViewUtils.showErrorMessageToast(requireContext(), response.errorMessage)
                }

                is PostDetailViewModel.ErrorMessageEvent.DeleteComment -> {
                    ViewUtils.showErrorMessageToast(requireContext(), response.errorMessage)
                }

                is PostDetailViewModel.ErrorMessageEvent.GetComment -> {
                    ViewUtils.showErrorMessageToast(requireContext(), response.errorMessage)
                }

                is PostDetailViewModel.ErrorMessageEvent.EditComment -> {
                    ViewUtils.showErrorMessageToast(requireContext(), response.errorMessage)
                }
            }
        }.observeInLifecycle(viewLifecycleOwner)

        postActionsViewModel.errorMessageEventFlow.onEach { response ->
            when (response) {
                is PostActionsViewModel.ErrorMessageEvent.LikePost -> {
                    //get post
                    val post = mPostDetailAdapter[postDataPosition] as PostViewData

                    //update post view data
                    val updatedPost = post.toBuilder()
                        .isLiked(false)
                        .fromPostLiked(true)
                        .likesCount(post.likesCount - 1)
                        .build()

                    postEvent.notify(Pair(updatedPost.id, updatedPost))

                    //update recycler view
                    mPostDetailAdapter.update(postDataPosition, updatedPost)

                    //show error message
                    val errorMessage = response.errorMessage
                    ViewUtils.showErrorMessageToast(requireContext(), errorMessage)
                }

                is PostActionsViewModel.ErrorMessageEvent.SavePost -> {
                    //get post
                    val post = mPostDetailAdapter[postDataPosition] as PostViewData

                    //update post view data
                    val updatedPost = post.toBuilder()
                        .isSaved(false)
                        .fromPostSaved(true)
                        .build()

                    postEvent.notify(Pair(updatedPost.id, updatedPost))

                    //update recycler view
                    mPostDetailAdapter.update(postDataPosition, updatedPost)

                    //show error message
                    val errorMessage = response.errorMessage
                    ViewUtils.showErrorMessageToast(requireContext(), errorMessage)
                }

                is PostActionsViewModel.ErrorMessageEvent.DeletePost -> {
                    val errorMessage = response.errorMessage
                    ViewUtils.showErrorMessageToast(requireContext(), errorMessage)
                }

                is PostActionsViewModel.ErrorMessageEvent.PinPost -> {
                    //get post
                    val post = mPostDetailAdapter[postDataPosition] as PostViewData

                    //update post view data
                    val updatedPost = post.toBuilder()
                        .isPinned(!post.isPinned)
                        .build()

                    //update recycler view
                    mPostDetailAdapter.update(postDataPosition, updatedPost)

                    //show error message
                    val errorMessage = response.errorMessage
                    ViewUtils.showErrorMessageToast(requireContext(), errorMessage)
                }
            }
        }.observeInLifecycle(viewLifecycleOwner)
    }

    /**
     * Initializes the [postVideoAutoPlayHelper] with the recyclerView
     * And starts observing
     **/
    private fun initiateAutoPlayer() {
        postVideoAutoPlayHelper = PostVideoAutoPlayHelper.getInstance(binding.rvPostDetails)
        postVideoAutoPlayHelper.attachScrollListenerForVideo()
        postVideoAutoPlayHelper.playIfPostVisible()
    }

    /*
    * UI Block
    */

    // updates the comments count on toolbar
    private fun updateCommentsCount(commentsCount: Int) {
        (requireActivity() as PostDetailActivity).binding.tvToolbarSubTitle.text =
            this.resources.getQuantityString(
                R.plurals.comments_small,
                commentsCount,
                commentsCount
            )
    }

    // attach scroll listener for pagination for comments
    private fun attachScrollListener(
        recyclerView: RecyclerView,
        layoutManager: LinearLayoutManager
    ) {
        mScrollListener = object : EndlessRecyclerScrollListener(layoutManager) {
            override fun onLoadMore(currentPage: Int) {
                if (currentPage > 0) {
                    viewModel.getPost(postDetailExtras.postId, currentPage)
                }
            }
        }
        recyclerView.addOnScrollListener(mScrollListener)
    }

    // callback when add comment is clicked on post
    override fun comment(postId: String) {
        binding.etComment.focusAndShowKeyboard()
    }

    // processes delete post request
    private fun deletePost(
        postId: String,
        creatorId: String
    ) {
        val deleteExtras = DeleteExtras.Builder()
            .postId(postId)
            .entityType(DELETE_TYPE_POST)
            .postAsVariable(lmFeedHelperViewModel.getPostVariable())
            .build()

        showDeleteDialog(creatorId, deleteExtras)
    }

    // processes edit comment request
    private fun editComment(commentId: String, parentCommentId: String? = null) {
        // gets text of the comment/reply
        val commentText =
            if (parentCommentId == null) {
                val comment = getIndexAndCommentFromAdapter(commentId)?.second ?: return
                comment.text
            } else {
                val parentComment = getIndexAndCommentFromAdapter(parentCommentId)?.second ?: return
                val reply = getIndexAndReplyFromComment(parentComment, commentId) ?: return
                reply.second.text
            }

        // updates the edittext with the comment to be edited
        binding.apply {
            editCommentId = commentId
            parentId = parentCommentId
            // decodes the comment text and sets to the edit text
            MemberTaggingDecoder.decode(
                etComment,
                commentText,
                LMFeedBranding.getTextLinkColor()
            )
            etComment.setSelection(etComment.length())
            etComment.focusAndShowKeyboard()
        }
    }

    // processes delete comment request
    private fun deleteComment(
        postId: String,
        commentId: String,
        uuid: String,
        parentCommentId: String? = null,
    ) {
        val deleteExtras = DeleteExtras.Builder()
            .postId(postId)
            .commentId(commentId)
            .entityType(DELETE_TYPE_COMMENT)
            .parentCommentId(parentCommentId)
            .build()

        showDeleteDialog(uuid, deleteExtras)
    }

    private fun showDeleteDialog(uuid: String, deleteExtras: DeleteExtras) {
        if (uuid == postActionsViewModel.getUUID()) {
            // when user deletes their own entity
            LMFeedSelfDeleteDialogFragment.showDialog(
                childFragmentManager,
                deleteExtras
            )
        } else {
            // when CM deletes other user's entity
            LMFeedAdminDeleteDialogFragment.showDialog(
                childFragmentManager,
                deleteExtras
            )
        }
    }

    // hides the replying to view
    private fun hideReplyingToView() {
        binding.apply {
            parentCommentIdToReply = null
            tvReplyingTo.hide()
            ivRemoveReplyingTo.hide()
        }
    }

    // adds the comment locally and calls api
    private fun addComment(postId: String, updatedText: String) {
        val createdAt = System.currentTimeMillis()
        val tempId = "-${createdAt}"

        // calls api
        viewModel.addComment(postId, tempId, updatedText)

        // adds comment locally
        val commentViewData = viewModel.getCommentViewDataForLocalHandling(
            postId,
            createdAt,
            tempId,
            updatedText,
            null
        )

        // remove NoCommentsViewData if visible
        if (mPostDetailAdapter[commentsCountPosition] is NoCommentsViewData) {
            mPostDetailAdapter.removeIndex(commentsCountPosition)
        }
        // gets old [CommentsCountViewData] from adapter
        if (mPostDetailAdapter[commentsCountPosition] != null) {
            val oldCommentsCountViewData =
                (mPostDetailAdapter[commentsCountPosition] as CommentsCountViewData)

            // updates old [CommentsCountViewData] by adding to [commentsCount]
            val updatedCommentsCountViewData = oldCommentsCountViewData.toBuilder()
                .commentsCount(oldCommentsCountViewData.commentsCount + 1)
                .build()

            // updates [CommentsCountViewData]
            mPostDetailAdapter.update(commentsCountPosition, updatedCommentsCountViewData)
        } else {
            // creates new [CommentsCountViewData] when the added comment is first
            val newCommentsCountViewData = CommentsCountViewData.Builder()
                .commentsCount(1)
                .build()
            mPostDetailAdapter.add(commentsCountPosition, newCommentsCountViewData)
        }

        // gets post from adapter
        var post = mPostDetailAdapter[postDataPosition] as PostViewData
        post = post.toBuilder()
            .commentsCount(post.commentsCount + 1)
            .build()

        // notifies the subscribers about the change in post data
        postEvent.notify(Pair(post.id, post))

        // updates comments count on header
        updateCommentsCount(post.commentsCount)

        // adds new comment to adapter
        mPostDetailAdapter.add(commentsStartPosition, commentViewData)

        // scroll to comment's position
        scrollToPositionWithOffset(commentsStartPosition, 75)

        // updates comment data in post
        mPostDetailAdapter.update(postDataPosition, post)
    }

    // adds the reply locally and calls api
    private fun addReply(updatedText: String) {
        val createdAt = System.currentTimeMillis()
        val tempId = "-${createdAt}"
        val postId = postDetailExtras.postId

        // input text is reply to a comment
        val parentCommentId = parentCommentIdToReply ?: return
        val parentComment = getIndexAndCommentFromAdapter(parentCommentId)?.second
            ?: return
        val parentCommentCreatorUUID = parentComment.user.sdkClientInfoViewData.uuid
        viewModel.replyComment(
            parentCommentCreatorUUID,
            postId,
            parentCommentId,
            updatedText,
            tempId
        )
        hideReplyingToView()

        // view data of comment with level-1
        val replyViewData = viewModel.getCommentViewDataForLocalHandling(
            postId,
            createdAt,
            tempId,
            updatedText,
            parentCommentId,
            level = 1
        )

        // adds reply to the adapter
        addReplyToAdapter(parentCommentId, replyViewData)
    }

    // edits the comment locally and calls api
    private fun editCommentLocally(updatedText: String) {
        // when an existing comment is edited
        val commentId = editCommentId ?: return

        // calls api
        viewModel.editComment(
            postDetailExtras.postId,
            commentId,
            updatedText
        )

        if (parentId == null) {
            // edited comment is of level-0

            val pair = getIndexAndCommentFromAdapter(commentId) ?: return
            val commentPosition = pair.first
            val comment = pair.second

            //update comment view data
            val updatedComment = comment.toBuilder()
                .fromCommentLiked(false)
                .fromCommentEdited(true)
                .isEdited(true)
                .text(updatedText)
                .build()

            mPostDetailAdapter.update(commentPosition, updatedComment)
        } else {
            // edited comment is of level-1 (reply)

            val pair = getIndexAndCommentFromAdapter(parentId ?: "") ?: return
            val parentIndex = pair.first
            val parentCommentInAdapter = pair.second

            // finds index of the reply inside the comment
            val replyPair =
                getIndexAndReplyFromComment(parentCommentInAdapter, commentId) ?: return

            val index = replyPair.first
            val reply = replyPair.second.toBuilder()
                .isEdited(true)
                .text(updatedText)
                .fromCommentEdited(true)
                .build()

            if (index == -1) return

            parentCommentInAdapter.replies[index] = reply

            val newViewData = parentCommentInAdapter.toBuilder()
                .fromCommentLiked(false)
                .fromCommentEdited(false)
                .build()

            // updates the parentComment with edited reply
            mPostDetailAdapter.update(parentIndex, newViewData)
        }
        editCommentId = null
        parentId = null
    }

    /*
    * Navigation Block
    */

    // callback when likes count of post is clicked - opens likes screen
    override fun showLikesScreen(postId: String) {
        val likesScreenExtras = LikesScreenExtras.Builder()
            .postId(postId)
            .entityType(POST)
            .build()
        LMFeedLikesActivity.start(requireContext(), likesScreenExtras)
    }

    // callback when likes count of a comment is clicked - opens likes screen
    override fun showLikesScreen(postId: String, commentId: String) {
        val likesScreenExtras = LikesScreenExtras.Builder()
            .postId(postId)
            .commentId(commentId)
            .entityType(COMMENT)
            .build()
        LMFeedLikesActivity.start(requireContext(), likesScreenExtras)
    }

    // Processes report action on entity
    private fun reportEntity(
        entityId: String,
        uuid: String,
        @ReportType
        entityType: Int,
        postId: String,
        postViewType: Int? = null,
        parentCommentId: String? = null
    ) {
        //create extras for [ReportActivity]
        val reportExtras = ReportExtras.Builder()
            .entityId(entityId)
            .uuid(uuid)
            .entityType(entityType)
            .postId(postId)
            .postViewType(postViewType)
            .parentCommentId(parentCommentId)
            .build()

        //get Intent for [ReportActivity]
        val intent = LMFeedReportActivity.getIntent(requireContext(), reportExtras)

        //start [ReportActivity] and check for result
        reportPostLauncher.launch(intent)
    }

    // launcher to start [ReportActivity] and show success dialog for result
    private val reportPostLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data?.getStringExtra(LMFeedReportFragment.REPORT_RESULT)

                val entityType = if (data == "Post") {
                    lmFeedHelperViewModel.getPostVariable()
                        .pluralizeOrCapitalize(WordAction.FIRST_LETTER_CAPITAL_SINGULAR)
                } else {
                    data
                }

                LMFeedReportSuccessDialog(entityType ?: "").show(
                    childFragmentManager,
                    LMFeedReportSuccessDialog.TAG
                )
            }
        }

    /**
     * Adapter Util Block
     */

    // sets page-1 data of post and scrolls to top
    private fun setPostDataAndScrollToTop(post: PostViewData) {
        // [ArrayList] to add all the items to adapter
        val postDetailList = ArrayList<BaseViewType>()
        // adds the post data at [postDataPosition]
        postDetailList.add(postDataPosition, post)

        if (post.commentsCount == 0) {
            // adds no comments view data
            val noCommentViewData = NoCommentsViewData.Builder().build()
            postDetailList.add(noCommentViewData)
        } else {
            // adds commentsCountViewData if comments are present
            postDetailList.add(
                commentsCountPosition,
                ViewDataConverter.convertCommentsCount(post.commentsCount)
            )
        }

        val comments = post.replies.toList()
        // adds all the comments to the [postDetailList]
        postDetailList.addAll(comments)
        mPostDetailAdapter.replace(postDetailList)

        if (toFindComment) {
            //find the comments already present in adapter
            val index = mPostDetailAdapter.items().indexOfFirst {
                (it is CommentViewData) && (it.id == postDetailExtras.commentId)
            }

            //comment not present -> get it from api
            if (index == -1) {
                viewModel.getComment(post.id, postDetailExtras.commentId ?: "", 1)
            } else {
                //scroll to that comment
                scrollToPositionWithOffset(index, 0)
            }
        } else {
            scrollToPositionWithOffset(postDataPosition, 0)
        }
    }

    // updates the post and add comments to adapter
    private fun updatePostAndAddComments(post: PostViewData) {
        // notifies the subscribers about the change in post data
        postEvent.notify(Pair(post.id, post))

        // updates the post
        mPostDetailAdapter.update(postDataPosition, post)
        // adds the paginated comments
        mPostDetailAdapter.addAll(post.replies.toList())
    }

    // refreshes the whole post detail screen
    private fun refreshPostData() {
        mSwipeRefreshLayout.isRefreshing = true
        mScrollListener.resetData()
        fetchPostData(true)
    }

    override fun likePost(position: Int) {
        //get item
        val item = mPostDetailAdapter[position]
        if (item is PostViewData) {
            //new like count
            val newLikesCount = if (item.isLiked) {
                item.likesCount - 1
            } else {
                item.likesCount + 1
            }

            //update post view data
            val newViewData = item.toBuilder()
                .fromPostLiked(true)
                .isLiked(!item.isLiked)
                .likesCount(newLikesCount)
                .build()

            postEvent.notify(Pair(newViewData.id, newViewData))

            //call api
            postActionsViewModel.likePost(newViewData.id, !item.isLiked)
            //update recycler
            mPostDetailAdapter.update(position, newViewData)
        }
    }

    override fun savePost(position: Int) {
        //get item
        val item = mPostDetailAdapter[position]
        if (item is PostViewData) {
            //update the post view data
            val newViewData = item.toBuilder()
                .fromPostSaved(true)
                .isSaved(!item.isSaved)
                .build()

            // notifies the subscribers about the change
            postEvent.notify(Pair(newViewData.id, newViewData))

            //create toast message
            val postAsVariable = lmFeedHelperViewModel.getPostVariable()
            val toastMessage = if (!item.isSaved) {
                getString(
                    R.string.s_saved,
                    postAsVariable.pluralizeOrCapitalize(WordAction.FIRST_LETTER_CAPITAL_SINGULAR)
                )
            } else {
                getString(
                    R.string.s_unsaved,
                    postAsVariable.pluralizeOrCapitalize(WordAction.FIRST_LETTER_CAPITAL_SINGULAR)
                )
            }

            //show toast
            Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_SHORT).show()


            //call api
            postActionsViewModel.savePost(newViewData.id)

            //update recycler
            mPostDetailAdapter.update(position, newViewData)
        }
    }

    // updates post view data when see more is clicked
    override fun updatePostSeenFullContent(position: Int, alreadySeenFullContent: Boolean) {
        val item = mPostDetailAdapter[position]
        if (item is PostViewData) {
            val newViewData = item.toBuilder()
                .alreadySeenFullContent(alreadySeenFullContent)
                .build()
            mPostDetailAdapter.update(position, newViewData)
        }
    }

    // updates comment view data when see more is clicked
    override fun updateCommentSeenFullContent(
        position: Int,
        alreadySeenFullContent: Boolean,
        parentCommentId: String?
    ) {
        val item =
            if (parentCommentId == null) {
                mPostDetailAdapter[position]
            } else {
                val indexAndComment = getIndexAndCommentFromAdapter(parentCommentId) ?: return
                val comment = indexAndComment.second
                comment.replies[position]
            }
        if (item is CommentViewData) {
            val newViewData = item.toBuilder()
                .alreadySeenFullContent(alreadySeenFullContent)
                .fromCommentLiked(false)
                .fromCommentEdited(false)
                .build()
            mPostDetailAdapter.update(position, newViewData)
        }
    }

    // adds the reply to its parentComment
    private fun addReplyToAdapter(parentCommentId: String, reply: CommentViewData) {
        // gets the parentComment from adapter
        val parentComment = getIndexAndCommentFromAdapter(parentCommentId) ?: return
        val parentIndex = parentComment.first
        val parentCommentViewData = parentComment.second

        // adds the reply at first
        parentCommentViewData.replies.add(0, reply)

        val newCommentViewData = parentCommentViewData.toBuilder()
            .fromCommentLiked(false)
            .fromCommentEdited(false)
            .repliesCount(parentCommentViewData.repliesCount + 1)
            .build()

        // updates the parentComment with added reply
        mPostDetailAdapter.update(parentIndex, newCommentViewData)

        // scroll to comment's position
        scrollToPositionWithOffset(parentIndex, 75)
    }

    // removes the reply from its parentComment
    private fun removeReplyFromAdapter(parentCommentId: String, replyId: String) {
        // gets the parentComment from adapter
        val parentComment = getIndexAndCommentFromAdapter(parentCommentId) ?: return
        val parentIndex = parentComment.first
        val parentCommentViewData = parentComment.second

        // removes the reply with specified replyId
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            parentCommentViewData.replies.removeIf {
                it.id == replyId
            }
        } else {
            val index = parentCommentViewData.replies.indexOfFirst {
                it.id == replyId
            }
            if (index == -1) return

            parentCommentViewData.replies.removeAt(index)
        }

        val newCommentViewData = parentCommentViewData.toBuilder()
            .fromCommentLiked(false)
            .fromCommentEdited(false)
            .repliesCount(parentCommentViewData.repliesCount - 1)
            .build()

        // updates the parentComment with removed reply
        mPostDetailAdapter.update(parentIndex, newCommentViewData)
    }

    // adds paginated replies to comment
    private fun addReplies(comment: CommentViewData, page: Int) {
        // gets comment from adapter
        val indexAndComment = getIndexAndCommentFromAdapter(comment.id)

        //if comment is not present in adapter
        if (indexAndComment == null) {
            //set to false because comment is added
            toFindComment = false
            //add comment to adapter
            mPostDetailAdapter.add(commentsStartPosition, comment)
            //scroll to the comment
            scrollToPositionWithOffset(commentsStartPosition, 0)
        } else {
            val index = indexAndComment.first
            val adapterComment = indexAndComment.second
            if (page == 1) {
                // updates the comment with page-1 replies
                mPostDetailAdapter.update(index, comment)
                scrollToPositionWithOffset(index, 75)
            } else {
                // adds replies in adapter and fetched replies
                comment.replies.addAll(
                    0,
                    adapterComment.replies
                )
                mPostDetailAdapter.update(index, comment)
                scrollToPositionWithOffset(index + 1, 150)
            }
        }
    }

    // callback when comment/reply is liked
    override fun likeComment(commentId: String) {
        val indexAndComment = getIndexAndCommentFromAdapter(commentId) ?: return
        val position = indexAndComment.first
        val comment = indexAndComment.second
        //new like count
        val newLikesCount = if (comment.isLiked) {
            comment.likesCount - 1
        } else {
            comment.likesCount + 1
        }

        //update comment view data
        val newViewData = comment.toBuilder()
            .fromCommentLiked(true)
            .fromCommentEdited(false)
            .isLiked(!comment.isLiked)
            .likesCount(newLikesCount)
            .build()

        //call api
        viewModel.likeComment(newViewData.postId, newViewData.id, !comment.isLiked)
        //update recycler
        mPostDetailAdapter.update(position, newViewData)
    }

    // callback when reply is liked by user
    override fun likeReply(parentCommentId: String, replyId: String) {
        // gets parentComment from adapter
        val parentIndexAndComment = getIndexAndCommentFromAdapter(parentCommentId) ?: return
        val position = parentIndexAndComment.first
        val parentComment = parentIndexAndComment.second

        // gets reply from the comment
        val reply = getIndexAndReplyFromComment(parentComment, replyId) ?: return
        val replyIndex = reply.first
        val replyViewData = reply.second

        //new like count
        val newLikesCount = if (replyViewData.isLiked) {
            replyViewData.likesCount - 1
        } else {
            replyViewData.likesCount + 1
        }

        val updatedReply = parentComment.replies[replyIndex]
            .toBuilder()
            .isLiked(!replyViewData.isLiked)
            .likesCount(newLikesCount)
            .build()

        parentComment.replies[replyIndex] = updatedReply

        //update comment view data
        val newViewData = parentComment.toBuilder()
            .fromCommentLiked(false)
            .fromCommentEdited(false)
            .replies(parentComment.replies)
            .build()

        //call api
        viewModel.likeComment(newViewData.postId, updatedReply.id, !replyViewData.isLiked)
        //update recycler
        mPostDetailAdapter.update(position, newViewData)
    }

    // callback when replying on a comment
    override fun replyOnComment(
        commentId: String,
        commentPosition: Int,
        parentCommenter: UserViewData
    ) {
        parentCommentIdToReply = commentId
        binding.apply {
            tvReplyingTo.show()
            ivRemoveReplyingTo.show()

            tvReplyingTo.text = String.format(
                getString(R.string.replying_to_s),
                parentCommenter.name
            )

            etComment.focusAndShowKeyboard()

            rvPostDetails.smoothScrollToPosition(
                commentPosition
            )
        }
    }

    // callback when post menu items are clicked
    override fun onPostMenuItemClicked(
        postId: String,
        postCreatorUUID: String,
        menuId: Int
    ) {
        when (menuId) {
            EDIT_POST_MENU_ITEM_ID -> {
                val editPostExtras = LMFeedEditPostExtras.Builder()
                    .postId(postId)
                    .build()
                val intent = LMFeedEditPostActivity.getIntent(requireContext(), editPostExtras)
                editPostLauncher.launch(intent)
            }

            DELETE_POST_MENU_ITEM_ID -> {
                deletePost(
                    postId,
                    postCreatorUUID
                )
            }

            REPORT_POST_MENU_ITEM_ID -> {
                val postData = mPostDetailAdapter[postDataPosition] as PostViewData
                val postViewType = postData.viewType
                reportEntity(
                    postId,
                    postCreatorUUID,
                    REPORT_TYPE_POST,
                    postId,
                    postViewType = postViewType
                )
            }

            PIN_POST_MENU_ITEM_ID -> {
                pinPost()
            }

            UNPIN_POST_MENU_ITEM_ID -> {
                unpinPost()
            }
        }
    }

    // launcher for [EditPostActivity]
    private val editPostLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    refreshPostData()
                }
            }
        }

    private fun pinPost() {
        //get item
        val post = mPostDetailAdapter[postDataPosition] as PostViewData

        //get pin menu item
        val menuItems = post.menuItems.toMutableList()
        val pinPostIndex = menuItems.indexOfFirst {
            (it.id == PIN_POST_MENU_ITEM_ID)
        }

        //if pin item doesn't exist
        if (pinPostIndex == -1) return

        //update pin menu item
        val pinPostMenuItem = menuItems[pinPostIndex]
        val newPinPostMenuItem =
            pinPostMenuItem.toBuilder()
                .id(UNPIN_POST_MENU_ITEM_ID)
                .title(
                    getString(
                        R.string.unpin_this_s,
                        lmFeedHelperViewModel.getPostVariable()
                            .pluralizeOrCapitalize(WordAction.FIRST_LETTER_CAPITAL_SINGULAR)
                    )
                )
                .build()
        menuItems[pinPostIndex] = newPinPostMenuItem

        //update the post view data
        val newViewData = post.toBuilder()
            .isPinned(!post.isPinned)
            .menuItems(menuItems)
            .build()

        //call api
        postActionsViewModel.pinPost(post)

        //update recycler
        mPostDetailAdapter.update(postDataPosition, newViewData)

        // notifies the subscribers about the change in post data
        postEvent.notify(Pair(newViewData.id, newViewData))
    }

    private fun unpinPost() {
        //get item
        val post = mPostDetailAdapter[postDataPosition] as PostViewData

        //get unpin menu item
        val menuItems = post.menuItems.toMutableList()
        val unPinPostIndex = menuItems.indexOfFirst {
            (it.id == UNPIN_POST_MENU_ITEM_ID)
        }

        //if unpin item doesn't exist
        if (unPinPostIndex == -1) return

        //update unpin menu item
        val unPinPostMenuItem = menuItems[unPinPostIndex]
        val newUnPinPostMenuItem =
            unPinPostMenuItem.toBuilder().id(PIN_POST_MENU_ITEM_ID)
                .title(
                    getString(
                        R.string.pin_this_s,
                        lmFeedHelperViewModel.getPostVariable()
                            .pluralizeOrCapitalize(WordAction.FIRST_LETTER_CAPITAL_SINGULAR)
                    )
                )
                .build()
        menuItems[unPinPostIndex] = newUnPinPostMenuItem

        //update the post view data
        val newViewData = post.toBuilder()
            .isPinned(!post.isPinned)
            .menuItems(menuItems)
            .build()

        //call api
        postActionsViewModel.pinPost(post)

        //update recycler
        mPostDetailAdapter.update(postDataPosition, newViewData)

        // notifies the subscribers about the change in post data
        postEvent.notify(Pair(newViewData.id, newViewData))
    }

    // callback when replyCount is clicked to view replies
    override fun fetchReplies(commentId: String) {
        val comment = getIndexAndCommentFromAdapter(commentId)?.second ?: return

        // gets page-1 replies
        viewModel.getComment(
            comment.postId,
            comment.id,
            1
        )
    }

    // callback for comment's menu is item
    override fun onCommentMenuItemClicked(
        postId: String,
        commentId: String,
        commentCreatorUUID: String,
        menuId: Int
    ) {
        when (menuId) {
            EDIT_COMMENT_MENU_ITEM_ID -> {
                editComment(commentId)
            }

            DELETE_COMMENT_MENU_ITEM_ID -> {
                deleteComment(
                    postId,
                    commentId,
                    commentCreatorUUID
                )
            }

            REPORT_COMMENT_MENU_ITEM_ID -> {
                reportEntity(
                    commentId,
                    commentCreatorUUID,
                    REPORT_TYPE_COMMENT,
                    postId
                )
            }
        }
    }

    // callback when view more replies is clicked
    override fun viewMoreReplies(
        parentCommentId: String,
        page: Int
    ) {
        val comment = getIndexAndCommentFromAdapter(parentCommentId)?.second ?: return
        viewModel.getComment(
            comment.postId,
            parentCommentId,
            page
        )
    }

    // callback when the item of reply menu is clicked
    override fun onReplyMenuItemClicked(
        postId: String,
        parentCommentId: String,
        replyId: String,
        replyCreatorUUID: String,
        menuId: Int
    ) {
        when (menuId) {
            EDIT_COMMENT_MENU_ITEM_ID -> {
                editComment(replyId, parentCommentId)
            }

            DELETE_COMMENT_MENU_ITEM_ID -> {
                deleteComment(
                    postId,
                    replyId,
                    replyCreatorUUID,
                    parentCommentId
                )
            }

            REPORT_COMMENT_MENU_ITEM_ID -> {
                reportEntity(
                    replyId,
                    replyCreatorUUID,
                    REPORT_TYPE_REPLY,
                    postId,
                    parentCommentId = parentCommentId
                )
            }
        }
    }

    // callback when self post is deleted by user
    override fun selfDelete(deleteExtras: DeleteExtras) {
        when (deleteExtras.entityType) {
            DELETE_TYPE_POST -> {
                val post = mPostDetailAdapter[postDataPosition] as PostViewData
                postActionsViewModel.deletePost(post)
            }

            DELETE_TYPE_COMMENT -> {
                val commentId = deleteExtras.commentId ?: return
                viewModel.deleteComment(
                    deleteExtras.postId,
                    commentId,
                    parentCommentId = deleteExtras.parentCommentId
                )
            }
        }
    }

    // callback when other's post is deleted by CM
    override fun adminDelete(deleteExtras: DeleteExtras, reason: String) {
        when (deleteExtras.entityType) {
            DELETE_TYPE_POST -> {
                val post = mPostDetailAdapter[postDataPosition] as PostViewData
                postActionsViewModel.deletePost(post, reason)
            }

            DELETE_TYPE_COMMENT -> {
                val commentId = deleteExtras.commentId ?: return
                viewModel.deleteComment(
                    deleteExtras.postId,
                    commentId,
                    parentCommentId = deleteExtras.parentCommentId,
                    reason = reason
                )
            }
        }
    }

    // callback when +x more text is clicked to see more documents
    override fun onMultipleDocumentsExpanded(postData: PostViewData, position: Int) {
        if (position == mPostDetailAdapter.items().size - 1) {
            scrollToPositionWithOffset(position, 75)
        }

        mPostDetailAdapter.update(
            position,
            postData.toBuilder().isExpanded(true).build()
        )
    }

    // updates the fromPostLiked/fromPostSaved variables and updates the rv list
    override fun updateFromLikedSaved(position: Int) {
        var postData = mPostDetailAdapter[postDataPosition] as PostViewData
        postData = postData.toBuilder()
            .fromPostLiked(false)
            .fromPostSaved(false)
            .build()
        mPostDetailAdapter.updateWithoutNotifyingRV(position, postData)
    }

    // callback when user clicks to share the post
    override fun sharePost(postId: String) {
        ShareUtils.sharePost(
            requireContext(),
            postId,
            ShareUtils.domain,
            lmFeedHelperViewModel.getPostVariable()
        )
        val post = mPostDetailAdapter[postDataPosition] as PostViewData
        postActionsViewModel.sendPostShared(post)
    }

    //get index and post from the adapter using tempId
    private fun getIndexAndCommentFromAdapterUsingTempId(tempId: String?): Pair<Int, CommentViewData>? {
        val index = mPostDetailAdapter.items().indexOfFirst {
            (it is CommentViewData) && (it.tempId == tempId)
        }

        if (index == -1) {
            return null
        }

        val comment = getCommentFromAdapter(index)

        return Pair(index, comment)
    }

    //get index and post from the adapter using commentId
    private fun getIndexAndCommentFromAdapter(commentId: String): Pair<Int, CommentViewData>? {
        val index = mPostDetailAdapter.items().indexOfFirst {
            (it is CommentViewData) && (it.id == commentId)
        }

        if (index == -1) {
            return null
        }

        val comment = getCommentFromAdapter(index)

        return Pair(index, comment)
    }

    private fun getCommentFromAdapter(position: Int): CommentViewData {
        return mPostDetailAdapter.items()[position] as CommentViewData
    }

    //get index and reply from the parentComment using replyId
    private fun getIndexAndReplyFromComment(
        parentComment: CommentViewData,
        replyId: String
    ): Pair<Int, CommentViewData>? {
        val index = parentComment.replies.indexOfFirst {
            it.id == replyId
        }

        if (index == -1) {
            return null
        }

        val reply = parentComment.replies[index]

        return Pair(index, reply)
    }

    /**
     * Scroll to a position with offset from the top header
     * @param position Index of the item to scroll to
     * @param offset value with which to scroll
     */
    private fun scrollToPositionWithOffset(position: Int, offset: Int) {
        binding.rvPostDetails.post {
            val px = (ViewUtils.dpToPx(offset) * 1.5).toInt()
            (binding.rvPostDetails.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
                position,
                px
            )
        }
    }

    // removes the player and destroys the [postVideoAutoPlayHelper]
    private fun destroyAutoPlayer() {
        if (::postVideoAutoPlayHelper.isInitialized) {
            postVideoAutoPlayHelper.detachScrollListenerForVideo()
            postVideoAutoPlayHelper.destroy()
        }
    }

    override fun doCleanup() {
        super.doCleanup()
        destroyAutoPlayer()
    }
}