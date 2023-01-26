package com.kunzisoft.keepass.view

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.text.InputType
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.utils.readEnum
import com.kunzisoft.keepass.utils.writeEnum


class HardwareKeySelectionView @JvmOverloads constructor(context: Context,
                                                         attrs: AttributeSet? = null,
                                                         defStyle: Int = 0)
    : ConstraintLayout(context, attrs, defStyle) {

    private var mHardwareKey: HardwareKey? = null

    private val hardwareKeyLayout: TextInputLayout
    private val hardwareKeyCompletion: AppCompatAutoCompleteTextView
    var selectionListener: ((HardwareKey)-> Unit)? = null

    private val mHardwareKeyAdapter = ArrayAdapterNoFilter(context)

    private class ArrayAdapterNoFilter(context: Context)
        : ArrayAdapter<String>(context, android.R.layout.simple_list_item_1) {
        val hardwareKeys = HardwareKey.values()

        override fun getCount(): Int {
            return hardwareKeys.size
        }

        override fun getItem(position: Int): String {
            return hardwareKeys[position].value
        }

        override fun getItemId(position: Int): Long {
            // Or just return p0
            return hardwareKeys[position].hashCode().toLong()
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(p0: CharSequence?): FilterResults {
                    return FilterResults().apply {
                        values = hardwareKeys
                    }
                }

                override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                    notifyDataSetChanged()
                }
            }
        }
    }

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_hardware_key_selection, this)

        hardwareKeyLayout = findViewById(R.id.input_entry_hardware_key_layout)
        hardwareKeyCompletion = findViewById(R.id.input_entry_hardware_key_completion)

        hardwareKeyCompletion.isFocusable = false
        hardwareKeyCompletion.isFocusableInTouchMode = false
        //hardwareKeyCompletion.isEnabled = false
        hardwareKeyCompletion.isCursorVisible = false
        hardwareKeyCompletion.setTextIsSelectable(false)
        hardwareKeyCompletion.inputType = InputType.TYPE_NULL
        hardwareKeyCompletion.setAdapter(mHardwareKeyAdapter)

        hardwareKeyCompletion.setOnClickListener {
            hardwareKeyCompletion.showDropDown()
        }
        hardwareKeyCompletion.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                mHardwareKey = HardwareKey.fromPosition(position)
                mHardwareKey?.let { hardwareKey ->
                    selectionListener?.invoke(hardwareKey)
                }
            }
    }

    var hardwareKey: HardwareKey?
        get() {
            return mHardwareKey
        }
        set(value) {
            mHardwareKey = value
            hardwareKeyCompletion.setText(value?.toString() ?: "")
        }

    var error: CharSequence?
        get() = hardwareKeyLayout.error
        set(value) {
            hardwareKeyLayout.error = value
        }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val saveState = SavedState(superState)
        saveState.mHardwareKey = this.mHardwareKey
        return saveState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        this.mHardwareKey = state.mHardwareKey
    }

    internal class SavedState : BaseSavedState {
        var mHardwareKey: HardwareKey? = null

        constructor(superState: Parcelable?) : super(superState)

        private constructor(parcel: Parcel) : super(parcel) {
            mHardwareKey = parcel.readEnum<HardwareKey>()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeEnum(mHardwareKey)
        }

        companion object CREATOR : Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }
}