package com.likeminds.feedsx.feed.view.adapter.databinder

import android.view.LayoutInflater
import android.view.ViewGroup
import com.likeminds.feedsx.databinding.ItemLikesScreenBinding
import com.likeminds.feedsx.feed.view.model.LikeViewData
import com.likeminds.feedsx.utils.MemberImageUtil
import com.likeminds.feedsx.utils.ViewUtils.show
import com.likeminds.feedsx.utils.customview.ViewDataBinder
import com.likeminds.feedsx.utils.model.ITEM_LIKES_SCREEN

class ItemLikesScreenViewDataBinder : ViewDataBinder<ItemLikesScreenBinding, LikeViewData>() {

    override val viewType: Int
        get() = ITEM_LIKES_SCREEN

    override fun createBinder(parent: ViewGroup): ItemLikesScreenBinding {
        return ItemLikesScreenBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun bindData(binding: ItemLikesScreenBinding, data: LikeViewData, position: Int) {
        initLikeItem(binding, data)
    }

    private fun initLikeItem(
        binding: ItemLikesScreenBinding,
        data: LikeViewData
    ) {
        val user = data.user
        MemberImageUtil.setImage(
            user.imageUrl,
            user.name,
            data.id,
            binding.memberImage,
            showRoundImage = true
        )

        binding.tvMemberName.text = user.name
        if (!user.customTitle.isNullOrEmpty()) {
            binding.viewDot.show()
            binding.tvCustomTitle.text = user.customTitle
        }
    }
}