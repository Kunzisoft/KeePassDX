package com.kunzisoft.keepass.utils

import android.content.ContentResolver
import android.net.Uri
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream

@Throws(FileNotFoundException::class)
fun getUriInputStream(contentResolver: ContentResolver, uri: Uri?): InputStream? {
    if (uri == null) return null

    val scheme = uri.scheme
    return if (scheme == null || scheme.isEmpty() || scheme == "file") {
        FileInputStream(uri.path!!)
    } else if (scheme == "content") {
        contentResolver.openInputStream(uri)
    } else {
        null
    }
}