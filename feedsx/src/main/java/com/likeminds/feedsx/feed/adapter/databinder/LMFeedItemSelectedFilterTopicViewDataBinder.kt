package com.likeminds.feedsx.feed.adapter.databinder

import android.view.LayoutInflater
import android.view.ViewGroup
import com.likeminds.feedsx.branding.model.LMFeedBranding
import com.likeminds.feedsx.databinding.LmFeedItemFilterSelectedTopicsBinding
import com.likeminds.feedsx.feed.adapter.LMFeedSelectedTopicAdapterListener
import com.likeminds.feedsx.topic.model.LMFeedTopicViewData
import com.likeminds.feedsx.utils.customview.ViewDataBinder
import com.likeminds.feedsx.utils.model.ITEM_TOPIC

class LMFeedItemSelectedFilterTopicViewDataBinder(private val listener: LMFeedSelectedTopicAdapterListener) :
    ViewDataBinder<LmFeedItemFilterSelectedTopicsBinding, LMFeedTopicViewData>() {
    override val viewType: Int
        get() = ITEM_TOPIC

    override fun createBinder(parent: ViewGroup): LmFeedItemFilterSelectedTopicsBinding {
        val binding = LmFeedItemFilterSelectedTopicsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        setListeners(binding)

        return binding
    }

    override fun bindData(
        binding: LmFeedItemFilterSelectedTopicsBinding,
        data: LMFeedTopicViewData,
        position: Int
    ) {
        binding.apply {
            this.position = position
            buttonColor = LMFeedBranding.getButtonsColor()

            tvTopicName.text = data.name
        }
    }

    private fun setListeners(binding: LmFeedItemFilterSelectedTopicsBinding) {
        binding.apply {
            ivCross.setOnClickListener {
                val position = binding.position
                listener.topicCleared(position)
            }
        }
    }
}