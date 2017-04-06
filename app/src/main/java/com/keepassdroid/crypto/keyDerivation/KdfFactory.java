/*
 * Copyright 2017 Brian Pellin.
 *
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.crypto.keyDerivation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KdfFactory {

    public static List<KdfEngine> kdfList = new ArrayList<KdfEngine>();

    static {
        kdfList.add(new AesKdf());
        kdfList.add(new Argon2Kdf());
    }

    public static KdfParameters getDefaultParameters() {
        return kdfList.get(0).getDefaultParameters();
    }

    public static KdfEngine get(UUID uuid) {
        for (KdfEngine engine: kdfList) {
            if (engine.uuid.equals(uuid)) {
                return engine;
            }
        }

        return null;
    }

}
