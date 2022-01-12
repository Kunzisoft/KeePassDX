package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.widget.CompoundButton
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import com.kunzisoft.androidclearchroma.view.ChromaColorView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.viewmodels.ColorPickerViewModel

class ColorPickerDialogFragment : DatabaseDialogFragment() {

    private val mColorPickerViewModel: ColorPickerViewModel by activityViewModels()

    private lateinit var enableSwitchView: CompoundButton
    private lateinit var chromaColorView: ChromaColorView

    private var mDefaultColor = Color.WHITE

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        activity?.let { activity ->
            val root = activity.layoutInflater.inflate(R.layout.fragment_color_picker, null)
            enableSwitchView = root.findViewById(R.id.switch_element)
            chromaColorView = root.findViewById(R.id.chroma_color_view)

            var activated = false
            arguments?.apply {
                if (containsKey(ARG_INITIAL_COLOR)) {
                    mDefaultColor = getInt(ARG_INITIAL_COLOR)
                }
                if (containsKey(ARG_ACTIVATED)) {
                    activated = getBoolean(ARG_ACTIVATED)
                }
            }
            enableSwitchView.isChecked = activated

            val builder = AlertDialog.Builder(activity)
            builder.setView(root)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val color: Int? = if (enableSwitchView.isChecked)
                        chromaColorView.currentColor
                    else
                        null
                    mColorPickerViewModel.pickColor(color)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    // Do nothing
                }

            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    companion object {
        private const val ARG_INITIAL_COLOR = "ARG_INITIAL_COLOR"
        private const val ARG_ACTIVATED = "ARG_ACTIVATED"

        fun newInstance(
            @ColorInt initialColor: Int?,
        ): ColorPickerDialogFragment {
            val colorPickerDialogFragment = ColorPickerDialogFragment()
            val args = Bundle()
            args.putInt(ARG_INITIAL_COLOR, initialColor ?: Color.WHITE)
            args.putBoolean(ARG_ACTIVATED, initialColor != null)
            return colorPickerDialogFragment
        }
    }
}