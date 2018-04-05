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
package com.keepassdroid.database;

public interface ISmallTimeLogger {

    PwDate getLastModificationTime();
    void setLastModificationTime(PwDate date);

    PwDate getCreationTime();
    void setCreationTime(PwDate date);

    PwDate getLastAccessTime();
    void setLastAccessTime(PwDate date);

    PwDate getExpiryTime();
    void setExpiryTime(PwDate date);

    boolean isExpires();
    void setExpires(boolean exp);
}
