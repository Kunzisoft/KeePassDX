/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.kunzisoft.keepass.coordination

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

/** An opt-in, app-private coordinator configuration. No password data or master keys are stored. */
internal object CoordinatorConfiguration {
    data class Config(val url: String, val id: String, val token: String)
    private const val PREFERENCES = "coordinated_kdbx"
    private const val KEY_ALIAS = "coordinated_kdbx_access_token"

    private fun key(): java.security.Key {
        check(Build.VERSION.SDK_INT >= 23) { "Android 6 or newer is required for coordinated editing" }
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        store.getKey(KEY_ALIAS, null)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build())
        return generator.generateKey()
    }

    fun put(context: Context, value: Config) {
        // Validate before storing credentials; invalid HTTP endpoints are rejected by the client.
        KdbxCoordinatorClient(value.url, value.id, value.token, "android").close()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val ciphertext = cipher.doFinal(value.token.toByteArray(Charsets.UTF_8))
        val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        check(prefs.edit().putString("url", value.url).putString("id", value.id)
            .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("encrypted_token", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .commit()) { "Failed to persist configuration" }
    }

    fun get(context: Context): Config? {
        val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val url = prefs.getString("url", null) ?: return null
        val id = prefs.getString("id", null) ?: return null
        val iv = prefs.getString("iv", null) ?: return null
        val ciphertext = prefs.getString("encrypted_token", null) ?: return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(),
            GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        return Config(url, id, String(cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)), Charsets.UTF_8))
    }
}
