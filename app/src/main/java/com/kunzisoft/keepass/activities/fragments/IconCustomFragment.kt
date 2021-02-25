package com.kunzisoft.keepass.activities.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.database.element.icon.IconPool
import com.kunzisoft.keepass.icons.IconDrawableFactory
import com.kunzisoft.keepass.icons.assignDatabaseIcon

/**
 * Content fragments
 */

class IconCustomFragment : Fragment() {

    private lateinit var currIconGridView: GridView
    private var iconPool: IconPool? = null
    private var iconDrawableFactory: IconDrawableFactory? = null
    var iconCustomPickerListener: ((icon: IconImageCustom) -> Unit)? = null

    override fun onDetach() {
        iconCustomPickerListener = null
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


        val database = Database.getInstance()
        iconPool = database.iconPool
        iconDrawableFactory = database.drawFactory

        currIconGridView.adapter = IconCustomAdapter(requireActivity()).apply {
            iconPool?.doForEachCustomIcon {
                putIcon(it)
            }
        }
    }

    inner class IconCustomAdapter(private val context: Context) : BaseAdapter() {

        private val customIconList = ArrayList<IconImageCustom>()

        fun putIcon(icon: IconImageCustom) {
            customIconList.add(icon)
        }

        override fun getCount(): Int {
            return customIconList.size
        }

        override fun getItem(position: Int): Any {
            return customIconList[position]
        }

        override fun getItemId(position: Int): Long {
            return customIconList[position].uuid.leastSignificantBits
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val currentView: View = convertView
                    ?: (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
                            .inflate(R.layout.item_icon, parent, false)

            val iconImage = IconImage(customIconList[position])

            iconDrawableFactory?.let {
                val iconImageView = currentView.findViewById<ImageView>(R.id.icon_image)
                iconImageView.assignDatabaseIcon(it, iconImage)
            }

            currentView.setOnClickListener {
                iconCustomPickerListener?.invoke(iconImage.custom)
            }

            return currentView
        }
    }
}