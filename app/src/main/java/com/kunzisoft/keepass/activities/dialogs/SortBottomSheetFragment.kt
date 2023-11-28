package com.kunzisoft.keepass.activities.dialogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.RadioGroup
import androidx.annotation.IdRes
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.SortNodeEnum

class SortBottomSheetFragment : BottomSheetDialogFragment() {

    private var mListener: SortSelectionListener? = null

    private var mSortNodeEnum: SortNodeEnum = SortNodeEnum.DB

    @IdRes
    private var mCheckedId: Int = 0
    private var mGroupsBefore: Boolean = true
    private var mAscending: Boolean = true
    private var mRecycleBinBottom: Boolean = true

    private var recycleBinBottomView: CompoundButton? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mListener = context as SortSelectionListener
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString()
                    + " must implement " + SortSelectionListener::class.java.name)
        }
    }

    override fun onDetach() {
        mListener = null
        super.onDetach()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var recycleBinAllowed = false

        arguments?.apply {
            if (containsKey(SORT_NODE_ENUM_BUNDLE_KEY))
                getString(SORT_NODE_ENUM_BUNDLE_KEY)?.let {
                    mSortNodeEnum = SortNodeEnum.valueOf(it)
                }
            if (containsKey(SORT_ASCENDING_BUNDLE_KEY))
                mAscending = getBoolean(SORT_ASCENDING_BUNDLE_KEY)
            if (containsKey(SORT_GROUPS_BEFORE_BUNDLE_KEY))
                mGroupsBefore = getBoolean(SORT_GROUPS_BEFORE_BUNDLE_KEY)
            if (containsKey(SORT_RECYCLE_BIN_BOTTOM_BUNDLE_KEY)) {
                recycleBinAllowed = true
                mRecycleBinBottom = getBoolean(SORT_RECYCLE_BIN_BOTTOM_BUNDLE_KEY)
            }
        }

        mCheckedId = retrieveViewFromEnum(mSortNodeEnum)

        val rootView = inflater.inflate(R.layout.fragment_sort_selection, container, false)

        val ascendingView = rootView.findViewById<CompoundButton>(R.id.sort_selection_ascending)
        ascendingView.isChecked = mAscending
        ascendingView.setOnCheckedChangeListener { _, isChecked -> mAscending = isChecked }

        val groupsBeforeView =
            rootView.findViewById<CompoundButton>(R.id.sort_selection_groups_before)
        groupsBeforeView.isChecked = mGroupsBefore
        groupsBeforeView.setOnCheckedChangeListener { _, isChecked -> mGroupsBefore = isChecked }

        recycleBinBottomView = rootView.findViewById(R.id.sort_selection_recycle_bin_bottom)
        if (!recycleBinAllowed) {
            recycleBinBottomView?.visibility = View.GONE
        } else {
            // Check if recycle bin at the bottom
            recycleBinBottomView?.isChecked = mRecycleBinBottom
            recycleBinBottomView?.setOnCheckedChangeListener { _, isChecked -> mRecycleBinBottom = isChecked }

            disableRecycleBinBottomOptionIfNaturalOrder()
        }

        val sortSelectionRadioGroupView =
            rootView.findViewById<RadioGroup>(R.id.sort_selection_radio_group)
        sortSelectionRadioGroupView.check(mCheckedId)
        sortSelectionRadioGroupView.setOnCheckedChangeListener { _, checkedId ->
            mSortNodeEnum = retrieveSortEnumFromViewId(checkedId)
            disableRecycleBinBottomOptionIfNaturalOrder()
        }

        rootView.findViewById<View>(R.id.ok).setOnClickListener {
            mListener?.onSortSelected(
                mSortNodeEnum,
                SortNodeEnum.SortNodeParameters(mAscending, mGroupsBefore, mRecycleBinBottom)
            )
            dismiss()
        }

        disableRecycleBinBottomOptionIfNaturalOrder()

        return rootView
    }

    private fun disableRecycleBinBottomOptionIfNaturalOrder() {
        // Disable recycle bin if natural order
        recycleBinBottomView?.isEnabled = mSortNodeEnum != SortNodeEnum.DB
    }

    @IdRes
    private fun retrieveViewFromEnum(sortNodeEnum: SortNodeEnum): Int {
        return when (sortNodeEnum) {
            SortNodeEnum.DB -> R.id.sort_selection_db
            SortNodeEnum.TITLE -> R.id.sort_selection_title
            SortNodeEnum.USERNAME -> R.id.sort_selection_username
            SortNodeEnum.CREATION_TIME -> R.id.sort_selection_creation_time
            SortNodeEnum.LAST_MODIFY_TIME -> R.id.sort_selection_last_modify_time
            SortNodeEnum.LAST_ACCESS_TIME -> R.id.sort_selection_last_access_time
        }
    }

    private fun retrieveSortEnumFromViewId(@IdRes checkedId: Int): SortNodeEnum {
        return when (checkedId) {
            R.id.sort_selection_db -> SortNodeEnum.DB
            R.id.sort_selection_title -> SortNodeEnum.TITLE
            R.id.sort_selection_username -> SortNodeEnum.USERNAME
            R.id.sort_selection_creation_time -> SortNodeEnum.CREATION_TIME
            R.id.sort_selection_last_modify_time -> SortNodeEnum.LAST_MODIFY_TIME
            R.id.sort_selection_last_access_time -> SortNodeEnum.LAST_ACCESS_TIME
            else -> SortNodeEnum.TITLE
        }
    }

    interface SortSelectionListener {
        fun onSortSelected(
            sortNodeEnum: SortNodeEnum,
            sortNodeParameters: SortNodeEnum.SortNodeParameters
        )
    }

    companion object {
        private const val SORT_NODE_ENUM_BUNDLE_KEY = "SORT_NODE_ENUM_BUNDLE_KEY"
        private const val SORT_ASCENDING_BUNDLE_KEY = "SORT_ASCENDING_BUNDLE_KEY"
        private const val SORT_GROUPS_BEFORE_BUNDLE_KEY = "SORT_GROUPS_BEFORE_BUNDLE_KEY"
        private const val SORT_RECYCLE_BIN_BOTTOM_BUNDLE_KEY = "SORT_RECYCLE_BIN_BOTTOM_BUNDLE_KEY"

        private fun buildBundle(
            sortNodeEnum: SortNodeEnum,
            ascending: Boolean,
            groupsBefore: Boolean
        ): Bundle {
            val bundle = Bundle()
            bundle.putString(SORT_NODE_ENUM_BUNDLE_KEY, sortNodeEnum.name)
            bundle.putBoolean(SORT_ASCENDING_BUNDLE_KEY, ascending)
            bundle.putBoolean(SORT_GROUPS_BEFORE_BUNDLE_KEY, groupsBefore)
            return bundle
        }

        fun getInstance(
            sortNodeEnum: SortNodeEnum,
            ascending: Boolean,
            groupsBefore: Boolean
        ): SortBottomSheetFragment {
            val bundle = buildBundle(sortNodeEnum, ascending, groupsBefore)
            val fragment = SortBottomSheetFragment()
            fragment.arguments = bundle
            return fragment
        }

        fun getInstance(
            sortNodeEnum: SortNodeEnum,
            ascending: Boolean,
            groupsBefore: Boolean,
            recycleBinBottom: Boolean
        ): SortBottomSheetFragment {
            val bundle = buildBundle(sortNodeEnum, ascending, groupsBefore)
            bundle.putBoolean(SORT_RECYCLE_BIN_BOTTOM_BUNDLE_KEY, recycleBinBottom)
            val fragment = SortBottomSheetFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}
