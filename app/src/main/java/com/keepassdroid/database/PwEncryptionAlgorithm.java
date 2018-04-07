/*
 * Copyright 2018 Brian Pellin, Jeremy Jamet / Kunzisoft, Justin Gross.
 *     
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass Libre is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass Libre.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.database;

import android.content.res.Resources;

import tech.jgross.keepass.R;

public enum PwEncryptionAlgorithm {

    AES_Rijndael,
	Twofish,
	ChaCha20;

    public String getName(Resources resources) {
        switch (this) {
            default:
            case AES_Rijndael:
                return resources.getString(R.string.rijndael);
            case Twofish:
                return resources.getString(R.string.twofish);
            case ChaCha20:
                return resources.getString(R.string.chacha20);
        }
    }
}
