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

public class PwIconStandard extends PwIcon {
	public final int iconId;

	// The first is number 0
	public static PwIconStandard FIRST = new PwIconStandard(0);
	
	public static final int TRASH_BIN = 43;
	public static final int FOLDER = 48;
	
	public PwIconStandard(int iconId) {
		this.iconId = iconId;
	}

    public PwIconStandard(PwIconStandard icon) {
        this.iconId = icon.iconId;
    }

	@Override
	public boolean isMetaStreamIcon() {
		return iconId == 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + iconId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PwIconStandard other = (PwIconStandard) obj;
		if (iconId != other.iconId)
			return false;
		return true;
	}
}
