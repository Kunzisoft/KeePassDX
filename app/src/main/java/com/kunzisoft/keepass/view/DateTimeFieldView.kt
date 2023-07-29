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
package com.kunzisoft.keepass.view

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.utils.TimeUtil.getDateTimeString

class DateTimeFieldView @JvmOverloads constructor(context: Context,
                                                  attrs: AttributeSet? = null,
                                                  defStyle: Int = 0)
    : FrameLayout(context, attrs, defStyle), GenericDateTimeFieldView {

    private var dateTimeLabelView: TextView
    private var dateTimeValueView: TextView
    private var expiresImage: ImageView

    private var mActivated: Boolean = false
    private var mDateTime: DateInstant = DateInstant.IN_ONE_MONTH_DATE_TIME

    private var mDefault: DateInstant = DateInstant.NEVER_EXPIRES

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_date_time, this)

        dateTimeLabelView = findViewById(R.id.date_time_label)
        dateTimeValueView = findViewById(R.id.date_time_value)
        expiresImage = findViewById(R.id.expires_image)
    }

    private fun assignExpiresDateText() {
        val isExpires = mDateTime.isCurrentlyExpire()

        // Show or not the warning icon
        expiresImage.isVisible = if (mActivated) {
            isExpires
        } else {
            false
        }

        // Build the datetime string
        dateTimeValueView.text = if (mActivated) {
            val dateTimeString = mDateTime.getDateTimeString(resources)
            if (isExpires) {
                // Add strike
                SpannableString(dateTimeString).apply {
                    setSpan(StrikethroughSpan(),
                        0, dateTimeString.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            } else {
                dateTimeString
            }
        } else {
            resources.getString(R.string.never)
        }
    }

    override var label: String
        get() {
            return dateTimeLabelView.text.toString()
        }
        set(value) {
            dateTimeLabelView.text = value
        }

    var type: DateInstant.Type
        get() {
            return mDateTime.type
        }
        set(value) {
            mDateTime.type = value
        }

    override var activation: Boolean
        get() {
            return mActivated
        }
        set(value) {
            mActivated = value
            dateTime = if (value) {
                when (mDateTime.type) {
                    DateInstant.Type.DATE_TIME -> DateInstant.IN_ONE_MONTH_DATE_TIME
                    DateInstant.Type.DATE -> DateInstant.IN_ONE_MONTH_DATE
                    DateInstant.Type.TIME -> DateInstant.IN_ONE_HOUR_TIME
                }
            } else {
                mDefault
            }
        }

    /**
     * Warning dateTime.type is ignore, use type instead
     */
    override var dateTime: DateInstant
        get() {
            return if (activation)
                mDateTime
            else
                mDefault
        }
        set(value) {
            mDateTime = DateInstant(value.date, mDateTime.type)
            assignExpiresDateText()
        }

    override var value: String
        get() {
            return if (activation) dateTime.toString() else ""
        }
        set(value) {
            mDateTime = try {
                DateInstant(value)
            } catch (e: Exception) {
                mDefault
            }
        }

    override var default: String
        get() = mDefault.toString()
        set(value) {
            mDefault = try {
                DateInstant(value)
            } catch (e: Exception) {
                mDefault
            }
        }

    override var isFieldVisible: Boolean
        get() {
            return isVisible
        }
        set(value) {
            isVisible = value
        }
}
