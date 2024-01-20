package com.likeminds.feedsx.utils.recyclerview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import androidx.annotation.AnimRes
import androidx.recyclerview.widget.*
import com.likeminds.feedsx.R

class LMFeedAnimatedRecyclerView : RecyclerView {
    private var orientation = LinearLayoutManager.VERTICAL
    private var reverse = false
    private var animationDuration = 600
    private var layoutManagerType = LayoutManagerType.LINEAR
    private var columns = 1

    @AnimRes
    private var animation: Int = R.anim.layout_animation_from_bottom_scale
    private var animationController: LayoutAnimationController? = null

    constructor(
        context: Context, orientation: Int, reverse: Boolean,
        animationDuration: Int, layoutManagerType: Int, columns: Int,
        animation: Int, animationController: LayoutAnimationController?
    ) : super(context) {
        this.orientation = orientation
        this.reverse = reverse
        this.animationDuration = animationDuration
        this.layoutManagerType = layoutManagerType
        this.columns = columns
        this.animation = animation
        this.animationController = animationController
        init(context, null)
    }

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(context, attrs)
    }

    @SuppressLint("Recycle", "WrongConstant")
    private fun init(context: Context, attrs: AttributeSet?) {
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.LMFeedAnimatedRecyclerView, 0, 0)
        orientation = typedArray.getInt(
            R.styleable.LMFeedAnimatedRecyclerView_lm_feed_layoutManagerOrientation,
            orientation
        )
        reverse =
            typedArray.getBoolean(R.styleable.LMFeedAnimatedRecyclerView_lm_feed_layoutManagerReverse, reverse)
        animationDuration =
            typedArray.getInt(R.styleable.LMFeedAnimatedRecyclerView_lm_feed_animationDuration, animationDuration)
        layoutManagerType =
            typedArray.getInt(R.styleable.LMFeedAnimatedRecyclerView_lm_feed_layoutManagerType, layoutManagerType)
        columns =
            typedArray.getInt(R.styleable.LMFeedAnimatedRecyclerView_lm_feed_gridLayoutManagerColumns, columns)
        animation = typedArray.getResourceId(R.styleable.LMFeedAnimatedRecyclerView_lm_feed_layoutAnimation, -1)
        if (animationController == null) animationController =
            if (animation != -1) AnimationUtils.loadLayoutAnimation(
                getContext(),
                animation
            ) else AnimationUtils.loadLayoutAnimation(
                getContext(),
                R.anim.layout_animation_from_bottom_scale
            )
        animationController!!.animation.duration = animationDuration.toLong()
        layoutAnimation = animationController
        if (layoutManagerType == LayoutManagerType.LINEAR) layoutManager =
            LinearLayoutManager(
                context,
                orientation,
                reverse
            ) else if (layoutManagerType == LayoutManagerType.GRID) layoutManager =
            GridLayoutManager(context, columns, orientation, reverse)
    }

    class Builder(private val context: Context) {
        private var orientation = LinearLayoutManager.VERTICAL
        private var reverse = false
        private var animationDuration = 600
        private var layoutManagerType = LayoutManagerType.LINEAR
        private var columns = 1

        @AnimRes
        private var animation: Int = R.anim.layout_animation_from_bottom_scale
        private var animationController: LayoutAnimationController? = null
        fun orientation(orientation: Int): Builder {
            this.orientation = orientation
            return this
        }

        fun reverse(reverse: Boolean): Builder {
            this.reverse = reverse
            return this
        }

        fun animationDuration(animationDuration: Int): Builder {
            this.animationDuration = animationDuration
            return this
        }

        fun layoutManagerType(@LayoutManagerType layoutManagerType: Int): Builder {
            this.layoutManagerType = layoutManagerType
            return this
        }

        fun columns(columns: Int): Builder {
            this.columns = columns
            return this
        }

        fun animation(@AnimRes animation: Int): Builder {
            this.animation = animation
            return this
        }

        fun animationController(animationController: LayoutAnimationController?): Builder {
            this.animationController = animationController
            return this
        }

        fun build(): LMFeedAnimatedRecyclerView {
            return LMFeedAnimatedRecyclerView(
                context, orientation, reverse, animationDuration, layoutManagerType, columns,
                animation, animationController
            )
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Throws(Exception::class)
    fun notifyDataSetChanged() {
        if (adapter != null) {
            adapter!!.notifyDataSetChanged()
            scheduleLayoutAnimation()
        } else {
            throw Exception("The adapter must be set")
        }
    }

    annotation class LayoutManagerType {
        companion object {
            var LINEAR = 0
            var GRID = 1
        }
    }
}