package com.kunzisoft.keepass.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.view.collapse

abstract class AnimatedItemsAdapter<Item, T: RecyclerView.ViewHolder>(val context: Context)
    : RecyclerView.Adapter<T>() {

    protected val inflater: LayoutInflater = LayoutInflater.from(context)
    var itemsList: MutableList<Item> = ArrayList()
        private set

    var onDeleteButtonClickListener: ((item: Item)->Unit)? = null
    private var mItemToRemove: Item? = null

    var onListSizeChangedListener: ((previousSize: Int, newSize: Int)->Unit)? = null

    override fun getItemCount(): Int {
        return itemsList.size
    }

    fun assignItems(items: List<Item>) {
        val previousSize = itemsList.size
        itemsList.apply {
            clear()
            addAll(items)
        }
        notifyDataSetChanged()
        onListSizeChangedListener?.invoke(previousSize, itemsList.size)
    }

    open fun putItem(item: Item) {
        val previousSize = itemsList.size
        if (itemsList.contains(item)) {
            val index = itemsList.indexOf(item)
            itemsList.removeAt(index)
            itemsList.add(index, item)
            notifyItemChanged(index)
        } else {
            itemsList.add(item)
            notifyItemInserted(itemsList.indexOf(item))
        }
        onListSizeChangedListener?.invoke(previousSize, itemsList.size)
    }

    fun onBindDeleteButton(holder: T, deleteButton: View, item: Item, position: Int) {
        deleteButton.apply {
            visibility = View.VISIBLE
            if (mItemToRemove == item) {
                holder.itemView.collapse(true) {
                    deleteItem(item)
                }
                setOnClickListener(null)
            } else {
                setOnClickListener {
                    onDeleteButtonClickListener?.invoke(item)
                    mItemToRemove = item
                    notifyItemChanged(position)
                }
            }
        }
    }

    private fun deleteItem(item: Item) {
        val previousSize = itemsList.size
        val position = itemsList.indexOf(item)
        if (position >= 0) {
            itemsList.removeAt(position)
            notifyItemRemoved(position)
            mItemToRemove = null
            for (i in 0 until itemsList.size) {
                notifyItemChanged(i)
            }
        }
        onListSizeChangedListener?.invoke(previousSize, itemsList.size)
    }

    fun clear() {
        itemsList.clear()
    }
}