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
package com.keepassdroid.crypto.keyDerivation;

import java.io.IOException;
import java.util.UUID;

public abstract class KdfEngine {
    public UUID uuid;

    public KdfParameters getDefaultParameters() {
        return new KdfParameters(uuid);
    }

    public abstract byte[] transform(byte[] masterKey, KdfParameters p) throws IOException;

    public abstract void randomize(KdfParameters p);

    public abstract String getName();

    public abstract long getKeyRounds(KdfParameters p);

    public abstract void setKeyRounds(KdfParameters p, long keyRounds);

}
