package com.kunzisoft.keepass.view

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import com.kunzisoft.keepass.R

class InheritedCompletionView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatAutoCompleteTextView(context, attrs) {

    private val adapter = ArrayAdapterNoFilter(
        context,
        android.R.layout.simple_list_item_1
    )

    private class ArrayAdapterNoFilter(context: Context,
                                       @LayoutRes private val layoutResource: Int)
        : ArrayAdapter<String>(context, layoutResource) {
        val items = InheritedStatus.listOfStrings(context)

        override fun getCount(): Int {
            return items.size
        }

        override fun getItem(position: Int): String {
            return items[position]
        }

        override fun getItemId(position: Int): Long {
            // Or just return p0
            return items[position].hashCode().toLong()
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(p0: CharSequence?): FilterResults {
                    return FilterResults().apply {
                        values = items
                    }
                }

                override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                    notifyDataSetChanged()
                }
            }
        }
    }

    init {
        isFocusable = false
        isFocusableInTouchMode = false
        //hardwareKeyCompletion.isEnabled = false
        isCursorVisible = false
        setTextIsSelectable(false)
        inputType = InputType.TYPE_NULL
        setAdapter(adapter)
        setOnClickListener {
            showDropDown()
        }
    }

    fun getValue(): Boolean? {
        return InheritedStatus.getStatusFromString(context, text.toString()).value
    }

    fun setValue(inherited: Boolean?) {
        setText(context.getString(InheritedStatus.getStatusFromValue(inherited).stringId))
    }

    private enum class InheritedStatus(val stringId: Int, val value: Boolean?) {
        INHERITED(R.string.inherited, null),
        ENABLE(R.string.enable, true),
        DISABLE(R.string.disable, false);

        companion object {
            fun listOfStrings(context: Context): List<String> {
                return listOf(
                    context.getString(INHERITED.stringId),
                    context.getString(ENABLE.stringId),
                    context.getString(DISABLE.stringId)
                )
            }

            fun getStatusFromValue(value: Boolean?): InheritedStatus {
                values().find { it.value == value }?.let {
                    return it
                }
                return INHERITED
            }

            fun getStatusFromString(context: Context, text: String): InheritedStatus {
                values().find { context.getString(it.stringId) == text }?.let {
                    return it
                }
                return INHERITED
            }
        }
    }
}