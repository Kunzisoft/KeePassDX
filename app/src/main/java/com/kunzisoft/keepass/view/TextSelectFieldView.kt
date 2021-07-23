package com.kunzisoft.keepass.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.kunzisoft.keepass.R

class TextSelectFieldView @JvmOverloads constructor(context: Context,
                                                    attrs: AttributeSet? = null,
                                                    defStyle: Int = 0)
    : RelativeLayout(context, attrs, defStyle), GenericTextFieldView {

    private var labelViewId = ViewCompat.generateViewId()
    private var valueViewId = ViewCompat.generateViewId()
    private var valueSpinnerAdapter = ValueSpinnerAdapter(context)
    private var actionImageButtonId = ViewCompat.generateViewId()

    private val labelView = AppCompatTextView(context).apply {
        setTextAppearance(context, R.style.KeepassDXStyle_TextAppearance_LabelTextStyle)
        layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT).also {
            val leftMargin = 4f
            it.leftMargin = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                leftMargin,
                resources.displayMetrics
            ).toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                it.marginStart = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    leftMargin,
                    resources.displayMetrics
                ).toInt()
            }
        }
    }
    private val valueSpinnerView = Spinner(context).apply {
        layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT)
    }
    private var actionImageButton = AppCompatImageButton(
            ContextThemeWrapper(context, R.style.KeepassDXStyle_ImageButton_Simple), null, 0).apply {
        layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT).also {
            it.topMargin = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    8f,
                    resources.displayMetrics
            ).toInt()
            it.addRule(ALIGN_PARENT_RIGHT)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                it.addRule(ALIGN_PARENT_END)
            }
        }
        visibility = View.GONE
        contentDescription = context.getString(R.string.menu_edit)
    }

    init {
        // Manually write view to avoid view id bugs
        buildViews()
        addView(labelView)
        addView(valueSpinnerView)
        addView(actionImageButton)

        valueSpinnerView.apply {
            adapter = valueSpinnerAdapter
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    valueSpinnerAdapter.getItem(position)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun buildViews() {
        labelView.apply {
            id = labelViewId
            layoutParams = (layoutParams as LayoutParams?).also {
                it?.addRule(LEFT_OF, actionImageButtonId)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    it?.addRule(START_OF, actionImageButtonId)
                }
            }
        }
        valueSpinnerView.apply {
            id = valueViewId
            layoutParams = (layoutParams as LayoutParams?).also {
                it?.addRule(LEFT_OF, actionImageButtonId)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    it?.addRule(START_OF, actionImageButtonId)
                }
                it?.addRule(BELOW, labelViewId)
            }
        }
        actionImageButton.apply {
            id = actionImageButtonId
        }
    }

    override fun applyFontVisibility(fontInVisibility: Boolean) {
        valueSpinnerAdapter.fontVisibility = fontInVisibility
    }

    fun getActionImageView(): View {
        return actionImageButton
    }

    override var label: String
        get() {
            return labelView.hint?.toString() ?: ""
        }
        set(value) {
            labelView.hint = value
            // Define views Ids with label value
            labelViewId = "labelViewId $value".hashCode()
            valueViewId = "valueViewId $value".hashCode()
            actionImageButtonId = "actionImageButtonId $value".hashCode()
            buildViews()
        }

    fun setItems(items: List<String>) {
        valueSpinnerAdapter.setItems(items)
    }

    // To define default value and retrieve selected one
    override var value: String
        get() {
            return valueSpinnerView.selectedItem?.toString() ?: valueSpinnerAdapter.getItem(0)
        }
        set(value) {
            valueSpinnerView.setSelection(valueSpinnerAdapter.getPosition(value))
        }

    override fun setOnActionClickListener(onActionClickListener: OnClickListener?,
                                          @DrawableRes actionImageId: Int?) {
        actionImageId?.let {
            actionImageButton.setImageDrawable(ContextCompat.getDrawable(context, it))
        }
        actionImageButton.setOnClickListener(onActionClickListener)
        actionImageButton.visibility = if (onActionClickListener == null) View.GONE else View.VISIBLE
    }

    override var isFieldVisible: Boolean
        get() {
            return isVisible
        }
        set(value) {
            isVisible = value
        }

    private class ValueSpinnerAdapter(context: Context) : BaseAdapter() {

        private val inflater: LayoutInflater = LayoutInflater.from(context)
        private var listItems: MutableList<String> = mutableListOf<String>().apply{
            // Don't know why but must be init with at least one item, else no view
            add("")
        }

        var fontVisibility: Boolean = false

        override fun getCount(): Int {
            return listItems.size
        }

        override fun getItem(position: Int): String {
            return listItems[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        fun getPosition(value: String): Int {
            val index = listItems.indexOf(value)
            if (index < 0)
                return 0
            return index
        }

        fun setItems(items: List<String>) {
            listItems.clear()
            listItems.addAll(items)
        }

        fun clear() {
            listItems.clear()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val value: String = getItem(position)

            val holder: ValueSelectorViewHolder
            var valueView = convertView
            if (valueView == null) {
                holder = ValueSelectorViewHolder()
                valueView = inflater.inflate(R.layout.item_select_field_value, parent, false)
                holder.valueText = valueView?.findViewById(R.id.value_string)
                if (fontVisibility)
                    holder.valueText?.applyFontVisibility()
                valueView?.tag = holder
            } else {
                holder = valueView.tag as ValueSelectorViewHolder
            }

            holder.valueText?.text = value

            return valueView!!
        }

        inner class ValueSelectorViewHolder {
            var valueText: TextView? = null
        }
    }
}