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
package com.kunzisoft.keepass.tests

import junit.framework.TestCase

import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.utils.DatabaseInputOutputUtils
import org.junit.Assert

class PwDateTest : TestCase() {

    fun testDate() {
        val jDate = DateInstant(System.currentTimeMillis())
        val intermediate = DateInstant(jDate)
        val cDate = DatabaseInputOutputUtils.readCDate(DatabaseInputOutputUtils.writeCDate(intermediate.date)!!, 0)

        Assert.assertTrue("jDate and intermediate not equal", jDate == intermediate)
        Assert.assertTrue("jDate $jDate and cDate $cDate not equal", cDate == jDate)
    }
}
