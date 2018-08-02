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

import java.util.UUID;

public class PwIconCustom extends PwIcon {
	public static final PwIconCustom ZERO = new PwIconCustom(PwDatabase.UUID_ZERO, new byte[0]);
	
	private final UUID uuid;
	transient private byte[] imageData;
	
	public PwIconCustom(UUID uuid, byte[] data) {
	    super();
		this.uuid = uuid;
		this.imageData = data;
	}

    public PwIconCustom(PwIconCustom icon) {
	    super();
        uuid = icon.uuid;
        imageData = icon.imageData;
    }

	protected PwIconCustom(Parcel in) {
	    super(in);
        uuid = (UUID) in.readSerializable();
        // TODO Take too much memories
        // in.readByteArray(imageData);
	}

	@Override
	public boolean isUnknown() {
		return uuid == null || this.equals(ZERO);
	}

    public UUID getUUID() {
        return uuid;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    @Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeSerializable(uuid);
		// Too big for a parcelable dest.writeByteArray(imageData);
	}

    public static final Creator<PwIconCustom> CREATOR = new Creator<PwIconCustom>() {
        @Override
        public PwIconCustom createFromParcel(Parcel in) {
            return new PwIconCustom(in);
        }

        @Override
        public PwIconCustom[] newArray(int size) {
            return new PwIconCustom[size];
        }
    };

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
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
		PwIconCustom other = (PwIconCustom) obj;
		if (uuid == null) {
			return other.uuid == null;
		} else return uuid.equals(other.uuid);
	}
}
