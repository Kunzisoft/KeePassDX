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
package com.kunzisoft.keepass.model

import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.model.EntryInfo.Companion.suffixFieldNamePosition
import com.kunzisoft.keepass.utils.contains

object AppOriginEntryField {

    const val WEB_DOMAIN_FIELD_NAME = "URL"
    const val APPLICATION_ID_FIELD_NAME = "AndroidApp"
    const val APPLICATION_SIGNATURE_FIELD_NAME = "AndroidApp Signature"

    /**
     * Parse the fields of an entry to retrieve an AppOrigin
     */
    fun parseFields(url: String?, getField: (id: String) -> CharArray?): AppOrigin {
        val appOrigin = AppOrigin(verified = true)
        // Get Application identifiers
        generateSequence(0) { it + 1 }
            .map { position ->
                val appId = getField(APPLICATION_ID_FIELD_NAME + suffixFieldNamePosition(position))
                val appSignature = getField(APPLICATION_SIGNATURE_FIELD_NAME + suffixFieldNamePosition(position))
                // Pair them up, if appId is null, we stop
                if (appId != null) {
                    appId to (appSignature ?: charArrayOf())
                } else {
                    // Stop
                    null
                }
            }.takeWhile { it != null }
            .forEach { pair ->
                appOrigin.addAndroidOrigin(
                    AndroidOrigin(
                        packageName = String(pair!!.first),
                        fingerprint = if (pair.second.isNotEmpty()) String(pair.second) else null)
                )
            }
        // Get Domains
        // Add URL from standard field
        if (!url.isNullOrEmpty()) {
            appOrigin.addWebOrigin(WebOrigin(origin = url))
        }
        // Add URLs from custom fields
        var domainFieldPosition = 1
        while (true) {
            val domainKey = WEB_DOMAIN_FIELD_NAME + suffixFieldNamePosition(domainFieldPosition)
            val domainValue = getField(domainKey)
            if (domainValue != null && domainValue.isNotEmpty()) {
                appOrigin.addWebOrigin(WebOrigin(origin = String(domainValue)))
                domainFieldPosition++
            } else {
                break // No more domain found
            }
        }

        return appOrigin
    }

    /**
     * Useful for checking whether another KeePass-compatible app has already added a website or an app ID
     */
    fun EntryInfo.containsDomainOrApplicationId(search: String): Boolean {
        if (url.contains(search))
            return true
        return customFields.find {
            it.protectedValue.charArrayValue.contains(search)
        } != null
    }

    fun EntryInfo.setWebDomain(webDomain: String?, scheme: String?, customFieldsAllowed: Boolean) {
        // If unable to save web domain in custom field or URL not populated, save in URL
        webDomain?.let {
            val webOrigin = WebOrigin.fromDomain(webDomain, scheme)?.toOriginValue()
            if (webOrigin != null && !containsDomainOrApplicationId(webDomain)) {
                if (!customFieldsAllowed || url.isEmpty()) {
                    url = webOrigin
                } else {
                    // Save web domain in custom field
                    addUniqueField(
                        Field(
                            WEB_DOMAIN_FIELD_NAME,
                            ProtectedString(false, webOrigin)
                        ),
                        1 // Start to one because URL is a standard field name
                    )
                }
            }
        }
    }

    /**
     * Save application id in custom field and the application signature if provided
     */
    fun EntryInfo.setApplicationId(applicationId: String?, signature: String? = null) {
        // Save application id in custom field
        applicationId?.let {
            // Check compatibility with other KeePass client unless a signature need to be saved
            if (!containsDomainOrApplicationId(applicationId) || signature != null) {
                val position = addUniqueField(
                    Field(
                        APPLICATION_ID_FIELD_NAME,
                        ProtectedString(false, applicationId)
                    )
                ).first
                signature?.let {
                    addOrReplaceFieldWithSuffix(
                        Field(
                            APPLICATION_SIGNATURE_FIELD_NAME,
                            ProtectedString(true, signature)
                        ),
                        position
                    )
                }
            }
        }
    }

    /**
     * Assign an AppOrigin to an EntryInfo,
     * Only if [customFieldsAllowed] is true
     */
    fun EntryInfo.setAppOrigin(appOrigin: AppOrigin?, customFieldsAllowed: Boolean) {
        appOrigin?.androidOrigins?.forEach { appIdentifier ->
            setApplicationId(appIdentifier.packageName, appIdentifier.fingerprint)
        }
        appOrigin?.webOrigins?.forEach { webOrigin ->
            setWebDomain(webOrigin.origin, null, customFieldsAllowed)
        }
    }

    /**
     * Detect if the current field is an application id
     */
    fun Field.isAppId(): Boolean {
        return this.name.startsWith(APPLICATION_ID_FIELD_NAME)
    }

    /**
     * Detect if the current field is an application id signature
     */
    fun Field.isAppIdSignature(): Boolean {
        return this.name.startsWith(APPLICATION_SIGNATURE_FIELD_NAME)
    }

    /**
     * Detect if the current field is a web domain
     */
    fun Field.isWebDomain(): Boolean {
        return this.name.startsWith(WEB_DOMAIN_FIELD_NAME)
                || this.name.contains("_$WEB_DOMAIN_FIELD_NAME")
                || this.name.contains("${WEB_DOMAIN_FIELD_NAME}_")
    }
}