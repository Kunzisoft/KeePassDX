/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import android.widget.RadioGroup
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.SortNodeEnum
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.viewmodels.GroupViewModel

class SortDialogFragment : DatabaseDialogFragment() {

    private val mGroupViewModel: GroupViewModel by activityViewModels()

    private var mSortNodeEnum: SortNodeEnum = SortNodeEnum.DB
    @IdRes
    private var mCheckedId: Int = 0
    private var mGroupsBefore: Boolean = true
    private var mAscending: Boolean = true
    private var mRecycleBinBottom: Boolean = true
    private var mRecycleBinAllowed: Boolean = false

    private var recycleBinBottomView: CompoundButton? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            val builder = AlertDialog.Builder(activity)

            mSortNodeEnum = PreferencesUtil.getListSort(activity)
            mAscending = PreferencesUtil.getAscendingSort(activity)
            mGroupsBefore = PreferencesUtil.getGroupsBeforeSort(activity)
            mRecycleBinBottom = PreferencesUtil.getRecycleBinBottomSort(activity)

            mCheckedId = retrieveViewFromEnum(mSortNodeEnum)

            val rootView = activity.layoutInflater.inflate(R.layout.fragment_sort_selection, null)
            builder.setTitle(R.string.sort_menu)
            builder.setView(rootView)
                    // Add action buttons
                    .setPositiveButton(android.R.string.ok
                    ) { _, _ ->
                        mGroupViewModel.onSortSelected(
                            sortNode = mSortNodeEnum,
                            sortNodeParameters = SortNodeEnum.SortNodeParameters(
                                ascending = mAscending,
                                groupsBefore = mGroupsBefore,
                                recycleBinBottom = mRecycleBinBottom
                            )
                        )
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }

            val ascendingView = rootView.findViewById<CompoundButton>(R.id.sort_selection_ascending)
            // Check if ascending or descending
            ascendingView.isChecked = mAscending
            ascendingView.setOnCheckedChangeListener { _, isChecked -> mAscending = isChecked }

            val groupsBeforeView = rootView.findViewById<CompoundButton>(R.id.sort_selection_groups_before)
            // Check if groups before
            groupsBeforeView.isChecked = mGroupsBefore
            groupsBeforeView.setOnCheckedChangeListener { _, isChecked -> mGroupsBefore = isChecked }

            recycleBinBottomView = rootView.findViewById(R.id.sort_selection_recycle_bin_bottom)
            if (!mRecycleBinAllowed) {
                recycleBinBottomView?.visibility = View.GONE
            } else {
                // Check if recycle bin at the bottom
                recycleBinBottomView?.isChecked = mRecycleBinBottom
                recycleBinBottomView?.setOnCheckedChangeListener { _, isChecked -> mRecycleBinBottom = isChecked }

                disableRecycleBinBottomOptionIfNaturalOrder()
            }

            val sortSelectionRadioGroupView = rootView.findViewById<RadioGroup>(R.id.sort_selection_radio_group)
            // Check value by default
            sortSelectionRadioGroupView.check(mCheckedId)
            sortSelectionRadioGroupView.setOnCheckedChangeListener { _, checkedId ->
                mSortNodeEnum = retrieveSortEnumFromViewId(checkedId)
                disableRecycleBinBottomOptionIfNaturalOrder()
            }

            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase) {
        super.onDatabaseRetrieved(database)
        mRecycleBinAllowed = database.isRecycleBinEnabled
        disableRecycleBinBottomOptionIfNaturalOrder()
    }

    private fun disableRecycleBinBottomOptionIfNaturalOrder() {
        // Disable recycle bin if natural order
        recycleBinBottomView?.isVisible = mRecycleBinAllowed && mSortNodeEnum != SortNodeEnum.DB
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
        // Change enum
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

    companion object {

        fun getInstance(): SortDialogFragment {
            val fragment = SortDialogFragment()
            return fragment
        }
    }
}
