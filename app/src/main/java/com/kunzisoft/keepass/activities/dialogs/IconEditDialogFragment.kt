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
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.utils.getParcelableCompat
import com.kunzisoft.keepass.viewmodels.IconPickerViewModel

class IconEditDialogFragment : DatabaseDialogFragment() {

    private val mIconPickerViewModel: IconPickerViewModel by activityViewModels()

    private var mPopulateIconMethod: ((ImageView, IconImage) -> Unit)? = null
    private lateinit var iconView: ImageView
    private lateinit var nameTextLayoutView: TextInputLayout
    private lateinit var nameTextView: TextView

    private var mCustomIcon: IconImageCustom? = null

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)
        mPopulateIconMethod = { imageView, icon ->
            database?.iconDrawableFactory?.assignDatabaseIcon(imageView, icon)
        }
        mCustomIcon?.let { customIcon ->
            populateViewsWithCustomIcon(customIcon)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            val root = activity.layoutInflater.inflate(R.layout.fragment_icon_edit, null)
            iconView = root.findViewById(R.id.icon_edit_image)
            nameTextLayoutView = root.findViewById(R.id.icon_edit_name_container)
            nameTextView = root.findViewById(R.id.icon_edit_name)

            if (savedInstanceState != null
                    && savedInstanceState.containsKey(KEY_CUSTOM_ICON_ID)) {
                mCustomIcon = savedInstanceState.getParcelableCompat(KEY_CUSTOM_ICON_ID) ?: mCustomIcon
            } else {
                arguments?.apply {
                    if (containsKey(KEY_CUSTOM_ICON_ID)) {
                        mCustomIcon = getParcelableCompat(KEY_CUSTOM_ICON_ID) ?: mCustomIcon
                    }
                }
            }

            val builder = AlertDialog.Builder(activity)
            builder.setView(root)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        retrieveIconInfoFromViews()
                        mCustomIcon?.let { customIcon ->
                            mIconPickerViewModel.updateCustomIcon(
                                IconPickerViewModel.IconCustomState(customIcon, false)
                            )
                        }
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        // Do nothing
                        mIconPickerViewModel.updateCustomIcon(
                            IconPickerViewModel.IconCustomState(null, false)
                        )
                    }

            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    private fun populateViewsWithCustomIcon(customIcon: IconImageCustom) {
        mPopulateIconMethod?.invoke(iconView, customIcon.getIconImageToDraw())
        nameTextView.text = customIcon.name
    }

    private fun retrieveIconInfoFromViews() {
        mCustomIcon?.name = nameTextView.text.toString()
        mCustomIcon?.lastModificationTime = DateInstant()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        retrieveIconInfoFromViews()
        outState.putParcelable(KEY_CUSTOM_ICON_ID, mCustomIcon)
        super.onSaveInstanceState(outState)
    }

    companion object {

        const val TAG_UPDATE_ICON = "TAG_UPDATE_ICON"
        const val KEY_CUSTOM_ICON_ID = "KEY_CUSTOM_ICON_ID"

        fun update(customIcon: IconImageCustom): IconEditDialogFragment {
            val bundle = Bundle()
            bundle.putParcelable(KEY_CUSTOM_ICON_ID, IconImageCustom(customIcon))
            val fragment = IconEditDialogFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}
