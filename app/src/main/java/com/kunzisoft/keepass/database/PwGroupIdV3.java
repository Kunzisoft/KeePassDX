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

public class PwGroupIdV3 extends PwGroupId {

	private int id;
	
	public PwGroupIdV3(int groupId) {
	    super();
		this.id = groupId;
	}

	public PwGroupIdV3(Parcel in) {
        super(in);
        id = in.readInt();
	}

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(id);
    }

    public static final Creator<PwGroupIdV3> CREATOR = new Creator<PwGroupIdV3>() {
        @Override
        public PwGroupIdV3 createFromParcel(Parcel in) {
            return new PwGroupIdV3(in);
        }

        @Override
        public PwGroupIdV3[] newArray(int size) {
            return new PwGroupIdV3[size];
        }
    };
	
	@Override
	public boolean equals(Object compare) {
		if ( ! (compare instanceof PwGroupIdV3) ) {
			return false;
		}
		PwGroupIdV3 cmp = (PwGroupIdV3) compare;
		return id == cmp.id;
	}

	@Override
	public int hashCode() {
		Integer i = id;
		return i.hashCode();
	}
	
	public int getId() {
		return id;
	}
}
