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
package com.kunzisoft.keepass.database.file

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Duration
import java.util.*

object DateKDBXUtil {

    private val dotNetEpoch = DateTime(1, 1, 1, 0, 0, 0, DateTimeZone.UTC)
    private val javaEpoch = DateTime(1970, 1, 1, 0, 0, 0, DateTimeZone.UTC)
    private val epochOffset = (javaEpoch.millis - dotNetEpoch.millis) / 1000L

    fun convertKDBX4Time(seconds: Long): Date {
        val dt = dotNetEpoch.plus(seconds * 1000L)
        // Switch corrupted dates to a more recent date that won't cause issues on the client
        return if (dt.isBefore(javaEpoch)) {
            javaEpoch.toDate()
        } else dt.toDate()
    }

    fun convertDateToKDBX4Time(date: Date): Long {
        val duration = Duration(javaEpoch, DateTime(date))
        val seconds = duration.millis / 1000L
        return seconds + epochOffset
    }
}