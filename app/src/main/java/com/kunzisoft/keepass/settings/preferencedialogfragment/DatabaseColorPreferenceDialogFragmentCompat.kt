/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.CompoundButton
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import com.kunzisoft.androidclearchroma.ChromaUtil
import com.kunzisoft.androidclearchroma.IndicatorMode
import com.kunzisoft.androidclearchroma.colormode.ColorMode
import com.kunzisoft.androidclearchroma.fragment.ChromaColorFragment
import com.kunzisoft.androidclearchroma.fragment.ChromaColorFragment.*
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.tasks.ActionRunnable
import java.lang.Exception

class DatabaseColorPreferenceDialogFragmentCompat : DatabaseSavePreferenceDialogFragmentCompat() {

    private lateinit var rootView: View
    private lateinit var enableSwitchView: CompoundButton
    private var chromaColorFragment: ChromaColorFragment? = null

    var onColorSelectedListener: ((enable: Boolean, color: Int) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val alertDialogBuilder = AlertDialog.Builder(activity!!)

        rootView = activity!!.layoutInflater.inflate(R.layout.pref_dialog_input_color, null)
        enableSwitchView = rootView.findViewById(R.id.switch_element)

        val fragmentManager = childFragmentManager
        chromaColorFragment = fragmentManager.findFragmentByTag(TAG_FRAGMENT_COLORS) as ChromaColorFragment?
        val fragmentTransaction = fragmentManager.beginTransaction()

        database?.let { database ->
            val initColor = try {
                enableSwitchView.isChecked = true
                Color.parseColor(database.color)
            } catch (e: Exception) {
                enableSwitchView.isChecked = false
                DEFAULT_COLOR
            }
            arguments?.putInt(ARG_INITIAL_COLOR, initColor)
        }

        if (chromaColorFragment == null) {
            chromaColorFragment = newInstance(arguments)
            fragmentTransaction.add(com.kunzisoft.androidclearchroma.R.id.color_dialog_container, chromaColorFragment!!, TAG_FRAGMENT_COLORS).commit()
        }

        alertDialogBuilder.setPositiveButton(android.R.string.ok) { _, _ ->
            val currentColor = chromaColorFragment!!.currentColor
            val customColorEnable = enableSwitchView.isChecked

            onColorSelectedListener?.invoke(customColorEnable, currentColor)

            database?.let { database ->
                val newColor = if (customColorEnable) {
                    ChromaUtil.getFormattedColorString(currentColor, false)
                } else {
                    ""
                }
                val oldColor = database.color
                database.color = newColor

                actionInUIThreadAfterSaveDatabase = AfterColorSave(newColor, oldColor)
            }

            super.onDialogClosed(true)
            dismiss()
        }

        alertDialogBuilder.setNegativeButton(android.R.string.cancel) { _, _ ->
            super.onDialogClosed(false)
            dismiss()
        }

        alertDialogBuilder.setView(rootView)

        val dialog = alertDialogBuilder.create()
        // request a window without the title
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)

        dialog.setOnShowListener { measureLayout(it as Dialog) }

        return dialog
    }

    /**
     * Set new dimensions to dialog
     * @param ad dialog
     */
    private fun measureLayout(ad: Dialog) {
        val typedValue = TypedValue()
        resources.getValue(com.kunzisoft.androidclearchroma.R.dimen.chroma_dialog_height_multiplier, typedValue, true)
        val heightMultiplier = typedValue.float
        val height = (ad.context.resources.displayMetrics.heightPixels * heightMultiplier).toInt()

        resources.getValue(com.kunzisoft.androidclearchroma.R.dimen.chroma_dialog_width_multiplier, typedValue, true)
        val widthMultiplier = typedValue.float
        val width = (ad.context.resources.displayMetrics.widthPixels * widthMultiplier).toInt()

        ad.window?.setLayout(width, height)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return rootView
    }

    private inner class AfterColorSave(private val mNewColor: String,
                                       private val mOldColor: String)
        : ActionRunnable() {

        override fun onFinishRun(result: Result) {
            val defaultColorToShow =
                    if (result.isSuccess) {
                        mNewColor
                    } else {
                        database?.color = mOldColor
                        mOldColor
                    }
            preference.summary = defaultColorToShow
        }
    }

    companion object {
        private const val TAG_FRAGMENT_COLORS = "TAG_FRAGMENT_COLORS"

        @ColorInt
        const val DEFAULT_COLOR: Int = Color.TRANSPARENT

        fun newInstance(key: String): DatabaseColorPreferenceDialogFragmentCompat {
            val fragment = DatabaseColorPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            bundle.putInt(ARG_INITIAL_COLOR, Color.BLACK)
            bundle.putInt(ARG_COLOR_MODE, ColorMode.RGB.ordinal)
            bundle.putInt(ARG_INDICATOR_MODE, IndicatorMode.HEX.ordinal)
            fragment.arguments = bundle

            return fragment
        }
    }
}
