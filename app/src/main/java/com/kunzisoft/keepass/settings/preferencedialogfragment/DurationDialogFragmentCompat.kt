/*
 * Copyright 2021 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.os.Bundle
import android.view.View
import android.widget.NumberPicker
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.settings.preference.DurationDialogPreference


class DurationDialogFragmentCompat : InputPreferenceDialogFragmentCompat() {

    private var mEnabled = true
    private var mDays = 0
    private var mHours = 0
    private var mMinutes = 0
    private var mSeconds = 0

    private var daysNumberPicker: NumberPicker? = null
    private var hoursNumberPicker: NumberPicker? = null
    private var minutesNumberPicker: NumberPicker? = null
    private var secondsNumberPicker: NumberPicker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // To get items from saved instance state
        if (savedInstanceState != null
                && savedInstanceState.containsKey(ENABLE_KEY)
                && savedInstanceState.containsKey(DAYS_KEY)
                && savedInstanceState.containsKey(HOURS_KEY)
                && savedInstanceState.containsKey(MINUTES_KEY)
                && savedInstanceState.containsKey(SECONDS_KEY)) {
            mEnabled = savedInstanceState.getBoolean(ENABLE_KEY)
            mDays = savedInstanceState.getInt(DAYS_KEY)
            mHours = savedInstanceState.getInt(HOURS_KEY)
            mMinutes = savedInstanceState.getInt(MINUTES_KEY)
            mSeconds = savedInstanceState.getInt(SECONDS_KEY)
        } else {
            val currentPreference = preference
            if (currentPreference is DurationDialogPreference) {
                durationToDaysHoursMinutesSeconds(currentPreference.getDuration())
            }
        }
    }

    private fun durationToDaysHoursMinutesSeconds(duration: Long) {
        if (duration < 0) {
            mDays = 0
            mHours = 0
            mMinutes = 0
            mSeconds = 0
        } else {
            mDays = (duration / (24L * 60L * 60L * 1000L)).toInt()
            val daysMilliseconds = mDays * 24L * 60L * 60L * 1000L
            mHours = ((duration - daysMilliseconds) / (60L * 60L * 1000L)).toInt()
            val hoursMilliseconds = mHours * 60L * 60L * 1000L
            mMinutes = ((duration - daysMilliseconds - hoursMilliseconds) / (60L * 1000L)).toInt()
            val minutesMilliseconds = mMinutes * 60L * 1000L
            mSeconds = ((duration - daysMilliseconds - hoursMilliseconds - minutesMilliseconds) / (1000L)).toInt()
        }
    }

    private fun assignValuesInViews() {
        daysNumberPicker?.value = mDays
        hoursNumberPicker?.value = mHours
        minutesNumberPicker?.value = mMinutes
        secondsNumberPicker?.value = mSeconds
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        daysNumberPicker = view.findViewById<NumberPicker>(R.id.days_picker).apply {
            minValue = 0
            maxValue = 364
            setOnValueChangedListener { _, _, newVal ->
                mDays = newVal
                activateSwitch()
            }
        }

        hoursNumberPicker = view.findViewById<NumberPicker>(R.id.hours_picker).apply {
            minValue = 0
            maxValue = 23
            setOnValueChangedListener { _, _, newVal ->
                mHours = newVal
                activateSwitch()
            }
        }

        minutesNumberPicker = view.findViewById<NumberPicker>(R.id.minutes_picker).apply {
            minValue = 0
            maxValue = 59
            setOnValueChangedListener { _, _, newVal ->
                mMinutes = newVal
                activateSwitch()
            }
        }

        secondsNumberPicker = view.findViewById<NumberPicker>(R.id.seconds_picker).apply {
            minValue = 0
            maxValue = 59
            setOnValueChangedListener { _, _, newVal ->
                mSeconds = newVal
                activateSwitch()
            }
        }

        mEnabled = isSwitchActivated()
        setSwitchAction({ isChecked ->
            mEnabled = isChecked
        }, mDays + mHours + mMinutes + mSeconds > 0)

        assignValuesInViews()
    }

    private fun buildDuration(): Long {
        return if (mEnabled) {
            mDays * 24L * 60L * 60L * 1000L +
                    mHours * 60L * 60L * 1000L +
                    mMinutes * 60L * 1000L +
                    mSeconds * 1000L
        } else {
            -1
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(ENABLE_KEY, mEnabled)
        outState.putInt(DAYS_KEY, mDays)
        outState.putInt(HOURS_KEY, mHours)
        outState.putInt(MINUTES_KEY, mMinutes)
        outState.putInt(SECONDS_KEY, mSeconds)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val currentPreference = preference
            if (currentPreference is DurationDialogPreference) {
                currentPreference.setDuration(buildDuration())
            }
        }
    }

    companion object {
        private const val ENABLE_KEY = "ENABLE_KEY"
        private const val DAYS_KEY = "DAYS_KEY"
        private const val HOURS_KEY = "HOURS_KEY"
        private const val MINUTES_KEY = "MINUTES_KEY"
        private const val SECONDS_KEY = "SECONDS_KEY"

        fun newInstance(key: String): DurationDialogFragmentCompat {
            val fragment = DurationDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
