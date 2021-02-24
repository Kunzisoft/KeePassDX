package com.kunzisoft.keepass.activities.dialogs

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
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.icons.IconPack
import com.kunzisoft.keepass.icons.IconPackChooser

/**
 * Content fragments
 */

class IconStandardFragment : Fragment() {

    private lateinit var currIconGridView: GridView
    private var iconPack: IconPack? = null
    var iconStandardPickerListener: ((icon: IconImageStandard) -> Unit)? = null

    override fun onDetach() {
        iconStandardPickerListener = null
        super.onDetach()
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val root =  inflater.inflate(R.layout.fragment_icon_standard_picker, container, false)
        currIconGridView = root.findViewById(R.id.IconGridView)
        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        iconPack = IconPackChooser.getSelectedIconPack(requireContext())
        currIconGridView.adapter = IconStandardAdapter(requireActivity())
        currIconGridView.setOnItemClickListener { _, _, position, _ ->
            iconStandardPickerListener?.invoke(IconImageStandard(position))
        }
    }

    interface IconStandardPickerListener {
        fun iconStandardPicked(icon: IconImageStandard)
    }

    inner class IconStandardAdapter(private val context: Context) : BaseAdapter() {

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
}