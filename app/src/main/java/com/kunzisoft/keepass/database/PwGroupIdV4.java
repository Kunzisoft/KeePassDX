/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
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

import java.util.UUID;

public class PwGroupIdV4 extends PwGroupId {

	private UUID uuid;
	
	public PwGroupIdV4(UUID uuid) {
	    super();
        this.uuid = uuid;
	}

	public PwGroupIdV4(Parcel in) {
	    super(in);
		uuid = (UUID) in.readSerializable();
	}

    @Override
    public void writeToParcel(Parcel dest, int flags) {
	    super.writeToParcel(dest, flags);
        dest.writeSerializable(uuid);
    }

    public static final Creator<PwGroupIdV4> CREATOR = new Creator<PwGroupIdV4>() {
        @Override
        public PwGroupIdV4 createFromParcel(Parcel in) {
            return new PwGroupIdV4(in);
        }

        @Override
        public PwGroupIdV4[] newArray(int size) {
            return new PwGroupIdV4[size];
        }
    };

	@Override
	public boolean equals(Object id) {
		if ( ! (id instanceof PwGroupIdV4) ) {
			return false;
		}
		PwGroupIdV4 v4 = (PwGroupIdV4) id;
		return uuid.equals(v4.uuid);
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}

	public UUID getId() {
		return uuid;
	}
}
