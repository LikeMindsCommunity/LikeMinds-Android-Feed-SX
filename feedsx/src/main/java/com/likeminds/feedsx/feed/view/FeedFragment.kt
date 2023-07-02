package com.likeminds.feedsx.feed.view

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.likeminds.feedsx.InitiateViewModel
import com.likeminds.feedsx.LMAnalytics
import com.likeminds.feedsx.R
import com.likeminds.feedsx.SDKApplication
import com.likeminds.feedsx.SDKApplication.Companion.LOG_TAG
import com.likeminds.feedsx.branding.model.LMBranding
import com.likeminds.feedsx.databinding.FragmentFeedBinding
import com.likeminds.feedsx.delete.model.DELETE_TYPE_POST
import com.likeminds.feedsx.delete.model.DeleteExtras
import com.likeminds.feedsx.delete.view.AdminDeleteDialogFragment
import com.likeminds.feedsx.delete.view.SelfDeleteDialogFragment
import com.likeminds.feedsx.feed.model.FeedExtras
import com.likeminds.feedsx.feed.util.PostEvent
import com.likeminds.feedsx.feed.util.PostEvent.PostObserver
import com.likeminds.feedsx.feed.viewmodel.FeedViewModel
import com.likeminds.feedsx.likes.model.LikesScreenExtras
import com.likeminds.feedsx.likes.model.POST
import com.likeminds.feedsx.likes.view.LikesActivity
import com.likeminds.feedsx.media.util.PostVideoAutoPlayHelper
import com.likeminds.feedsx.notificationfeed.view.NotificationFeedActivity
import com.likeminds.feedsx.overflowmenu.model.DELETE_POST_MENU_ITEM_ID
import com.likeminds.feedsx.overflowmenu.model.EDIT_POST_MENU_ITEM_ID
import com.likeminds.feedsx.overflowmenu.model.PIN_POST_MENU_ITEM_ID
import com.likeminds.feedsx.overflowmenu.model.REPORT_POST_MENU_ITEM_ID
import com.likeminds.feedsx.overflowmenu.model.UNPIN_POST_MENU_ITEM_ID
import com.likeminds.feedsx.post.create.view.CreatePostActivity
import com.likeminds.feedsx.post.detail.model.PostDetailExtras
import com.likeminds.feedsx.post.detail.view.PostDetailActivity
import com.likeminds.feedsx.post.edit.model.EditPostExtras
import com.likeminds.feedsx.post.edit.view.EditPostActivity
import com.likeminds.feedsx.post.viewmodel.PostActionsViewModel
import com.likeminds.feedsx.posttypes.model.PostViewData
import com.likeminds.feedsx.posttypes.model.UserViewData
import com.likeminds.feedsx.posttypes.view.adapter.PostAdapter
import com.likeminds.feedsx.posttypes.view.adapter.PostAdapterListener
import com.likeminds.feedsx.report.model.REPORT_TYPE_POST
import com.likeminds.feedsx.report.model.ReportExtras
import com.likeminds.feedsx.report.view.ReportActivity
import com.likeminds.feedsx.report.view.ReportFragment
import com.likeminds.feedsx.report.view.ReportSuccessDialog
import com.likeminds.feedsx.utils.EndlessRecyclerScrollListener
import com.likeminds.feedsx.utils.MemberImageUtil
import com.likeminds.feedsx.utils.ProgressHelper
import com.likeminds.feedsx.utils.ShareUtils
import com.likeminds.feedsx.utils.ViewUtils
import com.likeminds.feedsx.utils.ViewUtils.hide
import com.likeminds.feedsx.utils.ViewUtils.show
import com.likeminds.feedsx.utils.customview.BaseFragment
import com.likeminds.feedsx.utils.databinding.ImageBindingUtil
import com.likeminds.feedsx.utils.mediauploader.MediaUploadWorker
import com.likeminds.feedsx.utils.model.BaseViewType
import com.likeminds.feedsx.utils.observeInLifecycle
import kotlinx.coroutines.flow.onEach
import java.util.UUID
import javax.inject.Inject

class FeedFragment :
    BaseFragment<FragmentFeedBinding, FeedViewModel>(),
    PostAdapterListener,
    AdminDeleteDialogFragment.DeleteDialogListener,
    SelfDeleteDialogFragment.DeleteAlertDialogListener,
    PostObserver {

    companion object {
        const val FEED_EXTRAS = "FEED_EXTRAS"
    }

    private lateinit var feedExtras: FeedExtras

    // shared viewModel between [FeedFragment] and [PostDetailFragment] for postActions
    @Inject
    lateinit var postActionsViewModel: PostActionsViewModel

    @Inject
    lateinit var initiateViewModel: InitiateViewModel

    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    private lateinit var mPostAdapter: PostAdapter
    private lateinit var mScrollListener: EndlessRecyclerScrollListener
    private lateinit var postVideoAutoPlayHelper: PostVideoAutoPlayHelper

    // variable to check if there is a post already uploading
    private var alreadyPosting: Boolean = false
    private val workersMap by lazy { ArrayList<UUID>() }

    private val postPublisher by lazy {
        PostEvent.getPublisher()
    }

    override val useSharedViewModel: Boolean
        get() = true

    override fun getViewModelClass(): Class<FeedViewModel> {
        return FeedViewModel::class.java
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().feedComponent()?.inject(this)
    }

    override fun getViewBinding(): FragmentFeedBinding {
        return FragmentFeedBinding.inflate(layoutInflater)
    }

    override fun receiveExtras() {
        super.receiveExtras()
        if (arguments == null || arguments?.containsKey(FEED_EXTRAS) == false) {
            requireActivity().supportFragmentManager.popBackStack()
            return
        }
        feedExtras = arguments?.getParcelable(FEED_EXTRAS)!!
    }

    override fun setUpViews() {
        super.setUpViews()
        initUI()
        initiateSDK()
        initToolbar()
    }

    override fun observeData() {
        super.observeData()
        observePosting()

        // observes userResponse LiveData
        initiateViewModel.userResponse.observe(viewLifecycleOwner) { response ->
            observeUserResponse(response)
        }

        // observes hasCreatePostRights LiveData
        initiateViewModel.hasCreatePostRights.observe(viewLifecycleOwner) {
            initNewPostClick(it)
        }

        // observes logoutResponse LiveData
        initiateViewModel.logoutResponse.observe(viewLifecycleOwner) {
            Log.d(
                LOG_TAG,
                "initiate api sdk called -> success and have not app access"
            )
            showInvalidAccess()
        }

        initiateViewModel.initiateErrorMessage.observe(viewLifecycleOwner) {
            ProgressHelper.hideProgress(binding.progressBar)
            ViewUtils.showErrorMessageToast(requireContext(), it)
        }

        // observe unread notification count
        viewModel.unreadNotificationCount.observe(viewLifecycleOwner) { count ->
            observeUnreadNotificationCount(count)
        }

        // observe universal feed
        viewModel.universalFeedResponse.observe(viewLifecycleOwner) { pair ->
            observeFeedUniversal(pair)
        }

        // observes deletePostResponse LiveData
        postActionsViewModel.deletePostResponse.observe(viewLifecycleOwner) { postId ->
            val indexToRemove = getIndexAndPostFromAdapter(postId)?.first ?: return@observe
            mPostAdapter.removeIndex(indexToRemove)
            checkForNoPost(mPostAdapter.items())
            refreshAutoPlayer()
            ViewUtils.showShortToast(
                requireContext(),
                getString(R.string.post_deleted)
            )
        }

        // observes pinPostResponse LiveData
        postActionsViewModel.pinPostResponse.observe(viewLifecycleOwner) { postId ->
            val post = getIndexAndPostFromAdapter(postId)?.second ?: return@observe
            if (post.isPinned) {
                ViewUtils.showShortToast(requireContext(), getString(R.string.post_pinned_to_top))
            } else {
                ViewUtils.showShortToast(requireContext(), getString(R.string.post_unpinned))
            }
        }

        //observes errorMessage for the apis
        viewModel.errorMessageEventFlow.onEach { response ->
            observeErrorMessage(response)
        }.observeInLifecycle(viewLifecycleOwner)

        //observes errorMessage for the apis in shared view model
        postActionsViewModel.errorMessageEventFlow.onEach { response ->
            observeSharedErrorMessage(response)
        }.observeInLifecycle(viewLifecycleOwner)
    }

    // observes user response from InitiateUser
    private fun observeUserResponse(user: UserViewData?) {
        initToolbar()
        setUserImage(user)
        viewModel.getUnreadNotificationCount()
        viewModel.getUniversalFeed(1)
    }

    // observe unread notification count
    private fun observeUnreadNotificationCount(count: Int) {
        binding.apply {
            ivNotification.show()
            when (count) {
                0 -> {
                    tvNotificationCount.isVisible = false
                }

                in 1..99 -> {
                    configureNotificationBadge(count.toString())
                }

                else -> {
                    configureNotificationBadge(getString(R.string.nine_nine_plus))
                }
            }
        }
    }

    /**
     * Configure the notification badge based on the text length and visibility
     * @param text Text to show on the counter, eg - 99+, 8, etc
     */
    private fun configureNotificationBadge(text: String) {
        binding.tvNotificationCount.apply {
            if (text.length > 2) {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 7f)
            } else {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            }
            this.text = text
            visibility = View.VISIBLE
        }
    }

    //observe feed response
    private fun observeFeedUniversal(pair: Pair<Int, List<PostViewData>>) {
        //hide progress bar
        ProgressHelper.hideProgress(binding.progressBar)
        //page in api send
        val page = pair.first

        //list of post
        val feed = pair.second

        //if pull to refresh is called
        if (mSwipeRefreshLayout.isRefreshing) {
            checkForNoPost(feed)
            setFeedAndScrollToTop(feed)
            mSwipeRefreshLayout.isRefreshing = false
            return
        }

        //normal adding
        if (page == 1) {
            checkForNoPost(feed)
            setFeedAndScrollToTop(feed)
        } else {
            mPostAdapter.addAll(feed)
            refreshAutoPlayer()
        }
    }

    // observes post live data
    private fun observePosting() {
        viewModel.postDataEventFlow.onEach { response ->
            when (response) {
                // when the post data comes from local db
                is FeedViewModel.PostDataEvent.PostDbData -> {
                    alreadyPosting = true
                    val post = response.post
                    binding.layoutPosting.apply {
                        root.show()
                        if (post.thumbnail.isNullOrEmpty()) {
                            ivPostThumbnail.hide()
                        } else {
                            val shimmer = ViewUtils.getShimmer()
                            ivPostThumbnail.show()
                            ImageBindingUtil.loadImage(
                                ivPostThumbnail,
                                Uri.parse(post.thumbnail),
                                shimmer
                            )
                        }
                        postingProgress.progress = 0
                        postingProgress.show()
                        ivPosted.hide()
                        tvRetry.hide()
                        observeMediaUpload(post)
                    }
                }
                // when the post data comes from api response
                is FeedViewModel.PostDataEvent.PostResponseData -> {
                    binding.apply {
                        ViewUtils.showShortToast(requireContext(), getString(R.string.post_created))
                        refreshFeed()
                        removePostingView()
                    }
                }
            }
        }.observeInLifecycle(viewLifecycleOwner)
    }

    // checks if there is any post or not
    private fun checkForNoPost(feed: List<BaseViewType>) {
        if (feed.isNotEmpty()) {
            binding.apply {
                layoutNoPost.root.hide()
                newPostButton.show()
                recyclerView.show()
            }
        } else {
            binding.apply {
                layoutNoPost.root.show()
                newPostButton.hide()
                recyclerView.hide()
            }
        }
    }

    // finds the upload worker by UUID and observes the worker
    private fun observeMediaUpload(postingData: PostViewData) {
        if (postingData.uuid.isEmpty()) {
            return
        }
        val uuid = UUID.fromString(postingData.uuid)
        if (!workersMap.contains(uuid)) {
            workersMap.add(uuid)
            WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(uuid)
                .observe(viewLifecycleOwner) { workInfo ->
                    observeMediaWorker(workInfo, postingData)
                }
        }
    }

    // observes the media worker through various worker lifecycle
    private fun observeMediaWorker(
        workInfo: WorkInfo,
        postingData: PostViewData
    ) {
        when (workInfo.state) {
            WorkInfo.State.SUCCEEDED -> {
                // uploading completed, call the add post api
                binding.layoutPosting.apply {
                    postingProgress.hide()
                    tvRetry.hide()
                    ivPosted.show()
                }
                viewModel.addPost(postingData)
            }

            WorkInfo.State.FAILED -> {
                // uploading failed, initiate retry mechanism
                val indexList = workInfo.outputData.getIntArray(
                    MediaUploadWorker.ARG_MEDIA_INDEX_LIST
                ) ?: return
                initRetryAction(
                    postingData.temporaryId,
                    indexList.size
                )
            }

            else -> {
                // uploading in progress, map the progress to progress bar
                val progress = MediaUploadWorker.getProgress(workInfo) ?: return
                binding.layoutPosting.apply {
                    val percentage = (((1.0 * progress.first) / progress.second) * 100)
                    val progressValue = percentage.toInt()
                    postingProgress.progress = progressValue
                }
            }
        }
    }

    // initializes retry mechanism for attachments uploading
    private fun initRetryAction(temporaryId: Long?, attachmentCount: Int) {
        binding.layoutPosting.apply {
            ivPosted.hide()
            postingProgress.hide()
            tvRetry.show()
            tvRetry.setOnClickListener {
                viewModel.createRetryPostMediaWorker(
                    requireContext(),
                    temporaryId,
                    attachmentCount
                )
            }
        }
    }

    //observe error handling
    private fun observeErrorMessage(response: FeedViewModel.ErrorMessageEvent) {
        when (response) {
            is FeedViewModel.ErrorMessageEvent.UniversalFeed -> {
                val errorMessage = response.errorMessage
                mSwipeRefreshLayout.isRefreshing = false
                ProgressHelper.hideProgress(binding.progressBar)
                ViewUtils.showErrorMessageToast(requireContext(), errorMessage)
            }

            is FeedViewModel.ErrorMessageEvent.AddPost -> {
                ViewUtils.showErrorMessageToast(requireContext(), response.errorMessage)
                removePostingView()
            }

            is FeedViewModel.ErrorMessageEvent.GetUnreadNotificationCount -> {
                binding.tvNotificationCount.hide()
                ViewUtils.showErrorMessageToast(requireContext(), response.errorMessage)
            }
        }
    }

    private fun observeSharedErrorMessage(response: PostActionsViewModel.ErrorMessageEvent) {
        when (response) {
            is PostActionsViewModel.ErrorMessageEvent.LikePost -> {
                val postId = response.postId

                //get post and index
                val pair = getIndexAndPostFromAdapter(postId) ?: return
                val post = pair.second
                val index = pair.first

                //update post view data
                val updatedPost = post.toBuilder()
                    .isLiked(false)
                    .fromPostLiked(true)
                    .likesCount(post.likesCount - 1)
                    .build()

                //update recycler view
                mPostAdapter.update(index, updatedPost)

                //show error message
                val errorMessage = response.errorMessage
                ViewUtils.showErrorMessageToast(requireContext(), errorMessage)
            }

            is PostActionsViewModel.ErrorMessageEvent.SavePost -> {
                val postId = response.postId

                //get post and index
                val pair = getIndexAndPostFromAdapter(postId) ?: return
                val post = pair.second
                val index = pair.first

                //update post view data
                val updatedPost = post.toBuilder()
                    .isSaved(false)
                    .fromPostSaved(true)
                    .build()

                //update recycler view
                mPostAdapter.update(index, updatedPost)

                //show error message
                val errorMessage = response.errorMessage
                ViewUtils.showErrorMessageToast(requireContext(), errorMessage)
            }

            is PostActionsViewModel.ErrorMessageEvent.DeletePost -> {
                val errorMessage = response.errorMessage
                ViewUtils.showErrorMessageToast(requireContext(), errorMessage)
            }

            is PostActionsViewModel.ErrorMessageEvent.PinPost -> {
                val postId = response.postId

                //get post and index
                val pair = getIndexAndPostFromAdapter(postId) ?: return
                val post = pair.second
                val index = pair.first

                //update post view data
                val updatedPost = post.toBuilder()
                    .isPinned(!post.isPinned)
                    .build()

                //update recycler view
                mPostAdapter.update(index, updatedPost)

                //show error message
                val errorMessage = response.errorMessage
                ViewUtils.showErrorMessageToast(requireContext(), errorMessage)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        postPublisher.subscribe(this)
    }

    override fun onResume() {
        super.onResume()

        // sends feed opened event
        viewModel.sendFeedOpenedEvent()

        val temporaryId = viewModel.getTemporaryId()
        if (temporaryId != -1L && !alreadyPosting) {
            removePostingView()
            viewModel.fetchPendingPostFromDB()
        }
        initiateAutoPlayer()
    }

    override fun onPause() {
        super.onPause()
        // removes the player and destroys the [postVideoAutoPlayHelper]
        postVideoAutoPlayHelper.destroy()
    }

    override fun onDestroy() {
        super.onDestroy()
        // unsubscribes itself from the [PostPublisher]
        postPublisher.unsubscribe(this)
    }

    // initiates SDK
    private fun initiateSDK() {
        ProgressHelper.showProgress(binding.progressBar)
        initiateViewModel.initiateUser()
    }

    /**
     * UI Block
     **/

    // initializes various UI components
    private fun initUI() {
        binding.toolbarColor = LMBranding.getToolbarColor()

        setStatusBarColor()
        initRecyclerView()
        initNewPostClick(true)
        initSwipeRefreshLayout()
    }

    private fun setStatusBarColor() {
        requireActivity().window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = LMBranding.getHeaderColor()
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    // initializes new post fab
    private fun initNewPostClick(hasCreatePostRights: Boolean) {
        binding.apply {
            // sets color of fab button as per user rights
            if (hasCreatePostRights) {
                layoutNoPost.fabNewPost.backgroundTintList =
                    ColorStateList.valueOf(LMBranding.getButtonsColor())
                newPostButton.backgroundTintList =
                    ColorStateList.valueOf(LMBranding.getButtonsColor())
            } else {
                layoutNoPost.fabNewPost.backgroundTintList =
                    ColorStateList.valueOf(Color.GRAY)
                newPostButton.backgroundTintList =
                    ColorStateList.valueOf(Color.GRAY)
            }

            layoutNoPost.fabNewPost.setOnClickListener {
                handleNewPostClick(hasCreatePostRights)
            }

            newPostButton.setOnClickListener {
                handleNewPostClick(hasCreatePostRights)
            }
        }
    }

    // handles new post fab click
    private fun handleNewPostClick(hasCreatePostRights: Boolean) {
        binding.apply {
            if (hasCreatePostRights) {
                if (alreadyPosting) {
                    ViewUtils.showShortToast(
                        requireContext(),
                        getString(R.string.a_post_is_already_uploading)
                    )
                } else {
                    // sends post creation started event
                    viewModel.sendPostCreationStartedEvent()

                    val intent = CreatePostActivity.getIntent(
                        requireContext(),
                        LMAnalytics.Source.UNIVERSAL_FEED
                    )
                    createPostLauncher.launch(intent)
                }
            } else {
                ViewUtils.showShortSnack(
                    root,
                    getString(R.string.you_do_not_have_permission_to_create_a_post)
                )
            }
        }
    }

    // launcher for [CreatePostActivity]
    private val createPostLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    // post of type text/link has been created and posted
                    refreshFeed()
                }

                CreatePostActivity.RESULT_UPLOAD_POST -> {
                    // post with attachments created, now upload and post it from db
                    viewModel.fetchPendingPostFromDB()
                }
            }
        }

    // initializes universal feed recyclerview
    private fun initRecyclerView() {
        // item decorator to add spacing between items
        val dividerItemDecorator =
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        dividerItemDecorator.setDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.feed_item_divider
            ) ?: return
        )
        val linearLayoutManager = LinearLayoutManager(context)
        mPostAdapter = PostAdapter(this)
        binding.recyclerView.apply {
            layoutManager = linearLayoutManager
            adapter = mPostAdapter
            if (itemAnimator is SimpleItemAnimator)
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
            addItemDecoration(dividerItemDecorator)
            show()

            attachScrollListener(
                this,
                linearLayoutManager
            )
        }
    }

    // initializes swipe refresh layout and sets refresh listener
    private fun initSwipeRefreshLayout() {
        mSwipeRefreshLayout = binding.swipeRefreshLayout
        mSwipeRefreshLayout.setColorSchemeColors(
            LMBranding.getButtonsColor(),
        )

        mSwipeRefreshLayout.setOnRefreshListener {
            refreshFeed()
        }
    }

    //set posts through diff utils and scroll to top of the feed
    private fun setFeedAndScrollToTop(feed: List<PostViewData>) {
        mPostAdapter.replace(feed)
        binding.recyclerView.scrollToPosition(0)
        refreshAutoPlayer()
    }

    //refresh the whole feed
    private fun refreshFeed() {
        mSwipeRefreshLayout.isRefreshing = true
        mScrollListener.resetData()
        viewModel.getUnreadNotificationCount()
        viewModel.getUniversalFeed(1)
    }

    //attach scroll listener for pagination
    private fun attachScrollListener(
        recyclerView: RecyclerView,
        layoutManager: LinearLayoutManager
    ) {
        mScrollListener = object : EndlessRecyclerScrollListener(layoutManager) {
            override fun onLoadMore(currentPage: Int) {
                if (currentPage > 0) {
                    viewModel.getUniversalFeed(currentPage)
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                binding.apply {
                    val isExtended = newPostButton.isExtended

                    // Scroll down
                    if (dy > 20 && isExtended) {
                        newPostButton.shrink()
                    }

                    // Scroll up
                    if (dy < -20 && !isExtended) {
                        newPostButton.extend()
                    }

                    // At the top
                    if (!recyclerView.canScrollVertically(-1)) {
                        newPostButton.extend()
                    }
                }
            }
        }

        recyclerView.addOnScrollListener(mScrollListener)
    }

    private fun initToolbar() {
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)

        binding.apply {
            //if user is guest user hide, profile icon from toolbar
            memberImage.isVisible = !isGuestUser

            //click listener -> open profile screen
            memberImage.setOnClickListener {
                // open profile on click
            }

            ivNotification.setOnClickListener {
                NotificationFeedActivity.start(requireContext())
            }

            ivSearch.setOnClickListener {
                // perform search in feed
            }
        }
    }

    // shows invalid access error and logs out invalid user
    private fun showInvalidAccess() {
        binding.apply {
            ProgressHelper.hideProgress(progressBar)
            recyclerView.hide()
            layoutAccessRemoved.root.show()
            memberImage.hide()
            ivSearch.hide()
            ivNotification.hide()
        }
    }

    // sets user profile image
    private fun setUserImage(user: UserViewData?) {
        if (user != null) {
            MemberImageUtil.setImage(
                user.imageUrl,
                user.name,
                user.userUniqueId,
                binding.memberImage,
                showRoundImage = true,
                objectKey = user.updatedAt
            )
        }
    }

    // removes the posting view and shows create post button
    private fun removePostingView() {
        binding.apply {
            alreadyPosting = false
            layoutPosting.root.hide()
        }
    }

    /**
     * Post Actions block
     **/

    // updates [alreadySeenFullContent] for the post
    override fun updatePostSeenFullContent(position: Int, alreadySeenFullContent: Boolean) {
        // get item from adapter
        val item = mPostAdapter[position]
        if (item is PostViewData) {
            // update the post view data
            val newViewData = item.toBuilder()
                .alreadySeenFullContent(alreadySeenFullContent)
                .fromPostSaved(false)
                .fromPostLiked(false)
                .build()
            mPostAdapter.update(position, newViewData)
        }
    }

    // calls the savePost api and updates the post in adapter
    override fun savePost(position: Int) {
        //get item
        val item = mPostAdapter[position]
        if (item is PostViewData) {
            //update the post view data
            val newViewData = item.toBuilder()
                .fromPostSaved(true)
                .isSaved(!item.isSaved)
                .build()

            //call api
            postActionsViewModel.savePost(newViewData.id)

            //update recycler
            mPostAdapter.update(position, newViewData)
        }
    }

    // calls the likePost api and updates the post in adapter
    override fun likePost(position: Int) {
        //get item
        val item = mPostAdapter[position]
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

            //call api
            postActionsViewModel.likePost(newViewData.id)
            //update recycler
            mPostAdapter.update(position, newViewData)
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
                val editPostExtras = EditPostExtras.Builder()
                    .postId(postId)
                    .build()
                val intent = EditPostActivity.getIntent(requireContext(), editPostExtras)
                startActivity(intent)
            }

            DELETE_POST_MENU_ITEM_ID -> {
                deletePost(postId, postCreatorUUID)
            }

            REPORT_POST_MENU_ITEM_ID -> {
                reportPost(postId, postCreatorUUID)
            }

            PIN_POST_MENU_ITEM_ID -> {
                pinPost(postId)
            }

            UNPIN_POST_MENU_ITEM_ID -> {
                unpinPost(postId)
            }
        }
    }

    // opens likes screen when likes count is clicked.
    override fun showLikesScreen(postId: String) {
        val likesScreenExtras = LikesScreenExtras.Builder()
            .postId(postId)
            .entityType(POST)
            .build()
        LikesActivity.start(requireContext(), likesScreenExtras)
    }

    //opens post detail screen when add comment/comments count is clicked
    override fun comment(postId: String) {
        // sends comment list open event
        viewModel.sendCommentListOpenEvent()

        val postDetailExtras = PostDetailExtras.Builder()
            .postId(postId)
            .isEditTextFocused(true)
            .build()
        PostDetailActivity.start(requireContext(), postDetailExtras)
    }

    //opens post detail screen when post content is clicked
    override fun postDetail(postId: String) {
        // sends comment list open event
        viewModel.sendCommentListOpenEvent()

        val postDetailExtras = PostDetailExtras.Builder()
            .postId(postId)
            .isEditTextFocused(false)
            .build()
        PostDetailActivity.start(requireContext(), postDetailExtras)
    }

    // callback when self post is deleted by user
    override fun selfDelete(deleteExtras: DeleteExtras) {
        val post = getIndexAndPostFromAdapter(deleteExtras.postId)?.second ?: return
        postActionsViewModel.deletePost(post)
    }

    // callback when other's post is deleted by CM
    override fun adminDelete(deleteExtras: DeleteExtras, reason: String) {
        val post = getIndexAndPostFromAdapter(deleteExtras.postId)?.second ?: return
        postActionsViewModel.deletePost(post, reason)
    }

    // updates the fromPostLiked/fromPostSaved variables and updates the rv list
    override fun updateFromLikedSaved(position: Int) {
        var postData = mPostAdapter[position] as PostViewData
        postData = postData.toBuilder()
            .fromPostLiked(false)
            .fromPostSaved(false)
            .build()
        mPostAdapter.updateWithoutNotifyingRV(position, postData)
    }

    // callback when user clicks to share the post
    override fun sharePost(postId: String) {
        ShareUtils.sharePost(requireContext(), postId)
        val post = getIndexAndPostFromAdapter(postId)?.second ?: return
        postActionsViewModel.sendPostShared(post)
    }

    // processes delete post request
    private fun deletePost(postId: String, postCreatorUUID: String) {
        val deleteExtras = DeleteExtras.Builder()
            .postId(postId)
            .entityType(DELETE_TYPE_POST)
            .build()

        if (postCreatorUUID == postActionsViewModel.getUUID()) {
            // if the post was created by current user
            SelfDeleteDialogFragment.showDialog(
                childFragmentManager,
                deleteExtras
            )
        } else {
            // if the post was not created by current user and they are admin
            AdminDeleteDialogFragment.showDialog(
                childFragmentManager,
                deleteExtras
            )
        }
    }

    // Processes report action on post
    private fun reportPost(postId: String, uuid: String) {
        val post = getIndexAndPostFromAdapter(postId)?.second ?: return
        //create extras for [ReportActivity]
        val reportExtras = ReportExtras.Builder()
            .entityId(postId)
            .uuid(uuid)
            .entityType(REPORT_TYPE_POST)
            .postViewType(post.viewType)
            .build()

        //get Intent for [ReportActivity]
        val intent = ReportActivity.getIntent(requireContext(), reportExtras)

        //start [ReportActivity] and check for result
        reportPostLauncher.launch(intent)
    }

    // launcher to start [ReportActivity] and show success dialog for result
    private val reportPostLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data?.getStringExtra(ReportFragment.REPORT_RESULT)
                ReportSuccessDialog(data ?: "").show(
                    childFragmentManager,
                    ReportSuccessDialog.TAG
                )
            }
        }

    // calls the pinPost api and updates pins the post in adapter
    private fun pinPost(postId: String) {
        //get item
        val postAndIndex = getIndexAndPostFromAdapter(postId) ?: return
        val index = postAndIndex.first
        val post = postAndIndex.second

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
            pinPostMenuItem.toBuilder().id(UNPIN_POST_MENU_ITEM_ID)
                .title(getString(R.string.unpin_this_post))
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
        mPostAdapter.update(index, newViewData)
    }

    // calls the pinPost api and updates unpins the post in adapter
    private fun unpinPost(postId: String) {
        //get item
        val postAndIndex = getIndexAndPostFromAdapter(postId) ?: return
        val index = postAndIndex.first
        val post = postAndIndex.second

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
                .title(getString(R.string.pin_this_post))
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
        mPostAdapter.update(index, newViewData)
    }

    /**
     * Media Block
     **/

    /**
     * Initializes the [postVideoAutoPlayHelper] with the recyclerView
     * And starts observing
     **/
    private fun initiateAutoPlayer() {
        postVideoAutoPlayHelper = PostVideoAutoPlayHelper.getInstance(binding.recyclerView)
        postVideoAutoPlayHelper.attachScrollListenerForVideo()
        postVideoAutoPlayHelper.playMostVisibleItem()
    }

    // removes the old player and refreshes auto play
    private fun refreshAutoPlayer() {
        if (!::postVideoAutoPlayHelper.isInitialized) {
            initiateAutoPlayer()
        }
        postVideoAutoPlayHelper.removePlayer()
        postVideoAutoPlayHelper.playMostVisibleItem()
    }

    // shows all attachment documents in list view and updates [isExpanded]
    override fun onMultipleDocumentsExpanded(postData: PostViewData, position: Int) {
        if (position == mPostAdapter.items().size - 1) {
            binding.recyclerView.post {
                scrollToPositionWithOffset(position)
            }
        }

        mPostAdapter.update(
            position,
            postData.toBuilder()
                .isExpanded(true)
                .fromPostSaved(false)
                .fromPostLiked(false)
                .build()
        )
    }

    // callback when publisher publishes any updated postData
    override fun update(postData: Pair<String, PostViewData?>) {
        val postId = postData.first
        // fetches post from adapter
        val postIndex = getIndexAndPostFromAdapter(postId)?.first ?: return

        val updatedPost = postData.second

        // updates the item in adapter
        if (updatedPost == null) {
            // Post was deleted!
            mPostAdapter.removeIndex(postIndex)
        } else {
            // Post was updated
            mPostAdapter.update(postIndex, updatedPost)
        }
    }

    /**
     * Adapter Util Block
     **/

    //get index and post from the adapter using postId
    private fun getIndexAndPostFromAdapter(postId: String): Pair<Int, PostViewData>? {
        val index = mPostAdapter.items().indexOfFirst {
            (it is PostViewData) && (it.id == postId)
        }

        if (index == -1) {
            return null
        }

        val post = getPostFromAdapter(index)

        return Pair(index, post)
    }

    //get post from the adapter using index
    private fun getPostFromAdapter(position: Int): PostViewData {
        return mPostAdapter.items()[position] as PostViewData
    }

    /**
     * Scroll to a position with offset from the top header
     * @param position Index of the item to scroll to
     */
    private fun scrollToPositionWithOffset(position: Int) {
        val px = (ViewUtils.dpToPx(75) * 1.5).toInt()
        (binding.recyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
            position,
            px
        )
    }
}