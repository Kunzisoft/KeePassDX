/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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

package com.kunzisoft.keepass.database;

/**
 * @author Brian Pellin <bpellin@gmail.com>
 * @author Naomaru Itoi <nao@phoneid.org>
 * @author Bill Zwicky <wrzwicky@pobox.com>
 * @author Dominik Reichl <dominik.reichl@t-online.de>
 */
public class PwGroupV3 extends PwGroup<PwGroupV3, PwGroupV3, PwEntryV3> {

	// for tree traversing
	private int groupId;

	private int level = 0; // short

	/** Used by KeePass internally, don't use */
	private int flags;

    public PwGroupV3() {
        super();
    }

    public PwGroupV3(PwGroupV3 p) {
        construct(p);
    }

    protected void updateWith(PwGroupV3 source) {
        super.assign(source);
        groupId = source.groupId;
        level = source.level;
        flags = source.flags;
    }

    @SuppressWarnings("unchecked")
    @Override
    public PwGroupV3 clone() {
        // newGroup.groupId stay the same in copy
        // newGroup.level stay the same in copy
        // newGroup.flags stay the same in copy
        return (PwGroupV3) super.clone();
    }

    @Override
    public void setParent(PwGroupV3 parent) {
        super.setParent(parent);
        level = this.parent.getLevel() + 1;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

	@Override
	public PwGroupId getId() {
		return new PwGroupIdV3(groupId);
	}

	@Override
	public void setId(PwGroupId id) {
		PwGroupIdV3 id3 = (PwGroupIdV3) id;
		groupId = id3.getId();
	}

	public int getFlags() {
	    return flags;
    }

    public void setFlags(int flags) {
	    this.flags = flags;
    }

	@Override
    public String toString() {
        return getName();
    }

    public void populateBlankFields(PwDatabaseV3 db) {
	    // TODO populate blanck field
        if (icon == null) {
            icon = db.getIconFactory().getFolderIcon();
        }

        if (name == null) {
            name = "";
        }
    }
}
