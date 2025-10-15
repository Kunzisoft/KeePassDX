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

import android.content.Context
import android.util.Log
import androidx.credentials.provider.CallingAppInfo
import com.kunzisoft.encrypt.Signature.getAllFingerprints
import com.kunzisoft.keepass.BuildConfig
import com.kunzisoft.keepass.credentialprovider.passkey.data.AndroidPrivilegedApp
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream

object PrivilegedAllowLists {

    private const val FILE_NAME_PRIVILEGED_APPS_CUSTOM = "passkeys_privileged_apps_custom.json"
    private const val FILE_NAME_PRIVILEGED_APPS_COMMUNITY = "passkeys_privileged_apps_community.json"
    private const val FILE_NAME_PRIVILEGED_APPS_GOOGLE = "passkeys_privileged_apps_google.json"

    private fun retrieveContentFromStream(
        inputStream: InputStream,
    ): String {
        return inputStream.use { fileInputStream ->
            fileInputStream.bufferedReader(Charsets.UTF_8).readText()
        }
    }

    /**
     * Get the origin from a predefined privileged allow list
     *
     * @param callingAppInfo CallingAppInfo to verify and retrieve the specific Origin
     * @param inputStream File input stream containing the origin list as JSON
     */
    private fun getOriginFromPrivilegedAllowListStream(
        callingAppInfo: CallingAppInfo,
        inputStream: InputStream
    ): String? {
        val privilegedAllowList = retrieveContentFromStream(inputStream)
        return callingAppInfo.getOrigin(privilegedAllowList)?.removeSuffix("/")
    }

    /**
     * Get the origin from the predefined privileged allow lists
     *
     * @param callingAppInfo CallingAppInfo to verify and retrieve the specific Origin
     * @param context Context for file operations.
     */
    fun getOriginFromPrivilegedAllowLists(
        callingAppInfo: CallingAppInfo,
        context: Context
    ): String? {
        return try {
            // Check the custom apps first
            getOriginFromPrivilegedAllowListStream(
                callingAppInfo = callingAppInfo,
                File(context.filesDir, FILE_NAME_PRIVILEGED_APPS_CUSTOM)
                    .inputStream()
            )
        } catch (e: Exception) {
            // Then the Google list if allowed
            if (BuildConfig.CLOSED_STORE) {
                try {
                    // Check the Google list if allowed
                    // http://www.gstatic.com/gpm-passkeys-privileged-apps/apps.json
                    getOriginFromPrivilegedAllowListStream(
                        callingAppInfo = callingAppInfo,
                        inputStream = context.assets.open(FILE_NAME_PRIVILEGED_APPS_GOOGLE)
                    )
                } catch (_: Exception) {
                    // Then the community apps list
                    getOriginFromPrivilegedAllowListStream(
                        callingAppInfo = callingAppInfo,
                        inputStream = context.assets.open(FILE_NAME_PRIVILEGED_APPS_COMMUNITY)
                    )
                }
            } else {
                when (e) {
                    is FileNotFoundException -> {
                        val attemptApp = AndroidPrivilegedApp(
                            packageName = callingAppInfo.packageName,
                            fingerprints = callingAppInfo.signingInfo
                                .getAllFingerprints() ?: emptySet()
                        )
                        throw PrivilegedException(
                            temptingApp = attemptApp,
                            message = "$attemptApp is not in the allow list"
                        )
                    }
                    else -> throw e
                }
            }
        }
    }

    /**
     * Retrieves a list of predefined AndroidPrivilegedApp objects from an asset JSON file.
     *
     * @param inputStream File input stream containing the origin list as JSON
     */
    private fun retrievePrivilegedApps(
        inputStream: InputStream
    ): List<AndroidPrivilegedApp> {
        val jsonObject = JSONObject(retrieveContentFromStream(inputStream))
        return AndroidPrivilegedApp.extractPrivilegedApps(jsonObject)
    }

    /**
     * Retrieves a list of predefined AndroidPrivilegedApp objects from a context
     *
     * @param context Context for file operations.
     */
    fun retrievePredefinedPrivilegedApps(
        context: Context
    ): List<AndroidPrivilegedApp> {
        return try {
            val predefinedApps = mutableListOf<AndroidPrivilegedApp>()
            predefinedApps.addAll(retrievePrivilegedApps(context.assets.open(FILE_NAME_PRIVILEGED_APPS_COMMUNITY)))
            if (BuildConfig.CLOSED_STORE) {
                predefinedApps.addAll(retrievePrivilegedApps(context.assets.open(FILE_NAME_PRIVILEGED_APPS_GOOGLE)))
            }
            predefinedApps
        } catch (e: Exception) {
            Log.e(PrivilegedAllowLists::class.simpleName, "Error retrieving privileged apps", e)
            emptyList()
        }
    }

    /**
     * Retrieves a list of AndroidPrivilegedApp objects from the custom JSON file.
     *
     * @param context Context for file operations.
     */
    fun retrieveCustomPrivilegedApps(
        context: Context
    ): List<AndroidPrivilegedApp> {
        return try {
            retrievePrivilegedApps(File(context.filesDir, FILE_NAME_PRIVILEGED_APPS_CUSTOM).inputStream())
        } catch (e: Exception) {
            Log.i(PrivilegedAllowLists::class.simpleName, "No custom privileged apps", e)
            emptyList()
        }
    }

    /**
     * Retrieves a list of all predefined and custom AndroidPrivilegedApp objects.
     */
    fun retrieveAllPrivilegedApps(
        context: Context
    ): List<AndroidPrivilegedApp> {
        return retrievePredefinedPrivilegedApps(context) + retrieveCustomPrivilegedApps(context)
    }

    /**
     * Saves a list of custom AndroidPrivilegedApp objects to a JSON file.
     *
     * @param context Context for file operations.
     * @param privilegedApps The list of apps to save.
     * @return True if saving was successful, false otherwise.
     */
    fun saveCustomPrivilegedApps(context: Context, privilegedApps: List<AndroidPrivilegedApp>): Boolean {
        return try {
            val jsonToSave = AndroidPrivilegedApp.toJsonObject(privilegedApps)
            val file = File(context.filesDir, FILE_NAME_PRIVILEGED_APPS_CUSTOM)

            // Delete existing file before writing to ensure atomicity if needed
            if (file.exists()) {
                file.delete()
            }

            file.outputStream().use { fileOutputStream ->
                fileOutputStream.write(
                    jsonToSave
                        .toString(4) // toString(4) for pretty print
                        .toByteArray(Charsets.UTF_8)
                )
            }
            true
        } catch (e: Exception) {
            Log.e(PrivilegedAllowLists::class.simpleName, "Error saving privileged apps", e)
            false
        }
    }

    /**
     * Deletes the custom JSON file.
     *
     * @param context Context for file operations.
     * @return True if deletion was successful or file didn't exist, false otherwise.
     */
    fun deletePrivilegedAppsFile(context: Context): Boolean {
        return try {
            val file = File(context.filesDir, FILE_NAME_PRIVILEGED_APPS_CUSTOM)
            if (file.exists()) {
                file.delete()
            } else {
                true // File didn't exist, so considered "successfully deleted"
            }
        } catch (e: SecurityException) {
            Log.e(PrivilegedAllowLists::class.simpleName, "Error deleting privileged apps file", e)
            false
        }
    }

    class PrivilegedException(
        val temptingApp: AndroidPrivilegedApp,
        message: String
    ) : Exception(message)
}