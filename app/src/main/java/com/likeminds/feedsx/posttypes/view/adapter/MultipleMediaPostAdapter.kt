package com.likeminds.feedsx.posttypes.view.adapter

import com.likeminds.feedsx.post.view.adapter.databinder.createpostmultiplemedia.ItemCreatePostMultipleMediaImageViewDataBinder
import com.likeminds.feedsx.posttypes.view.adapter.databinder.postmultiplemedia.ItemCreatePostMultipleMediaVideoViewDataBinder
import com.likeminds.feedsx.utils.customview.BaseRecyclerAdapter
import com.likeminds.feedsx.utils.customview.ViewDataBinder
import com.likeminds.feedsx.utils.model.BaseViewType

class MultipleMediaPostAdapter : BaseRecyclerAdapter<BaseViewType>() {

    init {
        initViewDataBinders()
    }

    override fun getSupportedViewDataBinder(): MutableList<ViewDataBinder<*, *>> {
        val viewDataBinders = ArrayList<ViewDataBinder<*, *>>(2)

        val multipleMediaImageBinder = ItemCreatePostMultipleMediaImageViewDataBinder()
        viewDataBinders.add(multipleMediaImageBinder)

        val multipleMediaVideoBinder= ItemCreatePostMultipleMediaVideoViewDataBinder()
        viewDataBinders.add(multipleMediaVideoBinder)

        return viewDataBinders
    }
}