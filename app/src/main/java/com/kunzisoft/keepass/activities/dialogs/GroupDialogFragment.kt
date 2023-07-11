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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.TagsAdapter
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.TimeUtil.getDateTimeString
import com.kunzisoft.keepass.utils.UuidUtil
import com.kunzisoft.keepass.utils.getParcelableCompat
import com.kunzisoft.keepass.view.DateTimeFieldView

class GroupDialogFragment : DatabaseDialogFragment() {

    private var mPopulateIconMethod: ((ImageView, IconImage) -> Unit)? = null
    private var mGroupInfo = GroupInfo()

    private lateinit var iconView: ImageView
    private var mIconColor: Int = 0
    private lateinit var nameTextView: TextView
    private lateinit var tagsListView: RecyclerView
    private var tagsAdapter: TagsAdapter? = null
    private lateinit var notesTextLabelView: TextView
    private lateinit var notesTextView: TextView
    private lateinit var expirationView: DateTimeFieldView
    private lateinit var creationView: TextView
    private lateinit var modificationView: TextView
    private lateinit var searchableLabelView: TextView
    private lateinit var searchableView: TextView
    private lateinit var autoTypeLabelView: TextView
    private lateinit var autoTypeView: TextView
    private lateinit var uuidContainerView: ViewGroup
    private lateinit var uuidReferenceView: TextView

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)
        mPopulateIconMethod = { imageView, icon ->
            database?.iconDrawableFactory?.assignDatabaseIcon(imageView, icon, mIconColor)
        }
        mPopulateIconMethod?.invoke(iconView, mGroupInfo.icon)

        if (database?.allowCustomSearchableGroup() == true) {
            searchableLabelView.visibility = View.VISIBLE
            searchableView.visibility = View.VISIBLE
        } else {
            searchableLabelView.visibility = View.GONE
            searchableView.visibility = View.GONE
        }

        // TODO Auto-Type
        /*
        if (database?.allowAutoType() == true) {
            autoTypeLabelView.visibility = View.VISIBLE
            autoTypeView.visibility = View.VISIBLE
        } else {
            autoTypeLabelView.visibility = View.GONE
            autoTypeView.visibility = View.GONE
        }
         */
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            val root = activity.layoutInflater.inflate(R.layout.fragment_group, null)
            iconView = root.findViewById(R.id.group_icon)
            nameTextView = root.findViewById(R.id.group_name)
            tagsListView = root.findViewById(R.id.group_tags_list_view)
            notesTextLabelView = root.findViewById(R.id.group_note_label)
            notesTextView = root.findViewById(R.id.group_note)
            expirationView = root.findViewById(R.id.group_expiration)
            creationView = root.findViewById(R.id.group_created)
            modificationView = root.findViewById(R.id.group_modified)
            searchableLabelView = root.findViewById(R.id.group_searchable_label)
            searchableView = root.findViewById(R.id.group_searchable)
            autoTypeLabelView = root.findViewById(R.id.group_auto_type_label)
            autoTypeView = root.findViewById(R.id.group_auto_type)
            uuidContainerView = root.findViewById(R.id.group_UUID_container)
            uuidReferenceView = root.findViewById(R.id.group_UUID_reference)

            // Retrieve the textColor to tint the icon
            val ta = activity.theme.obtainStyledAttributes(intArrayOf(R.attr.colorSecondary))
            mIconColor = ta.getColor(0, Color.WHITE)
            ta.recycle()

            if (savedInstanceState != null
                    && savedInstanceState.containsKey(KEY_GROUP_INFO)) {
                mGroupInfo = savedInstanceState.getParcelableCompat(KEY_GROUP_INFO) ?: mGroupInfo
            } else {
                arguments?.apply {
                    if (containsKey(KEY_GROUP_INFO)) {
                        mGroupInfo = getParcelableCompat(KEY_GROUP_INFO) ?: mGroupInfo
                    }
                }
            }

            // populate info in views
            val title = mGroupInfo.title
            if (title.isEmpty()) {
                nameTextView.visibility = View.GONE
            } else {
                nameTextView.text = title
                nameTextView.visibility = View.VISIBLE
            }
            tagsAdapter = TagsAdapter(activity)
            tagsListView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = tagsAdapter
            }
            val tags = mGroupInfo.tags
            tagsListView.visibility = if (tags.isEmpty()) View.GONE else View.VISIBLE
            tagsAdapter?.setTags(tags)
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
            searchableView.text = stringFromInheritableBoolean(mGroupInfo.searchable)
            autoTypeView.text = stringFromInheritableBoolean(mGroupInfo.enableAutoType,
                mGroupInfo.defaultAutoTypeSequence)
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

    private fun stringFromInheritableBoolean(enable: Boolean?, value: String? = null): String {
        val valueString = if (value != null && value.isNotEmpty()) " [$value]" else ""
        return when {
            enable == null -> getString(R.string.inherited) + valueString
            enable -> getString(R.string.enable) + valueString
            else -> getString(R.string.disable)
        }
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
