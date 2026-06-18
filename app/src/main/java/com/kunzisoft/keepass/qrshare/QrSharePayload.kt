package com.kunzisoft.keepass.qrshare

import android.util.Base64
import com.kunzisoft.keepass.model.EntryInfo
import org.json.JSONObject

object QrSharePayload {

    const val QR_PREFIX = "KPDX:"

    fun encode(entryInfo: EntryInfo, pin: CharArray, ttlMs: Long): String {
        val expiry = System.currentTimeMillis() + ttlMs
        val json = JSONObject().apply {
            put("t", entryInfo.title)
            put("u", entryInfo.username)
            put("p", String(entryInfo.password))
            put("url", entryInfo.url)
            put("n", entryInfo.notes)
            put("exp", expiry)
        }.toString()
        val jsonBytes = json.toByteArray(Charsets.UTF_8)
        val encrypted = QrShareCrypto.encrypt(jsonBytes, pin)
        jsonBytes.fill(0)
        return QR_PREFIX + Base64.encodeToString(encrypted, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    @Throws(IllegalArgumentException::class, ExpiredException::class, javax.crypto.AEADBadTagException::class)
    fun decode(qrContent: String, pin: CharArray): EntryInfo {
        if (!qrContent.startsWith(QR_PREFIX)) throw IllegalArgumentException("Not a KeePassDX QR code")
        val encrypted = Base64.decode(
            qrContent.removePrefix(QR_PREFIX),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        val plaintext = QrShareCrypto.decrypt(encrypted, pin)
        val json = JSONObject(String(plaintext))
        plaintext.fill(0)
        val expiry = json.getLong("exp")
        if (System.currentTimeMillis() > expiry) throw ExpiredException()
        return EntryInfo().apply {
            title = json.optString("t")
            username = json.optString("u")
            password = json.optString("p").toCharArray()
            url = json.optString("url")
            notes = json.optString("n")
        }
    }

    fun extractCiphertext(qrContent: String): ByteArray? {
        if (!qrContent.startsWith(QR_PREFIX)) return null
        return runCatching {
            Base64.decode(
                qrContent.removePrefix(QR_PREFIX),
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
        }.getOrNull()
    }

    class ExpiredException : Exception("QR code has expired")
}
