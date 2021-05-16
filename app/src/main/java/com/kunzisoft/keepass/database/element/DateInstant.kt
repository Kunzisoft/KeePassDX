/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.element

import android.content.res.Resources
import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ConfigurationCompat
import com.kunzisoft.keepass.utils.readEnum
import com.kunzisoft.keepass.utils.writeEnum
import org.joda.time.Duration
import org.joda.time.Instant
import java.text.SimpleDateFormat
import java.util.*

class DateInstant : Parcelable {

    private var jDate: Date = Date()
    private var mType: Type = Type.DATE_TIME

    val date: Date
        get() = jDate

    val type: Type
        get() = mType

    constructor(source: DateInstant) {
        this.jDate = Date(source.jDate.time)
        this.mType = source.mType
    }

    constructor(date: Date, type: Type = Type.DATE_TIME) {
        jDate = Date(date.time)
        mType = type
    }

    constructor(millis: Long, type: Type = Type.DATE_TIME) {
        jDate = Date(millis)
        mType = type
    }

    constructor(string: String, type: Type = Type.DATE_TIME) {
        jDate = when (type) {
            Type.DATE -> dateFormat.parse(string) ?: jDate
            Type.TIME -> timeFormat.parse(string) ?: jDate
            else -> dateTimeFormat.parse(string) ?: jDate
        }
        mType = type
    }

    constructor() {
        jDate = Date()
    }

    constructor(parcel: Parcel) {
        jDate = parcel.readSerializable() as? Date? ?: jDate
        mType = parcel.readEnum<Type>() ?: mType
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeSerializable(jDate)
        dest.writeEnum(mType)
    }

    fun getDateTimeString(resources: Resources): String {
        return when (mType) {
            Type.DATE -> getDateString(resources, jDate)
            Type.TIME -> getTimeString(resources, jDate)
            else -> Companion.getDateTimeString(resources, jDate)
        }
    }

    fun getMonthInt(): Int {
        return Companion.getMonthInt(jDate)
    }

    fun getYearInt(): Int {
        return Companion.getYearInt(jDate)
    }

    override fun toString(): String {
        return when (type) {
            Type.DATE -> dateFormat.format(jDate)
            Type.TIME -> timeFormat.format(jDate)
            else -> dateTimeFormat.format(jDate)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DateInstant) return false

        if (jDate != other.jDate) return false
        if (mType != other.mType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = jDate.hashCode()
        result = 31 * result + mType.hashCode()
        return result
    }

    enum class Type {
        DATE_TIME, DATE, TIME
    }

    companion object {

        val NEVER_EXPIRES = neverExpires()
        val IN_ONE_MONTH_DATE_TIME = DateInstant(
                Instant.now().plus(Duration.standardDays(30)).toDate(), Type.DATE_TIME)
        val IN_ONE_MONTH_DATE = DateInstant(
                Instant.now().plus(Duration.standardDays(30)).toDate(), Type.DATE)
        val IN_ONE_HOUR_TIME = DateInstant(
                Instant.now().plus(Duration.standardHours(1)).toDate(), Type.TIME)

        private val dateTimeFormat = SimpleDateFormat.getDateTimeInstance()
        private val dateFormat = SimpleDateFormat.getDateInstance()
        private val timeFormat = SimpleDateFormat.getTimeInstance()

        private fun neverExpires(): DateInstant {
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, 2999)
            cal.set(Calendar.MONTH, 11)
            cal.set(Calendar.DAY_OF_MONTH, 28)
            cal.set(Calendar.HOUR, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            return DateInstant(cal.time)
        }

        @JvmField
        val CREATOR: Parcelable.Creator<DateInstant> = object : Parcelable.Creator<DateInstant> {
            override fun createFromParcel(parcel: Parcel): DateInstant {
                return DateInstant(parcel)
            }

            override fun newArray(size: Int): Array<DateInstant?> {
                return arrayOfNulls(size)
            }
        }

        private fun isSameDate(d1: Date, d2: Date): Boolean {
            val cal1 = Calendar.getInstance()
            cal1.time = d1
            cal1.set(Calendar.MILLISECOND, 0)

            val cal2 = Calendar.getInstance()
            cal2.time = d2
            cal2.set(Calendar.MILLISECOND, 0)

            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                    cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH) &&
                    cal1.get(Calendar.HOUR) == cal2.get(Calendar.HOUR) &&
                    cal1.get(Calendar.MINUTE) == cal2.get(Calendar.MINUTE) &&
                    cal1.get(Calendar.SECOND) == cal2.get(Calendar.SECOND)

        }

        fun getMonthInt(date: Date): Int {
            val dateFormat = SimpleDateFormat("MM", Locale.ENGLISH)
            return dateFormat.format(date).toInt()
        }

        fun getYearInt(date: Date): Int {
            val dateFormat = SimpleDateFormat("yyyy", Locale.ENGLISH)
            return dateFormat.format(date).toInt()
        }

        fun getDateTimeString(resources: Resources, date: Date): String {
            return java.text.DateFormat.getDateTimeInstance(
                    java.text.DateFormat.MEDIUM,
                    java.text.DateFormat.SHORT,
                    ConfigurationCompat.getLocales(resources.configuration)[0])
                            .format(date)
        }

        fun getDateString(resources: Resources, date: Date): String {
            return java.text.DateFormat.getDateInstance(
                    java.text.DateFormat.MEDIUM,
                    ConfigurationCompat.getLocales(resources.configuration)[0])
                    .format(date)
        }

        fun getTimeString(resources: Resources, date: Date): String {
            return java.text.DateFormat.getTimeInstance(
                    java.text.DateFormat.SHORT,
                    ConfigurationCompat.getLocales(resources.configuration)[0])
                    .format(date)
        }
    }
}
