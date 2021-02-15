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

    open fun assignItems(items: List<Item>) {
        val previousSize = itemsList.size
        itemsList.apply {
            clear()
            addAll(items)
        }
        notifyDataSetChanged()
        onListSizeChangedListener?.invoke(previousSize, itemsList.size)
    }

    open fun isEmpty(): Boolean {
        return itemsList.isEmpty()
    }

    open fun contains(item: Item): Boolean {
        return itemsList.contains(item)
    }

    open fun indexOf(item: Item): Int {
        return itemsList.indexOf(item)
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

    /**
     * Only replace [oldItem] by [newItem] if [oldItem] exists
     */
    open fun replaceItem(oldItem: Item, newItem: Item) {
        if (itemsList.contains(oldItem)) {
            val index = itemsList.indexOf(oldItem)
            itemsList.removeAt(index)
            itemsList.add(index, newItem)
            notifyItemChanged(index)
        }
    }

    /**
     * Only remove [item] if doesn't exists
     */
    open fun removeItem(item: Item) {
        if (itemsList.contains(item)) {
            mItemToRemove = item
            notifyItemChanged(itemsList.indexOf(item))
        }
    }

    protected fun performDeletion(holder: T, item: Item): Boolean {
        val effectivelyDeletionPerformed = mItemToRemove == item
        if (effectivelyDeletionPerformed) {
            holder.itemView.collapse(true) {
                deleteItem(item)
            }
        }
        return effectivelyDeletionPerformed
    }

    protected fun onBindDeleteButton(holder: T, deleteButton: View, item: Item, position: Int) {
        deleteButton.apply {
            visibility = View.VISIBLE
            if (performDeletion(holder, item)) {
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
        if (itemsList.size > 0) {
            itemsList.clear()
            notifyDataSetChanged()
        }
    }
}