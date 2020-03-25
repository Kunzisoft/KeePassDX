package com.kunzisoft.keepass.activities.dialogs

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class DatePickerFragment : DialogFragment() {

    private var mDefaultYear: Int = 2000
    private var mDefaultMonth: Int = 1
    private var mDefaultDay: Int = 1

    private var mListener: DatePickerDialog.OnDateSetListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mListener = context as DatePickerDialog.OnDateSetListener
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString()
                    + " must implement " + DatePickerDialog.OnDateSetListener::class.java.name)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Create a new instance of DatePickerDialog and return it
        return context?.let {
            arguments?.apply {
                if (containsKey(DEFAULT_YEAR_BUNDLE_KEY))
                    mDefaultYear = getInt(DEFAULT_YEAR_BUNDLE_KEY)
                if (containsKey(DEFAULT_MONTH_BUNDLE_KEY))
                    mDefaultMonth = getInt(DEFAULT_MONTH_BUNDLE_KEY)
                if (containsKey(DEFAULT_DAY_BUNDLE_KEY))
                    mDefaultDay = getInt(DEFAULT_DAY_BUNDLE_KEY)
            }

            DatePickerDialog(it, mListener, mDefaultYear, mDefaultMonth, mDefaultDay)
        } ?: super.onCreateDialog(savedInstanceState)
    }

    companion object {

        private const val DEFAULT_YEAR_BUNDLE_KEY = "DEFAULT_YEAR_BUNDLE_KEY"
        private const val DEFAULT_MONTH_BUNDLE_KEY = "DEFAULT_MONTH_BUNDLE_KEY"
        private const val DEFAULT_DAY_BUNDLE_KEY = "DEFAULT_DAY_BUNDLE_KEY"

        fun getInstance(defaultYear: Int,
                        defaultMonth: Int,
                        defaultDay: Int): DatePickerFragment {
            return DatePickerFragment().apply {
                arguments = Bundle().apply {
                    putInt(DEFAULT_YEAR_BUNDLE_KEY, defaultYear)
                    putInt(DEFAULT_MONTH_BUNDLE_KEY, defaultMonth)
                    putInt(DEFAULT_DAY_BUNDLE_KEY, defaultDay)
                }
            }
        }
    }
}