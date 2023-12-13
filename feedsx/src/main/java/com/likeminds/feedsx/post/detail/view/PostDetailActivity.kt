package com.likeminds.feedsx.post.detail.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.likeminds.feedsx.R
import com.likeminds.feedsx.branding.model.LMFeedBranding
import com.likeminds.feedsx.databinding.LmFeedActivityPostDetailBinding
import com.likeminds.feedsx.post.detail.model.PostDetailExtras
import com.likeminds.feedsx.utils.ExtrasUtil
import com.likeminds.feedsx.utils.ViewUtils
import com.likeminds.feedsx.utils.customview.BaseAppCompatActivity

class PostDetailActivity : BaseAppCompatActivity() {

    lateinit var binding: LmFeedActivityPostDetailBinding

    private var postDetailExtras: PostDetailExtras? = null

    //Navigation
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var navController: NavController

    companion object {
        const val POST_DETAIL_EXTRAS = "POST_DETAIL_EXTRAS"

        @JvmStatic
        fun start(context: Context, extras: PostDetailExtras) {
            val intent = Intent(context, PostDetailActivity::class.java)
            val bundle = Bundle()
            bundle.putParcelable(POST_DETAIL_EXTRAS, extras)
            intent.putExtra("bundle", bundle)
            context.startActivity(intent)
        }

        @JvmStatic
        fun getIntent(context: Context, extras: PostDetailExtras): Intent {
            val intent = Intent(context, PostDetailActivity::class.java)
            val bundle = Bundle()
            bundle.putParcelable(POST_DETAIL_EXTRAS, extras)
            intent.putExtra("bundle", bundle)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = LmFeedActivityPostDetailBinding.inflate(layoutInflater)
        binding.toolbarColor = LMFeedBranding.getToolbarColor()
        setContentView(binding.root)

        val bundle = intent.getBundleExtra("bundle")

        if (bundle != null) {
            postDetailExtras = ExtrasUtil.getParcelable(
                bundle,
                POST_DETAIL_EXTRAS,
                PostDetailExtras::class.java
            )
            val args = Bundle().apply {
                putParcelable(POST_DETAIL_EXTRAS, postDetailExtras)
            }

            //Navigation
            navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navController = navHostFragment.navController
            navController.setGraph(R.navigation.lm_feed_nav_graph_post_detail, args)

            //Toolbar
            initActionBar()
        } else {
            redirectActivity()
        }
    }

    private fun redirectActivity() {
        ViewUtils.showShortToast(this, getString(R.string.request_not_processed))
        supportFragmentManager.popBackStack()
        onBackPressedDispatcher.onBackPressed()
        overridePendingTransition(R.anim.lm_feed_slide_from_left, R.anim.lm_feed_slide_to_right)
    }

    private fun initActionBar() {
        setSupportActionBar(binding.toolbar)
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }
}