package com.likeminds.feedsx.media.util

import android.graphics.Rect
import android.net.Uri
import android.widget.ProgressBar
import androidx.core.view.get
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.likeminds.feedsx.databinding.*
import com.likeminds.feedsx.media.customviews.LMFeedVideoPlayerView
import com.likeminds.feedsx.post.detail.view.adapter.PostDetailAdapter
import com.likeminds.feedsx.posttypes.model.PostViewData
import com.likeminds.feedsx.posttypes.view.adapter.PostAdapter
import com.likeminds.feedsx.utils.ViewUtils.show
import com.likeminds.feedsx.utils.customview.DataBoundViewHolder
import com.likeminds.feedsx.utils.model.ITEM_POST_MULTIPLE_MEDIA
import com.likeminds.feedsx.utils.model.ITEM_POST_SINGLE_VIDEO
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
class PostVideoAutoPlayHelper private constructor(private val recyclerView: RecyclerView) {
    companion object {
        private var postVideoAutoPlayHelper: PostVideoAutoPlayHelper? = null

        // When playerView will be less than [MIN_LIMIT_VISIBILITY]% visible than it will stop the player
        private const val MIN_LIMIT_VISIBILITY = 20

        fun getInstance(recyclerView: RecyclerView): PostVideoAutoPlayHelper {
            if (postVideoAutoPlayHelper == null) {
                postVideoAutoPlayHelper = PostVideoAutoPlayHelper(recyclerView)
            }
            return postVideoAutoPlayHelper!!
        }

        fun getInstance(): PostVideoAutoPlayHelper? {
            return postVideoAutoPlayHelper
        }
    }

    private var lastPlayerView: LMFeedVideoPlayerView? = null

    private var currentPlayingVideoItemPos = -1 // -1 indicates nothing playing

    private val autoPlayVideoScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            when (recyclerView.adapter) {
                // the recycler view is of [FeedFragment]
                is PostAdapter -> {
                    playMostVisibleItem()
                }

                // the recycler view is of [PostDetailFragment]
                is PostDetailAdapter -> {
                    playIfPostVisible()
                }
            }
        }
    }

    // attaches a scroll listener to auto play videos in the recycler view
    fun attachScrollListenerForVideo() {
        recyclerView.addOnScrollListener(autoPlayVideoScrollListener)
    }

    // detaches the scroll listener to auto play videos in the recycler view
    fun detachScrollListenerForVideo() {
        recyclerView.removeOnScrollListener(autoPlayVideoScrollListener)
    }

    /**
     * Finds the most visible post and attaches the player to it
     */
    fun playMostVisibleItem() {
        val firstVisiblePosition: Int = findFirstVisibleItemPosition()
        val lastVisiblePosition: Int = findLastVisibleItemPosition()

        var maxPercentage = -1
        var pos = 0
        recyclerView.post {
            for (i in firstVisiblePosition..lastVisiblePosition) {
                val viewHolder: RecyclerView.ViewHolder =
                    recyclerView.findViewHolderForAdapterPosition(i) ?: return@post

                val currentPercentage = getVisiblePercentage(viewHolder)
                if (currentPercentage > maxPercentage) {
                    maxPercentage = currentPercentage.toInt()
                    pos = i
                }
            }
            if (maxPercentage == -1 || maxPercentage < MIN_LIMIT_VISIBILITY) {
                pos = -1
            }
            if (pos == -1) {
                if (currentPlayingVideoItemPos != -1) {
                    // if a video is already playing
                    val viewHolder: RecyclerView.ViewHolder =
                        recyclerView.findViewHolderForAdapterPosition(currentPlayingVideoItemPos)!!

                    /* check if current view's visibility is more than MIN_LIMIT_VISIBILITY */
                    val currentVisibility = getVisiblePercentage(viewHolder)
                    if (currentVisibility < MIN_LIMIT_VISIBILITY) {
                        removePlayer()
                    }
                    currentPlayingVideoItemPos = -1
                }
            } else {
                // if no video is playing, directly attach a player at the [pos]
                currentPlayingVideoItemPos = pos
                attachVideoPlayerAt(pos)
            }
        }
    }

    /**
     * Checks if post's visibility is more than [MIN_LIMIT_VISIBILITY]%
     * attaches the player at position = 0
     */
    fun playIfPostVisible() {
        recyclerView.post {
            val viewHolder: RecyclerView.ViewHolder =
                recyclerView.findViewHolderForAdapterPosition(0) ?: return@post

            val currentVisiblePercentage = getVisiblePercentage(viewHolder)

            if (currentVisiblePercentage == (-1).toFloat() || currentVisiblePercentage < MIN_LIMIT_VISIBILITY) {
                // post item's visibility is less than [MIN_LIMIT_VISIBILITY] so remove the player
                removePlayer()
                currentPlayingVideoItemPos = -1
            } else {
                // post item's visibility is more than [MIN_LIMIT_VISIBILITY]
                currentPlayingVideoItemPos = 0
                attachVideoPlayerAt(0)
            }
        }
    }

    // returns the % visibility of item in recycler view
    private fun getVisiblePercentage(
        holder: RecyclerView.ViewHolder
    ): Float {
        val rectParent = Rect()
        recyclerView.getGlobalVisibleRect(rectParent)
        val location = IntArray(2)
        holder.itemView.getLocationOnScreen(location)

        val rectChild = Rect(
            location[0],
            location[1],
            location[0] + holder.itemView.width,
            location[1] + holder.itemView.height
        )

        val rectParentArea =
            ((rectChild.right - rectChild.left) * (rectChild.bottom - rectChild.top)).toFloat()
        val xOverlap = max(
            0,
            min(
                rectChild.right,
                rectParent.right
            ) - max(
                rectChild.left,
                rectParent.left
            )
        ).toFloat()

        val yOverlap = max(
            0,
            min(
                rectChild.bottom,
                rectParent.bottom
            ) - max(
                rectChild.top,
                rectParent.top
            )
        ).toFloat()

        val overlapArea = xOverlap * yOverlap
        return (overlapArea / rectParentArea * 100.0f)
    }

    // returns the position of first visible item in recycler view
    private fun findFirstVisibleItemPosition(): Int {
        if (recyclerView.layoutManager is LinearLayoutManager) {
            return (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        }
        return -1
    }

    // returns the position of last visible item in recycler view
    private fun findLastVisibleItemPosition(): Int {
        if (recyclerView.layoutManager is LinearLayoutManager) {
            return (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
        }
        return -1
    }

    // attaches a player at specified position
    private fun attachVideoPlayerAt(pos: Int) {
        recyclerView.adapter.apply {
            when (this) {
                is PostAdapter -> {
                    val item = this[pos]
                    handleVideoPlay(
                        pos,
                        (item?.viewType ?: 0),
                        item as PostViewData
                    )
                }

                is PostDetailAdapter -> {
                    if (pos != 0) {
                        return
                    }
                    val item = this[pos]
                    handleVideoPlay(
                        pos,
                        (item?.viewType ?: 0),
                        item as PostViewData
                    )
                }
            }
        }
    }

    // handles main logic to play the video at specified position
    private fun handleVideoPlay(
        pos: Int,
        viewType: Int,
        data: PostViewData
    ) {
        when (viewType) {
            ITEM_POST_SINGLE_VIDEO -> {
                // if the post is of type [ITEM_POST_SINGLE_VIDEO]
                val itemPostSingleVideoBinding =
                    (recyclerView.findViewHolderForAdapterPosition(pos) as? DataBoundViewHolder<*>)
                        ?.binding as? LmFeedItemPostSingleVideoBinding ?: return

                if (lastPlayerView == null || lastPlayerView != itemPostSingleVideoBinding.videoPost) {
                    val meta = data.attachments.first().attachmentMeta
                    startNewPlayer(
                        itemPostSingleVideoBinding.videoPost,
                        itemPostSingleVideoBinding.pbVideoLoader,
                        meta.url
                    )
                }
                lastPlayerView = itemPostSingleVideoBinding.videoPost
            }

            ITEM_POST_MULTIPLE_MEDIA -> {
                // if the post is of type [ITEM_POST_MULTIPLE_MEDIA]
                val itemMultipleMediaBinding =
                    (recyclerView.findViewHolderForAdapterPosition(pos) as? DataBoundViewHolder<*>)
                        ?.binding as? LmFeedItemPostMultipleMediaBinding ?: return

                val viewPager = itemMultipleMediaBinding.viewpagerMultipleMedia
                val currentItem = viewPager.currentItem

                // gets the video binding from view pager
                val itemMultipleMediaVideoBinding =
                    ((viewPager[0] as RecyclerView).findViewHolderForAdapterPosition(currentItem) as? DataBoundViewHolder<*>)
                        ?.binding as? LmFeedItemMultipleMediaVideoBinding

                if (itemMultipleMediaVideoBinding == null) {
                    // if itemMultipleMediaVideoBinding, that means it is an image
                    removePlayer()
                } else {
                    if (lastPlayerView == null || lastPlayerView != itemMultipleMediaVideoBinding.videoPost) {
                        val meta = data.attachments[currentItem].attachmentMeta
                        startNewPlayer(
                            itemMultipleMediaVideoBinding.videoPost,
                            itemMultipleMediaVideoBinding.pbVideoLoader,
                            meta.url
                        )
                    }
                    lastPlayerView = itemMultipleMediaVideoBinding.videoPost
                }
            }

            else -> {
                // if the post is does not have any video, simply remove the player
                removePlayer()
            }
        }
    }

    // starts player in new player view and stops last player
    private fun startNewPlayer(
        videoPost: LMFeedVideoPlayerView,
        progressBar: ProgressBar,
        url: String?
    ) {
        progressBar.show()
        val videoUri = Uri.parse(url)
        videoPost.startPlayingRemoteUri(videoUri, progressBar)
        removePlayer()
    }

    // removes the player from view and sets it to null
    fun removePlayer() {
        if (lastPlayerView != null) {
            // stop last player
            lastPlayerView?.removePlayer()
            lastPlayerView = null
        }
    }

    fun destroy() {
        removePlayer()
        postVideoAutoPlayHelper = null
    }
}