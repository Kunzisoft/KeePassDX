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

/**
 * @author Brian Pellin <bpellin@gmail.com>
 * @author Naomaru Itoi <nao@phoneid.org>
 * @author Bill Zwicky <wrzwicky@pobox.com>
 * @author Dominik Reichl <dominik.reichl@t-online.de>
 */
public class PwGroupV3 extends PwGroup {

	// for tree traversing
	private PwGroupV3 parent = null;
	private int groupId;

	private int level = 0; // short

	/** Used by KeePass internally, don't use */
	private int flags;

    public PwGroupV3() {
        super();
    }

	@Override
	public PwGroup getParent() {
		return parent;
	}

    @Override
    public void setParent(PwGroup prt) {
        parent = (PwGroupV3) prt;
        level = parent.getLevel() + 1;
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
            icon = db.getIconFactory().getFirstIcon();
        }

        if (name == null) {
            name = "";
        }
    }
}
