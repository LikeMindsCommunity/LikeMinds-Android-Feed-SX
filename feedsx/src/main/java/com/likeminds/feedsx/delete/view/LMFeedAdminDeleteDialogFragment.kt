package com.likeminds.feedsx.delete.view

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentManager
import com.likeminds.feedsx.R
import com.likeminds.feedsx.branding.model.LMFeedBranding
import com.likeminds.feedsx.databinding.LmFeedDialogFragmentAdminDeleteBinding
import com.likeminds.feedsx.delete.model.*
import com.likeminds.feedsx.utils.ExtrasUtil
import com.likeminds.feedsx.utils.ValueUtils.pluralizeOrCapitalize
import com.likeminds.feedsx.utils.customview.BaseDialogFragment
import com.likeminds.feedsx.utils.emptyExtrasException
import com.likeminds.feedsx.utils.pluralize.model.WordAction

//when cm deletes others user post
class LMFeedAdminDeleteDialogFragment :
    BaseDialogFragment<LmFeedDialogFragmentAdminDeleteBinding>(),
    LMFeedReasonChooseDialog.ReasonChooseDialogListener {

    companion object {
        private const val TAG = "DeleteContentDialogFragment"
        private const val ARG_DELETE_EXTRAS = "ARG_DELETE_EXTRAS"

        @JvmStatic
        fun showDialog(
            supportFragmentManager: FragmentManager, deleteExtras: DeleteExtras
        ) {
            LMFeedAdminDeleteDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_DELETE_EXTRAS, deleteExtras)
                }
            }.show(supportFragmentManager, TAG)
        }
    }

    private var deleteDialogListener: DeleteDialogListener? = null

    private lateinit var deleteExtras: DeleteExtras

    override fun getViewBinding(): LmFeedDialogFragmentAdminDeleteBinding {
        return LmFeedDialogFragmentAdminDeleteBinding.inflate(layoutInflater)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            deleteDialogListener = parentFragment as DeleteDialogListener?
        } catch (e: ClassCastException) {
            throw ClassCastException("Calling fragment must implement DeleteDialogListener interface")
        }
    }

    override fun receiveExtras() {
        super.receiveExtras()
        arguments?.let {
            deleteExtras = ExtrasUtil.getParcelable(
                it,
                ARG_DELETE_EXTRAS,
                DeleteExtras::class.java
            ) ?: throw emptyExtrasException(TAG)
        }
    }

    override fun setUpViews() {
        super.setUpViews()
        initView()
        initializeListeners()
    }

    // sets data as per content type [COMMENT/POST]
    private fun initView() {
        if (deleteExtras.entityType == DELETE_TYPE_POST) {
            binding.tvTitle.text = getString(
                R.string.delete_s_question,
                deleteExtras.postAsVariable.pluralizeOrCapitalize(WordAction.ALL_SMALL_SINGULAR)
            )
            binding.tvDescription.text = getString(
                R.string.delete_s_message,
                deleteExtras.postAsVariable.pluralizeOrCapitalize(WordAction.ALL_SMALL_SINGULAR)
            )
        } else {
            binding.tvTitle.text = getString(R.string.delete_comment_question)
            binding.tvDescription.text = getString(R.string.delete_comment_message)
        }
    }

    // sets listeners
    private fun initializeListeners() {
        binding.cvReason.setOnClickListener {
            LMFeedReasonChooseDialog.newInstance(childFragmentManager)
        }

        // submits post delete request with reason/tag and triggers callback
        binding.tvConfirm.setOnClickListener {
            val data = binding.reasonData ?: return@setOnClickListener
            val tag = data.value
            val reason = tag.ifEmpty {
                binding.etOtherReason.text.toString()
            }
            if (data.value.isNotEmpty() && isTagOthersAndReasonIsEmpty(tag, reason)) {
                return@setOnClickListener
            }

            deleteDialogListener?.adminDelete(
                deleteExtras, reason
            )
            dismiss()
        }

        // dismisses the delete dialog
        binding.tvCancel.setOnClickListener {
            dismiss()
        }

        // sets listener to the reason edit text
        binding.etOtherReason.doAfterTextChanged {
            val reason = it.toString().trim()
            handleConfirmButton(reason.isNotEmpty())
        }
    }

    //checks whether a tag is selected and "Others" and reason is also Empty
    private fun isTagOthersAndReasonIsEmpty(tag: String, reason: String): Boolean {
        return tag == "Others" && reason.isEmpty()
    }

    // handles confirm button (enabled/disabled)
    private fun handleConfirmButton(isEnabled: Boolean) {
        binding.tvConfirm.isEnabled = isEnabled
        if (isEnabled) {
            binding.tvConfirm.setTextColor(LMFeedBranding.getButtonsColor())
        } else {
            binding.tvConfirm.setTextColor(
                ContextCompat.getColor(
                    requireContext(), R.color.black_20
                )
            )
        }
    }

    // callback when a reason is selected from bottom sheet
    override fun onReasonSelected(viewData: ReasonChooseViewData) {
        binding.reasonData = viewData
        binding.tvReason.text = viewData.value
        binding.etOtherReason.setText("")

        if (viewData.value == "Others") {
            binding.cvOtherReason.visibility = View.VISIBLE
            handleConfirmButton(false)
        } else {
            binding.cvOtherReason.visibility = View.GONE
            handleConfirmButton(true)
        }
    }

    interface DeleteDialogListener {
        fun adminDelete(
            deleteExtras: DeleteExtras, reason: String
        )
    }
}