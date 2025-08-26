/*
 * Copyright 2025 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.credentialprovider.passkey.util

import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.content.res.AssetManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.provider.CallingAppInfo
import com.kunzisoft.keepass.model.OriginApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.P)
class OriginManager(
    private val providedClientDataHash: ByteArray?,
    private val callingAppInfo: CallingAppInfo?,
    private val assets: AssetManager
) {

    suspend fun getOriginAtCreation(
        onOriginRetrieved: (appInfoToStore: OriginApp, clientDataHash: ByteArray) -> Unit,
        onOriginCreated: (appInfoToStore: OriginApp, origin: String) -> Unit
    ) {
        getOrigin(
            onOriginRetrieved = { callOrigin, clientDataHash ->
                onOriginRetrieved(OriginApp(webDomain = callOrigin), clientDataHash)
            },
            onOriginNotRetrieved = { storeAppInfo ->
                // Create a new Android Origin and prepare the signature app storage
                onOriginCreated(
                    storeAppInfo,
                    buildAndroidOrigin(storeAppInfo.appId)
                )
            }
        )
    }

    suspend fun getOriginAtUsage(
        appInfoStored: OriginApp?,
        onOriginRetrieved: (clientDataHash: ByteArray) -> Unit,
        onOriginCreated: (origin: String) -> Unit
    ) {
        getOrigin(
            onOriginRetrieved = { origin, clientDataHash ->
                onOriginRetrieved(clientDataHash)
            },
            onOriginNotRetrieved = { appInfoCalled ->
                // Verify the app signature to retrieve the origin
                if (appInfoCalled.appId == appInfoStored?.appId
                    && appInfoCalled.appSignature == appInfoStored?.appSignature) {
                    onOriginCreated(buildAndroidOrigin(appInfoCalled.appId))
                } else {
                    throw SecurityException("Wrong signature for ${appInfoCalled.appId}, ${appInfoCalled.appSignature} retrieved but ${appInfoStored?.appSignature} expected")
                }
            }
        )
    }

    private suspend fun getOrigin(
        onOriginRetrieved: (origin: String, clientDataHash: ByteArray) -> Unit,
        onOriginNotRetrieved: (appInfoRetrieved: OriginApp) -> Unit
    ) {
        if (callingAppInfo == null) {
            throw SecurityException("Calling app info cannot be retrieved")
        }
        withContext(Dispatchers.IO) {
            var callOrigin: String?
            val privilegedAllowlist = assets.open("trustedPackages.json").bufferedReader().use {
                it.readText()
            }
            // for trusted browsers like Chrome and Firefox
            callOrigin = callingAppInfo.getOrigin(privilegedAllowlist)?.removeSuffix("/")
            withContext(Dispatchers.Main) {
                if (callOrigin != null && providedClientDataHash != null) {
                    Log.d(TAG, "Origin $callOrigin retrieved from callingAppInfo")
                    onOriginRetrieved(callOrigin, providedClientDataHash)
                } else {
                    onOriginNotRetrieved(
                        OriginApp(
                            appId = callingAppInfo.packageName,
                            appSignature = getApplicationSignatures(callingAppInfo.signingInfo)
                        )
                    )
                }
            }
        }
    }

    // TODO Move in Crypto package and make unit tests
    /**
     * Converts a Signature object into its SHA-256 fingerprint string.
     * The fingerprint is typically represented as uppercase hex characters separated by colons.
     */
    private fun signatureToSha256Fingerprint(signature: Signature): String? {
        return try {
            val certificateFactory = CertificateFactory.getInstance("X.509")
            val x509Certificate = certificateFactory.generateCertificate(
                signature.toByteArray().inputStream()
            ) as X509Certificate

            val messageDigest = MessageDigest.getInstance("SHA-256")
            val digest = messageDigest.digest(x509Certificate.encoded)

            // Format as colon-separated HEX uppercase string
            digest.joinToString(separator = ":") { byte -> "%02X".format(byte) }
                .uppercase(Locale.US)
        } catch (e: Exception) {
            Log.e("SigningInfoUtil", "Error converting signature to SHA-256 fingerprint", e)
            null
        }
    }

    /**
     * Retrieves all relevant SHA-256 signature fingerprints for a given package.
     *
     * @param signingInfo The SigningInfo object to retrieve the strings signatures
     * @return A List of SHA-256 fingerprint strings, or null if an error occurs or no signatures are found.
     */
    fun getAllSignatures(signingInfo: SigningInfo?): List<String>? {
        try {
            val signatures = mutableSetOf<String>()
            if (signingInfo != null) {
                // Includes past and current keys if rotation occurred. This is generally preferred.
                signingInfo.signingCertificateHistory?.forEach { signature ->
                    signatureToSha256Fingerprint(signature)?.let { signatures.add(it) }
                }
                // If only one signer and history is empty (e.g. new app), this might be needed.
                // Or if multiple signers are explicitly used for the APK content.
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners?.forEach { signature ->
                        signatureToSha256Fingerprint(signature)?.let { signatures.add(it) }
                    }
                } else { // Fallback for single signer if history was somehow null/empty
                    signingInfo.signingCertificateHistory?.firstOrNull()?.let {
                        signatureToSha256Fingerprint(it)?.let { fp -> signatures.add(fp) }
                    }
                }
            }
            return if (signatures.isEmpty()) null else signatures.toList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting signatures", e)
            return null
        }
    }


    /**
     * Combines a list of signature into a single string for database storage.
     *
     * @return A single string with fingerprints joined by a delimiter, or null if the input list is null or empty.
     */
    private fun getApplicationSignatures(signingInfo: SigningInfo?): String? {
        val fingerprints = getAllSignatures(signingInfo)
        if (fingerprints.isNullOrEmpty()) {
            return null
        }
        return fingerprints.joinToString(SIGNATURE_DELIMITER)
    }

    /**
     * Builds an Android Origin from a package name.
     */
    private fun buildAndroidOrigin(packageName: String?): String {
        if (packageName.isNullOrEmpty())
            throw SecurityException("Package name cannot be empty")
        val packageOrigin = "androidapp://${packageName}"
        Log.d(TAG, "Origin $packageOrigin retrieved from package name")
        return packageOrigin
    }

    companion object {
        private val TAG = OriginManager::class.simpleName

        private const val SIGNATURE_DELIMITER = "##SIG##"
    }
}