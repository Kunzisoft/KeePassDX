/*
 * Copyright 2018 Brian Pellin, Jeremy Jamet / Kunzisoft, Justin Gross.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
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
package com.keepassdroid.utils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Seconds;

import java.util.Date;

public class DateUtil {
    private static final DateTime dotNetEpoch = new DateTime(1, 1, 1, 0, 0, 0, DateTimeZone.UTC);
    private static final DateTime javaEpoch = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeZone.UTC);

    private static final long epochOffset;

    static {
        Date dotNet = dotNetEpoch.toDate();
        Date java = javaEpoch.toDate();

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
        Seconds secs = Seconds.secondsBetween(javaEpoch, dt);
        return secs.getSeconds() + epochOffset;
    }
}
