package com.kunzisoft.keepass.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.icon.IconImageDraw
import com.kunzisoft.keepass.icons.IconDrawableFactory

class IconPickerAdapter<I: IconImageDraw>(val context: Context, private val tintIcon: Int)
    : RecyclerView.Adapter<IconPickerAdapter<I>.CustomIconViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    private val iconList = ArrayList<I>()

    var iconDrawableFactory: IconDrawableFactory? = null
    var iconPickerListener: IconPickerListener<I>? = null

    val lastPosition: Int
        get() = iconList.lastIndex

    fun addIcon(icon: I) {
        if (!iconList.contains(icon)) {
            iconList.add(icon)
            notifyItemInserted(iconList.indexOf(icon))
        }
    }

    fun updateIcon(icon: I) {
        val index = iconList.indexOf(icon)
        if (index != -1) {
            iconList[index] = icon
            notifyItemChanged(index)
        }
    }

    fun updateIconSelectedState(icons: List<I>) {
        icons.forEach { icon ->
            val index = iconList.indexOf(icon)
            if (index != -1
                    && iconList[index].selected != icon.selected) {
                iconList[index] = icon
                notifyItemChanged(index)
            }
        }
    }

    fun removeIcon(icon: I) {
        if (iconList.contains(icon)) {
            val position = iconList.indexOf(icon)
            iconList.remove(icon)
            notifyItemRemoved(position)
        }
    }

    fun containsAnySelectedIcon(): Boolean {
        return iconList.firstOrNull { it.selected } != null
    }

    fun deselectAllIcons() {
        iconList.forEachIndexed { index, icon ->
            if (icon.selected) {
                icon.selected = false
                notifyItemChanged(index)
            }
        }
    }

    fun getSelectedIcons(): List<I> {
        return iconList.filter { it.selected }
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
        iconDrawableFactory?.assignDatabaseIcon(holder.iconImageView, icon, tintIcon)
        holder.iconContainerView.isSelected = icon.selected
        holder.itemView.setOnClickListener {
            iconPickerListener?.onIconClickListener(icon)
        }
        holder.itemView.setOnLongClickListener {
            iconPickerListener?.onIconLongClickListener(icon)
            true
        }
    }

    override fun getItemCount(): Int {
        return iconList.size
    }

    interface IconPickerListener<I: IconImageDraw> {
        fun onIconClickListener(icon: I)
        fun onIconLongClickListener(icon: I)
    }

    inner class CustomIconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var iconContainerView: ViewGroup = itemView.findViewById(R.id.icon_container)
        var iconImageView: ImageView = itemView.findViewById(R.id.icon_image)
    }
}