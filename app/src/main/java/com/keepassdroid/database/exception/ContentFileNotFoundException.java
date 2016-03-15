/*
 * Copyright 2016 Brian Pellin.
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
package com.keepassdroid.database.exception;

import android.net.Uri;

import com.keepassdroid.utils.EmptyUtils;

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
