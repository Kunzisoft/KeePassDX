/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.kunzisoft.keepass.coordination

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import com.kunzisoft.keepass.BuildConfig
import java.io.FileNotFoundException
import kotlin.concurrent.thread

/** Exposes a single opt-in coordinator KDBX in Android's built-in file picker. */
class CoordinatorDocumentsProvider : DocumentsProvider() {
    private val rootId = "coordinator"
    private val documentId = "remote"
    private val rootColumns = arrayOf(DocumentsContract.Root.COLUMN_ROOT_ID,
        DocumentsContract.Root.COLUMN_DOCUMENT_ID, DocumentsContract.Root.COLUMN_TITLE,
        DocumentsContract.Root.COLUMN_FLAGS, DocumentsContract.Root.COLUMN_MIME_TYPES)
    private val documentColumns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_FLAGS)

    override fun onCreate() = true

    override fun queryRoots(projection: Array<out String>?): Cursor =
        MatrixCursor(projection ?: rootColumns).apply {
            val row = newRow()
            val columns = columnNames
            for (column in columns) row.add(when (column) {
                DocumentsContract.Root.COLUMN_ROOT_ID -> rootId
                DocumentsContract.Root.COLUMN_DOCUMENT_ID -> rootId
                DocumentsContract.Root.COLUMN_TITLE -> "Coordinated KDBX"
                DocumentsContract.Root.COLUMN_FLAGS -> 0
                DocumentsContract.Root.COLUMN_MIME_TYPES -> "application/octet-stream"
                else -> null
            })
        }

    private fun documentCursor(projection: Array<out String>?, includeFile: Boolean): Cursor =
        MatrixCursor(projection ?: documentColumns).apply {
            if (includeFile && context?.let { CoordinatorConfiguration.get(it) } != null) {
                val row = newRow()
                for (column in columnNames) row.add(when (column) {
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID -> documentId
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME -> "shared.kdbx"
                    DocumentsContract.Document.COLUMN_MIME_TYPE -> "application/octet-stream"
                    DocumentsContract.Document.COLUMN_FLAGS -> 0 // No direct writes: app must publish with lease.
                    else -> null
                })
            }
        }

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        if (documentId != this.documentId) throw FileNotFoundException(documentId)
        return documentCursor(projection, true)
    }

    override fun queryChildDocuments(parentDocumentId: String, projection: Array<out String>?, sortOrder: String?): Cursor {
        if (parentDocumentId != rootId) throw FileNotFoundException(parentDocumentId)
        return documentCursor(projection, true)
    }

    override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
        if (documentId != this.documentId || mode != "r") throw FileNotFoundException("Read-only document provider")
        val config = context?.let { CoordinatorConfiguration.get(it) } ?: throw FileNotFoundException("Not configured")
        // Download before exposing an FD, so errors are not confused with a valid empty KDBX.
        val data = KdbxCoordinatorClient(config.url, config.id, config.token, "android-readonly")
            .use { it.downloadReadOnly() } ?: throw FileNotFoundException("Remote database missing")
        val (read, write) = ParcelFileDescriptor.createPipe()
        thread(name = "coordinated-kdbx-read", isDaemon = true) {
            try { ParcelFileDescriptor.AutoCloseOutputStream(write).use { it.write(data) } }
            catch (_: Exception) { runCatching { write.close() } }
        }
        return read
    }
}
