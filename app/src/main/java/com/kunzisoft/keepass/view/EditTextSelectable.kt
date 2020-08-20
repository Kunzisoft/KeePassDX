package com.kunzisoft.keepass.view

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputEditText

class EditTextSelectable @JvmOverloads constructor(context: Context,
                                                   attrs: AttributeSet? = null,
                                                   defStyle: Int = 0)
    : TextInputEditText(context, attrs) {

    private val mOnSelectionChangedListeners: MutableList<OnSelectionChangedListener>?

    init {
        mOnSelectionChangedListeners = ArrayList()
    }

    fun addOnSelectionChangedListener(onSelectionChangedListener: OnSelectionChangedListener) {
        mOnSelectionChangedListeners?.add(onSelectionChangedListener)
    }

    fun removeOnSelectionChangedListener(onSelectionChangedListener: OnSelectionChangedListener) {
        mOnSelectionChangedListeners?.remove(onSelectionChangedListener)
    }

    fun removeAllOnSelectionChangedListeners() {
        mOnSelectionChangedListeners?.clear()
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        mOnSelectionChangedListeners?.forEach {
            it.onSelectionChanged(selStart, selEnd)
        }
    }

    interface OnSelectionChangedListener {
        fun onSelectionChanged(start: Int, end: Int)
    }
}