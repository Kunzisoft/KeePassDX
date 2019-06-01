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
package com.kunzisoft.keepass.database.element;

import android.os.Parcel;

import java.util.UUID;

public class PwNodeIdUUID extends PwNodeId<UUID> {

	private UUID uuid;

	public PwNodeIdUUID() {
		this(UUID.randomUUID());
	}

	public PwNodeIdUUID(PwNodeIdUUID source) {
		this(source.uuid);
	}

	public PwNodeIdUUID(UUID uuid) {
	    super();
        this.uuid = uuid;
	}

	public PwNodeIdUUID(Parcel in) {
	    super(in);
		uuid = (UUID) in.readSerializable();
	}

    @Override
    public void writeToParcel(Parcel dest, int flags) {
	    super.writeToParcel(dest, flags);
        dest.writeSerializable(uuid);
    }

    public static final Creator<PwNodeIdUUID> CREATOR = new Creator<PwNodeIdUUID>() {
        @Override
        public PwNodeIdUUID createFromParcel(Parcel in) {
            return new PwNodeIdUUID(in);
        }

        @Override
        public PwNodeIdUUID[] newArray(int size) {
            return new PwNodeIdUUID[size];
        }
    };

	@Override
	public boolean equals(Object id) {
		if ( ! (id instanceof PwNodeIdUUID) ) {
			return false;
		}
		PwNodeIdUUID v4 = (PwNodeIdUUID) id;
		return uuid.equals(v4.uuid);
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}

	@Override
	public String toString() {
		return uuid.toString();
	}

	public UUID getId() {
		return uuid;
	}
}
