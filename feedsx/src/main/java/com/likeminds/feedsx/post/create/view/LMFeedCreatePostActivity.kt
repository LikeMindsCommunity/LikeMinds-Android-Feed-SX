package com.likeminds.feedsx.post.create.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.likeminds.feedsx.R
import com.likeminds.feedsx.branding.model.LMFeedBranding
import com.likeminds.feedsx.databinding.LmFeedActivityCreatePostBinding
import com.likeminds.feedsx.utils.customview.BaseAppCompatActivity

class LMFeedCreatePostActivity : BaseAppCompatActivity() {

    lateinit var binding: LmFeedActivityCreatePostBinding

    //Navigation
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var navController: NavController

    companion object {

        const val POST_ATTACHMENTS_LIMIT = 10
        const val RESULT_UPLOAD_POST = Activity.RESULT_FIRST_USER + 1
        const val SOURCE_EXTRA = "SOURCE_EXTRA"

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, LMFeedCreatePostActivity::class.java))
        }

        @JvmStatic
        fun getIntent(context: Context, source: String?): Intent {
            val intent = Intent(context, LMFeedCreatePostActivity::class.java)
            intent.putExtra(SOURCE_EXTRA, source)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LmFeedActivityCreatePostBinding.inflate(layoutInflater)
        binding.toolbarColor = LMFeedBranding.getToolbarColor()
        setContentView(binding.root)

        val source = intent.getStringExtra(SOURCE_EXTRA)
        val args = Bundle().apply {
            putString(SOURCE_EXTRA, source)
        }
        //Navigation
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        navController.setGraph(R.navigation.lm_feed_nav_graph_create_post, args)

        //Toolbar
        setSupportActionBar(binding.toolbar)
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }
}