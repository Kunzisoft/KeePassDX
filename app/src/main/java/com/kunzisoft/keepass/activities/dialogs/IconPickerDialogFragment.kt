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
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.icons.IconPack
import com.kunzisoft.keepass.icons.IconPackChooser


class IconPickerDialogFragment : DialogFragment() {

    private var iconPickerListener: IconPickerListener? = null
    private var iconPack: IconPack? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            iconPickerListener = context as IconPickerListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(context.toString()
                    + " must implement " + IconPickerListener::class.java.name)
        }
    }

    override fun onDetach() {
        iconPickerListener = null
        super.onDetach()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            val builder = AlertDialog.Builder(activity)

            iconPack = IconPackChooser.getSelectedIconPack(requireContext())

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            val root = activity.layoutInflater.inflate(R.layout.fragment_icon_picker, null)
            builder.setView(root)

            val currIconGridView = root.findViewById<GridView>(R.id.IconGridView)
            currIconGridView.adapter = ImageAdapter(activity)

            currIconGridView.setOnItemClickListener { _, _, position, _ ->
                val bundle = Bundle()
                bundle.putParcelable(KEY_ICON_STANDARD, IconImageStandard(position))
                iconPickerListener?.iconPicked(bundle)
                dismiss()
            }

            builder.setNegativeButton(android.R.string.cancel) { _, _ -> this@IconPickerDialogFragment.dialog?.cancel() }

            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    inner class ImageAdapter internal constructor(private val context: Context) : BaseAdapter() {

        override fun getCount(): Int {
            return iconPack?.numberOfIcons() ?: 0
        }

        override fun getItem(position: Int): Any? {
            return null
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val currentView: View = convertView
                    ?: (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
                        .inflate(R.layout.item_icon, parent, false)

            iconPack?.let { iconPack ->
                val iconImageView = currentView.findViewById<ImageView>(R.id.icon_image)
                iconImageView.setImageResource(iconPack.iconToResId(position))

                // Assign color if icons are tintable
                if (iconPack.tintable()) {
                    // Retrieve the textColor to tint the icon
                    val ta = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.textColor))
                    ImageViewCompat.setImageTintList(iconImageView, ColorStateList.valueOf(ta.getColor(0, Color.BLACK)))
                    ta.recycle()
                }
            }

            return currentView
        }
    }

    interface IconPickerListener {
        fun iconPicked(bundle: Bundle)
    }

    companion object {

        private const val KEY_ICON_STANDARD = "KEY_ICON_STANDARD"

        fun getIconStandardFromBundle(bundle: Bundle): IconImageStandard? {
            return bundle.getParcelable(KEY_ICON_STANDARD)
        }

        fun launch(activity: FragmentActivity) {
            // Create an instance of the dialog fragment and show it
            val dialog = IconPickerDialogFragment()
            dialog.show(activity.supportFragmentManager, "IconPickerDialogFragment")
        }
    }
}
