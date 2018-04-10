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
package com.kunzisoft.keepass.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.kunzisoft.keepass.compat.StorageAF;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by bpellin on 3/5/16.
 */
public class UriUtil {
    public static Uri parseDefaultFile(String text) {
        if (EmptyUtils.isNullOrEmpty(text)) {
            return null;
        }

        Uri uri = Uri.parse(text);
        if (EmptyUtils.isNullOrEmpty(uri.getScheme())) {
            uri = uri.buildUpon().scheme("file").authority("").build();
        }

        return uri;
    }
    public static Uri parseDefaultFile(Uri uri) {
        if (EmptyUtils.isNullOrEmpty(uri.getScheme())) {
            uri = uri.buildUpon().scheme("file").authority("").build();
        }

        return uri;
    }

    public static boolean equalsDefaultfile(Uri left, String right) {
        left = parseDefaultFile(left);
        Uri uriRight = parseDefaultFile(right);

        return left.equals(uriRight);
    }

    public static InputStream getUriInputStream(Context ctx, Uri uri) throws FileNotFoundException {
        if (uri == null) return null;

        String scheme = uri.getScheme();
        if (EmptyUtils.isNullOrEmpty(scheme) || scheme.equals("file")) {
            return new FileInputStream(uri.getPath());
        }
        else if (scheme.equals("content")) {
            return ctx.getContentResolver().openInputStream(uri);
        }
        else {
            return null;
        }
    }

    /**
     * Many android apps respond with non-writeable content URIs that correspond to files.
     * This will attempt to translate the content URIs to file URIs when possible/appropriate
     * @param uri
     * @return
     */
    public static Uri translate(Context ctx, Uri uri) {
        // StorageAF provides nice URIs
        if (StorageAF.useStorageFramework(ctx) || hasWritableContentUri(uri)) { return uri; }

        String scheme = uri.getScheme();
        if (EmptyUtils.isNullOrEmpty(scheme)) { return uri; }

        String filepath = null;

        try {
            // Use content resolver to try and find the file
            if (scheme.equalsIgnoreCase("content")) {
                Cursor cursor = ctx.getContentResolver().query(uri, new String[]{android.provider.MediaStore.Images.ImageColumns.DATA}, null, null, null);
                cursor.moveToFirst();

                if (cursor != null) {
                    filepath = cursor.getString(0);
                    cursor.close();


                    if (!isValidFilePath(filepath)) {
                        filepath = null;
                    }
                }
            }

            // Try using the URI path as a straight file
            if (EmptyUtils.isNullOrEmpty(filepath)) {
                filepath = uri.getEncodedPath();
                if (!isValidFilePath(filepath)) {
                    filepath = null;
                }
            }
        }
        // Fall back to URI if this fails.
        catch (Exception e) {
            filepath = null;
        }

        // Update the file to a file URI
        if (!EmptyUtils.isNullOrEmpty(filepath)) {
            Uri.Builder b = new Uri.Builder();
            uri = b.scheme("file").authority("").path(filepath).build();
        }

        return uri;
    }

    private static boolean isValidFilePath(String filepath) {
        if (EmptyUtils.isNullOrEmpty(filepath)) { return false; }

        File file = new File(filepath);
        return file.exists() && file.canRead();
    }

    /**
     * Whitelist for known content providers that support writing
     * @param uri
     * @return
     */
    private static boolean hasWritableContentUri(Uri uri) {
        String scheme = uri.getScheme();

        if (EmptyUtils.isNullOrEmpty(scheme)) { return false; }

        if (!scheme.equalsIgnoreCase("content")) { return false; }

        switch (uri.getAuthority()) {
            case "com.google.android.apps.docs.storage":
                return true;
        }

        return false;
    }

}
