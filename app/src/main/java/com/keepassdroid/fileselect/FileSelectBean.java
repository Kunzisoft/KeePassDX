/*
 * Copyright 2017 Jeremy Jamet / Kunzisoft.
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
package com.keepassdroid.fileselect;

import android.content.Context;
import android.net.Uri;
import android.support.v4.provider.DocumentFile;

import java.io.File;
import java.io.Serializable;
import java.util.Date;

public class FileSelectBean implements Serializable {

    private static final String EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents";

    private String fileName;
    private Uri fileUri;
    private Date lastModification;
    private long size;

    public FileSelectBean(Context context, String pathFile) {
        fileName = "";
        lastModification = new Date();
        size = 0;

        fileUri = Uri.parse(pathFile);
        if (EXTERNAL_STORAGE_AUTHORITY.equals(fileUri.getAuthority())) {
            DocumentFile file = DocumentFile.fromSingleUri(context, fileUri);
            size = file.length();
            fileName = file.getName();
            lastModification = new Date(file.lastModified());
        } else {
            File file = new File(fileUri.getPath());
            size = file.length();
            fileName = file.getName();
            lastModification = new Date(file.lastModified());
        }

        if (fileName == null || fileName.isEmpty()) {
            fileName = fileUri.getPath();
        }
    }

    public boolean notFound() {
        return getSize() == 0;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Uri getFileUri() {
        return fileUri;
    }

    public void setFileUri(Uri fileUri) {
        this.fileUri = fileUri;
    }

    public Date getLastModification() {
        return lastModification;
    }

    public void setLastModification(Date lastModification) {
        this.lastModification = lastModification;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
