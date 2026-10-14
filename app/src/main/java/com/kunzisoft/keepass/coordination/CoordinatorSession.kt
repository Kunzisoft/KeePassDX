/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.kunzisoft.keepass.coordination

import android.content.Context
import android.net.Uri
import com.kunzisoft.keepass.BuildConfig
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

/** Session is owned by the database's load/save lifecycle, not by a wrapper app. */
internal object CoordinatorSession {
    data class OpenResult(val input: InputStream, val readOnly: Boolean)
    private var active: KdbxCoordinatorClient? = null
    private var activeStorageIdentity: String? = null
    private var activeDocumentUri: String? = null
    private var databaseHash: String? = null

    // Recovery files must never be replayed onto a different coordinator
    // merely because its database ID happens to be identical.
    private fun recoveryIdentity(config: CoordinatorConfiguration.Config): String =
        config.url.trimEnd('/') + "\u0000" + config.id

    fun handles(uri: Uri?): Boolean = uri != null && uri.scheme == "content" &&
        uri.authority == "${BuildConfig.APPLICATION_ID}.coordinator"

    @Synchronized
    fun open(context: Context, uri: Uri, requestWrite: Boolean): OpenResult {
        require(handles(uri)) { "Not a coordinator URI" }
        close()
        val config = CoordinatorConfiguration.get(context)
            ?: throw IOException("Coordinator connection has not been configured")
        val client = KdbxCoordinatorClient(config.url, config.id, config.token, "android")
        var readOnly = !requestWrite
        try {
            if (requestWrite) {
                try { client.acquire() }
                catch (_: KdbxCoordinatorClient.BusyException) { readOnly = true }
            }
            // Reconcile durable encrypted changes BEFORE loading the remote database.
            // A save can reach the server while its acknowledgement gets lost. Never
            // overwrite a newer remote revision: only replay when the base matches.
            val pending = AtomicRecoveryStore.find(context.filesDir, recoveryIdentity(config))
            var data = if (readOnly) client.downloadReadOnly() else client.download()
            if (pending != null) {
                if (readOnly) {
                    throw IOException("Unsynchronized KDBX recovery copy exists; obtain the editing lock to recover it")
                }
                val remoteRevision = data?.let(::sha256) ?: "0"
                when (AtomicRecoveryStore.decide(pending, remoteRevision)) {
                    AtomicRecoveryStore.RecoveryAction.ALREADY_PUBLISHED -> {
                        AtomicRecoveryStore.acknowledge(context.filesDir, recoveryIdentity(config), remoteRevision)
                    }
                    AtomicRecoveryStore.RecoveryAction.PUBLISH -> {
                        val saved = pending.file.readBytes()
                        val confirmedRevision = client.publish(saved)
                        AtomicRecoveryStore.acknowledge(context.filesDir, recoveryIdentity(config), confirmedRevision)
                        data = saved
                    }
                    AtomicRecoveryStore.RecoveryAction.CONFLICT -> {
                        throw IOException("Recovery copy conflicts with a newer remote revision; " +
                            "no automatic overwrite was attempted. Manual merge is required")
                    }
                }
            }
            if (data == null) throw IOException("No remote KDBX database exists yet")
            databaseHash = sha256(data)
            if (!readOnly) {
                active = client
                activeStorageIdentity = recoveryIdentity(config)
                activeDocumentUri = uri.toString()
            } else client.close()
            return OpenResult(ByteArrayInputStream(data), readOnly)
        } catch (e: Exception) {
            runCatching { client.close() }
            throw e
        }
    }

    /** Persist a recoverable encrypted copy and publish only under the same revision/lease. */
    @Synchronized
    fun persistAndPublish(context: Context, uri: Uri, encryptedKdbx: ByteArray) {
        if (!handles(uri)) throw IOException("Not a coordinator URI")
        val config = CoordinatorConfiguration.get(context) ?: throw IOException("Coordinator not configured")
        // Reconfiguration must never redirect a pending encrypted revision to
        // another coordinator or database while an old lease is still active.
        if (activeStorageIdentity != recoveryIdentity(config) || activeDocumentUri != uri.toString()) {
            throw IOException("Coordinator settings changed during editing; close and reopen the database")
        }
        val client = active ?: throw IOException("No valid editable coordinator session; reload before saving")
        if (!client.hasLease()) throw IOException("Coordinator lease lost; save refused")
        AtomicRecoveryStore.retain(context.filesDir, recoveryIdentity(config), client.currentRevision(), encryptedKdbx)
        val confirmedRevision = client.publish(encryptedKdbx)
        AtomicRecoveryStore.acknowledge(context.filesDir, recoveryIdentity(config), confirmedRevision)
        databaseHash = confirmedRevision
    }

    @Synchronized
    fun close() {
        val client = active
        active = null
        activeStorageIdentity = null
        activeDocumentUri = null
        databaseHash = null
        if (client != null) runCatching { client.close() }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
