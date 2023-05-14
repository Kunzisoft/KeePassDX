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
package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.CompoundButton
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import com.kunzisoft.androidclearchroma.view.ChromaColorView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase

class DatabaseColorPreferenceDialogFragmentCompat : DatabaseSavePreferenceDialogFragmentCompat() {

    private lateinit var rootView: View
    private lateinit var enableSwitchView: CompoundButton
    private lateinit var chromaColorView: ChromaColorView

    var onColorSelectedListener: ((color: Int?) -> Unit)? = null

    private var mDefaultColor = Color.WHITE
    private var mActivated = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val alertDialogBuilder = AlertDialog.Builder(requireActivity())

        rootView = requireActivity().layoutInflater.inflate(R.layout.fragment_color_picker, null)
        enableSwitchView = rootView.findViewById(R.id.switch_element)
        chromaColorView = rootView.findViewById(R.id.chroma_color_view)

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(ARG_INITIAL_COLOR)) {
                mDefaultColor = savedInstanceState.getInt(ARG_INITIAL_COLOR)
            }
            if (savedInstanceState.containsKey(ARG_ACTIVATED)) {
                mActivated = savedInstanceState.getBoolean(ARG_ACTIVATED)
            }
        } else {
            arguments?.apply {
                if (containsKey(ARG_INITIAL_COLOR)) {
                    mDefaultColor = getInt(ARG_INITIAL_COLOR)
                }
                if (containsKey(ARG_ACTIVATED)) {
                    mActivated = getBoolean(ARG_ACTIVATED)
                }
            }
        }
        enableSwitchView.isChecked = mActivated
        chromaColorView.currentColor = mDefaultColor

        chromaColorView.setOnColorChangedListener {
            if (!enableSwitchView.isChecked)
                enableSwitchView.isChecked = true
        }

        alertDialogBuilder.setPositiveButton(android.R.string.ok) { _, _ ->
            onDialogClosed(true)
            dismiss()
        }

        alertDialogBuilder.setNegativeButton(android.R.string.cancel) { _, _ ->
            onDialogClosed(false)
            dismiss()
        }

        alertDialogBuilder.setView(rootView)

        val dialog = alertDialogBuilder.create()
        // request a window without the title
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)

        return dialog
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)

        database?.let {
            var initColor = it.customColor
            if (initColor != null) {
                enableSwitchView.isChecked = true
            } else {
                enableSwitchView.isChecked = false
                initColor = DEFAULT_COLOR
            }
            chromaColorView.currentColor = initColor
            arguments?.putInt(ARG_INITIAL_COLOR, initColor)
        }
    }

    override fun onDialogClosed(database: ContextualDatabase?, positiveResult: Boolean) {
        super.onDialogClosed(database, positiveResult)
        if (positiveResult) {
            val newColor: Int? = if (enableSwitchView.isChecked)
                chromaColorView.currentColor
            else
                null
            onColorSelectedListener?.invoke(newColor)
            database?.let {
                val oldColor = database.customColor
                database.customColor = newColor
                saveColor(oldColor, newColor)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return rootView
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ARG_INITIAL_COLOR, chromaColorView.currentColor)
        outState.putBoolean(ARG_ACTIVATED, mActivated)
    }

    companion object {
        private const val ARG_INITIAL_COLOR = "ARG_INITIAL_COLOR"
        private const val ARG_ACTIVATED = "ARG_ACTIVATED"
        @ColorInt
        const val DEFAULT_COLOR: Int = Color.WHITE

        fun newInstance(key: String): DatabaseColorPreferenceDialogFragmentCompat {
            val fragment = DatabaseColorPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            bundle.putInt(ARG_INITIAL_COLOR, DEFAULT_COLOR)
            fragment.arguments = bundle

            return fragment
        }
    }
}
