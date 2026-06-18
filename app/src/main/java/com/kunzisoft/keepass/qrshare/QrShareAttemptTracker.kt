package com.kunzisoft.keepass.qrshare

import android.content.Context
import java.security.MessageDigest

object QrShareAttemptTracker {

    const val MAX_ATTEMPTS = 5
    private const val PREFS_NAME = "qr_share_attempts"

    fun isBlocked(context: Context, ciphertext: ByteArray): Boolean =
        getAttempts(context, hashKey(ciphertext)) >= MAX_ATTEMPTS

    fun recordFailedAttempt(context: Context, ciphertext: ByteArray): Int {
        val key = hashKey(ciphertext)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val next = prefs.getInt(key, 0) + 1
        prefs.edit().putInt(key, next).apply()
        return next
    }

    fun remainingAttempts(context: Context, ciphertext: ByteArray): Int =
        maxOf(0, MAX_ATTEMPTS - getAttempts(context, hashKey(ciphertext)))

    private fun getAttempts(context: Context, key: String): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(key, 0)

    private fun hashKey(ciphertext: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(ciphertext)
            .take(16)
            .joinToString("") { "%02x".format(it) }
}
