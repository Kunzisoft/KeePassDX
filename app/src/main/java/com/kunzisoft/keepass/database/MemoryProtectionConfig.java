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
package com.kunzisoft.keepass.database;

public class MemoryProtectionConfig {

    public boolean protectTitle = false;
    public boolean protectUserName = false;
    public boolean protectPassword = false;
    public boolean protectUrl = false;
    public boolean protectNotes = false;

    public boolean autoEnableVisualHiding = false;

    public boolean isProtected(String field) {
        if (field.equalsIgnoreCase(PwDefsV4.TITLE_FIELD)) return protectTitle;
        if (field.equalsIgnoreCase(PwDefsV4.USERNAME_FIELD)) return protectUserName;
        if (field.equalsIgnoreCase(PwDefsV4.PASSWORD_FIELD)) return protectPassword;
        if (field.equalsIgnoreCase(PwDefsV4.URL_FIELD)) return protectUrl;
        if (field.equalsIgnoreCase(PwDefsV4.NOTES_FIELD)) return protectNotes;

        return false;
    }
}
