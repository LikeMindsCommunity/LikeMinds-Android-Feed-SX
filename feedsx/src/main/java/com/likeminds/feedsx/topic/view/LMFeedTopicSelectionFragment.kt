package com.likeminds.feedsx.topic.view

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.likeminds.feedsx.R
import com.likeminds.feedsx.SDKApplication
import com.likeminds.feedsx.branding.model.LMFeedBranding
import com.likeminds.feedsx.databinding.LmFeedFragmentTopicSelectionBinding
import com.likeminds.feedsx.search.util.LMFeedCustomSearchBar
import com.likeminds.feedsx.topic.adapter.LMFeedTopicSelectionAdapter
import com.likeminds.feedsx.topic.adapter.LMFeedTopicSelectionAdapterListener
import com.likeminds.feedsx.topic.model.*
import com.likeminds.feedsx.topic.view.LMFeedTopicSelectionActivity.Companion.TOPIC_SELECTION_RESULT_EXTRAS
import com.likeminds.feedsx.topic.viewmodel.LMFeedTopicSelectionViewModel
import com.likeminds.feedsx.utils.*
import com.likeminds.feedsx.utils.ViewUtils.hide
import com.likeminds.feedsx.utils.ViewUtils.show
import com.likeminds.feedsx.utils.customview.BaseFragment

class LMFeedTopicSelectionFragment :
    BaseFragment<LmFeedFragmentTopicSelectionBinding, LMFeedTopicSelectionViewModel>(),
    LMFeedTopicSelectionAdapterListener {
    override fun getViewModelClass(): Class<LMFeedTopicSelectionViewModel> {
        return LMFeedTopicSelectionViewModel::class.java
    }

    override fun getViewBinding(): LmFeedFragmentTopicSelectionBinding {
        return LmFeedFragmentTopicSelectionBinding.inflate(layoutInflater)
    }

    private lateinit var extras: LMFeedTopicSelectionExtras
    private lateinit var mAdapter: LMFeedTopicSelectionAdapter
    private lateinit var scrollListener: EndlessRecyclerScrollListener
    private var selectedTopics = 0
    private var searchKeyword: String? = null

    companion object {
        const val TAG = "LMFeedTopicSelectionFragment"
    }

    override fun attachDagger() {
        SDKApplication.getInstance().lmFeedTopicComponent()?.inject(this)
    }

    override fun receiveExtras() {
        super.receiveExtras()

        extras = ExtrasUtil.getParcelable(
            arguments,
            LMFeedTopicSelectionActivity.TOPIC_SELECTION_EXTRAS,
            LMFeedTopicSelectionExtras::class.java
        ) ?: throw emptyExtrasException(TAG)
    }

    override fun setUpViews() {
        super.setUpViews()
        checkForSelectedTopics()
        initToolbar()
        initSearchView()
        initRecyclerView()
        initListeners()
        fetchTopics()
    }

    override fun observeData() {
        super.observeData()

        viewModel.topicsViewData.observe(viewLifecycleOwner) { response ->
            val topics = response.second


            if (mAdapter.items().isEmpty() && topics.isEmpty()) {
                binding.apply {
                    rvTopics.hide()
                    layoutNoTopic.root.show()
                    fabSelected.hide()
                }
                mAdapter.clearAndNotify()
            } else {
                binding.apply {
                    rvTopics.show()
                    layoutNoTopic.root.hide()
                    fabSelected.show()
                }
                mAdapter.addAll(topics)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            ViewUtils.showErrorMessageToast(requireContext(), errorMessage)
        }
    }

    override fun allTopicSelected(lmFeedAllTopic: LMFeedAllTopicsViewData, position: Int) {
        if (lmFeedAllTopic.isSelected) return

        //update lmFeedAllTopic
        val updateAllTopic = lmFeedAllTopic.toBuilder()
            .isSelected(true)
            .build()

        //update recyclerview for all topics
        mAdapter.update(position, updateAllTopic)

        //update other topics
        mAdapter.items().forEachIndexed { index, topic ->
            if (topic is LMFeedTopicViewData) {
                val updatedTopic = topic.toBuilder().isSelected(false).build()

                mAdapter.update(index, updatedTopic)
            }
        }

        //update sub title
        selectedTopics = 0
        updateSelectedTopicsCount()
        viewModel.clearSelectedTopic()
    }

    override fun topicSelected(topic: LMFeedTopicViewData, position: Int) {

        val allTopicViewData = mAdapter.items()?.find {
            it is LMFeedAllTopicsViewData
        } as? LMFeedAllTopicsViewData

        //check all topic filter exists and it is selected
        if (allTopicViewData != null && allTopicViewData.isSelected) {
            //update view data to not selected
            val updatedAllTopicViewData = allTopicViewData.toBuilder()
                .isSelected(false)
                .build()

            mAdapter.update(0, updatedAllTopicViewData)
        }

        //update topic
        val updatedTopic = if (topic.isSelected) {
            selectedTopics--
            viewModel.removeSelectedTopic(topic)
            topic.toBuilder()
                .isSelected(false)
                .build()
        } else {
            selectedTopics++
            viewModel.addSelectedTopic(topic)
            topic.toBuilder()
                .isSelected(true)
                .build()
        }

        //update recycle
        mAdapter.update(position, updatedTopic)

        //update sub title
        updateSelectedTopicsCount()
    }

    //update selected count in subtitle
    private fun updateSelectedTopicsCount() {
        binding.tvToolbarSubTitle.apply {
            if (selectedTopics > 0) {
                show()
                text = getString(
                    R.string.topics_selected,
                    selectedTopics
                )
            } else {
                hide()
            }
        }
    }

    //calls api
    private fun fetchTopics() {
        viewModel.getTopics(
            extras.showAllTopicFilter,
            extras.showEnabledTopicOnly,
            1,
            null
        )
    }

    //init listeners for done
    private fun initListeners() {
        binding.fabSelected.setOnClickListener {
            //check for all topic is selected
            val allTopicViewData = mAdapter.items()?.find {
                it is LMFeedAllTopicsViewData
            } as? LMFeedAllTopicsViewData

            //create result extras
            val resultExtras = if (allTopicViewData != null && allTopicViewData.isSelected) {
                LMFeedTopicSelectionResultExtras.Builder()
                    .isAllTopicSelected(true)
                    .build()
            } else {
                val selectedTopics = mAdapter.items().filter {
                    it is LMFeedTopicViewData && it.isSelected
                }.map {
                    it as LMFeedTopicViewData
                }
                LMFeedTopicSelectionResultExtras.Builder()
                    .selectedTopics(selectedTopics)
                    .build()
            }

            //send result
            val resultIntent = Intent().apply {
                putExtra(TOPIC_SELECTION_RESULT_EXTRAS, resultExtras)
            }
            requireActivity().setResult(Activity.RESULT_OK, resultIntent)
            requireActivity().finish()
        }
    }

    //check whether extras contains selected and disabled topics and set it to view model
    private fun checkForSelectedTopics() {
        val previousSelectedTopics = extras.selectedTopics
        val disabledTopics = extras.disabledTopics

        viewModel.setPreviousSelectedTopics(previousSelectedTopics)
        viewModel.setPreviousDisabledTopics(disabledTopics)

        selectedTopics = previousSelectedTopics?.size ?: 0
        updateSelectedTopicsCount()
    }

    private fun initToolbar() {
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.ivSearch.setOnClickListener {
            showSearchToolbar()
        }

        binding.toolbarColor = LMFeedBranding.getToolbarColor()
    }

    private fun showSearchToolbar() {
        binding.searchBar.show()
        binding.searchBar.post {
            binding.searchBar.openSearch()
        }
    }

    //init recycler view
    private fun initRecyclerView() {
        val linearLayoutManager = LinearLayoutManager(requireContext())
        mAdapter = LMFeedTopicSelectionAdapter(this)
        binding.rvTopics.apply {
            layoutManager = linearLayoutManager
            adapter = mAdapter
            show()

            //attach scroll listener
            attachScrollListener(this, linearLayoutManager)
        }
    }

    // attach scroll listener for pagination
    private fun attachScrollListener(
        recyclerView: RecyclerView,
        layoutManager: LinearLayoutManager
    ) {
        scrollListener = object : EndlessRecyclerScrollListener(layoutManager) {
            override fun onLoadMore(currentPage: Int) {
                if (currentPage > 0) {
                    // calls api for paginated data
                    viewModel.getTopics(
                        extras.showAllTopicFilter,
                        extras.showEnabledTopicOnly,
                        currentPage,
                        searchKeyword
                    )
                }
            }
        }
        recyclerView.addOnScrollListener(scrollListener)
    }

    //init search view and handle listeners
    private fun initSearchView() {
        binding.searchBar.apply {
            initialize(lifecycleScope)
            setSearchViewListener(object : LMFeedCustomSearchBar.SearchViewListener {
                override fun onSearchViewClosed() {
                    hide()
                    clearSearchedTopics()
                }

                override fun crossClicked() {
                    clearSearchedTopics()
                }

                override fun keywordEntered(keyword: String) {
                    super.keywordEntered(keyword)
                    scrollListener.resetData()
                    mAdapter.clearAndNotify()
                    searchKeyword = keyword
                    viewModel.getTopics(
                        false,
                        extras.showEnabledTopicOnly,
                        1,
                        keyword
                    )
                }

                override fun emptyKeywordEntered() {
                    super.emptyKeywordEntered()
                    if (!searchKeyword.isNullOrEmpty()) {
                        clearSearchedTopics()
                    }
                }
            })
            observeSearchView(true)
        }
    }

    //reset search view and show default view
    private fun clearSearchedTopics() {
        scrollListener.resetData()
        mAdapter.clearAndNotify()
        searchKeyword = null
        viewModel.getTopics(
            extras.showAllTopicFilter,
            extras.showEnabledTopicOnly,
            1,
            null
        )
    }
}