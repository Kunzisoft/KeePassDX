/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.utils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Date;

public class DateUtil {
    private static final DateTime dotNetEpoch = new DateTime(1, 1, 1, 0, 0, 0, DateTimeZone.UTC);

    public static Date convertKDBX4Time(long seconds) {
        return dotNetEpoch.plus(seconds).toDate();
    }
}
