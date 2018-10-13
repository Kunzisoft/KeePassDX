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

import android.os.Parcel;

public class PwIconStandard extends PwIcon {
	private final int iconId;

	public static final int UNKNOWN = -1;
	public static final int KEY = 0;
	public static final int TRASH = 43;
	public static final int FOLDER = 48;

	public PwIconStandard() {
		this.iconId = KEY;
	}

	public PwIconStandard(int iconId) {
		this.iconId = iconId;
	}

    public PwIconStandard(PwIconStandard icon) {
        this.iconId = icon.iconId;
    }

	protected PwIconStandard(Parcel in) {
		super(in);
		iconId = in.readInt();
	}

	@Override
	public boolean isUnknown() {
	    return iconId == UNKNOWN;
    }

    public int getIconId() {
        return iconId;
    }

    @Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(iconId);
	}

	public static final Creator<PwIconStandard> CREATOR = new Creator<PwIconStandard>() {
		@Override
		public PwIconStandard createFromParcel(Parcel in) {
			return new PwIconStandard(in);
		}

		@Override
		public PwIconStandard[] newArray(int size) {
			return new PwIconStandard[size];
		}
	};

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
