package com.likeminds.feedsx.post.detail.view.adapter.databinder

import android.view.LayoutInflater
import android.view.ViewGroup
import com.likeminds.feedsx.R
import com.likeminds.feedsx.databinding.ItemPostDetailCommentBinding
import com.likeminds.feedsx.post.detail.view.adapter.PostDetailAdapter.PostDetailAdapterListener
import com.likeminds.feedsx.posttypes.model.CommentViewData
import com.likeminds.feedsx.utils.ViewUtils.hide
import com.likeminds.feedsx.utils.ViewUtils.show
import com.likeminds.feedsx.utils.customview.ViewDataBinder
import com.likeminds.feedsx.utils.model.ITEM_POST_DETAIL_COMMENT

class ItemPostDetailCommentViewDataBinder constructor(
    val listener: PostDetailAdapterListener
) : ViewDataBinder<ItemPostDetailCommentBinding, CommentViewData>() {

    override val viewType: Int
        get() = ITEM_POST_DETAIL_COMMENT

    override fun createBinder(parent: ViewGroup): ItemPostDetailCommentBinding {
        return ItemPostDetailCommentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    }

    override fun bindData(
        binding: ItemPostDetailCommentBinding,
        data: CommentViewData,
        position: Int
    ) {

        initCommentsView(
            binding,
            data
        )
    }

    private fun initCommentsView(
        binding: ItemPostDetailCommentBinding,
        data: CommentViewData
    ) {

        binding.apply {
            val context = root.context

            if (data.isLiked) ivLike.setImageResource(R.drawable.ic_like_comment_filled)
            else ivLike.setImageResource(R.drawable.ic_like_comment_unfilled)

            if (data.likesCount == 0) likesCount.hide()
            else {
                likesCount.text =
                    context.resources.getQuantityString(
                        R.plurals.likes,
                        data.likesCount,
                        data.likesCount
                    )
            }

            if (data.repliesCount == 0) groupReplies.hide()
            else {
                groupReplies.show()
                tvReplyCount.text = context.resources.getQuantityString(
                    R.plurals.replies,
                    data.repliesCount,
                    data.repliesCount
                )
            }

            tvReply.setOnClickListener {
                listener.replyOnComment()
            }

            tvReplyCount.setOnClickListener {
                listener.showReplies()
            }

            ivLike.setOnClickListener {
                listener.likeComment(data.id)
            }
        }
    }
}