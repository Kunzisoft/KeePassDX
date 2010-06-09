/*
 * Copyright 2010 Brian Pellin.
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
import java.util.UUID;
import java.util.Vector;

public class PwGroupV4 extends PwGroup implements ITimeLogger {

	public PwGroupV4 parent = null;
	public UUID uuid;
	public String name;
	public String notes;
	public int iconId;
	public UUID customIconUuid;
	public boolean isExpanded;
	public String defaultAutoTypeSequence;
	public Boolean enableAutoType;
	public Boolean enableSearching;
	public UUID lastTopVisibleEntry;
	private Date parentGroupLastMod;
	private Date creation;
	private Date lastMod;
	private Date lastAccess;
	private Date expireDate;
	private boolean expires = false;
	private long usageCount = 0;
		
	public PwGroupV4() {
		
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
	
	public void buildChildGroupsRecursive(Vector<PwGroup> list) {
		list.add(this);
		
		for ( int i = 0; i < childGroups.size(); i++) {
			PwGroupV4 child = (PwGroupV4) childGroups.get(i);
			child.buildChildGroupsRecursive(list);
			
		}
	}

	public void buildChildEntriesRecursive(Vector<PwEntry> list) {
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
	public String getName() {
		return name;
	}

	@Override
	public Date getLastMod() {
		return parentGroupLastMod;
	}

	@Override
	public Date getCreationTime() {
		return creation;
	}

	@Override
	public Date getExpiryTime() {
		return expireDate;
	}

	@Override
	public Date getLastAccessTime() {
		return lastAccess;
	}

	@Override
	public Date getLastModificationTime() {
		return lastMod;
	}

	@Override
	public Date getLocationChanged() {
		return parentGroupLastMod;
	}

	@Override
	public long getUsageCount() {
		return usageCount;
	}

	@Override
	public void setCreationTime(Date date) {
		creation = date;
		
	}

	@Override
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

	@Override
	public void setLocationChanged(Date date) {
		parentGroupLastMod = date;
	}

	@Override
	public void setUsageCount(long count) {
		usageCount = count;
	}

	@Override
	public boolean expires() {
		return expires;
	}

	@Override
	public void setExpires(boolean exp) {
		expires = exp;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PwGroupV4 other = (PwGroupV4) obj;
		if (creation == null) {
			if (other.creation != null)
				return false;
		} else if (!creation.equals(other.creation))
			return false;
		if (customIconUuid == null) {
			if (other.customIconUuid != null)
				return false;
		} else if (!customIconUuid.equals(other.customIconUuid))
			return false;
		if (defaultAutoTypeSequence == null) {
			if (other.defaultAutoTypeSequence != null)
				return false;
		} else if (!defaultAutoTypeSequence
				.equals(other.defaultAutoTypeSequence))
			return false;
		if (enableAutoType == null) {
			if (other.enableAutoType != null)
				return false;
		} else if (!enableAutoType.equals(other.enableAutoType))
			return false;
		if (enableSearching == null) {
			if (other.enableSearching != null)
				return false;
		} else if (!enableSearching.equals(other.enableSearching))
			return false;
		if (expireDate == null) {
			if (other.expireDate != null)
				return false;
		} else if (!expireDate.equals(other.expireDate))
			return false;
		if (expires != other.expires)
			return false;
		if (iconId != other.iconId)
			return false;
		if (isExpanded != other.isExpanded)
			return false;
		if (lastAccess == null) {
			if (other.lastAccess != null)
				return false;
		} else if (!lastAccess.equals(other.lastAccess))
			return false;
		if (lastMod == null) {
			if (other.lastMod != null)
				return false;
		} else if (!lastMod.equals(other.lastMod))
			return false;
		if (lastTopVisibleEntry == null) {
			if (other.lastTopVisibleEntry != null)
				return false;
		} else if (!lastTopVisibleEntry.equals(other.lastTopVisibleEntry))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (notes == null) {
			if (other.notes != null)
				return false;
		} else if (!notes.equals(other.notes))
			return false;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		if (parentGroupLastMod == null) {
			if (other.parentGroupLastMod != null)
				return false;
		} else if (!parentGroupLastMod.equals(other.parentGroupLastMod))
			return false;
		if (usageCount != other.usageCount)
			return false;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}
	
	

}
