package com.kunzisoft.keepass.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.icon.IconImageDraw
import com.kunzisoft.keepass.icons.IconDrawableFactory
import com.kunzisoft.keepass.icons.assignDatabaseIcon

class IconAdapter<I: IconImageDraw>(val context: Context) : RecyclerView.Adapter<IconAdapter<I>.CustomIconViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    private val iconList = ArrayList<I>()

    var iconDrawableFactory: IconDrawableFactory? = null
    var iconPickerListener: IconPickerListener<I>? = null

    var tintColor : Int = Color.BLACK

    init {
        // Retrieve the textColor to tint the icon
        val ta = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.textColor))
        tintColor = ta.getColor(0, Color.BLACK)
        ta.recycle()
    }

    val lastPosition: Int
        get() = iconList.lastIndex

    fun addIcon(icon: I) {
        if (!iconList.contains(icon)) {
            iconList.add(icon)
            notifyItemInserted(iconList.indexOf(icon))
        }
    }

    fun setList(icons: List<I>) {
        iconList.clear()
        icons.forEach { iconImage ->
            iconList.add(iconImage)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomIconViewHolder {
        val view = inflater.inflate(R.layout.item_icon, parent, false)
        return CustomIconViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomIconViewHolder, position: Int) {
        val icon = iconList[position]
        iconDrawableFactory?.let {
            holder.iconImageView.assignDatabaseIcon(it, icon, tintColor)
        }
        holder.itemView.setOnClickListener { iconPickerListener?.iconPicked(icon) }
    }

    override fun getItemCount(): Int {
        return iconList.size
    }

    interface IconPickerListener<I: IconImageDraw> {
        fun iconPicked(icon: I)
    }

    inner class CustomIconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var iconImageView: ImageView = itemView.findViewById(R.id.icon_image)
    }
}