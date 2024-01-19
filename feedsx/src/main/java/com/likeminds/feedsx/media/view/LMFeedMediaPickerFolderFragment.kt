package com.likeminds.feedsx.media.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.likeminds.feedsx.R
import com.likeminds.feedsx.SDKApplication
import com.likeminds.feedsx.branding.model.LMFeedBranding
import com.likeminds.feedsx.databinding.LmFeedFragmentMediaPickerFolderBinding
import com.likeminds.feedsx.media.model.*
import com.likeminds.feedsx.media.view.LMFeedMediaPickerActivity.Companion.ARG_MEDIA_PICKER_RESULT
import com.likeminds.feedsx.media.view.adapter.MediaPickerAdapter
import com.likeminds.feedsx.media.view.adapter.MediaPickerAdapterListener
import com.likeminds.feedsx.media.viewmodel.MediaViewModel
import com.likeminds.feedsx.utils.AndroidUtils
import com.likeminds.feedsx.utils.customview.BaseFragment
import com.likeminds.feedsx.utils.recyclerview.LMFeedGridSpacingItemDecoration

class LMFeedMediaPickerFolderFragment :
    BaseFragment<LmFeedFragmentMediaPickerFolderBinding, MediaViewModel>(),
    MediaPickerAdapterListener {

    private lateinit var mediaPickerAdapter: MediaPickerAdapter

    private lateinit var mediaPickerExtras: MediaPickerExtras
    private val appsList by lazy { ArrayList<LocalAppData>() }

    companion object {
        private const val BUNDLE_MEDIA_PICKER_FOLDER = "bundle of media picker folder"
        const val TAG = "MediaPickerFolder"

        @JvmStatic
        fun getInstance(extras: MediaPickerExtras): LMFeedMediaPickerFolderFragment {
            val fragment = LMFeedMediaPickerFolderFragment()
            val bundle = Bundle()
            bundle.putParcelable(BUNDLE_MEDIA_PICKER_FOLDER, extras)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun getViewModelClass(): Class<MediaViewModel> {
        return MediaViewModel::class.java
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().mediaComponent()?.inject(this)
    }

    override fun getViewBinding(): LmFeedFragmentMediaPickerFolderBinding {
        return LmFeedFragmentMediaPickerFolderBinding.inflate(layoutInflater)
    }

    override fun receiveExtras() {
        super.receiveExtras()
        mediaPickerExtras =
            LMFeedMediaPickerFolderFragmentArgs.fromBundle(requireArguments()).mediaPickerExtras
        getExternalAppList()
    }

    override fun setUpViews() {
        super.setUpViews()

        binding.toolbarColor = LMFeedBranding.getToolbarColor()
        setHasOptionsMenu(true)
        initializeUI()
        initializeListeners()
        viewModel.fetchAllFolders(requireContext(), mediaPickerExtras.mediaTypes)
            .observe(viewLifecycleOwner) {
                mediaPickerAdapter.replace(it)
            }
    }

    private fun initializeUI() {
        binding.toolbar.title = ""

        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        binding.tvToolbarTitle.text = getString(R.string.all_media)

        mediaPickerAdapter = MediaPickerAdapter(this)
        binding.rvFolder.apply {
            addItemDecoration(LMFeedGridSpacingItemDecoration(2, 12))
            adapter = mediaPickerAdapter
        }
    }

    private fun initializeListeners() {
        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        appsList.forEachIndexed { index, localAppData ->
            menu.add(0, localAppData.appId, index, localAppData.appName)
            menu.getItem(index).icon = localAppData.appIcon
        }
        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val localAppData = appsList.find {
            it.appId == item.itemId
        }

        return if (localAppData != null) {
            val extra = MediaPickerResult.Builder()
                .mediaPickerResultType(MEDIA_RESULT_BROWSE)
                .mediaTypes(mediaPickerExtras.mediaTypes)
                .allowMultipleSelect(mediaPickerExtras.allowMultipleSelect)
                .browseClassName(
                    Pair(
                        localAppData.resolveInfo.activityInfo.applicationInfo.packageName,
                        localAppData.resolveInfo.activityInfo.name
                    )
                )
                .build()
            val intent = Intent().apply {
                putExtras(Bundle().apply {
                    putParcelable(
                        ARG_MEDIA_PICKER_RESULT, extra
                    )
                })
            }
            requireActivity().setResult(Activity.RESULT_OK, intent)
            requireActivity().finish()
            true
        } else {
            false
        }
    }

    private fun getExternalAppList() {
        when {
            MediaType.isBothImageAndVideo(mediaPickerExtras.mediaTypes) -> {
                appsList.addAll(AndroidUtils.getExternalMediaPickerApps(requireContext()))
            }

            MediaType.isImage(mediaPickerExtras.mediaTypes) -> {
                appsList.addAll(AndroidUtils.getExternalImagePickerApps(requireContext()))
            }

            MediaType.isVideo(mediaPickerExtras.mediaTypes) -> {
                appsList.addAll(AndroidUtils.getExternalVideoPickerApps(requireContext()))
            }
        }
    }

    override fun onFolderClicked(folderData: MediaFolderViewData) {
        val extras = MediaPickerItemExtras.Builder()
            .bucketId(folderData.bucketId)
            .folderTitle(folderData.title)
            .mediaTypes(mediaPickerExtras.mediaTypes)
            .allowMultipleSelect(mediaPickerExtras.allowMultipleSelect)
            .build()

        findNavController().navigate(
            LMFeedMediaPickerFolderFragmentDirections.actionFolderToItems(extras)
        )
    }
}