/*
 * Copyright 2026 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.share

import android.net.Uri
import android.util.Base64
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.model.EntryData
import com.kunzisoft.keepass.model.EntryInfo
import org.json.JSONObject

object ShareUtil {

    /**
     * Encrypts the provided [EntryInfo] into a secure [Uri].
     *
     * @param entryInfo The entry data to be shared.
     * @param pin The secret PIN used for key derivation.
     * @return A secure URI containing the encrypted fragment.
     */
    fun encryptShareUri(
        entryInfo: EntryInfo,
        pin: CharArray
    ): Uri {
        val json = JSONObject().apply {
            put(REF_PAYLOAD_TITLE, entryInfo.title)
            put(REF_PAYLOAD_USER_NAME, entryInfo.username)
            put(REF_PAYLOAD_PASSWORD, String(entryInfo.password))
            put(REF_PAYLOAD_URL, entryInfo.url)
            put(REF_PAYLOAD_NOTES, entryInfo.notes)
            put(REF_PAYLOAD_UUID, entryInfo.nodeId.toString())
            val other = JSONObject()
            entryInfo.customFields.forEach { field ->
                other.put(field.name, String(field.protectedValue.charArrayValue))
            }
            put(REF_PAYLOAD_OTHER, other)
        }.toString()
        val jsonBytes = json.toByteArray(Charsets.UTF_8)
        val encrypted = ShareCrypto.encrypt(jsonBytes, pin, getAad())
        jsonBytes.fill(0)
        val fragment = Base64.encodeToString(
            encrypted,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        return Uri.Builder()
            .scheme(SHARE_SCHEME)
            .authority(SHARE_AUTHORITY)
            .fragment(fragment)
            .build()
    }

    /**
     * Decrypts a secure [Uri] into [EntryData].
     *
     * @param uri The secure share URI.
     * @param pin The secret PIN used for decryption.
     * @throws IllegalArgumentException if the URI is invalid or decryption fails.
     * @return The decrypted entry data.
     */
    fun decryptShareUri(
        uri: Uri,
        pin: CharArray
    ): EntryData {
        if (!uri.isSecureShareUri())
            throw IllegalArgumentException("Not a KeePass Secure Share URI")
        val encrypted = Base64.decode(
            uri.fragment,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        val plaintext = ShareCrypto.decrypt(encrypted, pin, getAad())
        val json = JSONObject(String(plaintext))
        plaintext.fill(0)

        val customFields = mutableListOf<Field>()
        json.optJSONObject(REF_PAYLOAD_OTHER)?.let { other ->
            other.keys().forEach { key ->
                customFields.add(Field(
                    name = key,
                    value = ProtectedString(
                        enableProtection = true,
                        value = other.getString(key)
                    )
                ))
            }
        }

        return EntryData(
            title = json.optString(REF_PAYLOAD_TITLE),
            username = json.optString(REF_PAYLOAD_USER_NAME),
            password = json.optString(REF_PAYLOAD_PASSWORD).toCharArray(),
            url = json.optString(REF_PAYLOAD_URL),
            notes = json.optString(REF_PAYLOAD_NOTES),
            customFields = customFields
        )
    }

    fun Uri.isSecureShareUri(): Boolean {
        return this.scheme == SHARE_SCHEME
            && this.authority == SHARE_AUTHORITY
    }

    // Additional Authenticated Data to prevent future algo downgrade attacks
    private fun getAad(): ByteArray {
        return "$SHARE_SCHEME://$SHARE_AUTHORITY".toByteArray(Charsets.UTF_8)
    }

    private const val SHARE_SCHEME = "keepass"
    private const val SHARE_AUTHORITY = "share"

    // TODO Icon, Colors, AutoType, Times, Tags
    // Based on FieldReferences
    private const val REF_PAYLOAD_TITLE = "t"
    private const val REF_PAYLOAD_USER_NAME = "u"
    private const val REF_PAYLOAD_PASSWORD = "p"
    private const val REF_PAYLOAD_URL = "a"
    private const val REF_PAYLOAD_NOTES = "n"
    private const val REF_PAYLOAD_UUID = "i"
    private const val REF_PAYLOAD_OTHER = "o"
}