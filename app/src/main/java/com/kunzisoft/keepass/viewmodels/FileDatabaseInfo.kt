package com.kunzisoft.keepass.viewmodels

import android.content.Context
import android.net.Uri
import android.text.format.Formatter
import java.io.Serializable
import java.text.DateFormat
import java.util.Date

class FileDatabaseInfo : Serializable {

    private var context: Context
    private var documentFile: androidx.documentfile.provider.DocumentFile? = null
    var fileUri: Uri?
        private set

    constructor(context: Context, fileUri: Uri) {
        this.context = context
        this.fileUri = fileUri
        init()
    }

    constructor(context: Context, filePath: String) {
        this.context = context
        this.fileUri = com.kunzisoft.keepass.utils.UriUtil.parse(filePath)
        init()
    }

    fun init() {
        // Check permission
        fileUri?.let { uri ->
            com.kunzisoft.keepass.utils.UriUtil.takeUriPermission(context.contentResolver, uri)
        }
        documentFile = com.kunzisoft.keepass.utils.UriUtil.getFileData(context, fileUri)
    }

    var exists: Boolean = false
        get() {
            return documentFile?.exists() ?: field
        }
        private set

    fun getLastModification(): Long? {
        return documentFile?.lastModified()
    }

    fun getLastModificationString(): String? {
        return documentFile?.lastModified()?.let {
            if (it != 0L) {
                DateFormat.getDateTimeInstance()
                        .format(Date(it))
            } else {
                null
            }
        }
    }

    fun getSize(): Long? {
        return documentFile?.length()
    }

    fun getSizeString(): String? {
        return documentFile?.let {
            Formatter.formatFileSize(context, it.length())
        }
    }

    fun retrieveDatabaseAlias(alias: String): String? {
        return when {
            alias.isNotEmpty() -> alias
            else -> if (exists) documentFile?.name else fileUri?.path
        }
    }
}
