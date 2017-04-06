/*
 * Copyright 2010-2017 Brian Pellin.
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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PwGroupV4 extends PwGroup implements ITimeLogger {

	//public static final int FOLDER_ICON = 48;
	public static final boolean DEFAULT_SEARCHING_ENABLED = true;
	
	public PwGroupV4 parent = null;
	public UUID uuid = PwDatabaseV4.UUID_ZERO;
	public String notes = "";
	public PwIconCustom customIcon = PwIconCustom.ZERO;
	public boolean isExpanded = true;
	public String defaultAutoTypeSequence = "";
	public Boolean enableAutoType = null;
	public Boolean enableSearching = null;
	public UUID lastTopVisibleEntry = PwDatabaseV4.UUID_ZERO;
	private Date parentGroupLastMod = PwDatabaseV4.DEFAULT_NOW;
	private Date creation = PwDatabaseV4.DEFAULT_NOW;
	private Date lastMod = PwDatabaseV4.DEFAULT_NOW;
	private Date lastAccess = PwDatabaseV4.DEFAULT_NOW;
	private Date expireDate = PwDatabaseV4.DEFAULT_NOW;
	private boolean expires = false;
	private long usageCount = 0;
	public Map<String, String> customData = new HashMap<String, String>();

	public PwGroupV4() {
		
	}
	
	public PwGroupV4(boolean createUUID, boolean setTimes, String name, PwIconStandard icon) {
		if (createUUID) {
			uuid = UUID.randomUUID();
		}
		
		if (setTimes) {
			creation = lastMod = lastAccess = new Date();
		}
		
		this.name = name;
		this.icon = icon;
	}
	
	public void AddGroup(PwGroupV4 subGroup, boolean takeOwnership) {
		AddGroup(subGroup, takeOwnership, false);
	}
	
	public void AddGroup(PwGroupV4 subGroup, boolean takeOwnership, boolean updateLocationChanged) {
		if ( subGroup == null ) throw new RuntimeException("subGroup");
		
		childGroups.add(subGroup);
		
		if ( takeOwnership ) subGroup.parent = this;
		
		if ( updateLocationChanged ) subGroup.parentGroupLastMod = new Date(System.currentTimeMillis());
		
	}
	
	public void AddEntry(PwEntryV4 pe, boolean takeOwnership) {
		AddEntry(pe, takeOwnership, false);
	}
	
	public void AddEntry(PwEntryV4 pe, boolean takeOwnership, boolean updateLocationChanged) {
		assert(pe != null);
		
		childEntries.add(pe);
		
		if ( takeOwnership ) pe.parent = this;
		
		if ( updateLocationChanged ) pe.setLocationChanged(new Date(System.currentTimeMillis()));
	}
	
	@Override
	public PwGroup getParent() {
		return parent;
	}
	
	public void buildChildGroupsRecursive(List<PwGroup> list) {
		list.add(this);
		
		for ( int i = 0; i < childGroups.size(); i++) {
			PwGroupV4 child = (PwGroupV4) childGroups.get(i);
			child.buildChildGroupsRecursive(list);
			
		}
	}

	public void buildChildEntriesRecursive(List<PwEntry> list) {
		for ( int i = 0; i < childEntries.size(); i++ ) {
			list.add(childEntries.get(i));
		}
		
		for ( int i = 0; i < childGroups.size(); i++ ) {
			PwGroupV4 child = (PwGroupV4) childGroups.get(i);
			child.buildChildEntriesRecursive(list);
		}
		
	}

	@Override
	public PwGroupId getId() {
		return new PwGroupIdV4(uuid);
	}

	@Override
	public void setId(PwGroupId id) {
		PwGroupIdV4 id4 = (PwGroupIdV4) id;
		uuid = id4.getId();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Date getLastMod() {
		return parentGroupLastMod;
	}

	public Date getCreationTime() {
		return creation;
	}

	public Date getExpiryTime() {
		return expireDate;
	}

	public Date getLastAccessTime() {
		return lastAccess;
	}

	public Date getLastModificationTime() {
		return lastMod;
	}

	public Date getLocationChanged() {
		return parentGroupLastMod;
	}

	public long getUsageCount() {
		return usageCount;
	}

	public void setCreationTime(Date date) {
		creation = date;
		
	}

	public void setExpiryTime(Date date) {
		expireDate = date;
	}

	@Override
	public void setLastAccessTime(Date date) {
		lastAccess = date;
	}

	@Override
	public void setLastModificationTime(Date date) {
		lastMod = date;
	}

	public void setLocationChanged(Date date) {
		parentGroupLastMod = date;
	}

	public void setUsageCount(long count) {
		usageCount = count;
	}

	public boolean expires() {
		return expires;
	}

	public void setExpires(boolean exp) {
		expires = exp;
	}

	@Override
	public void setParent(PwGroup prt) {
		parent = (PwGroupV4) prt;
		
	}

	@Override
	public PwIcon getIcon() {
		if (customIcon == null || customIcon.uuid.equals(PwDatabaseV4.UUID_ZERO)) {
			return super.getIcon();
		} else {
			return customIcon;
		}
	}
	
	@Override
	public void initNewGroup(String nm, PwGroupId newId) {
		super.initNewGroup(nm, newId);
		
		lastAccess = lastMod = creation = parentGroupLastMod = new Date();
	}
	
	public boolean isSearchEnabled() {
		PwGroupV4 group = this;
		while (group != null) {
			Boolean search = group.enableSearching;
			if (search != null) {
				return search;
			}
			
			group = group.parent;
		}
		
		// If we get to the root group and its null, default to true
		return true;
	}

}
