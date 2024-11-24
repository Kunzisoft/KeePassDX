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

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.kunzisoft.keepass.utils.readEnum
import com.kunzisoft.keepass.utils.readSerializableCompat
import com.kunzisoft.keepass.utils.writeEnum
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Duration
import org.joda.time.Instant
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter


class DateInstant : Parcelable {

    private var mInstant: Instant = Instant.now()
    private var mType: Type = Type.DATE_TIME

    val instant: Instant
        get() = mInstant

    var type: Type
        get() = mType
        set(value) {
            mType = value
        }

    constructor(source: DateInstant) {
        this.mInstant = Instant(source.mInstant)
        this.mType = source.mType
    }

    constructor(instant: Instant, type: Type = Type.DATE_TIME) {
        mInstant = Instant(instant)
        mType = type
    }

    private fun parse(value: String, type: Type): Instant {
        return Instant(when (type) {
            Type.DATE_TIME -> dateTimeFormat.parseDateTime(value) ?: DateTime()
            Type.DATE -> dateFormat.parseDateTime(value) ?: DateTime()
            Type.TIME -> timeFormat.parseDateTime(value) ?: DateTime()
        })
    }

    constructor(string: String, type: Type = Type.DATE_TIME) {
        try {
            mInstant = parse(string, type)
            mType = type
        } catch (e: Exception) {
            // Retry with second format
            try {
                when (type) {
                    Type.TIME -> {
                        mInstant = parse(string, Type.DATE)
                        mType = Type.DATE
                    }
                    else -> {
                        mInstant = parse(string, Type.TIME)
                        mType = Type.TIME
                    }
                }
            } catch (e: Exception) {
                // Retry with third format
                when (type) {
                    Type.DATE, Type.TIME -> {
                        mInstant = parse(string, Type.DATE_TIME)
                        mType = Type.DATE_TIME
                    }
                    else -> {
                        mInstant = parse(string, Type.DATE)
                        mType = Type.DATE
                    }
                }
            }
        }
    }

    constructor(type: Type) {
        mType = type
    }

    constructor() {
        mInstant = Instant.now()
    }

    constructor(parcel: Parcel) {
        mInstant = parcel.readSerializableCompat() ?: mInstant
        mType = parcel.readEnum<Type>() ?: mType
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeSerializable(mInstant)
        dest.writeEnum(mType)
    }

    fun setDate(year: Int, month: Int, day: Int) {
        mInstant = DateTime(mInstant, DateTimeZone.getDefault())
            .withYear(year)
            .withMonthOfYear(month)
            .withDayOfMonth(day)
            .toInstant()
    }

    fun setTime(hour: Int, minute: Int) {
        mInstant = DateTime(mInstant, DateTimeZone.getDefault())
            .withHourOfDay(hour)
            .withMinuteOfHour(minute)
            .toInstant()
    }

    fun getYear(): Int {
        return mInstant.toDateTime().year
    }

    fun getMonth(): Int {
        return mInstant.toDateTime().monthOfYear
    }

    fun getDay(): Int {
        return mInstant.toDateTime().dayOfMonth
    }

    fun getHour(): Int {
        return mInstant.toDateTime().hourOfDay
    }

    fun getMinute(): Int {
        return mInstant.toDateTime().minuteOfHour
    }

    fun getSecond(): Int {
        return mInstant.toDateTime().secondOfMinute
    }

    // If expireDate is before NEVER_EXPIRE date less 1 month (to be sure)
    // it is not expires
    fun isNeverExpires(): Boolean {
        return mInstant.isBefore(NEVER_EXPIRES.instant.minus(Duration.standardDays(30)))
    }

    fun isCurrentlyExpire(): Boolean {
        return when (type) {
            Type.DATE -> LocalDate.fromDateFields(mInstant.toDate()).isBefore(LocalDate.now())
            Type.TIME -> LocalTime.fromDateFields(mInstant.toDate()).isBefore(LocalTime.now())
            else -> LocalDateTime.fromDateFields(mInstant.toDate()).isBefore(LocalDateTime.now())
        }
    }

    /**
     * Returns:
     * the number of milliseconds since 1970-01-01T00:00:00Z
     */
    fun toMilliseconds(): Long {
        return mInstant.millis
    }

    override fun toString(): String {
        return when (type) {
            Type.DATE -> dateFormat.print(mInstant)
            Type.TIME -> timeFormat.print(mInstant)
            else -> dateTimeFormat.print(mInstant)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DateInstant) return false

        if (mType != other.mType) return false
        if (mType == Type.DATE || mType == Type.DATE_TIME) {
            if (getYear() != other.getYear()) return false
            if (getMonth() != other.getMonth()) return false
            if (getDay() != other.getDay()) return false
            if (getHour() != other.getHour()) return false
        }
        if (mType == Type.TIME || mType == Type.DATE_TIME) {
            if (getMinute() != other.getMinute()) return false
            if (getSecond() != other.getSecond()) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = mInstant.hashCode()
        result = 31 * result + mType.hashCode()
        return result
    }

    fun isBefore(dateInstant: DateInstant): Boolean {
        return this.mInstant.isBefore(dateInstant.mInstant)
    }

    fun isAfter(dateInstant: DateInstant): Boolean {
        return this.mInstant.isAfter(dateInstant.mInstant)
    }

    fun compareTo(other: DateInstant): Int {
        return mInstant.compareTo(other.mInstant)
    }

    enum class Type {
        DATE_TIME, DATE, TIME
    }

    companion object {

        private val TAG = DateInstant::class.java.name

        private val DOT_NET_EPOCH_DATE_TIME = DateTime(1, 1, 1, 0, 0, 0, DateTimeZone.UTC)
        private val JAVA_EPOCH_DATE_TIME = DateTime(1970, 1, 1, 0, 0, 0, DateTimeZone.UTC)
        private val EPOCH_OFFSET = (JAVA_EPOCH_DATE_TIME.millis - DOT_NET_EPOCH_DATE_TIME.millis) / 1000L
        private val NEVER_EXPIRES_DATE_TIME = DateTime(2999, 11, 28, 23, 59, 59, DateTimeZone.UTC)

        val NEVER_EXPIRES = DateInstant(NEVER_EXPIRES_DATE_TIME.toInstant())
        val IN_ONE_MONTH_DATE_TIME = DateInstant(
                Instant.now().plus(Duration.standardDays(30)), Type.DATE_TIME)
        val IN_ONE_MONTH_DATE = DateInstant(
                Instant.now().plus(Duration.standardDays(30)), Type.DATE)
        val IN_ONE_HOUR_TIME = DateInstant(
                Instant.now().plus(Duration.standardHours(1)), Type.TIME)

        private val ISO8601Format: DateTimeFormatter =
            DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .withZoneUTC()
        private var dateTimeFormat: DateTimeFormatter =
            DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm'Z'")
                .withZoneUTC()
        private var dateFormat: DateTimeFormatter =
            DateTimeFormat.forPattern("yyyy-MM-dd'Z'")
                .withZoneUTC()
        private var timeFormat: DateTimeFormatter =
            DateTimeFormat.forPattern("HH:mm'Z'")
                .withZoneUTC()

        fun Long.fromDotNetSeconds(): DateInstant {
            val dt = DOT_NET_EPOCH_DATE_TIME.plus(this * 1000L)
            // Switch corrupted dates to a more recent date that won't cause issues on the client
            return DateInstant((if (dt.isBefore(JAVA_EPOCH_DATE_TIME)) { JAVA_EPOCH_DATE_TIME } else dt).toInstant())
        }

        fun DateInstant.toDotNetSeconds(): Long {
            val duration = Duration(JAVA_EPOCH_DATE_TIME, mInstant)
            val seconds = duration.millis / 1000L
            return seconds + EPOCH_OFFSET
        }

        fun String.fromISO8601Format(): DateInstant {
            return DateInstant(try {
                ISO8601Format.parseDateTime(this).toInstant()
            } catch (e: Exception) {
                Log.e(TAG, "Unable to parse date time $this", e)
                Instant.now()
            })
        }

        fun DateInstant.toISO8601Format(): String {
            return ISO8601Format.print(this.instant)
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
    }
}
