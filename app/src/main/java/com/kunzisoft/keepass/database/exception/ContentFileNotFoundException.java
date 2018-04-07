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
package com.kunzisoft.keepass.database.exception;

import android.net.Uri;

import com.kunzisoft.keepass.utils.EmptyUtils;

import java.io.FileNotFoundException;

/**
 * Created by bpellin on 3/14/16.
 */
public class ContentFileNotFoundException extends FileNotFoundException {
    public static FileNotFoundException getInstance(Uri uri) {
        if (uri == null) { return new FileNotFoundException(); }

        String scheme = uri.getScheme();

        if (!EmptyUtils.isNullOrEmpty(scheme) && scheme.equalsIgnoreCase("content")) {
            return new ContentFileNotFoundException();
        }

        return new FileNotFoundException();
    }

    public  ContentFileNotFoundException() {
        super();
    }
}
