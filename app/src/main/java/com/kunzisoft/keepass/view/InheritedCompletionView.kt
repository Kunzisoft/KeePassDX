package com.kunzisoft.keepass.view

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import com.kunzisoft.keepass.R

class InheritedCompletionView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatAutoCompleteTextView(context, attrs) {

    val adapter = ArrayAdapter(
        context,
        android.R.layout.simple_list_item_1,
        InheritedStatus.listOfStrings(context))

    init {
        setAdapter(adapter)
        inputType = InputType.TYPE_NULL
        adapter.filter.filter(null)
    }

    fun getValue(): Boolean? {
        return InheritedStatus.getStatusFromString(context, text.toString()).value
    }

    fun setValue(inherited: Boolean?) {
        setText(context.getString(InheritedStatus.getStatusFromValue(inherited).stringId))
        adapter.filter.filter(null)
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