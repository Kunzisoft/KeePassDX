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
import com.kunzisoft.keepass.database.element.CustomData
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer
import java.io.StringReader
import java.io.StringWriter
import java.nio.ByteBuffer
import java.util.UUID

data class KeeShareReference(
    val type: Int = INACTIVE,
    val uuid: UUID = UUID(0, 0),
    val path: String = "",
    val password: String = "",
    val keepGroups: Boolean = false
) {

    val isImporting: Boolean get() = type and IMPORT_FROM != 0 && path.isNotEmpty()

    val isExporting: Boolean get() = type and EXPORT_TO != 0 && path.isNotEmpty()

    val isValid: Boolean get() = type != INACTIVE && path.isNotEmpty()

    fun serialize(): String {
        val writer = StringWriter()
        val serializer = XmlPullParserFactory.newInstance().newSerializer()
        serializer.setOutput(writer)
        serializer.startDocument("UTF-8", null)
        serializer.startTag(null, TAG_ROOT)

        serializer.startTag(null, TAG_TYPE)
        if (type and IMPORT_FROM != 0) serializer.emptyTag(TAG_IMPORT)
        if (type and EXPORT_TO != 0) serializer.emptyTag(TAG_EXPORT)
        serializer.endTag(null, TAG_TYPE)

        serializer.textElement(TAG_GROUP, uuidToBase64(uuid))
        serializer.textElement(TAG_PATH, Base64.encodeToString(path.toByteArray(), Base64.NO_WRAP))
        serializer.textElement(TAG_PASSWORD, Base64.encodeToString(password.toByteArray(), Base64.NO_WRAP))
        serializer.textElement(TAG_KEEP_GROUPS, if (keepGroups) "True" else "False")

        serializer.endTag(null, TAG_ROOT)
        serializer.endDocument()
        return Base64.encodeToString(writer.toString().toByteArray(), Base64.NO_WRAP)
    }

    companion object {
        const val CUSTOM_DATA_KEY = "KeeShare/Reference"

        const val INACTIVE = 0
        const val IMPORT_FROM = 1
        const val EXPORT_TO = 2
        const val SYNCHRONIZE = IMPORT_FROM or EXPORT_TO

        private const val TAG_ROOT = "KeeShare"
        private const val TAG_TYPE = "Type"
        private const val TAG_IMPORT = "Import"
        private const val TAG_EXPORT = "Export"
        private const val TAG_GROUP = "Group"
        private const val TAG_PATH = "Path"
        private const val TAG_PASSWORD = "Password"
        private const val TAG_KEEP_GROUPS = "KeepGroups"

        fun fromCustomData(customData: CustomData): KeeShareReference? {
            val value = customData.get(CUSTOM_DATA_KEY)?.value ?: return null
            return deserialize(value)
        }

        fun deserialize(base64Xml: String): KeeShareReference? {
            val xml = try {
                String(Base64.decode(base64Xml, Base64.DEFAULT), Charsets.UTF_8)
            } catch (_: Exception) {
                base64Xml
            }
            return try {
                parseXml(xml)
            } catch (_: Exception) {
                null
            }
        }

        private fun parseXml(xml: String): KeeShareReference? {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var typeFlags = 0
            var uuid = UUID(0, 0)
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
                        if (name == TAG_TYPE) {
                            insideType = true
                        } else if (insideType) {
                            when (name) {
                                TAG_IMPORT -> typeFlags = typeFlags or IMPORT_FROM
                                TAG_EXPORT -> typeFlags = typeFlags or EXPORT_TO
                            }
                        } else {
                            currentTag = name
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim() ?: ""
                        if (text.isNotEmpty()) {
                            if (insideType) {
                                text.toIntOrNull()?.let { typeFlags = it }
                            } else when (currentTag) {
                                TAG_GROUP -> uuidFromBase64(text)?.let { uuid = it }
                                TAG_PATH -> path = decodeBase64OrPlain(text)
                                TAG_PASSWORD -> password = decodeBase64OrPlain(text)
                                TAG_KEEP_GROUPS -> keepGroups = text.equals("True", ignoreCase = true)
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == TAG_TYPE) insideType = false
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }

            if (typeFlags == INACTIVE && path.isEmpty()) return null
            return KeeShareReference(typeFlags, uuid, path, password, keepGroups)
        }

        private fun decodeBase64OrPlain(text: String): String {
            return try {
                String(Base64.decode(text, Base64.DEFAULT), Charsets.UTF_8)
            } catch (_: Exception) {
                text
            }
        }

        private fun uuidFromBase64(base64: String): UUID? {
            return try {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                if (bytes.size != 16) return null
                val buf = ByteBuffer.wrap(bytes)
                UUID(buf.long, buf.long)
            } catch (_: Exception) {
                try { UUID.fromString(base64) } catch (_: Exception) { null }
            }
        }

        private fun uuidToBase64(uuid: UUID): String {
            val buf = ByteBuffer.allocate(16)
            buf.putLong(uuid.mostSignificantBits)
            buf.putLong(uuid.leastSignificantBits)
            return Base64.encodeToString(buf.array(), Base64.NO_WRAP)
        }

        private fun XmlSerializer.emptyTag(name: String) {
            startTag(null, name)
            endTag(null, name)
        }

        private fun XmlSerializer.textElement(name: String, text: String) {
            startTag(null, name)
            this.text(text)
            endTag(null, name)
        }
    }
}
