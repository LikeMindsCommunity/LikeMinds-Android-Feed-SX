package com.likeminds.feedsx.posttypes.util

import android.net.Uri
import android.text.*
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.likeminds.feedsx.R
import com.likeminds.feedsx.branding.model.BrandingData
import com.likeminds.feedsx.databinding.*
import com.likeminds.feedsx.media.util.MediaUtils
import com.likeminds.feedsx.overflowmenu.model.OverflowMenuItemViewData
import com.likeminds.feedsx.overflowmenu.view.OverflowMenuPopup
import com.likeminds.feedsx.posttypes.model.*
import com.likeminds.feedsx.posttypes.view.adapter.DocumentsPostAdapter
import com.likeminds.feedsx.posttypes.view.adapter.MultipleMediaPostAdapter
import com.likeminds.feedsx.posttypes.view.adapter.PostAdapter.PostAdapterListener
import com.likeminds.feedsx.utils.*
import com.likeminds.feedsx.utils.ValueUtils.getValidTextForLinkify
import com.likeminds.feedsx.utils.ValueUtils.isValidYoutubeLink
import com.likeminds.feedsx.utils.ViewUtils.hide
import com.likeminds.feedsx.utils.ViewUtils.show
import com.likeminds.feedsx.utils.databinding.ImageBindingUtil
import com.likeminds.feedsx.utils.link.CustomLinkMovementMethod
import com.likeminds.feedsx.utils.membertagging.util.MemberTaggingDecoder
import com.likeminds.feedsx.utils.model.ITEM_MULTIPLE_MEDIA_IMAGE
import com.likeminds.feedsx.utils.model.ITEM_MULTIPLE_MEDIA_VIDEO

object PostTypeUtil {

    private const val TAG = "PostTypeUtil"
    private const val SHOW_MORE_COUNT = 2

    // initializes author data frame on the post
    private fun initAuthorFrame(
        binding: LayoutAuthorFrameBinding,
        data: PostViewData,
        overflowMenu: OverflowMenuPopup
    ) {
        //TODO: Change pin filled drawable
        if (data.isPinned) binding.ivPin.show()
        else binding.ivPin.hide()

        binding.ivPostMenu.setOnClickListener {
            showOverflowMenu(binding.ivPostMenu, overflowMenu)
        }

        // creator data
        val user = data.user
        binding.tvMemberName.text = user.name
        binding.tvCustomTitle.text = user.customTitle
        MemberImageUtil.setImage(
            user.imageUrl,
            user.name,
            data.id,
            binding.memberImage,
            showRoundImage = true
        )

        binding.viewDotEdited.hide()
        binding.tvEdited.hide()
        binding.tvTime.text = TimeUtil.getDaysHoursOrMinutes(data.createdAt)
    }

    //to show the overflow menu
    fun showOverflowMenu(ivMenu: ImageView, overflowMenu: OverflowMenuPopup) {
        overflowMenu.showAsDropDown(
            ivMenu,
            -ViewUtils.dpToPx(16),
            -ivMenu.height / 2,
            Gravity.START
        )
    }

    // initializes the recyclerview with attached documents
    fun initDocumentsRecyclerView(
        binding: ItemPostDocumentsBinding,
        postData: PostViewData,
        postAdapterListener: PostAdapterListener,
        position: Int
    ) {
        val mDocumentsAdapter = DocumentsPostAdapter(postAdapterListener)
        binding.rvDocuments.apply {
            adapter = mDocumentsAdapter
            layoutManager = LinearLayoutManager(binding.root.context)
        }

        val documents = postData.attachments

        if (postData.isExpanded || documents.size <= SHOW_MORE_COUNT) {
            binding.tvShowMore.hide()
            mDocumentsAdapter.replace(postData.attachments)
        } else {
            binding.tvShowMore.show()
            "+${documents.size - SHOW_MORE_COUNT} more".also { binding.tvShowMore.text = it }
            mDocumentsAdapter.replace(documents.take(SHOW_MORE_COUNT))
        }

        binding.tvShowMore.setOnClickListener {
            postAdapterListener.onMultipleDocumentsExpanded(postData, position)
        }
    }

    // initializes document item of the document recyclerview
    fun initDocument(
        binding: ItemDocumentBinding,
        document: AttachmentViewData,
    ) {
        binding.apply {
            tvMeta1.hide()
            viewMetaDot1.hide()
            tvMeta2.hide()
            viewMetaDot2.hide()
            tvMeta3.hide()

            val attachmentMeta = document.attachmentMeta
            val context = root.context

            tvDocumentName.text =
                attachmentMeta.name ?: context.getString(R.string.documents)

            val noOfPage = attachmentMeta.pageCount ?: 0
            val mediaType = attachmentMeta.format
            if (noOfPage > 0) {
                tvMeta1.show()
                tvMeta1.text = context.getString(
                    R.string.placeholder_pages, noOfPage
                )
            }
            if (attachmentMeta.size != null) {
                tvMeta2.show()
                tvMeta2.text = MediaUtils.getFileSizeText(attachmentMeta.size)
                if (tvMeta1.isVisible) {
                    viewMetaDot1.show()
                }
            }
            if (!mediaType.isNullOrEmpty() && (tvMeta1.isVisible || tvMeta2.isVisible)) {
                tvMeta3.show()
                tvMeta3.text = mediaType
                viewMetaDot2.show()
            }
            root.setOnClickListener {
                val pdfUri = Uri.parse(document.attachmentMeta.url ?: "")
                AndroidUtils.startDocumentViewer(root.context, pdfUri)
            }
        }
    }


    // initializes various actions on the post
    fun initActionsLayout(
        binding: LayoutPostActionsBinding,
        data: PostViewData,
        listener: PostAdapterListener,
        position: Int
    ) {

        val context = binding.root.context

        if (data.isLiked) binding.ivLike.setImageResource(R.drawable.ic_like_filled)
        else binding.ivLike.setImageResource(R.drawable.ic_like_unfilled)

        if (data.isSaved) binding.ivBookmark.setImageResource(R.drawable.ic_bookmark_filled)
        else binding.ivBookmark.setImageResource(R.drawable.ic_bookmark_unfilled)

        // bounce animation for like and save button
        val bounceAnim: Animation by lazy {
            AnimationUtils.loadAnimation(
                context,
                R.anim.bounce
            )
        }

        binding.likesCount.text =
            if (data.likesCount == 0) context.getString(R.string.like)
            else
                context.resources.getQuantityString(
                    R.plurals.likes,
                    data.likesCount,
                    data.likesCount
                )

        binding.likesCount.setOnClickListener {
            listener.showLikesScreen(data.id)
        }

        binding.commentsCount.text =
            if (data.commentsCount == 0) context.getString(R.string.add_comment)
            else
                context.resources.getQuantityString(
                    R.plurals.comments,
                    data.commentsCount,
                    data.commentsCount
                )

        binding.ivLike.setOnClickListener {
            bounceAnim.interpolator = LikeMindsBounceInterpolator(0.2, 20.0)
            it.startAnimation(bounceAnim)
            listener.likePost(position)
        }

        binding.ivBookmark.setOnClickListener {
            bounceAnim.interpolator = LikeMindsBounceInterpolator(0.2, 20.0)
            it.startAnimation(bounceAnim)
            listener.savePost(position)
        }

        binding.ivShare.setOnClickListener {
            listener.sharePost()
        }

        binding.ivComment.setOnClickListener {
            listener.comment(data.id)
        }

        binding.commentsCount.setOnClickListener {
            listener.comment(data.id)
        }
    }

    // initializes view pager for multiple media post
    fun initViewPager(binding: ItemPostMultipleMediaBinding, data: PostViewData) {
        val attachments = data.attachments.map {
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
        }
        binding.viewpagerMultipleMedia.isSaveEnabled = false
        val multipleMediaPostAdapter = MultipleMediaPostAdapter()
        binding.viewpagerMultipleMedia.adapter = multipleMediaPostAdapter
        binding.dotsIndicator.setViewPager2(binding.viewpagerMultipleMedia)
        multipleMediaPostAdapter.replace(attachments)
    }

    // handles the text content of each post
    private fun initTextContent(
        tvPostContent: TextView,
        data: PostViewData,
        itemPosition: Int,
        adapterListener: PostAdapterListener
    ) {
        val context = tvPostContent.context
        val textForLinkify = data.text.getValidTextForLinkify()

        var alreadySeenFullContent = data.alreadySeenFullContent == true

        if (textForLinkify.isEmpty()) {
            tvPostContent.hide()
            return
        } else {
            tvPostContent.show()
        }

        val seeMoreColor = ContextCompat.getColor(tvPostContent.context, R.color.brown_grey)
        val seeMore = SpannableStringBuilder(context.getString(R.string.see_more))
        seeMore.setSpan(
            ForegroundColorSpan(seeMoreColor),
            0,
            seeMore.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        val seeMoreClickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                alreadySeenFullContent = true
                adapterListener.updateSeenFullContent(itemPosition, true)
            }

            override fun updateDrawState(textPaint: TextPaint) {
                textPaint.isUnderlineText = false
            }
        }

        val seeLessColor = ContextCompat.getColor(tvPostContent.context, R.color.brown_grey)
        val seeLess = SpannableStringBuilder(context.getString(R.string.see_less))
        seeLess.setSpan(
            ForegroundColorSpan(seeLessColor),
            0,
            seeLess.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        val seeLessClickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                alreadySeenFullContent = false
                adapterListener.updateSeenFullContent(itemPosition, false)
            }

            override fun updateDrawState(textPaint: TextPaint) {
                textPaint.isUnderlineText = false
            }
        }

        val postTextClickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                adapterListener.postDetail(data)
            }

            override fun updateDrawState(textPaint: TextPaint) {
                textPaint.isUnderlineText = false
            }
        }

        // post is used here to get lines count in the text view
        tvPostContent.post {
            val shortText: String? = SeeMoreUtil.getShortContent(
                data.text,
                tvPostContent,
                3,
                500
            )

            val trimmedText =
                if (!alreadySeenFullContent && !shortText.isNullOrEmpty()) {
                    shortText
                } else {
                    textForLinkify
                }

            // TODO: remove branding
            MemberTaggingDecoder.decode(
                tvPostContent,
                trimmedText,
                enableClick = true,
                BrandingData.currentAdvanced?.third ?: ContextCompat.getColor(
                    tvPostContent.context,
                    R.color.pure_blue
                )
            ) { tag ->
                onMemberTagClicked()
            }

            val seeMoreSpannableStringBuilder = SpannableStringBuilder()
            if (!alreadySeenFullContent && !shortText.isNullOrEmpty()) {
                seeMoreSpannableStringBuilder.append("...")
                seeMoreSpannableStringBuilder.append(seeMore)
                seeMoreSpannableStringBuilder.setSpan(
                    seeMoreClickableSpan,
                    3,
                    seeMore.length + 3,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            val seeLessSpannableStringBuilder = SpannableStringBuilder()
            if (alreadySeenFullContent && !shortText.isNullOrEmpty()) {
                seeLessSpannableStringBuilder.append(seeLess)
                seeLessSpannableStringBuilder.setSpan(
                    seeLessClickableSpan,
                    0,
                    seeLess.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            val postTextSpannableStringBuilder = SpannableStringBuilder()
            postTextSpannableStringBuilder.append(trimmedText)
            postTextSpannableStringBuilder.setSpan(
                postTextClickableSpan,
                0,
                trimmedText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            tvPostContent.movementMethod = CustomLinkMovementMethod {
                //TODO: Handle links etc.
                true
            }

            tvPostContent.text = TextUtils.concat(
                tvPostContent.text,
                seeMoreSpannableStringBuilder,
                seeLessSpannableStringBuilder
            )
        }
    }

    fun initPostSingleImage(
        ivPost: ImageView,
        data: PostViewData,
        adapterListener: PostAdapterListener
    ) {

        ImageBindingUtil.loadImage(
            ivPost,
            data.attachments.first().attachmentMeta.url,
            placeholder = R.drawable.image_placeholder
        )

        ivPost.setOnClickListener {
            adapterListener.postDetail(data)
        }
    }

    // sets image in the multiple media image view
    fun initMultipleMediaImage(
        ivPost: ImageView,
        data: AttachmentViewData
    ) {
        ImageBindingUtil.loadImage(
            ivPost,
            data.attachmentMeta.url,
            placeholder = R.drawable.image_placeholder
        )
    }

    // handles link view in the post
    fun initLinkView(
        binding: ItemPostLinkBinding,
        data: LinkOGTagsViewData
    ) {
        val isYoutubeLink = data.url?.isValidYoutubeLink() == true
        binding.tvLinkTitle.text = if (data.title?.isNotBlank() == true) {
            data.title
        } else {
            binding.root.context.getString(R.string.link)
        }
        binding.tvLinkDescription.isVisible = !data.description.isNullOrEmpty()
        binding.tvLinkDescription.text = data.description

        if (isYoutubeLink) {
            binding.ivLink.hide()
            binding.ivPlay.isVisible = !data.image.isNullOrEmpty()
            binding.ivYoutubeLink.isVisible = !data.image.isNullOrEmpty()
            binding.ivYoutubeLogo.isVisible = !data.image.isNullOrEmpty()
        } else {
            binding.ivPlay.hide()
            binding.ivYoutubeLink.hide()
            binding.ivYoutubeLogo.hide()
            binding.ivLink.isVisible = !data.image.isNullOrEmpty()
        }

        ImageBindingUtil.loadImage(
            if (isYoutubeLink) binding.ivYoutubeLink else binding.ivLink,
            data.image,
            placeholder = R.drawable.ic_link_primary_40dp,
            cornerRadius = 8,
            isBlur = isYoutubeLink
        )

        binding.tvLinkUrl.text = data.url
    }

    // sets the items in overflow menu
    fun setOverflowMenuItems(
        overflowMenu: OverflowMenuPopup,
        menuItems: List<OverflowMenuItemViewData>
    ) {
        overflowMenu.setItems(menuItems)
    }

    // performs action when member tag is clicked
    private fun onMemberTagClicked() {
        // TODO: Change Implementation
    }

    // checks if binder is called from liking/saving post or not
    fun initPostTypeBindData(
        authorFrame: LayoutAuthorFrameBinding,
        overflowMenu: OverflowMenuPopup,
        tvPostContent: TextView,
        data: PostViewData,
        position: Int,
        listener: PostAdapterListener,
        returnBinder: () -> Unit,
        executeBinder: () -> Unit
    ) {
        if (data.fromPostLiked || data.fromPostSaved) {
            // update fromLiked/fromSaved variables and return from binder
            listener.updateFromLikedSaved(position)
            returnBinder()
        } else {
            // call all the common functions

            // sets items to overflow menu
            setOverflowMenuItems(
                overflowMenu,
                data.menuItems
            )

            // sets data to the creator frame
            initAuthorFrame(
                authorFrame,
                data,
                overflowMenu
            )

            // sets the text content of the post
            initTextContent(
                tvPostContent,
                data,
                itemPosition = position,
                listener
            )
            executeBinder()
        }
    }
}