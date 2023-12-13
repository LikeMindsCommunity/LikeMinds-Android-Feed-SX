package com.likeminds.feedsx.report.view

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.core.view.isVisible
import com.google.android.flexbox.*
import com.likeminds.feedsx.R
import com.likeminds.feedsx.SDKApplication
import com.likeminds.feedsx.SDKApplication.Companion.LOG_TAG
import com.likeminds.feedsx.databinding.LmFeedFragmentReportBinding
import com.likeminds.feedsx.post.edit.viewmodel.LMFeedHelperViewModel
import com.likeminds.feedsx.report.model.*
import com.likeminds.feedsx.report.view.adapter.ReportAdapter
import com.likeminds.feedsx.report.view.adapter.ReportAdapter.ReportAdapterListener
import com.likeminds.feedsx.report.viewmodel.ReportViewModel
import com.likeminds.feedsx.utils.*
import com.likeminds.feedsx.utils.ValueUtils.pluralizeOrCapitalize
import com.likeminds.feedsx.utils.customview.BaseFragment
import com.likeminds.feedsx.utils.pluralize.model.WordAction
import javax.inject.Inject

class LMFeedReportFragment : BaseFragment<LmFeedFragmentReportBinding, ReportViewModel>(),
    ReportAdapterListener {

    override fun getViewModelClass(): Class<ReportViewModel> {
        return ReportViewModel::class.java
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().reportComponent()?.inject(this)
    }

    override fun getViewBinding(): LmFeedFragmentReportBinding {
        return LmFeedFragmentReportBinding.inflate(layoutInflater)
    }

    companion object {
        const val TAG = "ReportFragment"
        const val REPORT_RESULT = "REPORT_RESULT"
    }

    @Inject
    lateinit var lmFeedHelperViewModel: LMFeedHelperViewModel

    private lateinit var extras: ReportExtras
    private lateinit var mAdapter: ReportAdapter
    private var tagSelected: ReportTagViewData? = null
    private lateinit var reasonOrTag: String

    override fun receiveExtras() {
        super.receiveExtras()
        extras = ExtrasUtil.getParcelable(
            requireActivity().intent?.getBundleExtra("bundle"),
            LMFeedReportActivity.ARG_REPORTS,
            ReportExtras::class.java
        ) ?: throw emptyExtrasException(TAG)
    }

    override fun reportTagSelected(reportTagViewData: ReportTagViewData) {
        super.reportTagSelected(reportTagViewData)
        //check if [Others] is selected, edit text for reason should be visible
        binding.etOthers.isVisible = reportTagViewData.name.contains("Others", true)

        //replace list in adapter and only highlight selected tag
        mAdapter.replace(
            mAdapter.items()
                .map {
                    (it as ReportTagViewData).toBuilder()
                        .isSelected(it.id == reportTagViewData.id)
                        .build()
                })
    }

    override fun setUpViews() {
        super.setUpViews()
        initRecyclerView()
        initViewAsType()
        initListeners()
        getReportTags()
    }

    override fun observeData() {
        super.observeData()

        viewModel.listOfTagViewData.observe(viewLifecycleOwner) { tags ->
            mAdapter.replace(tags)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            ViewUtils.showErrorMessageToast(requireContext(), error)
            requireActivity().setResult(Activity.RESULT_CANCELED)
            requireActivity().finish()
        }

        viewModel.postReportResponse.observe(viewLifecycleOwner) { success ->
            if (success) {
                Log.d(LOG_TAG, "report send successfully")

                //send analytics events
                sendReportEvent()

                val intent = Intent().apply {
                    putExtra(
                        REPORT_RESULT,
                        ReportType.getEntityType(this@LMFeedReportFragment.extras.entityType)
                    )
                }
                //set result, from where the result is coming.
                requireActivity().setResult(Activity.RESULT_OK, intent)
                requireActivity().finish()
            }
        }
    }

    //send report event depending upon which type of the report is created
    private fun sendReportEvent() {
        when (extras.entityType) {
            REPORT_TYPE_POST -> {
                // sends post reported event
                viewModel.sendPostReportedEvent(
                    extras.entityId,
                    extras.uuid,
                    ViewUtils.getPostTypeFromViewType(extras.postViewType),
                    reasonOrTag
                )
            }

            REPORT_TYPE_COMMENT -> {
                // sends comment reported event
                viewModel.sendCommentReportedEvent(
                    extras.postId,
                    extras.uuid,
                    extras.entityId,
                    reasonOrTag
                )
            }

            REPORT_TYPE_REPLY -> {
                // sends reply reported event
                viewModel.sendReplyReportedEvent(
                    extras.postId,
                    extras.uuid,
                    extras.parentCommentId,
                    extras.entityId,
                    reasonOrTag
                )
            }
        }
    }

    //setup recycler view
    private fun initRecyclerView() {
        mAdapter = ReportAdapter(this)
        val flexboxLayoutManager = FlexboxLayoutManager(requireContext())
        flexboxLayoutManager.flexDirection = FlexDirection.ROW
        flexboxLayoutManager.justifyContent = JustifyContent.FLEX_START
        binding.rvReport.layoutManager = flexboxLayoutManager
        binding.rvReport.adapter = mAdapter
    }

    //set headers and sub header as per report type
    private fun initViewAsType() {
        when (extras.entityType) {
            REPORT_TYPE_POST -> {
                binding.tvReportSubHeader.text = getString(
                    R.string.report_sub_header,
                    lmFeedHelperViewModel.getPostVariable()
                        .pluralizeOrCapitalize(WordAction.ALL_SMALL_SINGULAR)
                )
            }

            REPORT_TYPE_COMMENT -> {
                binding.tvReportSubHeader.text = getString(R.string.report_sub_header, "comment")
            }

            REPORT_TYPE_REPLY -> {
                binding.tvReportSubHeader.text = getString(R.string.report_sub_header, "reply")
            }
        }
    }

    private fun initListeners() {
        binding.ivCross.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnPostReport.setOnClickListener {
            //get selected tag
            tagSelected = mAdapter.items()
                .map { it as ReportTagViewData }
                .find { it.isSelected }

            //get reason for [edittext]
            val reason = binding.etOthers.text?.trim().toString()
            val isOthersSelected = tagSelected?.name?.contains("Others", true)

            //if no tag is selected
            if (tagSelected == null) {
                ViewUtils.showShortSnack(
                    binding.root,
                    "Please select at least on report tag."
                )
                return@setOnClickListener
            }

            //if [Others] is selected but reason is empty
            if (isOthersSelected == true && reason.isEmpty()) {
                ViewUtils.showShortSnack(
                    binding.root,
                    "Please enter a reason."
                )
                return@setOnClickListener
            }

            // update [reasonOrTag] with tag value or reason
            reasonOrTag = if (isOthersSelected == true) {
                reason
            } else {
                tagSelected?.name ?: reason
            }

            //call post api
            viewModel.postReport(
                extras.entityId,
                extras.uuid,
                extras.entityType,
                tagSelected?.id,
                reason
            )
        }
    }

    //get tags
    private fun getReportTags() {
        viewModel.getReportTags()
    }
}