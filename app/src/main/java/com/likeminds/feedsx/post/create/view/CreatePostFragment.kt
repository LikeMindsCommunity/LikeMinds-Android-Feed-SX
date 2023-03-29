package com.likeminds.feedsx.post.create.view

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CheckResult
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.likeminds.feedsx.R
import com.likeminds.feedsx.branding.model.BrandingData
import com.likeminds.feedsx.databinding.FragmentCreatePostBinding
import com.likeminds.feedsx.media.model.*
import com.likeminds.feedsx.media.util.MediaUtils
import com.likeminds.feedsx.media.view.MediaPickerActivity
import com.likeminds.feedsx.media.view.MediaPickerActivity.Companion.ARG_MEDIA_PICKER_RESULT
import com.likeminds.feedsx.post.create.util.CreatePostListener
import com.likeminds.feedsx.post.create.view.CreatePostActivity.Companion.POST_ATTACHMENTS_LIMIT
import com.likeminds.feedsx.post.create.view.adapter.CreatePostDocumentsAdapter
import com.likeminds.feedsx.post.create.view.adapter.CreatePostMultipleMediaAdapter
import com.likeminds.feedsx.post.create.viewmodel.CreatePostViewModel
import com.likeminds.feedsx.posttypes.model.LinkOGTagsViewData
import com.likeminds.feedsx.posttypes.model.UserViewData
import com.likeminds.feedsx.utils.AndroidUtils
import com.likeminds.feedsx.utils.MemberImageUtil
import com.likeminds.feedsx.utils.ViewDataConverter.convertSingleDataUri
import com.likeminds.feedsx.utils.ViewUtils.dpToPx
import com.likeminds.feedsx.utils.ViewUtils.getUrlIfExist
import com.likeminds.feedsx.utils.ViewUtils.hide
import com.likeminds.feedsx.utils.ViewUtils.isValidUrl
import com.likeminds.feedsx.utils.ViewUtils.show
import com.likeminds.feedsx.utils.ViewUtils.showErrorMessageToast
import com.likeminds.feedsx.utils.customview.BaseFragment
import com.likeminds.feedsx.utils.databinding.ImageBindingUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*


@AndroidEntryPoint
class CreatePostFragment :
    BaseFragment<FragmentCreatePostBinding>(),
    CreatePostListener {

    private val viewModel: CreatePostViewModel by viewModels()

    private var selectedMediaUris: ArrayList<SingleUriData> = arrayListOf()
    private var ogTags: LinkOGTagsViewData? = null

    private var multiMediaAdapter: CreatePostMultipleMediaAdapter? = null
    private var documentsAdapter: CreatePostDocumentsAdapter? = null

    override fun getViewBinding(): FragmentCreatePostBinding {
        return FragmentCreatePostBinding.inflate(layoutInflater)
    }

    override fun setUpViews() {
        super.setUpViews()

        fetchUserFromDB()
        initAddAttachmentsView()
        initPostContentTextListener()
        initPostDoneListener()
    }

    // observes data
    override fun observeData() {
        super.observeData()

        // observes error message
        observeErrors()

        // observes userData and initializes the user view
        viewModel.userData.observe(viewLifecycleOwner) {
            initAuthorFrame(it)
        }

        // observes decodeUrlResponse and returns link ogTags
        viewModel.decodeUrlResponse.observe(viewLifecycleOwner) { ogTags ->
            this.ogTags = ogTags
            initLinkView(ogTags)
        }

        // observes addPostResponse, once post is created
        viewModel.postAdded.observe(viewLifecycleOwner) { postAdded ->
            requireActivity().apply {
                if (postAdded) {
                    // post is already posted
                    setResult(Activity.RESULT_OK)
                } else {
                    // post is stored in db, now upload it from [FeedFragment]
                    setResult(CreatePostActivity.RESULT_UPLOAD_POST)
                }
                finish()
            }
        }
    }

    // observes error events
    private fun observeErrors() {
        viewModel.errorEventFlow.onEach { response ->
            when (response) {
                is CreatePostViewModel.ErrorMessageEvent.DecodeUrl -> {
                    val postText = binding.etPostContent.text.toString()
                    val link = postText.getUrlIfExist()
                    if (link != ogTags?.url) {
                        clearPreviewLink()
                    }
                }
                is CreatePostViewModel.ErrorMessageEvent.AddPost -> {
                    handlePostButton(clickable = true, showProgress = false)
                    showErrorMessageToast(requireContext(), response.errorMessage)
                }
            }
        }
    }

    private fun fetchUserFromDB() {
        viewModel.fetchUserFromDB()
    }

    // initializes click listeners on add attachment layouts
    private fun initAddAttachmentsView() {
        binding.apply {
            layoutAttachFiles.setOnClickListener {
                val extra = MediaPickerExtras.Builder()
                    .mediaTypes(listOf(PDF))
                    .allowMultipleSelect(true)
                    .build()
                val intent = MediaPickerActivity.getIntent(requireContext(), extra)
                documentLauncher.launch(intent)
            }

            layoutAddImage.setOnClickListener {
                initiateMediaPicker(listOf(IMAGE))
            }

            layoutAddVideo.setOnClickListener {
                initiateMediaPicker(listOf(VIDEO))
            }
        }
    }

    // adds text watcher on post content edit text
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun initPostContentTextListener() {
        binding.etPostContent.apply {
            textChanges()
                .debounce(500)
                .distinctUntilChanged()
                .onEach {
                    val text = it?.toString()?.trim()
                    if (text.isNullOrEmpty()) {
                        clearPreviewLink()
                        if (selectedMediaUris.isEmpty()) handlePostButton(false)
                        else handlePostButton(true)
                    } else {
                        showPostMedia()
                        handlePostButton(true)
                    }
                }
                .launchIn(lifecycleScope)
        }
    }

    // initializes post done button click listener
    private fun initPostDoneListener() {
        val createPostActivity = requireActivity() as CreatePostActivity
        createPostActivity.binding.apply {
            tvPostDone.setOnClickListener {
                val text = binding.etPostContent.text.toString()
                if (selectedMediaUris.isNotEmpty()) {
                    // when user has selected some medias to upload
                    viewModel.addPost(
                        requireContext(),
                        text,
                        selectedMediaUris,
                        ogTags
                    )
                } else {
                    // when user didn't select any media while creating post
                    handlePostButton(clickable = true, showProgress = false)
                    viewModel.addPost(
                        requireContext(),
                        text,
                        ogTags = ogTags
                    )
                }
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

    // clears link preview
    private fun clearPreviewLink() {
        ogTags = null
        binding.linkPreview.apply {
            root.hide()
        }
    }

    /**
     * Adds TextWatcher to edit text with Flow operators
     * **/
    @ExperimentalCoroutinesApi
    @CheckResult
    fun EditText.textChanges(): Flow<CharSequence?> {
        return callbackFlow<CharSequence?> {
            val listener = object : TextWatcher {
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
            addTextChangedListener(listener)
            awaitClose { removeTextChangedListener(listener) }
        }.onStart { emit(text) }
    }

    // triggers gallery launcher for (IMAGE)/(VIDEO)/(IMAGE & VIDEO)
    private fun initiateMediaPicker(list: List<String>) {
        val extras = MediaPickerExtras.Builder()
            .mediaTypes(list)
            .allowMultipleSelect(true)
            .build()

        val intent = MediaPickerActivity.getIntent(requireContext(), extras)
        galleryLauncher.launch(intent)
    }

    // process the result obtained from media picker
    private fun checkMediaPickedResult(result: MediaPickerResult?) {
        if (result != null) {
            when (result.mediaPickerResultType) {
                MEDIA_RESULT_BROWSE -> {
                    if (MediaType.isPDF(result.mediaTypes)) {
                        val intent = AndroidUtils.getExternalDocumentPickerIntent(
                            allowMultipleSelect = result.allowMultipleSelect
                        )
                        documentBrowseLauncher.launch(intent)
                    } else {
                        val intent = AndroidUtils.getExternalPickerIntent(
                            result.mediaTypes,
                            result.allowMultipleSelect,
                            result.browseClassName
                        )
                        if (intent != null)
                            mediaBrowseLauncher.launch(intent)
                    }
                }
                MEDIA_RESULT_PICKED -> {
                    onMediaPicked(result)
                }
            }
        }
    }

    // converts the picked media to SingleUriData and adds to the selected media
    private fun onMediaPicked(result: MediaPickerResult) {
        val data =
            MediaUtils.convertMediaViewDataToSingleUriData(requireContext(), result.medias)
        selectedMediaUris.addAll(data)
        showPostMedia()
    }

    private fun onMediaPickedFromGallery(data: Intent?) {
        val uris = MediaUtils.getExternalIntentPickerUris(data)
        viewModel.fetchUriDetails(requireContext(), uris) {
            val mediaUris = MediaUtils.convertMediaViewDataToSingleUriData(
                requireContext(), it
            )
            selectedMediaUris.addAll(mediaUris)
            if (mediaUris.isNotEmpty()) {
                showPostMedia()
            }
        }
    }

    private fun onPdfPicked(data: Intent?) {
        val uris = MediaUtils.getExternalIntentPickerUris(data)
        viewModel.fetchUriDetails(requireContext(), uris) {
            val mediaUris = MediaUtils.convertMediaViewDataToSingleUriData(
                requireContext(), it
            )
            selectedMediaUris.addAll(mediaUris)
            if (mediaUris.isNotEmpty()) {
                attachmentsLimitExceeded()
                showAttachedDocuments()
            }
        }
    }

    // shows toast and removes extra items if attachments limit is exceeded
    private fun attachmentsLimitExceeded() {
        if (selectedMediaUris.size > 10) {
            showErrorMessageToast(
                requireContext(), requireContext().resources.getQuantityString(
                    R.plurals.you_can_select_upto_x_items,
                    POST_ATTACHMENTS_LIMIT,
                    POST_ATTACHMENTS_LIMIT
                )
            )
            val size = selectedMediaUris.size
            selectedMediaUris.subList(POST_ATTACHMENTS_LIMIT, size).clear()
        }
    }

    // handles the logic to show the type of post
    private fun showPostMedia() {
        attachmentsLimitExceeded()
        when {
            selectedMediaUris.size >= 1 && MediaType.isPDF(selectedMediaUris.first().fileType) -> {
                ogTags = null
                showAttachedDocuments()
            }
            selectedMediaUris.size == 1 && MediaType.isImage(selectedMediaUris.first().fileType) -> {
                ogTags = null
                showAttachedImage()
            }
            selectedMediaUris.size == 1 && MediaType.isVideo(selectedMediaUris.first().fileType) -> {
                ogTags = null
                showAttachedVideo()
            }
            selectedMediaUris.size >= 1 -> {
                ogTags = null
                showMultiMediaAttachments()
            }
            else -> {
                val text = binding.etPostContent.text?.trim()
                if (selectedMediaUris.size == 0 && text != null) {
                    showLinkPreview(text.toString())
                } else {
                    clearPreviewLink()
                }
                handlePostButton(!text.isNullOrEmpty())
                handleAddAttachmentLayouts(true)
            }
        }
    }

    // shows attached video in single video post type
    private fun showAttachedVideo() {
        handleAddAttachmentLayouts(false)
        handlePostButton(true)
        binding.apply {
            singleVideoAttachment.root.show()
            singleImageAttachment.root.hide()
            linkPreview.root.hide()
            documentsAttachment.root.hide()
            multipleMediaAttachment.root.hide()
            singleVideoAttachment.btnAddMore.setOnClickListener {
                initiateMediaPicker(listOf(IMAGE, VIDEO))
            }
            singleVideoAttachment.layoutSingleVideoPost.ivCross.setOnClickListener {
                selectedMediaUris.clear()
                singleVideoAttachment.root.hide()
                handleAddAttachmentLayouts(true)
                val text = etPostContent.text?.trim()
                handlePostButton(!text.isNullOrEmpty())
            }

            //TODO: Use exo player
            singleVideoAttachment.layoutSingleVideoPost.vvSingleVideoPost.setVideoURI(
                selectedMediaUris.first().uri
            )
        }
    }

    // shows attached image in single image post type
    private fun showAttachedImage() {
        handleAddAttachmentLayouts(false)
        handlePostButton(true)
        binding.apply {
            singleImageAttachment.root.show()
            singleVideoAttachment.root.hide()
            linkPreview.root.hide()
            documentsAttachment.root.hide()
            multipleMediaAttachment.root.hide()
            singleImageAttachment.btnAddMore.setOnClickListener {
                initiateMediaPicker(listOf(IMAGE, VIDEO))
            }
            singleImageAttachment.layoutSingleImagePost.ivCross.setOnClickListener {
                selectedMediaUris.clear()
                singleImageAttachment.root.hide()
                handleAddAttachmentLayouts(true)
                val text = etPostContent.text?.trim()
                handlePostButton(!text.isNullOrEmpty())
            }

            ImageBindingUtil.loadImage(
                singleImageAttachment.layoutSingleImagePost.ivSingleImagePost,
                selectedMediaUris.first().uri,
                placeholder = R.drawable.image_placeholder
            )
        }
    }

    // shows view pager with multiple media
    private fun showMultiMediaAttachments() {
        handleAddAttachmentLayouts(false)
        handlePostButton(true)
        binding.apply {
            singleImageAttachment.root.hide()
            singleVideoAttachment.root.hide()
            linkPreview.root.hide()
            documentsAttachment.root.hide()
            multipleMediaAttachment.root.show()
            multipleMediaAttachment.btnAddMore.visibility =
                if (selectedMediaUris.size >= POST_ATTACHMENTS_LIMIT) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
            multipleMediaAttachment.btnAddMore.setOnClickListener {
                initiateMediaPicker(listOf(IMAGE, VIDEO))
            }

            val attachments = selectedMediaUris.map {
                convertSingleDataUri(it)
            }

            if (multiMediaAdapter == null) {
                multipleMediaAttachment.viewpagerMultipleMedia.isSaveEnabled = false
                multiMediaAdapter = CreatePostMultipleMediaAdapter(this@CreatePostFragment)
                multipleMediaAttachment.viewpagerMultipleMedia.adapter = multiMediaAdapter
                multipleMediaAttachment.dotsIndicator.setViewPager2(multipleMediaAttachment.viewpagerMultipleMedia)
            }
            multiMediaAdapter!!.replace(attachments)
        }
    }

    // shows document recycler view with attached files
    private fun showAttachedDocuments() {
        handleAddAttachmentLayouts(false)
        handlePostButton(true)
        binding.apply {
            singleVideoAttachment.root.hide()
            singleImageAttachment.root.hide()
            linkPreview.root.hide()
            documentsAttachment.root.show()
            multipleMediaAttachment.root.hide()
            documentsAttachment.btnAddMore.visibility =
                if (selectedMediaUris.size >= POST_ATTACHMENTS_LIMIT) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
            documentsAttachment.btnAddMore.setOnClickListener {
                initiateMediaPicker(listOf(PDF))
            }

            val attachments = selectedMediaUris.map {
                convertSingleDataUri(it)
            }

            if (documentsAdapter == null) {
                documentsAdapter = CreatePostDocumentsAdapter(this@CreatePostFragment)
                documentsAttachment.rvDocuments.apply {
                    adapter = documentsAdapter
                    layoutManager = LinearLayoutManager(context)
                }
            }
            documentsAdapter!!.replace(attachments)
        }
    }

    // shows link preview for link post type
    private fun showLinkPreview(text: String?) {
        binding.linkPreview.apply {
            clearPreviewLink()
            if (text.isNullOrEmpty()) {
                ogTags = null
                return
            }

            val link = text.getUrlIfExist()

            if (!link.isNullOrEmpty()) {
                if (link == ogTags?.url) {
                    return
                }
                viewModel.decodeUrl(link)
            } else {
                ogTags = null
                clearPreviewLink()
            }
        }
    }

    // renders data in the link view
    private fun initLinkView(data: LinkOGTagsViewData) {
        binding.linkPreview.apply {
            root.show()

            val isImageValid = (data.image != null && data.image.isValidUrl())
            ivLink.isVisible = isImageValid
            handleLinkPreviewConstraints(isImageValid)

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

            tvLinkUrl.text = data.url
            ivCross.setOnClickListener {
                this.root.hide()
            }
        }
    }

    // handles visibility of add attachment layouts
    private fun handleAddAttachmentLayouts(show: Boolean) {
        binding.groupAddAttachments.isVisible = show
    }

    // handles Post Done button click-ability
    private fun handlePostButton(
        clickable: Boolean,
        showProgress: Boolean? = null
    ) {
        val createPostActivity = requireActivity() as CreatePostActivity
        createPostActivity.binding.apply {
            if (showProgress == true || showProgress != null) {
                pbPosting.show()
                tvPostDone.hide()
            } else {
                pbPosting.hide()
                if (clickable) {
                    tvPostDone.isClickable = true
                    tvPostDone.setTextColor(BrandingData.getButtonsColor())
                } else {
                    tvPostDone.isClickable = false
                    tvPostDone.setTextColor(Color.parseColor("#666666"))
                }
            }
        }
    }

    // if image url is invalid/empty then handle link preview constraints
    private fun handleLinkPreviewConstraints(
        isImageValid: Boolean
    ) {
        binding.linkPreview.apply {
            val constraintLayout: ConstraintLayout = clLink
            val constraintSet = ConstraintSet()
            constraintSet.clone(constraintLayout)
            if (isImageValid) {
                // if image is valid then we show link image and set title constraints
                setValidLinkImageConstraints(constraintSet)
            } else {
                // if image is not valid then we don't show image and set title constraints
                setInvalidLinkImageConstraints(constraintSet)
            }
            constraintSet.applyTo(constraintLayout)
        }
    }

    // sets constraints of link preview when image is invalid
    private fun setInvalidLinkImageConstraints(constraintSet: ConstraintSet) {
        binding.linkPreview.apply {
            val margin16 = dpToPx(16)
            val margin4 = dpToPx(4)
            constraintSet.connect(
                tvLinkTitle.id,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP,
                margin16
            )
            constraintSet.connect(
                tvLinkTitle.id,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                margin16
            )
            constraintSet.connect(
                tvLinkTitle.id,
                ConstraintSet.END,
                ivCross.id,
                ConstraintSet.START,
                margin4
            )
        }
    }

    // sets constraints of link preview when image is valid
    private fun setValidLinkImageConstraints(constraintSet: ConstraintSet) {
        binding.linkPreview.apply {
            val margin = dpToPx(16)
            constraintSet.connect(
                tvLinkTitle.id,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END,
                margin
            )
            constraintSet.connect(
                tvLinkTitle.id,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                margin
            )
            constraintSet.connect(
                tvLinkTitle.id,
                ConstraintSet.TOP,
                ivLink.id,
                ConstraintSet.BOTTOM,
                margin
            )
        }
    }

    // triggered when a document/media from view pager is removed
    override fun onMediaRemoved(position: Int, mediaType: String) {
        selectedMediaUris.removeAt(position)
        if (mediaType == PDF) {
            documentsAdapter?.removeIndex(position)
            if (documentsAdapter?.itemCount == 0) binding.documentsAttachment.root.hide()
        } else multiMediaAdapter?.removeIndex(position)
        showPostMedia()
    }

    // launcher to handle gallery (IMAGE/VIDEO) intent
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data =
                    result.data?.extras?.getParcelable<MediaPickerResult>(ARG_MEDIA_PICKER_RESULT)
                checkMediaPickedResult(data)
            }
        }

    private val mediaBrowseLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                onMediaPickedFromGallery(result.data)
            }
        }

    private val documentBrowseLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                onPdfPicked(result.data)
            }
        }

    // launcher to handle document (PDF) intent
    private val documentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data =
                    result.data?.extras?.getParcelable<MediaPickerResult>(
                        ARG_MEDIA_PICKER_RESULT
                    )
                checkMediaPickedResult(data)
            }
        }
}