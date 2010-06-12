/*
 * Copyright 2009 Brian Pellin.
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
package com.keepassdroid.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public abstract class PwGroup {
	public List<PwGroup> childGroups = new ArrayList<PwGroup>();
	public List<PwEntry> childEntries = new ArrayList<PwEntry>();

	public abstract PwGroup getParent();
	
	public abstract PwGroupId getId();
	
	public abstract String getName();
	
	public abstract Date getLastMod();

	public void sortGroupsByName() {
		Collections.sort(childGroups, new GroupNameComparator());
	}

	private class GroupNameComparator implements Comparator<PwGroup> {

		@Override
		public int compare(PwGroup object1, PwGroup object2) {
			return object1.getName().compareToIgnoreCase(object2.getName());
		}
		
	}
	
	public void sortEntriesByName() {
		Collections.sort(childEntries, new EntryNameComparator());
	}
	
	private class EntryNameComparator implements Comparator<PwEntry> {

		@Override
		public int compare(PwEntry object1, PwEntry object2) {
			return object1.getTitle().compareToIgnoreCase(object2.getTitle());
		}
		
	}

}
