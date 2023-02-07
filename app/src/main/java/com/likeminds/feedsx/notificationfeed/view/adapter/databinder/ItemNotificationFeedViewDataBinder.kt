package com.likeminds.feedsx.notificationfeed.view.adapter.databinder

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.likeminds.feedsx.R
import com.likeminds.feedsx.databinding.ItemNotificationFeedBinding
import com.likeminds.feedsx.notificationfeed.model.NotificationFeedViewData
import com.likeminds.feedsx.notificationfeed.view.adapter.NotificationFeedAdapter.NotificationFeedAdapterListener
import com.likeminds.feedsx.overflowmenu.model.OverflowMenuItemViewData
import com.likeminds.feedsx.overflowmenu.view.OverflowMenuPopup
import com.likeminds.feedsx.overflowmenu.view.adapter.OverflowMenuAdapterListener
import com.likeminds.feedsx.posttypes.util.PostTypeUtil
import com.likeminds.feedsx.utils.MemberImageUtil
import com.likeminds.feedsx.utils.TimeUtil.getRelativeTime
import com.likeminds.feedsx.utils.ValueUtils.getValidTextForLinkify
import com.likeminds.feedsx.utils.ViewUtils
import com.likeminds.feedsx.utils.customview.ViewDataBinder
import com.likeminds.feedsx.utils.model.ITEM_NOTIFICATION_FEED

class ItemNotificationFeedViewDataBinder constructor(
    val listener: NotificationFeedAdapterListener
) : ViewDataBinder<ItemNotificationFeedBinding, NotificationFeedViewData>(),
    OverflowMenuAdapterListener {

    private lateinit var overflowMenu: OverflowMenuPopup

    override val viewType: Int
        get() = ITEM_NOTIFICATION_FEED

    override fun createBinder(parent: ViewGroup): ItemNotificationFeedBinding {
        overflowMenu = OverflowMenuPopup.create(parent.context, this)
        return ItemNotificationFeedBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    }

    override fun bindData(
        binding: ItemNotificationFeedBinding,
        data: NotificationFeedViewData,
        position: Int
    ) {

        // sets items to overflow menu
        PostTypeUtil.setOverflowMenuItems(
            overflowMenu,
            data.menuItems
        )

        initNotificationView(
            binding,
            data
        )

    }

    private fun initNotificationView(
        binding: ItemNotificationFeedBinding,
        data: NotificationFeedViewData
    ) {
        initNotificationTextContent(
            binding,
            data
        )

        val user = data.user
        binding.apply {

            if (data.isRead) {
                root.setBackgroundColor(
                    ContextCompat.getColor(
                        root.context,
                        R.color.white
                    )
                )
            } else {
                root.setBackgroundColor(
                    ContextCompat.getColor(
                        root.context,
                        R.color.cloudy_blue_40
                    )
                )
            }

            ivNotificationMenu.setOnClickListener {
                showOverflowMenu(ivNotificationMenu, overflowMenu)
            }

            MemberImageUtil.setImage(
                user.imageUrl,
                user.name,
                data.id,
                memberImage,
                showRoundImage = true
            )

            //TODO: logic to get post type.
            ivPostType.setImageResource(R.drawable.ic_doc_attachment)

            tvNotificationDate.text = getRelativeTime(data.createdAt)
        }
    }

    private fun initNotificationTextContent(
        binding: ItemNotificationFeedBinding,
        data: NotificationFeedViewData
    ) {
        binding.apply {
            val textForLinkify = data.activityMessage.getValidTextForLinkify()

            // TODO: handle the member name in the text.
            tvNotificationContent.text = textForLinkify
        }
    }

    //to show the options on the notification item
    private fun showOverflowMenu(ivNotificationMenu: ImageView, overflowMenu: OverflowMenuPopup) {
        overflowMenu.showAsDropDown(
            ivNotificationMenu,
            -ViewUtils.dpToPx(16),
            -ivNotificationMenu.height / 2,
            Gravity.START
        )
    }

    // handles the menu item click on the post
    override fun onMenuItemClicked(menu: OverflowMenuItemViewData) {
        listener.onPostMenuItemClicked(menu.dataId, menu.title)
    }
}