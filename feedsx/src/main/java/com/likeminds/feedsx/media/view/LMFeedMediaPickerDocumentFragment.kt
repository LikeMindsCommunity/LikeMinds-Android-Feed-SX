package com.likeminds.feedsx.media.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.likeminds.feedsx.R
import com.likeminds.feedsx.SDKApplication
import com.likeminds.feedsx.branding.model.LMFeedBranding
import com.likeminds.feedsx.databinding.LmFeedFragmentMediaPickerDocumentBinding
import com.likeminds.feedsx.media.model.*
import com.likeminds.feedsx.media.view.adapter.MediaPickerAdapter
import com.likeminds.feedsx.media.view.adapter.MediaPickerAdapterListener
import com.likeminds.feedsx.media.viewmodel.MediaViewModel
import com.likeminds.feedsx.search.util.LMFeedCustomSearchBar
import com.likeminds.feedsx.utils.ViewUtils.hide
import com.likeminds.feedsx.utils.customview.BaseFragment

class LMFeedMediaPickerDocumentFragment :
    BaseFragment<LmFeedFragmentMediaPickerDocumentBinding, MediaViewModel>(),
    MediaPickerAdapterListener {

    private lateinit var mediaPickerAdapter: MediaPickerAdapter

    private val fragmentActivity by lazy { activity as AppCompatActivity? }

    private val selectedMedias by lazy { HashMap<String, MediaViewData>() }
    private lateinit var mediaPickerExtras: MediaPickerExtras

    private var currentSort = SORT_BY_NAME

    companion object {
        const val TAG = "MediaPickerDocument"
        private const val BUNDLE_MEDIA_PICKER_DOC = "bundle of media picker doc"

        @JvmStatic
        fun getInstance(extras: MediaPickerExtras): LMFeedMediaPickerDocumentFragment {
            val fragment = LMFeedMediaPickerDocumentFragment()
            val bundle = Bundle()
            bundle.putParcelable(BUNDLE_MEDIA_PICKER_DOC, extras)
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

    override fun getViewBinding(): LmFeedFragmentMediaPickerDocumentBinding {
        return LmFeedFragmentMediaPickerDocumentBinding.inflate(layoutInflater)
    }

    override fun receiveExtras() {
        super.receiveExtras()
        mediaPickerExtras =
            LMFeedMediaPickerDocumentFragmentArgs.fromBundle(requireArguments()).mediaPickerExtras
    }

    override fun setUpViews() {
        super.setUpViews()

        binding.toolbarColor = LMFeedBranding.getToolbarColor()
        setHasOptionsMenu(true)
        initializeUI()
        initializeListeners()
        viewModel.fetchAllDocuments(requireContext()).observe(viewLifecycleOwner) {
            mediaPickerAdapter.replace(it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.media_picker_document_menu, menu)
        updateMenu(menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_search -> {
                showSearchToolbar()
            }
            R.id.menu_item_sort -> {
                val menuItemView = requireActivity().findViewById<View>(item.itemId)
                showSortingPopupMenu(menuItemView)
            }
            else -> return false
        }
        return true
    }

    private fun updateMenu(menu: Menu) {
        val toolbarColor = LMFeedBranding.getToolbarColor()

        //update search icon
        val item = menu.findItem(R.id.menu_item_search)
        item?.icon?.setTint(toolbarColor)

        //update sort icon
        val item2 = menu.findItem(R.id.menu_item_sort)
        item2?.icon?.setTint(toolbarColor)
    }

    private fun initializeUI() {
        binding.toolbar.title = ""
        fragmentActivity?.setSupportActionBar(binding.toolbar)

        mediaPickerAdapter = MediaPickerAdapter(this)
        binding.rvDocuments.apply {
            adapter = mediaPickerAdapter
        }

        updateSelectedCount()

        initializeSearchView()
    }

    private fun initializeListeners() {
        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.fabSend.setOnClickListener {
            sendSelectedMedias(selectedMedias.values.toList())
        }
    }

    private fun sendSelectedMedias(medias: List<MediaViewData>) {
        val extra = MediaPickerResult.Builder()
            .isResultOk(true)
            .mediaPickerResultType(MEDIA_RESULT_PICKED)
            .mediaTypes(mediaPickerExtras.mediaTypes)
            .allowMultipleSelect(mediaPickerExtras.allowMultipleSelect)
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

    private fun updateSelectedCount() {
        if (isMediaSelectionEnabled()) {
            binding.tvSelectedCount.text =
                String.format("%s selected", selectedMedias.size)
        } else {
            binding.tvSelectedCount.text = getString(R.string.tap_to_select)
        }
        binding.fabSend.isVisible = isMediaSelectionEnabled()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearSelectedMedias() {
        selectedMedias.clear()
        mediaPickerAdapter.notifyDataSetChanged()
        updateSelectedCount()
    }

    override fun onMediaItemClicked(mediaViewData: MediaViewData, itemPosition: Int) {
        if (isMultiSelectionAllowed()) {
            if (selectedMedias.containsKey(mediaViewData.uri.toString())) {
                selectedMedias.remove(mediaViewData.uri.toString())
            } else {
                selectedMedias[mediaViewData.uri.toString()] = mediaViewData
            }

            mediaPickerAdapter.notifyItemChanged(itemPosition)

            updateSelectedCount()
        } else {
            sendSelectedMedias(listOf(mediaViewData))
        }
    }

    override fun isMediaSelectionEnabled(): Boolean {
        return selectedMedias.isNotEmpty()
    }

    override fun isMediaSelected(key: String): Boolean {
        return selectedMedias.containsKey(key)
    }

    override fun browseDocumentClicked() {
        val extra = MediaPickerResult.Builder()
            .isResultOk(true)
            .mediaPickerResultType(MEDIA_RESULT_BROWSE)
            .mediaTypes(mediaPickerExtras.mediaTypes)
            .allowMultipleSelect(mediaPickerExtras.allowMultipleSelect)
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

    override fun isMultiSelectionAllowed(): Boolean {
        return mediaPickerExtras.allowMultipleSelect
    }

    private fun showSortingPopupMenu(view: View) {
        val popup = PopupMenu(view.context, view)
        popup.menuInflater.inflate(R.menu.document_sort_menu, popup.menu)
        when (currentSort) {
            SORT_BY_NAME ->
                popup.menu.findItem(R.id.menu_item_sort_name).isChecked = true

            SORT_BY_DATE ->
                popup.menu.findItem(R.id.menu_item_sort_date).isChecked = true
        }
        popup.setOnMenuItemClickListener { item ->
            item.isChecked = true
            when (item.itemId) {
                R.id.menu_item_sort_name -> {
                    if (currentSort != SORT_BY_NAME) {
                        currentSort = SORT_BY_NAME
                        viewModel.sortDocumentsByName()
                    }
                }

                R.id.menu_item_sort_date -> {
                    if (currentSort != SORT_BY_DATE) {
                        currentSort = SORT_BY_DATE
                        viewModel.sortDocumentsByDate()
                    }
                }
            }
            true
        }
        popup.show()
    }

    private fun initializeSearchView() {
        val searchBar = binding.searchBar
        searchBar.initialize(lifecycleScope)

        searchBar.setSearchViewListener(
            object : LMFeedCustomSearchBar.SearchViewListener {
                override fun onSearchViewClosed() {
                    searchBar.hide()
                    viewModel.clearDocumentFilter()
                }

                override fun crossClicked() {
                    viewModel.clearDocumentFilter()
                }

                override fun keywordEntered(keyword: String) {
                    viewModel.filterDocumentsByKeyword(keyword)
                }

                override fun emptyKeywordEntered() {
                    viewModel.clearDocumentFilter()
                }
            }
        )
        searchBar.observeSearchView(false)
    }

    private fun showSearchToolbar() {
        binding.searchBar.visibility = View.VISIBLE
        binding.searchBar.post {
            binding.searchBar.openSearch()
        }
    }

    fun onBackPressedFromFragment(): Boolean {
        when {
            binding.searchBar.isOpen -> binding.searchBar.closeSearch()
            isMediaSelectionEnabled() -> clearSelectedMedias()
            else -> return true
        }
        return false
    }
}