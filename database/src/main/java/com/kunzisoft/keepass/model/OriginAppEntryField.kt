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

object OriginAppEntryField {

    const val WEB_DOMAIN_FIELD_NAME = "URL"
    const val APPLICATION_ID_FIELD_NAME = "AndroidApp"
    const val APPLICATION_SIGNATURE_FIELD_NAME = "AndroidApp Signature"

    /**
     * Parse fields of an entry to retrieve a an OriginApp
     */
    fun parseFields(getField: (id: String) -> String?): OriginApp {
        val appIdField = getField(APPLICATION_ID_FIELD_NAME)
        val appSignatureField = getField(APPLICATION_SIGNATURE_FIELD_NAME)
        val webDomainField = getField(WEB_DOMAIN_FIELD_NAME)
        return OriginApp(
            appId = appIdField,
            appSignature = appSignatureField,
            webDomain = webDomainField
        )
    }

    /**
     * Useful to detect if an other KeePass compatibility app already add a web domain or an app id
     */
    private fun EntryInfo.containsDomainOrApplicationId(search: String): Boolean {
        if (url.contains(search))
            return true
        return customFields.find {
            it.protectedValue.stringValue.contains(search)
        } != null
    }

    fun EntryInfo.setWebDomain(webDomain: String?, scheme: String?, customFieldsAllowed: Boolean) {
        // If unable to save web domain in custom field or URL not populated, save in URL
        webDomain?.let {
            val webScheme = if (scheme.isNullOrEmpty()) "https" else scheme
            val webDomainToStore = "$webScheme://$webDomain"
            if (!containsDomainOrApplicationId(webDomain)) {
                if (!customFieldsAllowed || url.isEmpty()) {
                    url = webDomainToStore
                } else {
                    // Save web domain in custom field
                    addUniqueField(
                        Field(
                            WEB_DOMAIN_FIELD_NAME,
                            ProtectedString(false, webDomainToStore)
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

    fun EntryInfo.setOriginApp(originApp: OriginApp?, customFieldsAllowed: Boolean) {
        if (originApp != null) {
            setApplicationId(originApp.appId, originApp.appSignature)
            setWebDomain(originApp.webDomain, null, customFieldsAllowed)
        }
    }
}