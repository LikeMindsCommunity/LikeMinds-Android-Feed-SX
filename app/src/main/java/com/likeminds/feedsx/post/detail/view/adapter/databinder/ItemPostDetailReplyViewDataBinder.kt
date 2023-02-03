package com.likeminds.feedsx.post.detail.view.adapter.databinder

import android.view.LayoutInflater
import android.view.ViewGroup
import com.likeminds.feedsx.R
import com.likeminds.feedsx.databinding.ItemPostDetailReplyBinding
import com.likeminds.feedsx.post.detail.view.adapter.PostDetailAdapter.PostDetailAdapterListener
import com.likeminds.feedsx.posttypes.model.CommentViewData
import com.likeminds.feedsx.utils.ViewUtils.hide
import com.likeminds.feedsx.utils.customview.ViewDataBinder
import com.likeminds.feedsx.utils.model.ITEM_POST_DETAIL_REPLY

class ItemPostDetailReplyViewDataBinder constructor(
    val listener: PostDetailAdapterListener
) : ViewDataBinder<ItemPostDetailReplyBinding, CommentViewData>() {
    override val viewType: Int
        get() = ITEM_POST_DETAIL_REPLY

    override fun createBinder(parent: ViewGroup): ItemPostDetailReplyBinding {
        return ItemPostDetailReplyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    }

    override fun bindData(
        binding: ItemPostDetailReplyBinding,
        data: CommentViewData,
        position: Int
    ) {

        initReplyView(
            binding,
            data
        )
    }

    private fun initReplyView(
        binding: ItemPostDetailReplyBinding,
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

            ivLike.setOnClickListener {
                listener.likeComment(data.id)
            }
        }
    }
}