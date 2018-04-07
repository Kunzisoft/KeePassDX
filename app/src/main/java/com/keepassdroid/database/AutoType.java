/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AutoType implements Cloneable, Serializable {
    private static final long OBF_OPT_NONE = 0;

    public boolean enabled = true;
    public long obfuscationOptions = OBF_OPT_NONE;
    public String defaultSequence = "";

    private HashMap<String, String> windowSeqPairs = new HashMap<>();

    @SuppressWarnings("unchecked")
    public AutoType clone() {
        AutoType auto;
        try {
            auto = (AutoType) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        auto.windowSeqPairs = (HashMap<String, String>) windowSeqPairs.clone();
        return auto;
    }

    public void put(String key, String value) {
        windowSeqPairs.put(key, value);
    }

    public Set<Map.Entry<String, String>> entrySet() {
        return windowSeqPairs.entrySet();
    }

}
