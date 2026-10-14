/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.kunzisoft.keepass.coordination

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

/**
 * Durable, encrypted KDBX recovery copy written before any network publish.
 * The filename binds it to the database identifier and the base revision,
 * without a second metadata file that might be lost in an interrupted write.
 */
internal object AtomicRecoveryStore {
    private const val LEGACY_NAME = "coordinated-kdbx-pending.kdbx"
    private val validRevision = Regex("(?:0|[a-f0-9]{64})")

    internal data class Pending(val file: File, val baseRevision: String)
    internal enum class RecoveryAction { ALREADY_PUBLISHED, PUBLISH, CONFLICT }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun prefix(databaseId: String) = "coordinated-kdbx-${sha256(databaseId.toByteArray())}-"

    @Synchronized
    fun find(directory: File, databaseId: String): Pending? {
        if (File(directory, LEGACY_NAME).exists()) {
            throw IOException("An older unsynchronized KDBX recovery file exists. Preserve and recover it manually before continuing.")
        }
        val base = prefix(databaseId)
        val files = directory.listFiles() ?: throw IOException("Cannot read recovery directory")
        if (files.any { it.name.startsWith(base) && it.name.endsWith(".stage") }) {
            throw IOException("Incomplete recovery staging file exists. Preserve it for manual recovery")
        }
        val matches = files.filter { it.name.startsWith(base) && it.name.endsWith("-pending.kdbx") }
        if (matches.size > 1) throw IOException("Multiple recovery revisions exist; manual recovery required")
        return matches.singleOrNull()?.let { file ->
            val revision = file.name.removePrefix(base).removeSuffix("-pending.kdbx")
            if (!validRevision.matches(revision) || !file.isFile) {
                throw IOException("Invalid recovery metadata; manual recovery required")
            }
            Pending(file, revision)
        }
    }

    @Synchronized
    fun retain(directory: File, databaseId: String, baseRevision: String, encryptedDatabase: ByteArray): File {
        if (!validRevision.matches(baseRevision)) throw IOException("Invalid base revision")
        if (encryptedDatabase.isEmpty()) throw IOException("Refusing an empty recovery database")
        if (find(directory, databaseId) != null) throw IOException("Unsynchronized recovery copy already exists")
        val destination = File(directory, "${prefix(databaseId)}${baseRevision}-pending.kdbx")
        val staged = File.createTempFile("${prefix(databaseId)}${baseRevision}-", ".stage", directory)
        try {
            FileOutputStream(staged).use { output ->
                output.write(encryptedDatabase)
                output.fd.sync()
            }
            if (destination.exists() || !staged.renameTo(destination)) {
                throw IOException("Could not atomically retain KDBX recovery copy")
            }
            return destination
        } finally {
            staged.delete()
        }
    }

    @Synchronized
    fun decide(pending: Pending, remoteRevision: String): RecoveryAction {
        if (!validRevision.matches(remoteRevision)) throw IOException("Invalid remote revision")
        val localRevision = sha256(pending.file.readBytes())
        return when {
            localRevision == remoteRevision -> RecoveryAction.ALREADY_PUBLISHED
            pending.baseRevision == remoteRevision -> RecoveryAction.PUBLISH
            else -> RecoveryAction.CONFLICT
        }
    }

    @Synchronized
    fun acknowledge(directory: File, databaseId: String, expectedHash: String) {
        val pending = find(directory, databaseId) ?: throw IOException("Recovery copy missing")
        if (!validRevision.matches(expectedHash) || sha256(pending.file.readBytes()) != expectedHash) {
            throw IOException("Recovery copy changed; refusing to delete it")
        }
        if (!pending.file.delete()) {
            throw IOException("Remote save succeeded, but recovery copy could not be removed")
        }
    }
}
