package com.likeminds.feedsx.media.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.likeminds.feedsx.R
import com.likeminds.feedsx.SDKApplication
import com.likeminds.feedsx.branding.model.LMFeedBranding
import com.likeminds.feedsx.databinding.LmFeedFragmentMediaPickerItemBinding
import com.likeminds.feedsx.media.model.*
import com.likeminds.feedsx.media.view.adapter.MediaPickerAdapter
import com.likeminds.feedsx.media.view.adapter.MediaPickerAdapterListener
import com.likeminds.feedsx.media.viewmodel.MediaViewModel
import com.likeminds.feedsx.utils.actionmode.ActionModeCallback
import com.likeminds.feedsx.utils.actionmode.ActionModeListener
import com.likeminds.feedsx.utils.customview.BaseFragment
import com.likeminds.feedsx.utils.model.ITEM_MEDIA_PICKER_HEADER

class LMFeedMediaPickerItemFragment :
    BaseFragment<LmFeedFragmentMediaPickerItemBinding, MediaViewModel>(),
    MediaPickerAdapterListener,
    ActionModeListener<Nothing> {

    private var actionModeCallback: ActionModeCallback<Nothing>? = null

    lateinit var mediaPickerAdapter: MediaPickerAdapter

    private lateinit var mediaPickerItemExtras: MediaPickerItemExtras
    private val selectedMedias by lazy { HashMap<String, MediaViewData>() }

    companion object {
        private const val BUNDLE_MEDIA_PICKER_ITEM = "bundle of media picker item"
        const val TAG = "MediaPickerItem"

        @JvmStatic
        fun getInstance(extras: MediaPickerItemExtras): LMFeedMediaPickerItemFragment {
            val fragment = LMFeedMediaPickerItemFragment()
            val bundle = Bundle()
            bundle.putParcelable(BUNDLE_MEDIA_PICKER_ITEM, extras)
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

    override fun getViewBinding(): LmFeedFragmentMediaPickerItemBinding {
        return LmFeedFragmentMediaPickerItemBinding.inflate(layoutInflater)
    }

    override fun receiveExtras() {
        super.receiveExtras()
        mediaPickerItemExtras =
            LMFeedMediaPickerItemFragmentArgs.fromBundle(requireArguments()).mediaPickerItemExtras
    }

    override fun setUpViews() {
        super.setUpViews()
        binding.toolbarColor = LMFeedBranding.getToolbarColor()
        if (mediaPickerItemExtras.allowMultipleSelect) {
            setHasOptionsMenu(true)
        }
        initializeUI()
        initializeListeners()

        viewModel.fetchMediaInBucket(
            requireContext(),
            mediaPickerItemExtras.bucketId,
            mediaPickerItemExtras.mediaTypes as MutableList<String>
        ).observe(viewLifecycleOwner) {
            mediaPickerAdapter.replace(it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.media_picker_item_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.select_multiple -> {
                startActionMode()
                true
            }

            else -> false
        }
    }

    private fun initializeUI() {
        binding.toolbar.title = ""
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)

        binding.tvToolbarTitle.text = mediaPickerItemExtras.folderTitle

        mediaPickerAdapter = MediaPickerAdapter(this)
        binding.rvMediaItem.apply {
            val mLayoutManager = GridLayoutManager(requireContext(), 3)
            mLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (mediaPickerAdapter.getItemViewType(position)) {
                        ITEM_MEDIA_PICKER_HEADER -> 3
                        else -> 1
                    }
                }
            }
            layoutManager = mLayoutManager
            adapter = mediaPickerAdapter
        }
    }

    private fun initializeListeners() {
        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun startActionMode() {
        if (actionModeCallback == null) {
            actionModeCallback = ActionModeCallback()
        }
        if (actionModeCallback?.isActionModeEnabled() != true) {
            actionModeCallback?.startActionMode(
                this,
                requireActivity() as AppCompatActivity,
                R.menu.media_picker_actions_menu
            )
        }
        updateActionTitle()
    }

    private fun updateActionTitle() {
        if (selectedMedias.size > 0) {
            actionModeCallback?.updateTitle("${selectedMedias.size} selected")
        } else {
            actionModeCallback?.updateTitle("Tap photo to select")
        }
    }

    override fun onActionItemClick(item: MenuItem?) {
        when (item?.itemId) {
            R.id.menu_item_ok -> {
                sendSelectedMedia(selectedMedias.values.toMutableList())
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onActionModeDestroyed() {
        selectedMedias.clear()
        mediaPickerAdapter.notifyDataSetChanged()
    }

    override fun onMediaItemClicked(mediaViewData: MediaViewData, itemPosition: Int) {
        sendSelectedMedia(listOf(mediaViewData))
    }

    override fun onMediaItemLongClicked(mediaViewData: MediaViewData, itemPosition: Int) {
        if (selectedMedias.containsKey(mediaViewData.uri.toString())) {
            selectedMedias.remove(mediaViewData.uri.toString())
        } else {
            selectedMedias[mediaViewData.uri.toString()] = mediaViewData
        }

        mediaPickerAdapter.notifyItemChanged(itemPosition)

        // Invalidate Action Menu Items
        if (selectedMedias.isNotEmpty()) {
            startActionMode()
        } else {
            actionModeCallback?.finishActionMode()
        }
    }

    override fun isMediaSelectionEnabled(): Boolean {
        return actionModeCallback?.isActionModeEnabled() == true
    }

    override fun isMediaSelected(key: String): Boolean {
        return selectedMedias.containsKey(key)
    }

    override fun isMultiSelectionAllowed(): Boolean {
        return mediaPickerItemExtras.allowMultipleSelect
    }

    fun onBackPressedFromFragment() {
        if (isMediaSelectionEnabled()) actionModeCallback?.finishActionMode()
        else findNavController().navigateUp()
    }

    private fun sendSelectedMedia(medias: List<MediaViewData>) {
        val extra = MediaPickerResult.Builder()
            .isResultOk(true)
            .mediaPickerResultType(MEDIA_RESULT_PICKED)
            .mediaTypes(mediaPickerItemExtras.mediaTypes)
            .allowMultipleSelect(mediaPickerItemExtras.allowMultipleSelect)
            .medias(medias)
            .build()

        val intent = Intent().apply {
            putExtras(Bundle().apply {
                putParcelable(
                    LMFeedMediaPickerActivity.ARG_MEDIA_PICKER_RESULT, extra
                )
            })
        }
        requireActivity().setResult(Activity.RESULT_OK, intent)
        requireActivity().finish()
    }
}