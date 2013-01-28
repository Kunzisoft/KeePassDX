/*
 * Copyright 2013 Brian Pellin.
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
package com.keepassdroid;

import java.util.UUID;

import android.content.Intent;

import com.keepassdroid.database.PwGroupId;
import com.keepassdroid.database.PwGroupIdV4;
import com.keepassdroid.database.PwGroupV4;
import com.keepassdroid.utils.Types;

public class EntryEditActivityV4 extends EntryEditActivity {

	protected static void putParentId(Intent i, String parentKey, PwGroupV4 parent) {
		PwGroupId id = parent.getId();
		PwGroupIdV4 id4 = (PwGroupIdV4) id;
		
		i.putExtra(parentKey, Types.UUIDtoBytes(id4.getId()));
		
	}

	@Override
	protected PwGroupId getParentGroupId(Intent i, String key) {
		byte[] buf = i.getByteArrayExtra(key);
		UUID id = Types.bytestoUUID(buf);
		
		return new PwGroupIdV4(id);
	}
}
