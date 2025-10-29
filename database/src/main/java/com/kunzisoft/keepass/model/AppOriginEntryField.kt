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

object AppOriginEntryField {

    const val WEB_DOMAIN_FIELD_NAME = "URL"
    const val APPLICATION_ID_FIELD_NAME = "AndroidApp"
    const val APPLICATION_SIGNATURE_FIELD_NAME = "AndroidApp Signature"

    /**
     * Parse fields of an entry to retrieve a an AppOrigin
     */
    fun parseFields(getField: (id: String) -> String?): AppOrigin {
        val appOrigin = AppOrigin(verified = true)
        // Get Application identifiers
        generateSequence(0) { it + 1 }
            .map { position ->
                val appId = getField(APPLICATION_ID_FIELD_NAME + suffixFieldNamePosition(position))
                val appSignature = getField(APPLICATION_SIGNATURE_FIELD_NAME + suffixFieldNamePosition(position))
                // Pair them up, if appId is null, we stop
                if (appId != null) {
                    appId to (appSignature ?: "")
                } else {
                    // Stop
                    null
                }
            }.takeWhile { it != null }
            .forEach { pair ->
                appOrigin.addAndroidOrigin(
                    AndroidOrigin(pair!!.first, pair.second)
                )
            }
        // Get Domains
        var domainFieldPosition = 0
        while (true) {
            val domainKey = WEB_DOMAIN_FIELD_NAME + suffixFieldNamePosition(domainFieldPosition)
            val domainValue = getField(domainKey)
            if (domainValue != null) {
                appOrigin.addWebOrigin(WebOrigin(origin = domainValue))
                domainFieldPosition++
            } else {
                break // No more domain found
            }
        }

        return appOrigin
    }

    /**
     * Useful to detect if an other KeePass compatibility app already add a web domain or an app id
     */
    fun EntryInfo.containsDomainOrApplicationId(search: String): Boolean {
        if (url.contains(search))
            return true
        return customFields.find {
            it.protectedValue.stringValue.contains(search)
        } != null
    }

    fun EntryInfo.setWebDomain(webDomain: String?, scheme: String?, customFieldsAllowed: Boolean) {
        // If unable to save web domain in custom field or URL not populated, save in URL
        webDomain?.let {
            val webOrigin = WebOrigin.fromDomain(webDomain, scheme).toOriginValue()
            if (!containsDomainOrApplicationId(webDomain)) {
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