package com.kunzisoft.keepass.credentialprovider.util

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.annotation.RequiresApi
import com.kunzisoft.keepass.activities.FileDatabaseSelectActivity
import com.kunzisoft.keepass.credentialprovider.activity.CreatePasskeyActivity
import com.kunzisoft.keepass.credentialprovider.activity.UsePasskeyActivity
import com.kunzisoft.keepass.utils.StringUtil.toHexString
import java.security.KeyStore
import java.security.MessageDigest
import java.time.Instant
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

@RequiresApi(Build.VERSION_CODES.O)
class IntentHelper {

    companion object {
        private const val HMAC_TYPE = "HmacSHA256"

        private const val KEY_NODE_ID = "nodeId"
        private const val KEY_TIMESTAMP = "timestamp"
        private const val KEY_AUTHENTICATION_CODE = "authenticationCode"

        private const val SEPARATOR = "_"

        private const val NAME_OF_HMAC_KEY = "KeePassDXCredentialProviderHMACKey"

        private const val KEYSTORE_TYPE = "AndroidKeyStore"

        private val PLACEHOLDER_FOR_NEW_NODE_ID = "0".repeat(32)

        private val REGEX_NODE_ID = "[A-F0-9]{32}".toRegex()
        private val REGEX_TIMESTAMP = "[0-9]{10}".toRegex()
        private val REGEX_AUTHENTICATION_CODE = "[A-F0-9]{64}".toRegex() // 256 bits = 64 hex chars

        private const val MAX_DIFF_IN_SECONDS = 60

        private var currentRequestCode: Int = 0

        private fun <T : Activity> createPendingIntent(
            clazz: Class<T>,
            applicationContext: Context,
            data: Bundle? = null
        ): PendingIntent {
            val intent = Intent().setClass(applicationContext, clazz)

            data?.let { intent.putExtras(data) }

            val requestCode = currentRequestCode
            // keeps the requestCodes unique, the limit is arbitrary
            currentRequestCode = (currentRequestCode + 1) % 1000
            return PendingIntent.getActivity(
                applicationContext,
                requestCode,
                intent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        private fun <T : Activity> createPendingIntentWithAuthenticationCode(
            clazz: Class<T>,
            applicationContext: Context,
            nodeId: String
        ): PendingIntent? {
            if (nodeId.matches(REGEX_NODE_ID).not()) return null

            val data = Bundle()
            val timestamp = Instant.now().epochSecond.toString()
            data.putString(KEY_NODE_ID, nodeId)
            data.putString(KEY_TIMESTAMP, timestamp)

            val message = nodeId + SEPARATOR + timestamp
            val authenticationCode = generateAuthenticationCode(message).toHexString()

            data.putString(KEY_AUTHENTICATION_CODE, authenticationCode)
            return createPendingIntent(clazz, applicationContext, data)
        }

        fun generateUnlockPendingIntent(applicationContext: Context): PendingIntent {
            // TODO after the database is unlocked by the user, return to the flow
            return createPendingIntent(FileDatabaseSelectActivity::class.java, applicationContext)
        }

        fun generateCreatePendingIntent(
            applicationContext: Context,
            nodeId: String = PLACEHOLDER_FOR_NEW_NODE_ID
        ): PendingIntent? {
            return createPendingIntentWithAuthenticationCode(
                CreatePasskeyActivity::class.java,
                applicationContext,
                nodeId
            )
        }

        fun generateUsagePendingIntent(
            applicationContext: Context,
            nodeId: String
        ): PendingIntent? {
            return createPendingIntentWithAuthenticationCode(
                UsePasskeyActivity::class.java,
                applicationContext,
                nodeId
            )
        }

        fun getVerifiedNodeId(intent: Intent): String? {
            val nodeId = intent.getStringExtra(KEY_NODE_ID) ?: return null
            val timestampString = intent.getStringExtra(KEY_TIMESTAMP) ?: return null
            val authenticationCode = intent.getStringExtra(KEY_AUTHENTICATION_CODE) ?: return null

            if (nodeId.matches(REGEX_NODE_ID).not() ||
                timestampString.matches(REGEX_TIMESTAMP).not() ||
                authenticationCode.matches(REGEX_AUTHENTICATION_CODE).not()
            ) {
                return null
            }

            val diff = Instant.now().epochSecond - timestampString.toLong()
            if (diff < 0 || diff > MAX_DIFF_IN_SECONDS) {
                return null
            }

            val message = (nodeId + SEPARATOR + timestampString)
            if (verifyAuthenticationCode(
                    message,
                    authenticationCode.decodeHexToByteArray()
                ).not()
            ) {
                return null
            }
            Log.d(this::class.java.simpleName, "nodeId $nodeId verified")
            return nodeId
        }

        private fun verifyAuthenticationCode(
            message: String,
            authenticationCodeIn: ByteArray
        ): Boolean {
            val authenticationCode = generateAuthenticationCode(message)
            return MessageDigest.isEqual(authenticationCodeIn, authenticationCode)
        }

        private fun generateAuthenticationCode(message: String): ByteArray {
            val keyStore = KeyStore.getInstance(KEYSTORE_TYPE)
            keyStore.load(null)
            val hmacKey = try {
                keyStore.getKey(NAME_OF_HMAC_KEY, null) as SecretKey
            } catch (e: Exception) {
                // key not found
                generateKey()
            }

            val mac = Mac.getInstance(HMAC_TYPE)
            mac.init(hmacKey)
            val authenticationCode = mac.doFinal(message.toByteArray())
            return authenticationCode
        }

        private fun generateKey(): SecretKey? {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_HMAC_SHA256, KEYSTORE_TYPE
            )
            val keySizeInBits = 128
            keyGenerator.init(
                KeyGenParameterSpec.Builder(NAME_OF_HMAC_KEY, KeyProperties.PURPOSE_SIGN)
                    .setKeySize(keySizeInBits)
                    .build()
            )
            val key = keyGenerator.generateKey()
            return key
        }

        fun isPlaceholderNodeId(nodeId: String): Boolean {
            return nodeId == PLACEHOLDER_FOR_NEW_NODE_ID
        }

        private fun String.decodeHexToByteArray(): ByteArray {
            if (length % 2 != 0) {
                throw IllegalArgumentException("Must have an even length")
            }
            return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        }
    }

}