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
package com.kunzisoft.keepass.database.keeshare

import android.util.Base64
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

/**
 * Configuration for per-device KeeShare sync stored in group custom data
 * under the key [KeeShareReference.PER_DEVICE_KEY].
 *
 * [syncDir] stores a URI string (content://...) for SAF-based access to
 * the shared sync directory.
 *
 * Format:
 * ```xml
 * <KeeShare.PerDeviceSync>
 *   <SyncDir>...base64-encoded-uri-string...</SyncDir>
 *   <Password>...base64-encoded-password...</Password>
 *   <KeepGroups>True|False</KeepGroups>
 * </KeeShare.PerDeviceSync>
 * ```
 *
 * The entire XML is base64-encoded when stored as a custom data value.
 */
data class PerDeviceSyncConfig(
    val syncDir: String,
    val password: String,
    val keepGroups: Boolean = false
) {

    companion object {
        private const val TAG_ROOT = "KeeShare.PerDeviceSync"
        private const val TAG_SYNC_DIR = "SyncDir"
        private const val TAG_PASSWORD = "Password"
        private const val TAG_KEEP_GROUPS = "KeepGroups"

        private const val CONTAINER_EXTENSION = ".kdbx"

        /**
         * Parse a per-device sync config from a base64-encoded custom data value.
         */
        fun fromCustomData(base64Value: String): PerDeviceSyncConfig? {
            return try {
                val xml = String(Base64.decode(base64Value, Base64.DEFAULT), Charsets.UTF_8)
                parseXml(xml)
            } catch (e: Exception) {
                // Try as plain XML
                try {
                    parseXml(base64Value)
                } catch (e2: Exception) {
                    null
                }
            }
        }

        private fun parseXml(xml: String): PerDeviceSyncConfig? {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var syncDirBase64 = ""
            var passwordBase64 = ""
            var keepGroups = false

            var currentTag = ""
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> currentTag = parser.name
                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim() ?: ""
                        when (currentTag) {
                            TAG_SYNC_DIR -> syncDirBase64 = text
                            TAG_PASSWORD -> passwordBase64 = text
                            TAG_KEEP_GROUPS -> keepGroups = text.equals("True", ignoreCase = true)
                        }
                    }
                    XmlPullParser.END_TAG -> currentTag = ""
                }
                eventType = parser.next()
            }

            if (syncDirBase64.isEmpty()) return null

            val syncDir = try {
                String(Base64.decode(syncDirBase64, Base64.DEFAULT), Charsets.UTF_8)
            } catch (e: Exception) {
                syncDirBase64 // Treat as plain text if not base64
            }

            val password = try {
                String(Base64.decode(passwordBase64, Base64.DEFAULT), Charsets.UTF_8)
            } catch (e: Exception) {
                passwordBase64
            }

            return PerDeviceSyncConfig(syncDir, password, keepGroups)
        }

        /**
         * Serialize a per-device sync config to base64-encoded XML for custom data storage.
         */
        fun toCustomData(config: PerDeviceSyncConfig): String {
            val syncDirBase64 = Base64.encodeToString(
                config.syncDir.toByteArray(Charsets.UTF_8), Base64.NO_WRAP
            )
            val passwordBase64 = Base64.encodeToString(
                config.password.toByteArray(Charsets.UTF_8), Base64.NO_WRAP
            )

            val xml = buildString {
                append("<$TAG_ROOT>")
                append("<$TAG_SYNC_DIR>$syncDirBase64</$TAG_SYNC_DIR>")
                append("<$TAG_PASSWORD>$passwordBase64</$TAG_PASSWORD>")
                append("<$TAG_KEEP_GROUPS>${if (config.keepGroups) "True" else "False"}</$TAG_KEEP_GROUPS>")
                append("</$TAG_ROOT>")
            }

            return Base64.encodeToString(xml.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }

        /**
         * Build the container file name for a given device ID.
         * Sanitizes the device ID to alphanumeric characters only to prevent
         * path traversal attacks.
         */
        fun containerFileName(deviceId: String): String {
            val sanitized = deviceId.replace(Regex("[^A-Za-z0-9]"), "")
            require(sanitized.isNotEmpty()) { "Device ID must contain at least one alphanumeric character" }
            return "$sanitized$CONTAINER_EXTENSION"
        }
    }
}
