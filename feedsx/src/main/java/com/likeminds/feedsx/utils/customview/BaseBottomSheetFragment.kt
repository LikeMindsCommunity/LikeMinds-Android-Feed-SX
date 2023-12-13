package com.likeminds.feedsx.utils.customview

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.likeminds.feedsx.R
import javax.inject.Inject

abstract class BaseBottomSheetFragment<B : ViewBinding, VM : ViewModel> :
    BottomSheetDialogFragment() {

    private var _binding: B? = null
    protected val binding get() = _binding!!

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    protected lateinit var viewModel: VM

    protected abstract fun getViewModelClass(): Class<VM>?

    protected abstract fun getViewBinding(): B

    protected open val useSharedViewModel = false
    protected open val state = BottomSheetBehavior.STATE_COLLAPSED
    protected open fun setUpViews() {
        //implement this to setup initial views related things
    }
    protected open fun observeData() {
        //implement this to add observers of all live data or flows
    }
    protected open fun receiveExtras() {
        //implement this to receive all the extras from source screen
    }

    protected open fun attachDagger() {
        //implement this to attach screen to dagger sub-component
    }

    protected open fun setPostVariable() {
        //This function can be called in case of set post as variable in any view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        attachDagger()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.LMFeedBottomSheetStyle)
        init()
        receiveExtras()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener {
            try {
                val bottomSheetDialog = dialog as BottomSheetDialog
                val bottomSheet = bottomSheetDialog.findViewById<View>(
                    com.google.android.material.R.id.design_bottom_sheet
                ) as FrameLayout
                BottomSheetBehavior.from(bottomSheet).state = state
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (_binding == null) {
            _binding = getViewBinding()
        }
        setPostVariable()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)
        setUpViews()
        observeData()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun init() {
        if (getViewModelClass() == null) {
            return
        }
        viewModel = if (useSharedViewModel) {
            ViewModelProvider(requireActivity(), viewModelFactory)[getViewModelClass()!!]
        } else {
            ViewModelProvider(this, viewModelFactory)[getViewModelClass()!!]
        }
    }
}