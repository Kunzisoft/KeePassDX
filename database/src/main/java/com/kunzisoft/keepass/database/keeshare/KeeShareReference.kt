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
            fun fromInt(value: Int): Type = when (value) {
                1 -> IMPORT
                2 -> EXPORT
                3 -> SYNCHRONIZE
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

            var type = Type.INACTIVE
            var uuid: UUID? = null
            var path = ""
            var password = ""

            var currentTag = ""
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> currentTag = parser.name
                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim() ?: ""
                        when (currentTag) {
                            "Type" -> type = Type.fromInt(text.toIntOrNull() ?: 0)
                            "Group" -> uuid = parseUuidFromBase64(text)
                            "Path" -> path = text
                            "Password" -> password = text
                        }
                    }
                    XmlPullParser.END_TAG -> currentTag = ""
                }
                eventType = parser.next()
            }

            if (type == Type.INACTIVE && path.isEmpty()) return null
            return KeeShareReference(type, uuid, path, password)
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
            writer.write("<KeeShare>")
            writer.write("<Type>${Type.toInt(ref.type)}</Type>")
            if (ref.uuid != null) {
                writer.write("<Group>${uuidToBase64(ref.uuid)}</Group>")
            }
            writer.write("<Path>${escapeXml(ref.path)}</Path>")
            writer.write("<Password>${escapeXml(ref.password)}</Password>")
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
}
