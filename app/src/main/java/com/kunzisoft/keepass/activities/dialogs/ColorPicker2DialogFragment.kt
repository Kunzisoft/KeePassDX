package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.tabs.TabLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.colorpicker.ColorPicker
import com.kunzisoft.keepass.viewmodels.ColorPicker2ViewModel

class ColorPicker2DialogFragment : DatabaseDialogFragment() {

    private val colorPickerViewModel: ColorPicker2ViewModel by activityViewModels()

    private lateinit var backgroundColorPreview: View
    private lateinit var backgroundEnableSwitch: MaterialSwitch
    private lateinit var backgroundColorPickerView: ColorPicker

    private lateinit var foregroundColorPreview: View
    private lateinit var foregroundEnableSwitch: MaterialSwitch
    private lateinit var foregroundColorPickerView: ColorPicker

    private var currentBackgroundColor = Color.WHITE
    private var currentForegroundColor = Color.WHITE
    private var isBackgroundActivated = false
    private var isForegroundActivated = false

    private lateinit var tabLayout: TabLayout

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (activity == null) return super.onCreateDialog(savedInstanceState)

        val root = requireActivity()
            .layoutInflater.inflate(R.layout.fragment_dialog_dual_color_picker, null)

        backgroundColorPreview = root.findViewById(R.id.color_preview_background)
        backgroundEnableSwitch = root.findViewById(R.id.switch_element_background)
        backgroundColorPickerView = root.findViewById(R.id.color_picker_background)

        foregroundColorPreview = root.findViewById(R.id.color_preview_foreground)
        foregroundEnableSwitch = root.findViewById(R.id.switch_element_foreground)
        foregroundColorPickerView = root.findViewById(R.id.color_picker_foreground)

        tabLayout = root.findViewById(R.id.tab_layout)

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(ARG_INITIAL_BG_COLOR))
                currentBackgroundColor = savedInstanceState.getInt(ARG_INITIAL_BG_COLOR)
            if (savedInstanceState.containsKey(ARG_INITIAL_FG_COLOR))
                currentForegroundColor = savedInstanceState.getInt(ARG_INITIAL_FG_COLOR)

            if (savedInstanceState.containsKey(ARG_BG_ACTIVATED))
                isBackgroundActivated = savedInstanceState.getBoolean(ARG_BG_ACTIVATED)
            if (savedInstanceState.containsKey(ARG_FG_ACTIVATED))
                isForegroundActivated = savedInstanceState.getBoolean(ARG_FG_ACTIVATED)

        } else {
            arguments?.apply {
                if (containsKey(ARG_INITIAL_BG_COLOR))
                    currentBackgroundColor = getInt(ARG_INITIAL_BG_COLOR)
                if (containsKey(ARG_INITIAL_FG_COLOR))
                    currentForegroundColor = getInt(ARG_INITIAL_FG_COLOR)

                if (containsKey(ARG_BG_ACTIVATED))
                    isBackgroundActivated = getBoolean(ARG_BG_ACTIVATED)
                if (containsKey(ARG_FG_ACTIVATED))
                    isForegroundActivated = getBoolean(ARG_FG_ACTIVATED)

            }
        }

        backgroundEnableSwitch.isChecked = isBackgroundActivated
        backgroundColorPickerView.setColor(currentBackgroundColor)
        backgroundColorPreview.setBackgroundColor(currentBackgroundColor)

        foregroundEnableSwitch.isChecked = isForegroundActivated
        foregroundColorPickerView.setColor(currentForegroundColor)
        foregroundColorPreview.setBackgroundColor(currentForegroundColor)

        backgroundColorPickerView.setOnColorChangedListener {
            if (!backgroundEnableSwitch.isChecked)
                backgroundEnableSwitch.isChecked = true
            backgroundColorPreview.setBackgroundColor(it)
            currentBackgroundColor = it
        }

        foregroundColorPickerView.setOnColorChangedListener {
            if (!foregroundEnableSwitch.isChecked)
                foregroundEnableSwitch.isChecked = true
            foregroundColorPreview.setBackgroundColor(it)
            currentForegroundColor = it
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                backgroundColorPickerView.isVisible = tab?.position == 0
                backgroundEnableSwitch.isVisible = tab?.position == 0

                foregroundColorPickerView.isVisible = tab?.position == 1
                foregroundEnableSwitch.isVisible = tab?.position == 1
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}

        })
        //select background_tab by default
        tabLayout.getTabAt(0)?.select()

        val builder = AlertDialog.Builder(requireActivity())
        builder.setView(root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val backgroundColor: Int? =
                    if (backgroundEnableSwitch.isChecked) currentBackgroundColor
                    else null
                val foregroundColor: Int? =
                    if (foregroundEnableSwitch.isChecked) currentForegroundColor
                    else null
                colorPickerViewModel.pickColors(arrayOf(backgroundColor, foregroundColor))
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                // Do nothing
            }

        return builder.create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ARG_INITIAL_BG_COLOR, currentBackgroundColor)
        outState.putInt(ARG_INITIAL_FG_COLOR, currentForegroundColor)
        outState.putBoolean(ARG_BG_ACTIVATED, isBackgroundActivated)
        outState.putBoolean(ARG_FG_ACTIVATED, isForegroundActivated)
    }

    companion object {
        private const val ARG_INITIAL_BG_COLOR = "ARG_INITIAL_BG_COLOR"
        private const val ARG_INITIAL_FG_COLOR = "ARG_INITIAL_FG_COLOR"
        private const val ARG_BG_ACTIVATED = "ARG_BG_ACTIVATED"
        private const val ARG_FG_ACTIVATED = "ARG_FG_ACTIVATED"

        fun newInstance(
            @ColorInt initialBackgroundColor: Int?,
            @ColorInt initialForegroundColor: Int?
        ): ColorPicker2DialogFragment {
            return ColorPicker2DialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_INITIAL_BG_COLOR, initialBackgroundColor ?: Color.WHITE)
                    putInt(ARG_INITIAL_FG_COLOR, initialForegroundColor ?: Color.WHITE)
                    putBoolean(ARG_BG_ACTIVATED, initialBackgroundColor != null)
                    putBoolean(ARG_FG_ACTIVATED, initialForegroundColor != null)
                }
            }
        }
    }
}