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
import java.io.StringWriter
import java.util.UUID

/**
 * Represents a KeeShare reference stored in group custom data.
 *
 * KeePassXC stores references under the key [CLASSIC_KEY] as base64-encoded XML.
 * KeePassDX per-device sync uses [PER_DEVICE_KEY] with a directory-based config.
 */
data class KeeShareReference(
    val type: Type,
    val uuid: UUID?,
    val path: String,
    val password: String,
    val keepGroups: Boolean = false
) {
    enum class Type {
        INACTIVE,
        IMPORT,
        EXPORT,
        SYNCHRONIZE;

        companion object {
            // Bitmask values matching KeePassXC: ImportFrom=1, ExportTo=2
            fun fromInt(value: Int): Type = when {
                value and 1 != 0 && value and 2 != 0 -> SYNCHRONIZE // both flags
                value and 1 != 0 -> IMPORT
                value and 2 != 0 -> EXPORT
                else -> INACTIVE
            }

            fun toInt(type: Type): Int = when (type) {
                INACTIVE -> 0
                IMPORT -> 1
                EXPORT -> 2
                SYNCHRONIZE -> 3
            }
        }
    }

    companion object {
        const val CLASSIC_KEY = "KeeShare/Reference"
        const val PER_DEVICE_KEY = "KeeShare/PerDeviceSync"

        /**
         * Parse KeePassXC's KeeShare/Reference custom data value.
         *
         * The value is base64-encoded XML:
         * ```xml
         * <KeeShare>
         *   <Type>3</Type>
         *   <Group>...base64-uuid...</Group>
         *   <Path>...file-path...</Path>
         *   <Password>...password...</Password>
         * </KeeShare>
         * ```
         */
        fun fromClassicCustomData(base64Xml: String): KeeShareReference? {
            return try {
                val xml = String(Base64.decode(base64Xml, Base64.DEFAULT), Charsets.UTF_8)
                parseClassicXml(xml)
            } catch (e: Exception) {
                // Try as plain XML (some implementations don't base64-encode)
                try {
                    parseClassicXml(base64Xml)
                } catch (e2: Exception) {
                    null
                }
            }
        }

        private fun parseClassicXml(xml: String): KeeShareReference? {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var typeFlags = 0 // bitmask: 1=Import, 2=Export
            var uuid: UUID? = null
            var path = ""
            var password = ""
            var keepGroups = false

            var insideType = false
            var currentTag = ""
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name
                        if (name == "Type") {
                            insideType = true
                        } else if (insideType) {
                            // KeePassXC uses empty elements <Import/> and <Export/> as flags
                            when (name) {
                                "Import" -> typeFlags = typeFlags or 1
                                "Export" -> typeFlags = typeFlags or 2
                            }
                        } else {
                            currentTag = name
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim() ?: ""
                        if (text.isNotEmpty()) {
                            when (currentTag) {
                                "Type" -> {
                                    // Legacy integer format (older KeePassXC versions)
                                    val intVal = text.toIntOrNull()
                                    if (intVal != null) typeFlags = intVal
                                }
                                "Group" -> uuid = parseUuidFromBase64(text)
                                "Path" -> path = tryBase64Decode(text)
                                "Password" -> password = tryBase64Decode(text)
                                "KeepGroups" -> keepGroups = text.equals("True", ignoreCase = true)
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "Type") insideType = false
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }

            val type = Type.fromInt(typeFlags)
            if (type == Type.INACTIVE && path.isEmpty()) return null
            return KeeShareReference(type, uuid, path, password, keepGroups)
        }

        /**
         * Try to base64-decode a string. KeePassXC base64-encodes Path and Password
         * inside the KeeShare XML. If decoding fails, return the original string.
         */
        private fun tryBase64Decode(text: String): String {
            return try {
                val decoded = String(Base64.decode(text, Base64.DEFAULT), Charsets.UTF_8)
                // Sanity check: if the decoded string contains control chars (except newline/tab),
                // it probably wasn't actually base64-encoded text
                if (decoded.any { it.code < 0x20 && it != '\n' && it != '\r' && it != '\t' }) {
                    text
                } else {
                    decoded
                }
            } catch (e: Exception) {
                text
            }
        }

        private fun parseUuidFromBase64(base64: String): UUID? {
            return try {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                if (bytes.size != 16) return null
                val msb = bytes.sliceArray(0..7).toLong()
                val lsb = bytes.sliceArray(8..15).toLong()
                UUID(msb, lsb)
            } catch (e: Exception) {
                // Try parsing as UUID string
                try {
                    UUID.fromString(base64)
                } catch (e2: Exception) {
                    null
                }
            }
        }

        private fun ByteArray.toLong(): Long {
            var result = 0L
            for (i in 0 until 8) {
                result = result shl 8
                result = result or (this[i].toLong() and 0xFF)
            }
            return result
        }

        /**
         * Serialize a KeeShareReference to classic KeePassXC XML format, then base64-encode.
         */
        fun toClassicCustomData(ref: KeeShareReference): String {
            val writer = StringWriter()
            writer.write("<?xml version=\"1.0\"?>")
            writer.write("<KeeShare>")
            writer.write("<Type>")
            val flags = Type.toInt(ref.type)
            if (flags and 1 != 0) writer.write("<Import/>")
            if (flags and 2 != 0) writer.write("<Export/>")
            writer.write("</Type>")
            if (ref.uuid != null) {
                writer.write("<Group>${uuidToBase64(ref.uuid)}</Group>")
            }
            // Path and Password are base64-encoded inside the XML (matching KeePassXC)
            writer.write("<Path>${Base64.encodeToString(ref.path.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)}</Path>")
            writer.write("<Password>${Base64.encodeToString(ref.password.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)}</Password>")
            writer.write("<KeepGroups>${if (ref.keepGroups) "True" else "False"}</KeepGroups>")
            writer.write("</KeeShare>")
            return Base64.encodeToString(
                writer.toString().toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )
        }

        private fun uuidToBase64(uuid: UUID): String {
            val bytes = ByteArray(16)
            var msb = uuid.mostSignificantBits
            var lsb = uuid.leastSignificantBits
            for (i in 7 downTo 0) {
                bytes[i] = (msb and 0xFF).toByte()
                msb = msb shr 8
            }
            for (i in 15 downTo 8) {
                bytes[i] = (lsb and 0xFF).toByte()
                lsb = lsb shr 8
            }
            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        }

        private fun escapeXml(text: String): String {
            return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;")
        }
    }

    /**
     * Returns true if this reference uses per-device mode (path is a directory,
     * not a single container file). Mirrors KeePassXC's Reference::isPerDeviceMode().
     */
    fun isPerDeviceMode(): Boolean {
        return path.isNotEmpty()
            && !path.endsWith(".kdbx", ignoreCase = true)
            && !path.endsWith(".kdbx.share", ignoreCase = true)
    }
}
