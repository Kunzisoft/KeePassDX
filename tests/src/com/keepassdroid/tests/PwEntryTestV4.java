/*
 * Copyright 2010-2013 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
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
package com.keepassdroid.tests;

import java.util.UUID;

import junit.framework.TestCase;

import com.keepassdroid.database.PwEntryV4;
import com.keepassdroid.database.PwGroupV4;
import com.keepassdroid.database.PwIconCustom;
import com.keepassdroid.database.PwIconStandard;
import com.keepassdroid.database.security.ProtectedBinary;

public class PwEntryTestV4 extends TestCase {
	public void testAssign() {
		PwEntryV4 entry = new PwEntryV4();
		
		entry.additional = "test223";
		
		entry.autoType = entry.new AutoType();
		entry.autoType.defaultSequence = "1324";
		entry.autoType.enabled = true;
		entry.autoType.obfuscationOptions = 123412432109L;
		entry.autoType.put("key", "value");
		
		entry.backgroupColor = "blue";
		entry.binaries.put("key1", new ProtectedBinary(false, new byte[] {0,1}));
		entry.customIcon = new PwIconCustom(UUID.randomUUID(), new byte[0]);
		entry.foregroundColor = "red";
		entry.history.add(new PwEntryV4());
		entry.icon = new PwIconStandard(5);
		entry.overrideURL = "override";
		entry.parent = new PwGroupV4();
		entry.strings.put("key2", "value2");
		entry.url = "http://localhost";
		entry.uuid = UUID.randomUUID();

		PwEntryV4 target = new PwEntryV4();
		target.assign(entry);
		
		/* This test is not so useful now that I am not implementing value equality for Entries
		assertTrue("Entries do not match.", entry.equals(target));
		*/
		
	}

}
