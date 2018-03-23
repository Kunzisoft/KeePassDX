/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
package com.keepassdroid.database;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PwGroupV4 extends PwGroup implements ITimeLogger {

	public static final boolean DEFAULT_SEARCHING_ENABLED = true;

	// TODO Same in PwEntry4
	private PwGroupV4 parent = null;
	private UUID uuid = PwDatabaseV4.UUID_ZERO;
	private PwIconCustom customIcon = PwIconCustom.ZERO;
    private long usageCount = 0;
    private PwDate parentGroupLastMod = new PwDate();
    private Map<String, String> customData = new HashMap<>();

    private boolean expires = false;

    private String notes = "";
	private boolean isExpanded = true;
	private String defaultAutoTypeSequence = "";
	private Boolean enableAutoType = null;
	private Boolean enableSearching = null;
	private UUID lastTopVisibleEntry = PwDatabaseV4.UUID_ZERO;

	public PwGroupV4() {}
	
	public PwGroupV4(boolean createUUID, String name, PwIconStandard icon) {
		if (createUUID) {
			uuid = UUID.randomUUID();
		}
		
		this.name = name;
		this.icon = icon;
	}

    @Override
    public void initNewGroup(String nm, PwGroupId newId) {
        super.initNewGroup(nm, newId);

        parentGroupLastMod = new PwDate();
    }

	public void AddGroup(PwGroupV4 subGroup, boolean takeOwnership) {
		AddGroup(subGroup, takeOwnership, false);
	}
	
	public void AddGroup(PwGroupV4 subGroup, boolean takeOwnership, boolean updateLocationChanged) {
		if ( subGroup == null ) throw new RuntimeException("subGroup");
		
		childGroups.add(subGroup);
		
		if ( takeOwnership ) subGroup.parent = this;
		
		if ( updateLocationChanged ) subGroup.parentGroupLastMod = new PwDate(System.currentTimeMillis());
		
	}
	
	public void AddEntry(PwEntryV4 pe, boolean takeOwnership) {
		AddEntry(pe, takeOwnership, false);
	}
	
	public void AddEntry(PwEntryV4 pe, boolean takeOwnership, boolean updateLocationChanged) {
		assert(pe != null);

		addChildEntry(pe);
		
		if ( takeOwnership ) pe.setParent(this);
		
		if ( updateLocationChanged ) pe.setLocationChanged(new PwDate(System.currentTimeMillis()));
	}
	
	@Override
	public PwGroup getParent() {
		return parent;
	}

    @Override
    public void setParent(PwGroup prt) {
        parent = (PwGroupV4) prt;
    }

    public UUID getUUID() {
        return uuid;
    }

    public void setUUID(UUID u) {
        uuid = u;
    }
	
	public void buildChildGroupsRecursive(List<PwGroup> list) {
		list.add(this);
		
		for ( int i = 0; i < numbersOfChildGroups(); i++) {
			PwGroupV4 child = (PwGroupV4) childGroups.get(i);
			child.buildChildGroupsRecursive(list);
			
		}
	}

	public void buildChildEntriesRecursive(List<PwEntry> list) {
		for ( int i = 0; i < numbersOfChildEntries(); i++ ) {
			list.add(childEntries.get(i));
		}
		
		for ( int i = 0; i < numbersOfChildGroups(); i++ ) {
			PwGroupV4 child = (PwGroupV4) childGroups.get(i);
			child.buildChildEntriesRecursive(list);
		}
	}

    public PwIconCustom getCustomIcon() {
        return customIcon;
    }

    public void setCustomIcon(PwIconCustom icon) {
        this.customIcon = icon;
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
	public PwDate getLocationChanged() {
		return parentGroupLastMod;
	}

	@Override
	public void setLocationChanged(PwDate date) {
		parentGroupLastMod = date;
	}

    @Override
    public long getUsageCount() {
        return usageCount;
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
	public boolean allowAddEntryIfIsRoot() {
		return true;
	}

	@Override
	public PwIcon getIcon() {
		if (customIcon == null || customIcon.uuid.equals(PwDatabaseV4.UUID_ZERO)) {
			return super.getIcon();
		} else {
			return customIcon;
		}
	}

    public Map<String, String> getCustomData() {
        return customData;
    }

    public void setCustomData(Map<String, String> customData) {
        this.customData = customData;
    }

    public int sizeOfCustomData() {
        return customData.size();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public String getDefaultAutoTypeSequence() {
        return defaultAutoTypeSequence;
    }

    public void setDefaultAutoTypeSequence(String defaultAutoTypeSequence) {
        this.defaultAutoTypeSequence = defaultAutoTypeSequence;
    }

    public Boolean getEnableAutoType() {
        return enableAutoType;
    }

    public void setEnableAutoType(Boolean enableAutoType) {
        this.enableAutoType = enableAutoType;
    }

    public Boolean getEnableSearching() {
        return enableSearching;
    }

    public void setEnableSearching(Boolean enableSearching) {
        this.enableSearching = enableSearching;
    }

    public UUID getLastTopVisibleEntry() {
        return lastTopVisibleEntry;
    }

    public void setLastTopVisibleEntry(UUID lastTopVisibleEntry) {
        this.lastTopVisibleEntry = lastTopVisibleEntry;
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
		
		// If we get to the root tree and its null, default to true
		return true;
	}

}
