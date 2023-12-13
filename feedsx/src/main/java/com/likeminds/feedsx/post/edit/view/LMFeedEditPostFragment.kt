package com.likeminds.feedsx.post.edit.view

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CheckResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import androidx.viewpager2.widget.ViewPager2
import com.likeminds.feedsx.R
import com.likeminds.feedsx.SDKApplication
import com.likeminds.feedsx.branding.model.LMFeedBranding
import com.likeminds.feedsx.databinding.LmFeedFragmentEditPostBinding
import com.likeminds.feedsx.databinding.LmFeedItemMultipleMediaVideoBinding
import com.likeminds.feedsx.feed.util.PostEvent
import com.likeminds.feedsx.media.util.VideoPreviewAutoPlayHelper
import com.likeminds.feedsx.post.edit.model.LMFeedEditPostExtras
import com.likeminds.feedsx.post.edit.view.LMFeedEditPostActivity.Companion.EDIT_POST_EXTRAS
import com.likeminds.feedsx.post.edit.view.adapter.EditPostDocumentsAdapter
import com.likeminds.feedsx.post.edit.viewmodel.LMFeedEditPostViewModel
import com.likeminds.feedsx.post.edit.viewmodel.LMFeedHelperViewModel
import com.likeminds.feedsx.posttypes.model.*
import com.likeminds.feedsx.posttypes.util.PostTypeUtil
import com.likeminds.feedsx.posttypes.view.adapter.MultipleMediaPostAdapter
import com.likeminds.feedsx.posttypes.view.adapter.PostAdapterListener
import com.likeminds.feedsx.topic.model.LMFeedTopicSelectionResultExtras
import com.likeminds.feedsx.topic.model.LMFeedTopicViewData
import com.likeminds.feedsx.topic.util.LMFeedTopicChipUtil
import com.likeminds.feedsx.topic.view.LMFeedTopicSelectionActivity
import com.likeminds.feedsx.utils.*
import com.likeminds.feedsx.utils.ValueUtils.getUrlIfExist
import com.likeminds.feedsx.utils.ValueUtils.isImageValid
import com.likeminds.feedsx.utils.ValueUtils.pluralizeOrCapitalize
import com.likeminds.feedsx.utils.ViewUtils.hide
import com.likeminds.feedsx.utils.ViewUtils.show
import com.likeminds.feedsx.utils.customview.BaseFragment
import com.likeminds.feedsx.utils.customview.DataBoundViewHolder
import com.likeminds.feedsx.utils.databinding.ImageBindingUtil
import com.likeminds.feedsx.utils.link.util.LinkUtil
import com.likeminds.feedsx.utils.membertagging.model.MemberTaggingExtras
import com.likeminds.feedsx.utils.membertagging.model.UserTagViewData
import com.likeminds.feedsx.utils.membertagging.util.*
import com.likeminds.feedsx.utils.membertagging.view.LMFeedMemberTaggingView
import com.likeminds.feedsx.utils.model.*
import com.likeminds.feedsx.utils.pluralize.model.WordAction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

class LMFeedEditPostFragment :
    BaseFragment<LmFeedFragmentEditPostBinding, LMFeedEditPostViewModel>(),
    PostAdapterListener {

    @Inject
    lateinit var lmFeedHelperViewModel: LMFeedHelperViewModel

    private lateinit var editPostExtras: LMFeedEditPostExtras

    private var fileAttachments: List<AttachmentViewData>? = null
    private var ogTags: LinkOGTagsViewData? = null

    private lateinit var etPostTextChangeListener: TextWatcher

    private lateinit var memberTagging: LMFeedMemberTaggingView

    // [postPublisher] to publish changes in the post
    private val postEvent = PostEvent.getPublisher()

    private var post: PostViewData? = null

    private val videoPreviewAutoPlayHelper by lazy {
        VideoPreviewAutoPlayHelper.getInstance()
    }

    private val selectedTopic by lazy {
        HashMap<String, LMFeedTopicViewData>()
    }

    private val disabledTopics by lazy {
        HashMap<String, LMFeedTopicViewData>()
    }

    companion object {
        const val TAG = "EditPostFragment"
    }

    override val useSharedViewModel: Boolean
        get() = true

    override fun getViewModelClass(): Class<LMFeedEditPostViewModel> {
        return LMFeedEditPostViewModel::class.java
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().editPostComponent()?.inject(this)
    }

    override fun getViewBinding(): LmFeedFragmentEditPostBinding {
        return LmFeedFragmentEditPostBinding.inflate(layoutInflater)
    }

    override fun receiveExtras() {
        super.receiveExtras()
        if (arguments == null || arguments?.containsKey(EDIT_POST_EXTRAS) == false) {
            requireActivity().supportFragmentManager.popBackStack()
            return
        }
        editPostExtras = ExtrasUtil.getParcelable(
            arguments,
            EDIT_POST_EXTRAS,
            LMFeedEditPostExtras::class.java
        ) ?: throw emptyExtrasException(TAG)
    }

    override fun onResume() {
        super.onResume()
        when (post?.viewType) {
            ITEM_POST_SINGLE_VIDEO -> {
                showSingleVideoPreview()
            }

            ITEM_POST_MULTIPLE_MEDIA -> {
                showMultimediaPreview()
            }

            else -> {
                return
            }
        }
    }

    override fun onPause() {
        super.onPause()
        videoPreviewAutoPlayHelper.removePlayer()
    }

    override fun setUpViews() {
        super.setUpViews()

        setBindingVariables()
        fetchUserFromDB()
        initMemberTaggingView()
        initToolbar()
        fetchPost()
        initPostSaveListener()
    }

    override fun setPostVariable() {
        super.setPostVariable()
        val postAsVariable = lmFeedHelperViewModel.getPostVariable()

        binding.apply {
            //toolbar title
            //post header
            tvToolbarTitle.text = getString(
                R.string.edit_s,
                postAsVariable.pluralizeOrCapitalize(WordAction.FIRST_LETTER_CAPITAL_SINGULAR)
            )

        }
    }

    // sets the binding variables
    private fun setBindingVariables() {
        binding.toolbarColor = LMFeedBranding.getToolbarColor()
        binding.buttonColor = LMFeedBranding.getButtonsColor()
    }


    // fetches user data from local db
    private fun fetchUserFromDB() {
        lmFeedHelperViewModel.fetchUserFromDB()
    }

    /**
     * initializes the [memberTaggingView] with the edit text
     * also sets listener to the [memberTaggingView]
     */
    private fun initMemberTaggingView() {
        memberTagging = binding.memberTaggingView
        memberTagging.initialize(
            MemberTaggingExtras.Builder()
                .editText(binding.etPostContent)
                .maxHeightInPercentage(0.4f)
                .color(
                    LMFeedBranding.getTextLinkColor()
                )
                .build()
        )
        memberTagging.addListener(object : MemberTaggingViewListener {
            override fun onMemberTagged(user: UserTagViewData) {
                // sends user tagged event
                lmFeedHelperViewModel.sendUserTagEvent(
                    user.userUniqueId,
                    memberTagging.getTaggedMemberCount()
                )
            }

            override fun callApi(page: Int, searchName: String) {
                lmFeedHelperViewModel.getMembersForTagging(page, searchName)
            }
        })
    }

    // initializes the toolbar
    private fun initToolbar() {
        binding.apply {
            toolbarColor = LMFeedBranding.getToolbarColor()

            (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)

            ivBack.setOnClickListener {
                requireActivity().finish()
            }
        }
    }

    // fetches the post data
    private fun fetchPost() {
        ProgressHelper.showProgress(binding.progressBar)
        viewModel.getPost(editPostExtras.postId)
    }

    // initializes post done button click listener
    private fun initPostSaveListener() {
        binding.tvSave.setOnClickListener {
            savePost()
        }
    }

    //check all the necessary conditions before saving a post
    private fun savePost() {
        val text = binding.etPostContent.text
        val updatedText = memberTagging.replaceSelectedMembers(text).trim()
        val topics = selectedTopic.values

        if (selectedTopic.isNotEmpty()) {
            if (disabledTopics.isEmpty()) {
                //call api as all topics are enabled
                handleSaveButton(clickable = true, showProgress = true)
                viewModel.editPost(
                    editPostExtras.postId,
                    updatedText,
                    attachments = fileAttachments,
                    ogTags = ogTags,
                    selectedTopics = topics.toList()
                )
            } else {
                //show dialog for disabled topics
                showDisabledTopicsAlert(disabledTopics.values.toList())
            }
        } else {
            //call api as no topics are enabled
            handleSaveButton(clickable = true, showProgress = true)
            viewModel.editPost(
                editPostExtras.postId,
                updatedText,
                attachments = fileAttachments,
                ogTags = ogTags,
                selectedTopics = topics.toList()
            )
        }
    }

    //show alert for disabled topics
    private fun showDisabledTopicsAlert(disabledTopics: List<LMFeedTopicViewData>) {
        val noOfDisabledTopics = disabledTopics.size

        //create message string
        val topicNameString = disabledTopics.joinToString(", ") { it.name }
        val firstLineMessage = resources.getQuantityString(
            R.plurals.topic_disabled_message_s,
            noOfDisabledTopics,
            lmFeedHelperViewModel.getPostVariable()
                .pluralizeOrCapitalize(WordAction.ALL_SMALL_SINGULAR)
        )
        val finalMessage = "$firstLineMessage \n $topicNameString"

        //create dialog
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(
                resources.getQuantityString(
                    R.plurals.topic_disabled,
                    noOfDisabledTopics,
                    noOfDisabledTopics
                )
            )
            .setMessage(finalMessage)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .show()

        //set positive button color
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton?.apply {
            isAllCaps = true
            setTextColor(LMFeedBranding.getButtonsColor())
        }
    }

    // adds text watcher on post content edit text
    @SuppressLint("ClickableViewAccessibility")
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun initPostContentTextListener() {
        binding.etPostContent.apply {
            /**
             * As the scrollable edit text is inside a scroll view,
             * this touch listener handles the scrolling of the edit text.
             * When the edit text is touched and has focus then it disables scroll of scroll-view.
             */
            setOnTouchListener(View.OnTouchListener { v, event ->
                if (hasFocus()) {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                    when (event.action and MotionEvent.ACTION_MASK) {
                        MotionEvent.ACTION_SCROLL -> {
                            v.parent.requestDisallowInterceptTouchEvent(false)
                            return@OnTouchListener true
                        }
                    }
                }
                false
            })

            if (fileAttachments == null) {
                // text watcher with debounce to add delay in api calls for ogTags
                textChanges()
                    .debounce(500)
                    .distinctUntilChanged()
                    .onEach {
                        val text = it?.toString()?.trim()
                        if (!text.isNullOrEmpty()) {
                            showLinkPreview(text)
                        }
                    }
                    .launchIn(lifecycleScope)
            }

            // text watcher to handleSaveButton click-ability
            addTextChangedListener {
                val text = it?.toString()?.trim()
                if (text.isNullOrEmpty()) {
                    clearPreviewLink()
                    if (fileAttachments != null) {
                        handleSaveButton(clickable = true)
                    } else {
                        handleSaveButton(clickable = false)
                    }
                } else {
                    handleSaveButton(clickable = true)
                }
            }
        }
    }

    // handles Save Done button click-ability
    private fun handleSaveButton(
        clickable: Boolean,
        showProgress: Boolean? = null
    ) {
        binding.apply {
            if (showProgress == true) {
                pbSaving.show()
                tvSave.hide()
            } else {
                pbSaving.hide()
                if (clickable) {
                    tvSave.isClickable = true
                    tvSave.setTextColor(LMFeedBranding.getButtonsColor())
                } else {
                    tvSave.isClickable = false
                    tvSave.setTextColor(Color.parseColor("#666666"))
                }
            }
        }
    }

    //handles topics chip group and separator line
    private fun handleTopicSelectionView(showView: Boolean) {
        binding.apply {
            cgTopics.isVisible = showView
            topicSeparator.isVisible = showView
        }
    }

    /**
     * Adds TextWatcher to edit text with Flow operators
     * **/
    @ExperimentalCoroutinesApi
    @CheckResult
    fun EditText.textChanges(): Flow<CharSequence?> {
        return callbackFlow<CharSequence?> {
            etPostTextChangeListener = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) = Unit
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    (this@callbackFlow).trySend(s.toString())
                }
            }
            addTextChangedListener(etPostTextChangeListener)
            awaitClose { removeTextChangedListener(etPostTextChangeListener) }
        }.onStart { emit(text) }
    }

    // shows link preview for link post type
    private fun showLinkPreview(text: String?) {
        binding.linkPreview.apply {
            if (text.isNullOrEmpty()) {
                clearPreviewLink()
                return
            }
            val link = text.getUrlIfExist()
            if (ogTags != null && link.equals(ogTags?.url)) {
                return
            }
            if (!link.isNullOrEmpty()) {
                if (link == ogTags?.url) {
                    return
                }
                clearPreviewLink()
                lmFeedHelperViewModel.decodeUrl(link)
            } else {
                clearPreviewLink()
            }
        }
    }

    // clears link preview
    private fun clearPreviewLink() {
        ogTags = null
        binding.linkPreview.apply {
            root.hide()
        }
    }

    override fun observeData() {
        super.observeData()

        // observes error message
        observeErrors()
        observeMembersTaggingList()

        // observes userData and initializes the user view
        lmFeedHelperViewModel.userData.observe(viewLifecycleOwner) {
            initAuthorFrame(it)
        }

        lmFeedHelperViewModel.showTopicFilter.observe(viewLifecycleOwner) { showTopics ->
            if (showTopics) {
                handleTopicSelectionView(true)
                initTopicSelectionView()
            } else {
                handleTopicSelectionView(false)
            }
        }

        // observes postResponse live data
        viewModel.postDataEventFlow.onEach { response ->
            when (response) {
                is LMFeedEditPostViewModel.PostDataEvent.EditPost -> {
                    // updated post from editPost response

                    val post = response.post

                    // notifies the subscribers about the change in post data
                    postEvent.notify(Pair(post.id, post))

                    requireActivity().apply {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }

                is LMFeedEditPostViewModel.PostDataEvent.GetPost -> {
                    // post from getPost response
                    setPostData(response.post)
                    post = response.post
                }
            }
        }.observeInLifecycle(viewLifecycleOwner)

        // observes decodeUrlResponse and returns link ogTags
        lmFeedHelperViewModel.decodeUrlResponse.observe(viewLifecycleOwner) { ogTags ->
            this.ogTags = ogTags
            initLinkView()
        }
    }

    // observes error events
    private fun observeErrors() {
        viewModel.errorEventFlow.onEach { response ->
            when (response) {
                is LMFeedEditPostViewModel.ErrorMessageEvent.EditPost -> {
                    handleSaveButton(clickable = true, showProgress = false)
                    ViewUtils.showErrorMessageToast(requireContext(), response.errorMessage)
                }

                is LMFeedEditPostViewModel.ErrorMessageEvent.GetPost -> {
                    requireActivity().apply {
                        ViewUtils.showErrorMessageToast(this, response.errorMessage)
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                }
            }
        }.observeInLifecycle(viewLifecycleOwner)

        lmFeedHelperViewModel.errorEventFlow.onEach { response ->
            when (response) {
                is LMFeedHelperViewModel.ErrorMessageEvent.DecodeUrl -> {
                    val postText = binding.etPostContent.text.toString()
                    val link = postText.getUrlIfExist()
                    if (link != ogTags?.url) {
                        clearPreviewLink()
                    }
                }

                is LMFeedHelperViewModel.ErrorMessageEvent.GetTaggingList -> {
                    ViewUtils.showErrorMessageToast(requireContext(), response.errorMessage)
                }

                is LMFeedHelperViewModel.ErrorMessageEvent.GetTopic -> {

                }
            }
        }.observeInLifecycle(viewLifecycleOwner)
    }

    /**
     * Observes for member tagging list, This is a live observer which will update itself on addition of new members
     * [taggingData] contains first -> page called in api
     * second -> Community Members and Groups
     */
    private fun observeMembersTaggingList() {
        lmFeedHelperViewModel.taggingData.observe(viewLifecycleOwner) { result ->
            MemberTaggingUtil.setMembersInView(memberTagging, result)
        }
    }

    // sets the post data in view
    private fun setPostData(post: PostViewData) {
        binding.apply {
            val attachments = post.attachments
            val topics = post.topics
            ProgressHelper.hideProgress(progressBar)
            nestedScroll.show()

            // decodes the post text and sets to the edit text
            MemberTaggingDecoder.decode(
                etPostContent,
                post.text,
                LMFeedBranding.getTextLinkColor()
            )

            // sets the cursor to the end and opens keyboard
            etPostContent.setSelection(etPostContent.length())
            etPostContent.focusAndShowKeyboard()

            when (post.viewType) {
                ITEM_POST_SINGLE_IMAGE -> {
                    fileAttachments = attachments
                    showSingleImagePreview()
                }

                ITEM_POST_SINGLE_VIDEO -> {
                    fileAttachments = attachments
                    showSingleVideoPreview()
                }

                ITEM_POST_DOCUMENTS -> {
                    fileAttachments = attachments
                    showDocumentsPreview()
                }

                ITEM_POST_MULTIPLE_MEDIA -> {
                    fileAttachments = attachments
                    showMultimediaPreview()
                }

                ITEM_POST_LINK -> {
                    ogTags = attachments.first().attachmentMeta.ogTags
                    initLinkView()
                }

                else -> {
                    Log.e(SDKApplication.LOG_TAG, "invalid view type")

                }
            }

            if (topics.isNotEmpty()) {
                handleTopicSelectionView(true)

                selectedTopic.clear()
                disabledTopics.clear()

                //filter disabled topics
                topics.forEach { topic ->
                    if (!topic.isEnabled) {
                        disabledTopics[topic.id] = topic
                    }
                }

                addTopicsToGroup(false, topics)
            } else {
                lmFeedHelperViewModel.getAllTopics(true)
            }

            initPostContentTextListener()
        }
    }

    // shows single image preview
    private fun showSingleImagePreview() {
        handleSaveButton(clickable = true)
        val attachmentUrl = fileAttachments?.first()?.attachmentMeta?.url ?: return
        // gets the shimmer drawable for placeholder
        val shimmerDrawable = ViewUtils.getShimmer()
        binding.apply {
            singleImageAttachment.root.show()
            ImageBindingUtil.loadImage(
                singleImageAttachment.ivSingleImagePost,
                attachmentUrl,
                placeholder = shimmerDrawable
            )
        }
    }

    // shows single video preview
    private fun showSingleVideoPreview() {
        val videoAttachment = fileAttachments?.first()
        binding.singleVideoAttachment.apply {
            root.show()
            val meta = videoAttachment?.attachmentMeta
            videoPreviewAutoPlayHelper.playVideo(
                videoPost,
                pbVideoLoader,
                url = meta?.url
            )
        }
    }

    // shows documents preview
    private fun showDocumentsPreview() {
        binding.apply {
            handleSaveButton(clickable = true)
            documentsAttachment.root.show()
            val mDocumentsAdapter = EditPostDocumentsAdapter()
            // item decorator to add spacing between items
            val dividerItemDecorator =
                DividerItemDecoration(root.context, DividerItemDecoration.VERTICAL)
            dividerItemDecorator.setDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.document_item_divider
                ) ?: return
            )
            documentsAttachment.rvDocuments.apply {
                adapter = mDocumentsAdapter
                layoutManager = LinearLayoutManager(root.context)
                // if separator is not there already, then only add
                if (itemDecorationCount < 1) {
                    addItemDecoration(dividerItemDecorator)
                }
            }

            val documents = fileAttachments ?: return

            if (documents.size <= PostTypeUtil.SHOW_MORE_COUNT) {
                documentsAttachment.tvShowMore.hide()
                mDocumentsAdapter.replace(documents)
            } else {
                documentsAttachment.tvShowMore.show()
                "+${documents.size - PostTypeUtil.SHOW_MORE_COUNT} more".also {
                    documentsAttachment.tvShowMore.text = it
                }
                mDocumentsAdapter.replace(documents.take(PostTypeUtil.SHOW_MORE_COUNT))
            }

            documentsAttachment.tvShowMore.setOnClickListener {
                documentsAttachment.tvShowMore.hide()
                mDocumentsAdapter.replace(documents)
            }
        }
    }

    // shows multimedia preview
    private fun showMultimediaPreview() {
        handleSaveButton(clickable = true)
        binding.apply {
            multipleMediaAttachment.root.show()
            multipleMediaAttachment.buttonColor = LMFeedBranding.getButtonsColor()
            val multiMediaAdapter = MultipleMediaPostAdapter(this@LMFeedEditPostFragment)
            val viewPager = multipleMediaAttachment.viewpagerMultipleMedia
            viewPager.adapter = multiMediaAdapter
            multipleMediaAttachment.dotsIndicator.setViewPager2(multipleMediaAttachment.viewpagerMultipleMedia)
            val attachments = fileAttachments?.map {
                when (it.attachmentType) {
                    IMAGE -> {
                        it.toBuilder().dynamicViewType(ITEM_MULTIPLE_MEDIA_IMAGE).build()
                    }

                    VIDEO -> {
                        it.toBuilder().dynamicViewType(ITEM_MULTIPLE_MEDIA_VIDEO).build()
                    }

                    else -> {
                        it
                    }
                }
            } ?: return
            multiMediaAdapter.replace(attachments)
            viewPager.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)

                    // processes the current video whenever view pager's page is changed
                    val itemMultipleMediaVideoBinding =
                        ((viewPager[0] as RecyclerView).findViewHolderForAdapterPosition(position) as? DataBoundViewHolder<*>)
                            ?.binding as? LmFeedItemMultipleMediaVideoBinding

                    if (itemMultipleMediaVideoBinding == null) {
                        // in case the item is not a video
                        videoPreviewAutoPlayHelper.removePlayer()
                    } else {
                        // processes the current video item
                        val meta = attachments[position].attachmentMeta
                        videoPreviewAutoPlayHelper.playVideo(
                            itemMultipleMediaVideoBinding.videoPost,
                            itemMultipleMediaVideoBinding.pbVideoLoader,
                            url = meta.url
                        )
                    }
                }
            })
        }
    }

    // renders data in the link view
    private fun initLinkView() {
        val data = ogTags ?: return
        val link = data.url ?: ""
        // sends link attached event with the link
        lmFeedHelperViewModel.sendLinkAttachedEvent(link)
        binding.linkPreview.apply {
            root.show()

            val isImageValid = data.image.isImageValid()
            ivLink.isVisible = isImageValid
            LinkUtil.handleLinkPreviewConstraints(this, isImageValid)

            tvLinkTitle.text = if (data.title?.isNotBlank() == true) {
                data.title
            } else {
                root.context.getString(R.string.link)
            }
            tvLinkDescription.isVisible = !data.description.isNullOrEmpty()
            tvLinkDescription.text = data.description

            if (isImageValid) {
                ImageBindingUtil.loadImage(
                    ivLink,
                    data.image,
                    placeholder = R.drawable.ic_link_primary_40dp,
                    cornerRadius = 8
                )
            }

            tvLinkUrl.text = data.url?.lowercase(Locale.getDefault()) ?: ""
            ivCrossLink.setOnClickListener {
                binding.etPostContent.removeTextChangedListener(etPostTextChangeListener)
                clearPreviewLink()
            }
        }
    }

    // sets data to the author frame
    private fun initAuthorFrame(user: UserViewData) {
        binding.authorFrame.apply {
            tvCreatorName.text = user.name
            MemberImageUtil.setImage(
                user.imageUrl,
                user.name,
                user.userUniqueId,
                creatorImage,
                showRoundImage = true,
                objectKey = user.updatedAt
            )
        }
    }

    //init initial topic selection view with "Select Topic Chip"
    private fun initTopicSelectionView() {
        binding.cgTopics.apply {
            removeAllViews()
            addView(LMFeedTopicChipUtil.createSelectTopicsChip(requireContext(), this) { intent ->
                topicSelectionLauncher.launch(intent)
            })
        }
    }

    //start activity -> Topic Selection and check for result with selected topics
    private val topicSelectionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val bundle = result.data?.extras
                val resultExtras = ExtrasUtil.getParcelable(
                    bundle,
                    LMFeedTopicSelectionActivity.TOPIC_SELECTION_RESULT_EXTRAS,
                    LMFeedTopicSelectionResultExtras::class.java
                ) ?: return@registerForActivityResult

                val selectedTopics = resultExtras.selectedTopics
                if (selectedTopics.isNotEmpty()) {
                    addTopicsToGroup(true, selectedTopics)
                }
            }
        }

    //add selected topics to group and add edit chip as well in the end
    private fun addTopicsToGroup(
        isAfterSelection: Boolean,
        newSelectedTopics: List<LMFeedTopicViewData>
    ) {
        if (isAfterSelection) {
            disabledTopics.clear()
            selectedTopic.clear()
        }

        newSelectedTopics.forEach { topic ->
            if (!topic.isEnabled) {
                disabledTopics[topic.id] = topic
            }
            selectedTopic[topic.id] = topic
        }

        val selectedTopics = selectedTopic.values.toList()

        binding.cgTopics.apply {
            removeAllViews()
            selectedTopics.forEach { topic ->
                addView(LMFeedTopicChipUtil.createTopicChip(this, topic.name))
            }
            addView(
                LMFeedTopicChipUtil.createEditChip(
                    requireContext(),
                    selectedTopics,
                    this,
                    disabledTopics.values.toList()
                ) { intent ->
                    topicSelectionLauncher.launch(intent)
                })
        }
    }
}