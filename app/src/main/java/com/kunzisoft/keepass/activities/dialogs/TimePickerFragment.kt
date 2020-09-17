package com.kunzisoft.keepass.activities.dialogs

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import androidx.fragment.app.DialogFragment

class TimePickerFragment : DialogFragment() {

    private var defaultHour: Int = 0
    private var defaultMinute: Int = 0

    private var mListener: TimePickerDialog.OnTimeSetListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mListener = context as TimePickerDialog.OnTimeSetListener
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString()
                    + " must implement " + DatePickerDialog.OnDateSetListener::class.java.name)
        }
    }

    override fun onDetach() {
        mListener = null
        super.onDetach()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Create a new instance of DatePickerDialog and return it
        return context?.let {
            arguments?.apply {
                if (containsKey(DEFAULT_HOUR_BUNDLE_KEY))
                    defaultHour = getInt(DEFAULT_HOUR_BUNDLE_KEY)
                if (containsKey(DEFAULT_MINUTE_BUNDLE_KEY))
                    defaultMinute = getInt(DEFAULT_MINUTE_BUNDLE_KEY)
            }

            TimePickerDialog(it, mListener, defaultHour, defaultMinute, DateFormat.is24HourFormat(activity))
        } ?: super.onCreateDialog(savedInstanceState)
    }

    companion object {

        private const val DEFAULT_HOUR_BUNDLE_KEY = "DEFAULT_HOUR_BUNDLE_KEY"
        private const val DEFAULT_MINUTE_BUNDLE_KEY = "DEFAULT_MINUTE_BUNDLE_KEY"

        fun getInstance(defaultHour: Int,
                        defaultMinute: Int): TimePickerFragment {
            return TimePickerFragment().apply {
                arguments = Bundle().apply {
                    putInt(DEFAULT_HOUR_BUNDLE_KEY, defaultHour)
                    putInt(DEFAULT_MINUTE_BUNDLE_KEY, defaultMinute)
                }
            }
        }
    }
}