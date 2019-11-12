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
import com.kunzisoft.keepass.utils.Types
import java.util.*

/**
 * Converting from the C Date format to the Java data format is
 * expensive when done for every record at once.
 */
class PwDate : Parcelable {

    private var jDate: Date = Date()
    private var jDateBuilt = false
    @Transient
    private var cDate: ByteArray? = null
    @Transient
    private var cDateBuilt = false

    val date: Date
        get() {
            if (!jDateBuilt) {
                jDate = readTime(cDate, 0, calendar)
                jDateBuilt = true
            }

            return jDate
        }

    val byteArrayDate: ByteArray?
        get() {
            if (!cDateBuilt) {
                cDate = writeTime(jDate, calendar)
                cDateBuilt = true
            }

            return cDate
        }

    constructor(buf: ByteArray, offset: Int) {
        cDate = ByteArray(DATE_SIZE)
        System.arraycopy(buf, offset, cDate!!, 0, DATE_SIZE)
        cDateBuilt = true
    }

    constructor(source: PwDate) {
        this.jDate = Date(source.jDate.time)
        this.jDateBuilt = source.jDateBuilt

        if (source.cDate != null) {
            val dateLength = source.cDate!!.size
            this.cDate = ByteArray(dateLength)
            System.arraycopy(source.cDate!!, 0, this.cDate!!, 0, dateLength)
        }
        this.cDateBuilt = source.cDateBuilt
    }

    constructor(date: Date) {
        jDate = Date(date.time)
        jDateBuilt = true
    }

    constructor(millis: Long) {
        jDate = Date(millis)
        jDateBuilt = true
    }

    constructor() {
        jDate = Date()
        jDateBuilt = true
    }

    protected constructor(parcel: Parcel) {
        jDate = parcel.readSerializable() as Date
        jDateBuilt = parcel.readByte().toInt() != 0
        cDateBuilt = false
    }

    override fun describeContents(): Int {
        return 0
    }

    fun getDateTimeString(resources: Resources): String {
        return Companion.getDateTimeString(resources, this.date)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeSerializable(date)
        dest.writeByte((if (jDateBuilt) 1 else 0).toByte())
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

        val date = other as PwDate?
        return if (cDateBuilt && date!!.cDateBuilt) {
            Arrays.equals(cDate, date.cDate)
        } else if (jDateBuilt && date!!.jDateBuilt) {
            isSameDate(jDate, date.jDate)
        } else if (cDateBuilt && date!!.jDateBuilt) {
            Arrays.equals(date.byteArrayDate, cDate)
        } else {
            isSameDate(date!!.date, jDate)
        }
    }

    override fun hashCode(): Int {
        var result = jDate.hashCode()
        result = 31 * result + jDateBuilt.hashCode()
        result = 31 * result + (cDate?.contentHashCode() ?: 0)
        result = 31 * result + cDateBuilt.hashCode()
        return result
    }

    companion object {

        private const val DATE_SIZE = 5

        private var mCalendar: Calendar? = null

        val NEVER_EXPIRE = neverExpire

        private val calendar: Calendar?
            get() {
                if (mCalendar == null) {
                    mCalendar = Calendar.getInstance()
                }
                return mCalendar
            }

        private val neverExpire: PwDate
            get() {
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, 2999)
                cal.set(Calendar.MONTH, 11)
                cal.set(Calendar.DAY_OF_MONTH, 28)
                cal.set(Calendar.HOUR, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)

                return PwDate(cal.time)
            }

        @JvmField
        val CREATOR: Parcelable.Creator<PwDate> = object : Parcelable.Creator<PwDate> {
            override fun createFromParcel(parcel: Parcel): PwDate {
                return PwDate(parcel)
            }

            override fun newArray(size: Int): Array<PwDate?> {
                return arrayOfNulls(size)
            }
        }

        /**
         * Unpack date from 5 byte format. The five bytes at 'offset' are unpacked
         * to a java.util.Date instance.
         */
        fun readTime(buf: ByteArray?, offset: Int, calendar: Calendar?): Date {
            var time = calendar
            val dw1 = Types.readUByte(buf!!, offset)
            val dw2 = Types.readUByte(buf, offset + 1)
            val dw3 = Types.readUByte(buf, offset + 2)
            val dw4 = Types.readUByte(buf, offset + 3)
            val dw5 = Types.readUByte(buf, offset + 4)

            // Unpack 5 byte structure to date and time
            val year = dw1 shl 6 or (dw2 shr 2)
            val month = dw2 and 0x00000003 shl 2 or (dw3 shr 6)

            val day = dw3 shr 1 and 0x0000001F
            val hour = dw3 and 0x00000001 shl 4 or (dw4 shr 4)
            val minute = dw4 and 0x0000000F shl 2 or (dw5 shr 6)
            val second = dw5 and 0x0000003F

            if (time == null) {
                time = Calendar.getInstance()
            }
            // File format is a 1 based month, java Calendar uses a zero based month
            // File format is a 1 based day, java Calendar uses a 1 based day
            time!!.set(year, month - 1, day, hour, minute, second)

            return time.time

        }

        @JvmOverloads
        fun writeTime(date: Date?, calendar: Calendar? = null): ByteArray? {
            var cal = calendar
            if (date == null) {
                return null
            }

            val buf = ByteArray(5)
            if (cal == null) {
                cal = Calendar.getInstance()
            }
            cal!!.time = date

            val year = cal.get(Calendar.YEAR)
            // File format is a 1 based month, java Calendar uses a zero based month
            val month = cal.get(Calendar.MONTH) + 1
            // File format is a 0 based day, java Calendar uses a 1 based day
            val day = cal.get(Calendar.DAY_OF_MONTH) - 1
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            val second = cal.get(Calendar.SECOND)

            buf[0] = Types.writeUByte(year shr 6 and 0x0000003F)
            buf[1] = Types.writeUByte(year and 0x0000003F shl 2 or (month shr 2 and 0x00000003))
            buf[2] = (month and 0x00000003 shl 6
                    or (day and 0x0000001F shl 1) or (hour shr 4 and 0x00000001)).toByte()
            buf[3] = (hour and 0x0000000F shl 4 or (minute shr 2 and 0x0000000F)).toByte()
            buf[4] = (minute and 0x00000003 shl 6 or (second and 0x0000003F)).toByte()

            return buf
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
