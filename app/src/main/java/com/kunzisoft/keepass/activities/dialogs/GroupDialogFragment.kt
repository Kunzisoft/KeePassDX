/*
 * Copyright 2021 Jeremy Jamet / Kunzisoft.
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
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.UuidUtil
import com.kunzisoft.keepass.view.DateTimeFieldView

class GroupDialogFragment : DatabaseDialogFragment() {

    private var mPopulateIconMethod: ((ImageView, IconImage) -> Unit)? = null
    private var mGroupInfo = GroupInfo()

    private lateinit var iconView: ImageView
    private var mIconColor: Int = 0
    private lateinit var nameTextView: TextView
    private lateinit var notesTextLabelView: TextView
    private lateinit var notesTextView: TextView
    private lateinit var expirationView: DateTimeFieldView
    private lateinit var creationView: TextView
    private lateinit var modificationView: TextView
    private lateinit var uuidContainerView: ViewGroup
    private lateinit var uuidReferenceView: TextView

    override fun onDatabaseRetrieved(database: Database?) {
        super.onDatabaseRetrieved(database)
        mPopulateIconMethod = { imageView, icon ->
            database?.iconDrawableFactory?.assignDatabaseIcon(imageView, icon, mIconColor)
        }
        mPopulateIconMethod?.invoke(iconView, mGroupInfo.icon)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            val root = activity.layoutInflater.inflate(R.layout.fragment_group, null)
            iconView = root.findViewById(R.id.group_icon)
            nameTextView = root.findViewById(R.id.group_name)
            notesTextLabelView = root.findViewById(R.id.group_note_label)
            notesTextView = root.findViewById(R.id.group_note)
            expirationView = root.findViewById(R.id.group_expiration)
            creationView = root.findViewById(R.id.group_created)
            modificationView = root.findViewById(R.id.group_modified)
            uuidContainerView = root.findViewById(R.id.group_UUID_container)
            uuidReferenceView = root.findViewById(R.id.group_UUID_reference)

            // Retrieve the textColor to tint the icon
            val ta = activity.theme.obtainStyledAttributes(intArrayOf(R.attr.colorAccent))
            mIconColor = ta.getColor(0, Color.WHITE)
            ta.recycle()

            if (savedInstanceState != null
                    && savedInstanceState.containsKey(KEY_GROUP_INFO)) {
                mGroupInfo = savedInstanceState.getParcelable(KEY_GROUP_INFO) ?: mGroupInfo
            } else {
                arguments?.apply {
                    if (containsKey(KEY_GROUP_INFO)) {
                        mGroupInfo = getParcelable(KEY_GROUP_INFO) ?: mGroupInfo
                    }
                }
            }

            // populate info in views
            nameTextView.text = mGroupInfo.title
            val notes = mGroupInfo.notes
            if (notes == null || notes.isEmpty()) {
                notesTextLabelView.visibility = View.GONE
                notesTextView.visibility = View.GONE
            } else {
                notesTextView.text = notes
                notesTextLabelView.visibility = View.VISIBLE
                notesTextView.visibility = View.VISIBLE
            }
            expirationView.activation = mGroupInfo.expires
            expirationView.dateTime = mGroupInfo.expiryTime
            creationView.text = mGroupInfo.creationTime.getDateTimeString(resources)
            modificationView.text = mGroupInfo.lastModificationTime.getDateTimeString(resources)
            val uuid = UuidUtil.toHexString(mGroupInfo.id)
            if (uuid == null || uuid.isEmpty()) {
                uuidContainerView.visibility = View.GONE
            } else {
                uuidReferenceView.text = uuid
                uuidContainerView.apply {
                    visibility = if (PreferencesUtil.showUUID(context)) View.VISIBLE else View.GONE
                }
            }

            val builder = AlertDialog.Builder(activity)
            builder.setView(root)
                    .setPositiveButton(android.R.string.ok){ _, _ ->
                        // Do nothing
                    }
            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(KEY_GROUP_INFO, mGroupInfo)
        super.onSaveInstanceState(outState)
    }

    data class Error(val isError: Boolean, val messageId: Int?)

    companion object {
        const val TAG_SHOW_GROUP = "TAG_SHOW_GROUP"
        private const val KEY_GROUP_INFO = "KEY_GROUP_INFO"

        fun launch(groupInfo: GroupInfo): GroupDialogFragment {
            val bundle = Bundle()
            bundle.putParcelable(KEY_GROUP_INFO, groupInfo)
            val fragment = GroupDialogFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}
