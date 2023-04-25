package com.likeminds.feedsample.posttypes.view.adapter.databinder

import android.view.LayoutInflater
import android.view.ViewGroup
import com.likeminds.feedsample.databinding.ItemPostSingleImageBinding
import com.likeminds.feedsample.posttypes.model.PostViewData
import com.likeminds.feedsample.posttypes.util.PostTypeUtil
import com.likeminds.feedsample.posttypes.view.adapter.PostAdapterListener
import com.likeminds.feedsample.utils.customview.ViewDataBinder
import com.likeminds.feedsample.utils.model.ITEM_POST_SINGLE_IMAGE

class ItemPostSingleImageViewDataBinder constructor(
    val listener: PostAdapterListener
) : ViewDataBinder<ItemPostSingleImageBinding, PostViewData>() {

    override val viewType: Int
        get() = ITEM_POST_SINGLE_IMAGE

    override fun createBinder(parent: ViewGroup): ItemPostSingleImageBinding {
        return ItemPostSingleImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    }

    override fun bindData(
        binding: ItemPostSingleImageBinding,
        data: PostViewData,
        position: Int
    ) {
        // handles various actions for the post
        PostTypeUtil.initActionsLayout(
            binding.postActionsLayout,
            data,
            listener,
            position
        )

        // checks whether to bind complete data or not and execute corresponding lambda function
        PostTypeUtil.initPostTypeBindData(
            binding.authorFrame,
            binding.tvPostContent,
            data,
            position,
            listener,
            returnBinder = {
                return@initPostTypeBindData
            }, executeBinder = {
                // loads post image and attaches listener
                PostTypeUtil.initPostSingleImage(
                    binding.ivPost,
                    data,
                    listener
                )

            })
    }
}