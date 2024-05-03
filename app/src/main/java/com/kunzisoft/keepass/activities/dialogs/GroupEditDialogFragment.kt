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
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.GroupEditDialogFragment.EditGroupDialogAction.CREATION
import com.kunzisoft.keepass.activities.dialogs.GroupEditDialogFragment.EditGroupDialogAction.NONE
import com.kunzisoft.keepass.activities.dialogs.GroupEditDialogFragment.EditGroupDialogAction.UPDATE
import com.kunzisoft.keepass.adapters.TagsProposalAdapter
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.utils.getParcelableCompat
import com.kunzisoft.keepass.view.DateTimeEditFieldView
import com.kunzisoft.keepass.view.InheritedCompletionView
import com.kunzisoft.keepass.view.TagsCompletionView
import com.kunzisoft.keepass.viewmodels.GroupEditViewModel
import com.tokenautocomplete.FilteredArrayAdapter
import org.joda.time.DateTime

class GroupEditDialogFragment : DatabaseDialogFragment() {

    private val mGroupEditViewModel: GroupEditViewModel by activityViewModels()

    private var mPopulateIconMethod: ((ImageView, IconImage) -> Unit)? = null
    private var mEditGroupDialogAction = NONE
    private var mGroupInfo = GroupInfo()
    private var mGroupNamesNotAllowed: List<String>? = null

    private lateinit var iconButtonView: ImageView
    private var mIconColor: Int = 0
    private lateinit var nameTextLayoutView: TextInputLayout
    private lateinit var nameTextView: TextView
    private lateinit var notesTextLayoutView: TextInputLayout
    private lateinit var notesTextView: TextView
    private lateinit var expirationView: DateTimeEditFieldView
    private lateinit var searchableContainerView: TextInputLayout
    private lateinit var searchableView: InheritedCompletionView
    private lateinit var autoTypeContainerView: ViewGroup
    private lateinit var autoTypeInheritedView: InheritedCompletionView
    private lateinit var autoTypeSequenceView: TextView
    private lateinit var tagsContainerView: TextInputLayout
    private lateinit var tagsCompletionView: TagsCompletionView
    private var tagsAdapter: FilteredArrayAdapter<String>? = null

    enum class EditGroupDialogAction {
        CREATION, UPDATE, NONE;

        companion object {
            fun getActionFromOrdinal(ordinal: Int): EditGroupDialogAction {
                return values()[ordinal]
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mGroupEditViewModel.onIconSelected.observe(this) { iconImage ->
            mGroupInfo.icon = iconImage
            mPopulateIconMethod?.invoke(iconButtonView, mGroupInfo.icon)
        }

        mGroupEditViewModel.onDateSelected.observe(this) { dateMilliseconds ->
            // Save the date
            mGroupInfo.expiryTime = DateInstant(
                DateTime(mGroupInfo.expiryTime.date)
                    .withMillis(dateMilliseconds)
                    .toDate())
            expirationView.dateTime = mGroupInfo.expiryTime
            if (expirationView.dateTime.type == DateInstant.Type.DATE_TIME) {
                val instantTime = DateInstant(mGroupInfo.expiryTime.date, DateInstant.Type.TIME)
                // Trick to recall selection with time
                mGroupEditViewModel.requestDateTimeSelection(instantTime)
            }
        }

        mGroupEditViewModel.onTimeSelected.observe(this) { viewModelTime ->
            // Save the time
            mGroupInfo.expiryTime = DateInstant(
                DateTime(mGroupInfo.expiryTime.date)
                    .withHourOfDay(viewModelTime.hours)
                    .withMinuteOfHour(viewModelTime.minutes)
                    .toDate(), mGroupInfo.expiryTime.type)
            expirationView.dateTime = mGroupInfo.expiryTime
        }

        mGroupEditViewModel.groupNamesNotAllowed.observe(this) { namesNotAllowed ->
            this.mGroupNamesNotAllowed = namesNotAllowed
        }
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)

        mPopulateIconMethod = { imageView, icon ->
            database?.iconDrawableFactory?.assignDatabaseIcon(imageView, icon, mIconColor)
        }
        mPopulateIconMethod?.invoke(iconButtonView, mGroupInfo.icon)

        searchableContainerView.visibility = if (database?.allowCustomSearchableGroup() == true) {
            View.VISIBLE
        } else {
            View.GONE
        }

        if (database?.allowAutoType() == true) {
            autoTypeContainerView.visibility = View.VISIBLE
        } else {
            autoTypeContainerView.visibility = View.GONE
        }

        tagsAdapter = TagsProposalAdapter(requireContext(), database?.tagPool)
        tagsCompletionView.apply {
            threshold = 1
            setAdapter(tagsAdapter)
        }
        tagsContainerView.visibility = if (database?.allowTags() == true) View.VISIBLE else View.GONE
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            val root = activity.layoutInflater.inflate(R.layout.fragment_group_edit, null)
            iconButtonView = root.findViewById(R.id.group_edit_icon_button)
            nameTextLayoutView = root.findViewById(R.id.group_edit_name_container)
            nameTextView = root.findViewById(R.id.group_edit_name)
            notesTextLayoutView = root.findViewById(R.id.group_edit_note_container)
            notesTextView = root.findViewById(R.id.group_edit_note)
            expirationView = root.findViewById(R.id.group_edit_expiration)
            searchableContainerView = root.findViewById(R.id.group_edit_searchable_container)
            searchableView = root.findViewById(R.id.group_edit_searchable)
            autoTypeContainerView = root.findViewById(R.id.group_edit_auto_type_container)
            autoTypeInheritedView = root.findViewById(R.id.group_edit_auto_type_inherited)
            autoTypeSequenceView = root.findViewById(R.id.group_edit_auto_type_sequence)
            tagsContainerView = root.findViewById(R.id.group_tags_label)
            tagsCompletionView = root.findViewById(R.id.group_tags_completion_view)

            // Retrieve the textColor to tint the icon
            val ta = activity.theme.obtainStyledAttributes(intArrayOf(android.R.attr.textColor))
            mIconColor = ta.getColor(0, Color.WHITE)
            ta.recycle()

            if (savedInstanceState != null
                    && savedInstanceState.containsKey(KEY_ACTION_ID)
                    && savedInstanceState.containsKey(KEY_GROUP_INFO)) {
                mEditGroupDialogAction = EditGroupDialogAction.getActionFromOrdinal(savedInstanceState.getInt(KEY_ACTION_ID))
                mGroupInfo = savedInstanceState.getParcelableCompat(KEY_GROUP_INFO) ?: mGroupInfo
            } else {
                arguments?.apply {
                    if (containsKey(KEY_ACTION_ID))
                        mEditGroupDialogAction = EditGroupDialogAction.getActionFromOrdinal(getInt(KEY_ACTION_ID))
                    if (containsKey(KEY_GROUP_INFO)) {
                        mGroupInfo = getParcelableCompat(KEY_GROUP_INFO) ?: mGroupInfo
                    }
                }
            }

            // populate info in views
            populateInfoToViews(mGroupInfo)

            iconButtonView.setOnClickListener { _ ->
                mGroupEditViewModel.requestIconSelection(mGroupInfo.icon)
            }
            expirationView.setOnDateClickListener = { dateInstant ->
                mGroupEditViewModel.requestDateTimeSelection(dateInstant)
            }

            val builder = AlertDialog.Builder(activity)
            builder.setView(root)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        // Do nothing
                    }

            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        // To prevent auto dismiss
        val alertDialog = dialog as AlertDialog?
        if (alertDialog != null) {
            val positiveButton = alertDialog.getButton(Dialog.BUTTON_POSITIVE) as Button
            positiveButton.setOnClickListener {
                retrieveGroupInfoFromViews()
                if (isValid()) {
                    when (mEditGroupDialogAction) {
                        CREATION ->
                            mGroupEditViewModel.approveGroupCreation(mGroupInfo)
                        UPDATE ->
                            mGroupEditViewModel.approveGroupUpdate(mGroupInfo)
                        NONE -> {}
                    }
                    alertDialog.dismiss()
                }
            }
        }
    }

    private fun populateInfoToViews(groupInfo: GroupInfo) {
        mGroupEditViewModel.selectIcon(groupInfo.icon)
        nameTextView.text = groupInfo.title
        notesTextLayoutView.visibility = if (groupInfo.notes == null) View.GONE else View.VISIBLE
        groupInfo.notes?.let {
            notesTextView.text = it
        }
        expirationView.activation = groupInfo.expires
        expirationView.dateTime = groupInfo.expiryTime

        // Set searchable
        searchableView.setValue(groupInfo.searchable)
        // Set auto-type
        autoTypeInheritedView.setValue(groupInfo.enableAutoType)
        autoTypeSequenceView.text = groupInfo.defaultAutoTypeSequence
        // Set Tags
        groupInfo.tags.let { tags ->
            tagsCompletionView.setText("")
            for (i in 0 until tags.size()) {
                tagsCompletionView.addObjectSync(tags.get(i))
            }
        }
    }

    private fun retrieveGroupInfoFromViews() {
        mGroupInfo.title = nameTextView.text.toString()
        // Only if there
        val newNotes = notesTextView.text.toString()
        if (newNotes.isNotEmpty()) {
            mGroupInfo.notes = newNotes
        }
        mGroupInfo.expires = expirationView.activation
        mGroupInfo.expiryTime = expirationView.dateTime
        mGroupInfo.searchable = searchableView.getValue()
        mGroupInfo.enableAutoType = autoTypeInheritedView.getValue()
        mGroupInfo.defaultAutoTypeSequence = autoTypeSequenceView.text.toString()
        mGroupInfo.tags = tagsCompletionView.getTags()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        retrieveGroupInfoFromViews()
        outState.putInt(KEY_ACTION_ID, mEditGroupDialogAction.ordinal)
        outState.putParcelable(KEY_GROUP_INFO, mGroupInfo)
        super.onSaveInstanceState(outState)
    }

    private fun isValid(): Boolean {
        val name = nameTextView.text.toString()
        val error = when {
            name.isEmpty() -> {
                Error(true, R.string.error_no_name)
            }
            mGroupNamesNotAllowed == null -> {
                Error(true, R.string.error_word_reserved)
            }
            mGroupNamesNotAllowed?.find { it.equals(name, ignoreCase = true) } != null -> {
                Error(true, R.string.error_word_reserved)
            }
            else -> {
                Error(false, null)
            }
        }
        error.messageId?.let { messageId ->
            nameTextLayoutView.error = getString(messageId)
        } ?: kotlin.run {
            nameTextLayoutView.error = null
        }
        return !error.isError
    }

    data class Error(val isError: Boolean, val messageId: Int?)

    companion object {

        const val TAG_CREATE_GROUP = "TAG_CREATE_GROUP"
        private const val KEY_ACTION_ID = "KEY_ACTION_ID"
        private const val KEY_GROUP_INFO = "KEY_GROUP_INFO"

        fun create(groupInfo: GroupInfo): GroupEditDialogFragment {
            val bundle = Bundle()
            bundle.putInt(KEY_ACTION_ID, CREATION.ordinal)
            bundle.putParcelable(KEY_GROUP_INFO, groupInfo)
            val fragment = GroupEditDialogFragment()
            fragment.arguments = bundle
            return fragment
        }

        fun update(groupInfo: GroupInfo): GroupEditDialogFragment {
            val bundle = Bundle()
            bundle.putInt(KEY_ACTION_ID, UPDATE.ordinal)
            bundle.putParcelable(KEY_GROUP_INFO, groupInfo)
            val fragment = GroupEditDialogFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}
