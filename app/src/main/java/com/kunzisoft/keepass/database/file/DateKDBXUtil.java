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
package com.kunzisoft.keepass.database.file;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import java.util.Date;

public class DateKDBXUtil {
    private static final DateTime dotNetEpoch = new DateTime(1, 1, 1, 0, 0, 0, DateTimeZone.UTC);
    private static final DateTime javaEpoch = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeZone.UTC);

    private static final long epochOffset;

    static {
        epochOffset = (javaEpoch.getMillis() - dotNetEpoch.getMillis()) / 1000L;
    }

    public static Date convertKDBX4Time(long seconds) {
        DateTime dt = dotNetEpoch.plus(seconds * 1000L);
        // Switch corrupted dates to a more recent date that won't cause issues on the client
        if (dt.isBefore(javaEpoch)) {
            return javaEpoch.toDate();
        }
        return dt.toDate();
    }

    public static long convertDateToKDBX4Time(DateTime dt) {
        Duration duration = new Duration( javaEpoch, dt );
        long seconds = ( duration.getMillis() / 1000L );
        return seconds + epochOffset;
    }
}
