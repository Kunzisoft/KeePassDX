/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.database.element

import android.content.res.Resources
import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ConfigurationCompat
import java.util.*

class DateInstant : Parcelable {

    private var jDate: Date = Date()

    val date: Date
        get() = jDate

    constructor(source: DateInstant) {
        this.jDate = Date(source.jDate.time)
    }

    constructor(date: Date) {
        jDate = Date(date.time)
    }

    constructor(millis: Long) {
        jDate = Date(millis)
    }

    constructor() {
        jDate = Date()
    }

    protected constructor(parcel: Parcel) {
        jDate = parcel.readSerializable() as Date
    }

    override fun describeContents(): Int {
        return 0
    }

    fun getDateTimeString(resources: Resources): String {
        return Companion.getDateTimeString(resources, this.date)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeSerializable(date)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }

        val date = other as DateInstant
        return isSameDate(jDate, date.jDate)
    }

    override fun hashCode(): Int {
        return jDate.hashCode()
    }

    override fun toString(): String {
        return jDate.toString()
    }

    companion object {

        val NEVER_EXPIRE = neverExpire

        private val neverExpire: DateInstant
            get() {
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

        private fun isSameDate(d1: Date?, d2: Date?): Boolean {
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

        fun getDateTimeString(resources: Resources, date: Date): String {
            return java.text.DateFormat.getDateTimeInstance(
                        java.text.DateFormat.MEDIUM,
                        java.text.DateFormat.MEDIUM,
                        ConfigurationCompat.getLocales(resources.configuration)[0])
                            .format(date)
        }
    }
}
