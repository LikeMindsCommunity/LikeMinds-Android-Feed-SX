package com.likeminds.feedsx.media.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.NavHostFragment
import com.likeminds.feedsx.R
import com.likeminds.feedsx.media.model.*
import com.likeminds.feedsx.utils.ExtrasUtil
import com.likeminds.feedsx.utils.ViewUtils.currentFragment
import com.likeminds.feedsx.utils.customview.BaseAppCompatActivity
import com.likeminds.feedsx.utils.permissions.util.*

class LMFeedMediaPickerActivity : BaseAppCompatActivity() {

    private lateinit var mediaPickerExtras: MediaPickerExtras

    private val settingsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkStoragePermission()
        }

    companion object {
        private const val ARG_MEDIA_PICKER_EXTRAS = "mediaPickerExtras"
        const val ARG_MEDIA_PICKER_RESULT = "mediaPickerResult"

        fun start(context: Context, extras: MediaPickerExtras) {
            val intent = Intent(context, LMFeedMediaPickerActivity::class.java)
            intent.apply {
                putExtras(Bundle().apply {
                    putParcelable(ARG_MEDIA_PICKER_EXTRAS, extras)
                })
            }
            context.startActivity(intent)
        }

        fun getIntent(context: Context, extras: MediaPickerExtras): Intent {
            return Intent(context, LMFeedMediaPickerActivity::class.java).apply {
                putExtras(Bundle().apply {
                    putParcelable(ARG_MEDIA_PICKER_EXTRAS, extras)
                })
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.lm_feed_activity_media_picker)
        setupOnBackPressedCallback()
        val extras = ExtrasUtil.getParcelable(
            intent.extras,
            ARG_MEDIA_PICKER_EXTRAS,
            MediaPickerExtras::class.java
        )
        if (extras == null) {
            throw IllegalArgumentException("Arguments are missing")
        } else {
            mediaPickerExtras = extras
        }

        checkStoragePermission()
    }

    // checks if the application has the required media permission
    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val mediaTypes = mediaPickerExtras.mediaTypes
            if (mediaTypes.contains(PDF)) {
                startMediaPickerFragment()
                return
            }
            val LMFeedPermissionExtras = LMFeedPermission.getGalleryPermissionExtras(this)

            LMFeedPermissionManager.performTaskWithPermissionExtras(
                this,
                settingsPermissionLauncher,
                { startMediaPickerFragment() },
                LMFeedPermissionExtras,
                showInitialPopup = true,
                showDeniedPopup = true,
                LMFeedPermissionDeniedCallback = object : LMFeedPermissionDeniedCallback {
                    override fun onDeny() {
                        onBackPressedDispatcher.onBackPressed()
                    }

                    override fun onCancel() {
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            )
        } else {
            LMFeedPermissionManager.performTaskWithPermission(
                this,
                settingsPermissionLauncher,
                { startMediaPickerFragment() },
                LMFeedPermission.getStoragePermissionData(),
                showInitialPopup = true,
                showDeniedPopup = true,
                LMFeedPermissionDeniedCallback = object : LMFeedPermissionDeniedCallback {
                    override fun onDeny() {
                        onBackPressedDispatcher.onBackPressed()
                    }

                    override fun onCancel() {
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            )
        }
    }

    private fun startMediaPickerFragment() {
        checkIfDocumentPickerInitiated()

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as? NavHostFragment ?: return
        val graphInflater = navHostFragment.navController.navInflater
        val navGraph = graphInflater.inflate(R.navigation.lm_feed_nav_media_picker_graph)
        val navController = navHostFragment.navController

        when {
            MediaType.isImageOrVideo(mediaPickerExtras.mediaTypes) -> {
                navGraph.setStartDestination(R.id.media_picker_folder_fragment)
            }

            MediaType.isPDF(mediaPickerExtras.mediaTypes) -> {
                navGraph.setStartDestination(R.id.media_picker_document_fragment)
            }

            else -> {
                finish()
            }
        }
        val args = Bundle().apply {
            putParcelable(ARG_MEDIA_PICKER_EXTRAS, mediaPickerExtras)
        }
        navController.setGraph(navGraph, args)
    }

    /**
     * If Media Picker type is Pdf and device version is >= Q(29), then show system app picker.
     * This is done due to storage restrictions for non-media files in Android 10.
     * */
    private fun checkIfDocumentPickerInitiated() {
        if (MediaType.isPDF(mediaPickerExtras.mediaTypes)
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        ) {
            val intent = Intent().apply {
                putExtras(Bundle().apply {
                    putParcelable(
                        ARG_MEDIA_PICKER_RESULT, MediaPickerResult.Builder()
                            .mediaPickerResultType(MEDIA_RESULT_BROWSE)
                            .mediaTypes(mediaPickerExtras.mediaTypes)
                            .allowMultipleSelect(mediaPickerExtras.allowMultipleSelect)
                            .build()
                    )
                })
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    // setups up on back pressed callback
    private fun setupOnBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this) {
            when (val fragment = supportFragmentManager.currentFragment(R.id.nav_host)) {
                is LMFeedMediaPickerFolderFragment -> {
                    finish()
                }

                is LMFeedMediaPickerItemFragment -> {
                    fragment.onBackPressedFromFragment()
                }

                is LMFeedMediaPickerDocumentFragment -> {
                    if (fragment.onBackPressedFromFragment()) {
                        finish()
                    }
                }

                else -> {
                    finish()
                }
            }
        }
    }
}