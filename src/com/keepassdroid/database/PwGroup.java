/*
 * Copyright 2009-2014 Brian Pellin.
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

import com.keepassdroid.utils.StrUtil;

public abstract class PwGroup {
	public List<PwGroup> childGroups = new ArrayList<PwGroup>();
	public List<PwEntry> childEntries = new ArrayList<PwEntry>();
	public String name = "";
	public PwIconStandard icon;

	public abstract PwGroup getParent();
	public abstract void setParent(PwGroup parent);
	
	public abstract PwGroupId getId();
	public abstract void setId(PwGroupId id);

	public abstract String getName();
	
	public abstract Date getLastMod();
	
	public PwIcon getIcon() {
		return icon;
	}

	public void sortGroupsByName() {
		Collections.sort(childGroups, new GroupNameComparator());
	}

	public static class GroupNameComparator implements Comparator<PwGroup> {

		public int compare(PwGroup object1, PwGroup object2) {
			return object1.getName().compareToIgnoreCase(object2.getName());
		}
		
	}
	
	public abstract void setLastAccessTime(Date date);

	public abstract void setLastModificationTime(Date date);
	
	public void sortEntriesByName() {
		Collections.sort(childEntries, new PwEntry.EntryNameComparator());
	}
	
	public void initNewGroup(String nm, PwGroupId newId) {
		setId(newId);
		name = nm;
	}
	
	public boolean isContainedIn(PwGroup container) {
		PwGroup cur = this;
		while (cur != null) {
			if (cur == container) {
				return true;
			}
			
			cur = cur.getParent();
		}
		
		return false;
	}
	
	public void touch(boolean modified, boolean touchParents) {
		Date now = new Date();
		
		setLastAccessTime(now);
		
		if (modified) {
			setLastModificationTime(now);
		}
		
		PwGroup parent = getParent();
		if (touchParents && parent != null) {
			parent.touch(modified, true);
		}
	}
	
	
	public void searchEntries(SearchParameters sp, List<PwEntry> listStorage) {
		if (sp == null)  { return; }
		if (listStorage == null) { return; }
		
		List<String> terms = StrUtil.splitSearchTerms(sp.searchString);
		if (terms.size() <= 1 || sp.regularExpression) {
			searchEntriesSingle(sp, listStorage);
			return;
		}
		
		// Search longest term first
		Comparator<String> stringLengthComparator = new Comparator<String>() {
	
			@Override
			public int compare(String lhs, String rhs) {
				return lhs.length() - rhs.length();
			}
			
		};
		Collections.sort(terms, stringLengthComparator);
		
		String fullSearch = sp.searchString;
		List<PwEntry> pg = this.childEntries;
		for (int i = 0; i < terms.size(); i ++) {
			List<PwEntry> pgNew = new ArrayList<PwEntry>();
			
			sp.searchString = terms.get(i);
			
			boolean negate = false;
			if (sp.searchString.startsWith("-")) {
				sp.searchString.substring(1);
				negate = sp.searchString.length() > 0;
			}
			
			if (!searchEntriesSingle(sp, pgNew)) {
				pg = null;
				break;
			}
			
			List<PwEntry> complement = new ArrayList<PwEntry>();
			if (negate) {
				for (PwEntry entry: pg) {
					if (!pgNew.contains(entry)) {
						complement.add(entry);
					}
				}
				
				pg = complement;
			}
			else {
				pg = pgNew;
			}
		}
		
		if (pg != null) {
			listStorage.addAll(pg);
		}
		sp.searchString = fullSearch;

	}
	
	private boolean searchEntriesSingle(SearchParameters spIn, List<PwEntry> listStorage) {
		SearchParameters sp = (SearchParameters) spIn.clone();
		
		EntryHandler<PwEntry> eh;
		if (sp.searchString.length() <= 0) {
			eh = new EntrySearchHandlerAll(sp, listStorage);
		} else {
			eh = EntrySearchHandler.getInstance(this, sp, listStorage);
		}
		
		if (!preOrderTraverseTree(null, eh)) { return false; }
		
		return true;
	}

	public boolean preOrderTraverseTree(GroupHandler<PwGroup> groupHandler, EntryHandler<PwEntry> entryHandler) {
		if (entryHandler != null) {
			for (PwEntry entry : childEntries) {
				if (!entryHandler.operate(entry)) return false;
				
			}
		}
	
		for (PwGroup group : childGroups) {
			
			if ((groupHandler != null) && !groupHandler.operate(group)) return false;
			
			group.preOrderTraverseTree(groupHandler, entryHandler);
		}
		
		
		return true;
		
	}
}
