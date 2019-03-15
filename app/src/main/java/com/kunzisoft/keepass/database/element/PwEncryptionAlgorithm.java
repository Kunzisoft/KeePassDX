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
package com.kunzisoft.keepass.database.element;

import android.content.res.Resources;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.crypto.engine.AesEngine;
import com.kunzisoft.keepass.crypto.engine.ChaCha20Engine;
import com.kunzisoft.keepass.crypto.engine.CipherEngine;
import com.kunzisoft.keepass.crypto.engine.TwofishEngine;
import com.kunzisoft.keepass.database.ObjectNameResource;

import java.util.UUID;

public enum PwEncryptionAlgorithm implements ObjectNameResource {

    AES_Rijndael,
	Twofish,
	ChaCha20;

    public String getName(Resources resources) {
        switch (this) {
            default:
            case AES_Rijndael:
                return resources.getString(R.string.encryption_rijndael);
            case Twofish:
                return resources.getString(R.string.encryption_twofish);
            case ChaCha20:
                return resources.getString(R.string.encryption_chacha20);
        }
    }

    public CipherEngine getCipherEngine() {
        switch (this) {
            default:
            case AES_Rijndael:
                return new AesEngine();
            case Twofish:
                return new TwofishEngine();
            case ChaCha20:
                return new ChaCha20Engine();
        }
    }

    public UUID getDataCipher() {
        switch (this) {
            default:
            case AES_Rijndael:
                return AesEngine.CIPHER_UUID;
            case Twofish:
                return TwofishEngine.CIPHER_UUID;
            case ChaCha20:
                return ChaCha20Engine.CIPHER_UUID;
        }
    }
}
