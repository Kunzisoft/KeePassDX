package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.colorpicker.ColorPicker
import com.kunzisoft.keepass.viewmodels.ColorPickerViewModel

class ColorPickerDialogFragment : DatabaseDialogFragment() {

    private val colorPickerViewModel: ColorPickerViewModel by activityViewModels()

    private lateinit var colorPreview: View
    private lateinit var enableSwitchView: CompoundButton
    private lateinit var colorPickerView: ColorPicker

    private var currentColor = Color.WHITE
    private var isActivated = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (activity == null) return super.onCreateDialog(savedInstanceState)

        val root = requireActivity().layoutInflater.inflate(R.layout.fragment_color_picker, null)
        colorPreview = root.findViewById(R.id.color_preview)
        enableSwitchView = root.findViewById(R.id.switch_element)
        colorPickerView = root.findViewById(R.id.color_picker)

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(ARG_INITIAL_COLOR)) {
                currentColor = savedInstanceState.getInt(ARG_INITIAL_COLOR)
            }
            if (savedInstanceState.containsKey(ARG_ACTIVATED)) {
                isActivated = savedInstanceState.getBoolean(ARG_ACTIVATED)
            }
        } else {
            arguments?.apply {
                if (containsKey(ARG_INITIAL_COLOR)) {
                    currentColor = getInt(ARG_INITIAL_COLOR)
                }
                if (containsKey(ARG_ACTIVATED)) {
                    isActivated = getBoolean(ARG_ACTIVATED)
                }
            }
        }

        enableSwitchView.isChecked = isActivated
        colorPickerView.setColor(currentColor)
        colorPreview.setBackgroundColor(currentColor)

        colorPickerView.setOnColorChangedListener {
            if (!enableSwitchView.isChecked)
                enableSwitchView.isChecked = true
            colorPreview.setBackgroundColor(it)
            currentColor = it
        }

        val builder = AlertDialog.Builder(requireActivity())
        builder.setView(root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val color: Int? =
                    if (enableSwitchView.isChecked) currentColor
                    else null
                colorPickerViewModel.pickColor(color)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                // Do nothing
            }

        return builder.create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ARG_INITIAL_COLOR, currentColor)
        outState.putBoolean(ARG_ACTIVATED, isActivated)
    }

    companion object {
        private const val ARG_INITIAL_COLOR = "ARG_INITIAL_COLOR"
        private const val ARG_ACTIVATED = "ARG_ACTIVATED"

        fun newInstance(
            @ColorInt initialColor: Int?,
        ): ColorPickerDialogFragment {
            return ColorPickerDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_INITIAL_COLOR, initialColor ?: Color.WHITE)
                    putBoolean(ARG_ACTIVATED, initialColor != null)
                }
            }
        }
    }
}