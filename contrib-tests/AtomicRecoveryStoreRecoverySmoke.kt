package com.kunzisoft.keepass.coordination

import java.io.File
import java.nio.file.Files
import java.io.IOException
import java.security.MessageDigest

private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256")
    .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

fun main() {
    val directory = Files.createTempDirectory("kdbx-recovery-").toFile()
    try {
        val base = "original database".toByteArray()
        val changed = "new encrypted revision".toByteArray()
        val external = "other client revision".toByteArray()
        val id = "shared-database"
        val origin = sha(base)
        val pending = AtomicRecoveryStore.retain(directory, id, origin, changed)
        check(pending.exists())
        check(pending.readBytes().contentEquals(changed))
        check(AtomicRecoveryStore.find(directory, id)?.baseRevision == origin)
        check(AtomicRecoveryStore.find(directory, "different-database") == null)
        check(AtomicRecoveryStore.decide(AtomicRecoveryStore.find(directory, id)!!, origin) ==
            AtomicRecoveryStore.RecoveryAction.PUBLISH)
        check(AtomicRecoveryStore.decide(AtomicRecoveryStore.find(directory, id)!!, sha(external)) ==
            AtomicRecoveryStore.RecoveryAction.CONFLICT)
        check(AtomicRecoveryStore.decide(AtomicRecoveryStore.find(directory, id)!!, sha(changed)) ==
            AtomicRecoveryStore.RecoveryAction.ALREADY_PUBLISHED)
        check(runCatching { AtomicRecoveryStore.retain(directory, id, origin, external) }.exceptionOrNull() is IOException)
        check(runCatching { AtomicRecoveryStore.acknowledge(directory, id, sha(external)) }.exceptionOrNull() is IOException)
        check(pending.exists())
        AtomicRecoveryStore.acknowledge(directory, id, sha(changed))
        check(!pending.exists())
        val newPending = AtomicRecoveryStore.retain(directory, id, sha(changed), external)
        check(newPending.exists())
        check(directory.listFiles()!!.none { it.extension == "stage" })
        val incomplete = File(directory, newPending.name.removeSuffix("-pending.kdbx") + "-incomplete.stage")
        incomplete.writeBytes(byteArrayOf(1, 2, 3))
        check(runCatching { AtomicRecoveryStore.find(directory, id) }.exceptionOrNull() is IOException)
        check(newPending.exists())
        incomplete.delete()
        println("PASS: recovery decisions, isolation, no-overwrite, ack, interrupted staging")
    } finally {
        directory.deleteRecursively()
    }
}
