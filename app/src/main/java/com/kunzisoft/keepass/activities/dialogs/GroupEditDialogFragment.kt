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
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.IconPickerActivity
import com.kunzisoft.keepass.activities.dialogs.GroupEditDialogFragment.EditGroupDialogAction.CREATION
import com.kunzisoft.keepass.activities.dialogs.GroupEditDialogFragment.EditGroupDialogAction.UPDATE
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.icons.assignDatabaseIcon
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.view.ExpirationView
import org.joda.time.DateTime

class GroupEditDialogFragment : DialogFragment() {

    private var mDatabase: Database? = null

    private var mEditGroupListener: EditGroupListener? = null

    private var mEditGroupDialogAction = EditGroupDialogAction.NONE
    private var mGroupInfo = GroupInfo()

    private lateinit var iconButtonView: ImageView
    private var iconColor: Int = 0
    private lateinit var nameTextLayoutView: TextInputLayout
    private lateinit var nameTextView: TextView
    private lateinit var notesTextLayoutView: TextInputLayout
    private lateinit var notesTextView: TextView
    private lateinit var expirationView: ExpirationView

    enum class EditGroupDialogAction {
        CREATION, UPDATE, NONE;

        companion object {
            fun getActionFromOrdinal(ordinal: Int): EditGroupDialogAction {
                return values()[ordinal]
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mEditGroupListener = context as EditGroupListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(context.toString()
                    + " must implement " + GroupEditDialogFragment::class.java.name)
        }
    }

    override fun onDetach() {
        mEditGroupListener = null
        super.onDetach()
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

            // Retrieve the textColor to tint the icon
            val ta = activity.theme.obtainStyledAttributes(intArrayOf(android.R.attr.textColor))
            iconColor = ta.getColor(0, Color.WHITE)
            ta.recycle()

            // Init elements
            mDatabase = Database.getInstance()

            if (savedInstanceState != null
                    && savedInstanceState.containsKey(KEY_ACTION_ID)
                    && savedInstanceState.containsKey(KEY_GROUP_INFO)) {
                mEditGroupDialogAction = EditGroupDialogAction.getActionFromOrdinal(savedInstanceState.getInt(KEY_ACTION_ID))
                mGroupInfo = savedInstanceState.getParcelable(KEY_GROUP_INFO) ?: mGroupInfo
            } else {
                arguments?.apply {
                    if (containsKey(KEY_ACTION_ID))
                        mEditGroupDialogAction = EditGroupDialogAction.getActionFromOrdinal(getInt(KEY_ACTION_ID))
                    if (mEditGroupDialogAction === CREATION)
                        mGroupInfo.notes = ""
                    if (containsKey(KEY_GROUP_INFO)) {
                        mGroupInfo = getParcelable(KEY_GROUP_INFO) ?: mGroupInfo
                    }
                }
            }

            // populate info in views
            populateInfoToViews()
            expirationView.setOnDateClickListener = {
                expirationView.expiryTime.date.let { expiresDate ->
                    val dateTime = DateTime(expiresDate)
                    val defaultYear = dateTime.year
                    val defaultMonth = dateTime.monthOfYear-1
                    val defaultDay = dateTime.dayOfMonth
                    DatePickerFragment.getInstance(defaultYear, defaultMonth, defaultDay)
                            .show(parentFragmentManager, "DatePickerFragment")
                }
            }

            val builder = AlertDialog.Builder(activity)
            builder.setView(root)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        retrieveGroupInfoFromViews()
                        mEditGroupListener?.cancelEditGroup(
                                mEditGroupDialogAction,
                                mGroupInfo)
                    }

            iconButtonView.setOnClickListener { _ ->
                IconPickerActivity.launch(activity)
            }

            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        // To prevent auto dismiss
        val d = dialog as AlertDialog?
        if (d != null) {
            val positiveButton = d.getButton(Dialog.BUTTON_POSITIVE) as Button
            positiveButton.setOnClickListener {
                retrieveGroupInfoFromViews()
                if (isValid()) {
                    mEditGroupListener?.approveEditGroup(
                            mEditGroupDialogAction,
                            mGroupInfo)
                    d.dismiss()
                }
            }
        }
    }

    fun getExpiryTime(): DateInstant {
        retrieveGroupInfoFromViews()
        return mGroupInfo.expiryTime
    }

    fun setExpiryTime(expiryTime: DateInstant) {
        mGroupInfo.expiryTime = expiryTime
        populateInfoToViews()
    }

    private fun populateInfoToViews() {
        assignIconView()
        nameTextView.text = mGroupInfo.title
        notesTextLayoutView.visibility = if (mGroupInfo.notes == null) View.GONE else View.VISIBLE
        mGroupInfo.notes?.let {
            notesTextView.text = it
        }
        expirationView.expires = mGroupInfo.expires
        expirationView.expiryTime = mGroupInfo.expiryTime
    }

    private fun retrieveGroupInfoFromViews() {
        mGroupInfo.title = nameTextView.text.toString()
        // Only if there
        val newNotes = notesTextView.text.toString()
        if (newNotes.isNotEmpty()) {
            mGroupInfo.notes = newNotes
        }
        mGroupInfo.expires = expirationView.expires
        mGroupInfo.expiryTime = expirationView.expiryTime
    }

    private fun assignIconView() {
        if (mDatabase?.iconDrawableFactory != null) {
            iconButtonView.assignDatabaseIcon(mDatabase?.iconDrawableFactory!!, mGroupInfo.icon, iconColor)
        }
    }

    fun setIcon(icon: IconImage) {
        mGroupInfo.icon = icon
        assignIconView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        retrieveGroupInfoFromViews()
        outState.putInt(KEY_ACTION_ID, mEditGroupDialogAction.ordinal)
        outState.putParcelable(KEY_GROUP_INFO, mGroupInfo)
        super.onSaveInstanceState(outState)
    }

    private fun isValid(): Boolean {
        if (nameTextView.text.toString().isEmpty()) {
            nameTextLayoutView.error = getString(R.string.error_no_name)
            return false
        }
        return true
    }

    interface EditGroupListener {
        fun approveEditGroup(action: EditGroupDialogAction,
                             groupInfo: GroupInfo)
        fun cancelEditGroup(action: EditGroupDialogAction,
                            groupInfo: GroupInfo)
    }

    companion object {

        const val TAG_CREATE_GROUP = "TAG_CREATE_GROUP"
        const val KEY_ACTION_ID = "KEY_ACTION_ID"
        const val KEY_GROUP_INFO = "KEY_GROUP_INFO"

        fun build(): GroupEditDialogFragment {
            val bundle = Bundle()
            bundle.putInt(KEY_ACTION_ID, CREATION.ordinal)
            val fragment = GroupEditDialogFragment()
            fragment.arguments = bundle
            return fragment
        }

        fun build(groupInfo: GroupInfo): GroupEditDialogFragment {
            val bundle = Bundle()
            bundle.putInt(KEY_ACTION_ID, UPDATE.ordinal)
            bundle.putParcelable(KEY_GROUP_INFO, groupInfo)
            val fragment = GroupEditDialogFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}
