/*
 * Copyright 2025 AOSP modified by Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.credentialprovider.passkey.data

import android.os.Build
import android.os.Parcelable
import android.util.Log
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Represents an Android privileged app, based on AOSP code
 */
@Parcelize
data class AndroidPrivilegedApp(
    val packageName: String,
    val fingerprints: Set<String>
): Parcelable {

    override fun toString(): String {
        return "$packageName ($fingerprints)"
    }

    companion object {
        private const val PACKAGE_NAME_KEY = "package_name"
        private const val SIGNATURES_KEY = "signatures"
        private const val FINGERPRINT_KEY = "cert_fingerprint_sha256"
        private const val BUILD_KEY = "build"
        private const val USER_DEBUG_KEY = "userdebug"
        private const val TYPE_KEY = "type"
        private const val APP_INFO_KEY = "info"
        private const val ANDROID_TYPE_KEY = "android"
        private const val USER_BUILD_TYPE = "userdebug"
        private const val APPS_KEY = "apps"

        /**
         * Extracts a list of AndroidPrivilegedApp objects from a JSONObject.
         */
        @JvmStatic
        fun extractPrivilegedApps(jsonObject: JSONObject): List<AndroidPrivilegedApp> {
            val apps = mutableListOf<AndroidPrivilegedApp>()
            if (!jsonObject.has(APPS_KEY)) {
                return apps
            }
            val appsJsonArray = jsonObject.getJSONArray(APPS_KEY)
            for (i in 0 until appsJsonArray.length()) {
                try {
                    val appJsonObject = appsJsonArray.getJSONObject(i)
                    if (appJsonObject.getString(TYPE_KEY) != ANDROID_TYPE_KEY) {
                        continue
                    }
                    if (!appJsonObject.has(APP_INFO_KEY)) {
                        continue
                    }
                    apps.add(
                        createFromJSONObject(
                            appJsonObject.getJSONObject(APP_INFO_KEY)
                        )
                    )
                } catch (e: JSONException) {
                    Log.e(AndroidPrivilegedApp::class.simpleName, "Error parsing privileged app", e)
                }
            }
            return apps
        }

        /**
         * Creates an AndroidPrivilegedApp object from a JSONObject.
         */
        @JvmStatic
        private fun createFromJSONObject(
            appInfoJsonObject: JSONObject,
            filterUserDebug: Boolean = true
        ): AndroidPrivilegedApp {
            val signaturesJson = appInfoJsonObject.getJSONArray(SIGNATURES_KEY)
            val fingerprints = mutableSetOf<String>()
            for (j in 0 until signaturesJson.length()) {
                if (filterUserDebug) {
                    if (USER_DEBUG_KEY == signaturesJson.getJSONObject(j)
                            .optString(BUILD_KEY) && USER_BUILD_TYPE != Build.TYPE
                    ) {
                        continue
                    }
                }
                fingerprints.add(signaturesJson.getJSONObject(j).getString(FINGERPRINT_KEY))
            }
            return AndroidPrivilegedApp(
                packageName = appInfoJsonObject.getString(PACKAGE_NAME_KEY),
                fingerprints = fingerprints
            )
        }

        /**
         * Creates a JSONObject from a list of AndroidPrivilegedApp objects.
         * The structure will be similar to what `extractPrivilegedApps` expects.
         *
         * @param privilegedApps The list of AndroidPrivilegedApp objects.
         * @return A JSONObject representing the list.
         */
        @JvmStatic
        fun toJsonObject(privilegedApps: List<AndroidPrivilegedApp>): JSONObject {
            val rootJsonObject = JSONObject()
            val appsJsonArray = JSONArray()

            for (app in privilegedApps) {
                val appInfoObject = JSONObject()
                appInfoObject.put(PACKAGE_NAME_KEY, app.packageName)

                val signaturesArray = JSONArray()
                for (fingerprint in app.fingerprints) {
                    val signatureObject = JSONObject()
                    signatureObject.put(FINGERPRINT_KEY, fingerprint)
                    // If needed: signatureObject.put(BUILD_KEY, "user")
                    signaturesArray.put(signatureObject)
                }
                appInfoObject.put(SIGNATURES_KEY, signaturesArray)

                val appContainerObject = JSONObject()
                appContainerObject.put(TYPE_KEY, ANDROID_TYPE_KEY)
                appContainerObject.put(APP_INFO_KEY, appInfoObject)

                appsJsonArray.put(appContainerObject)
            }

            rootJsonObject.put(APPS_KEY, appsJsonArray)
            return rootJsonObject
        }
    }
}
