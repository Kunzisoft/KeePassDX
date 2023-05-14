/*
 * Copyright 2022 Jeremy Jamet / Kunzisoft.
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
 */
package com.kunzisoft.keepass.database.element

import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import android.util.Log
import com.kunzisoft.encrypt.HashManager
import com.kunzisoft.keepass.database.element.binary.BinaryData.Companion.BASE64_FLAG
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.utils.StringUtil.removeSpaceChars
import com.kunzisoft.keepass.utils.StringUtil.toHexString
import com.kunzisoft.keepass.utils.readByteArrayCompat
import com.kunzisoft.keepass.utils.readEnum
import com.kunzisoft.keepass.utils.writeByteArrayCompat
import com.kunzisoft.keepass.utils.writeEnum
import org.apache.commons.codec.binary.Hex
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

data class MasterCredential(var password: String? = null,
                            var keyFileData: ByteArray? = null,
                            var hardwareKey: HardwareKey? = null): Parcelable {

    constructor(parcel: Parcel) : this() {
        password = parcel.readString()
        keyFileData = parcel.readByteArrayCompat()
        hardwareKey = parcel.readEnum<HardwareKey>()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(password)
        parcel.writeByteArrayCompat(keyFileData)
        parcel.writeEnum(hardwareKey)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MasterCredential

        if (password != other.password) return false
        if (!keyFileData.contentEquals(other.keyFileData)) return false
        if (hardwareKey != other.hardwareKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = password?.hashCode() ?: 0
        result = 31 * result + (keyFileData?.hashCode() ?: 0)
        result = 31 * result + (hardwareKey?.hashCode() ?: 0)
        return result
    }

    companion object CREATOR : Parcelable.Creator<MasterCredential> {
        override fun createFromParcel(parcel: Parcel): MasterCredential {
            return MasterCredential(parcel)
        }

        override fun newArray(size: Int): Array<MasterCredential?> {
            return arrayOfNulls(size)
        }

        private val TAG = MasterCredential::class.java.simpleName

        @Throws(IOException::class)
        fun retrievePasswordKey(key: String,
                                encoding: Charset
        ): ByteArray {
            val bKey: ByteArray = try {
                key.toByteArray(encoding)
            } catch (e: UnsupportedEncodingException) {
                key.toByteArray()
            }
            return HashManager.hashSha256(bKey)
        }

        @Throws(IOException::class)
        fun retrieveKeyFileDecodedKey(
            keyFileData: ByteArray,
            allowXML: Boolean
        ): ByteArray {
            try {
                // Check XML key file
                val xmlKeyByteArray = if (allowXML)
                    loadXmlKeyFile(ByteArrayInputStream(keyFileData))
                else
                    null
                if (xmlKeyByteArray != null) {
                    return xmlKeyByteArray
                }

                // Check 32 bytes key file
                when (keyFileData.size) {
                    32 -> return keyFileData
                    64 -> try {
                        return Hex.decodeHex(String(keyFileData).toCharArray())
                    } catch (ignoredException: Exception) {
                        // Key is not base 64, treat it as binary data
                    }
                }
                // Hash file as binary data
                return HashManager.hashSha256(keyFileData)
            } catch (e: Exception) {
                throw IOException("Unable to load the keyfile.", e)
            }
        }

        @Throws(IOException::class)
        fun retrieveHardwareKey(keyData: ByteArray): ByteArray {
            return HashManager.hashSha256(keyData)
        }

        private fun loadXmlKeyFile(keyInputStream: InputStream): ByteArray? {
            try {
                val documentBuilderFactory = DocumentBuilderFactory.newInstance()

                // Disable certain unsecure XML-Parsing DocumentBuilderFactory features
                try {
                    documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
                } catch (e : ParserConfigurationException) {
                    Log.w(TAG, "Unable to add FEATURE_SECURE_PROCESSING to prevent XML eXternal Entity injection (XXE)")
                }

                val documentBuilder = documentBuilderFactory.newDocumentBuilder()
                val doc = documentBuilder.parse(keyInputStream)

                var xmlKeyFileVersion = 1F

                val docElement = doc.documentElement
                val keyFileChildNodes = docElement.childNodes
                // <KeyFile> Root node
                if (docElement == null
                    || !docElement.nodeName.equals(XML_NODE_ROOT_NAME, ignoreCase = true)) {
                    return null
                }
                if (keyFileChildNodes.length < 2)
                    return null
                for (keyFileChildPosition in 0 until keyFileChildNodes.length) {
                    val keyFileChildNode = keyFileChildNodes.item(keyFileChildPosition)
                    // <Meta>
                    if (keyFileChildNode.nodeName.equals(XML_NODE_META_NAME, ignoreCase = true)) {
                        val metaChildNodes = keyFileChildNode.childNodes
                        for (metaChildPosition in 0 until metaChildNodes.length) {
                            val metaChildNode = metaChildNodes.item(metaChildPosition)
                            // <Version>
                            if (metaChildNode.nodeName.equals(XML_NODE_VERSION_NAME, ignoreCase = true)) {
                                val versionChildNodes = metaChildNode.childNodes
                                for (versionChildPosition in 0 until versionChildNodes.length) {
                                    val versionChildNode = versionChildNodes.item(versionChildPosition)
                                    if (versionChildNode.nodeType == Node.TEXT_NODE) {
                                        val versionText = versionChildNode.textContent.removeSpaceChars()
                                        try {
                                            xmlKeyFileVersion = versionText.toFloat()
                                            Log.i(TAG, "Reading XML KeyFile version : $xmlKeyFileVersion")
                                        } catch (e: Exception) {
                                            Log.e(TAG, "XML Keyfile version cannot be read : $versionText")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // <Key>
                    if (keyFileChildNode.nodeName.equals(XML_NODE_KEY_NAME, ignoreCase = true)) {
                        val keyChildNodes = keyFileChildNode.childNodes
                        for (keyChildPosition in 0 until keyChildNodes.length) {
                            val keyChildNode = keyChildNodes.item(keyChildPosition)
                            // <Data>
                            if (keyChildNode.nodeName.equals(XML_NODE_DATA_NAME, ignoreCase = true)) {
                                var hashString : String? = null
                                if (keyChildNode.hasAttributes()) {
                                    val dataNodeAttributes = keyChildNode.attributes
                                    hashString = dataNodeAttributes
                                        .getNamedItem(XML_ATTRIBUTE_DATA_HASH).nodeValue
                                }
                                val dataChildNodes = keyChildNode.childNodes
                                for (dataChildPosition in 0 until dataChildNodes.length) {
                                    val dataChildNode = dataChildNodes.item(dataChildPosition)
                                    if (dataChildNode.nodeType == Node.TEXT_NODE) {
                                        val dataString = dataChildNode.textContent.removeSpaceChars()
                                        when (xmlKeyFileVersion) {
                                            1F -> {
                                                // No hash in KeyFile XML version 1
                                                return Base64.decode(dataString, BASE64_FLAG)
                                            }
                                            2F -> {
                                                return if (hashString != null
                                                    && checkKeyFileHash(dataString, hashString)
                                                ) {
                                                    Log.i(TAG, "Successful key file hash check.")
                                                    Hex.decodeHex(dataString.toCharArray())
                                                } else {
                                                    Log.e(TAG, "Unable to check the hash of the key file.")
                                                    null
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                return null
            }
            return null
        }

        private fun checkKeyFileHash(data: String, hash: String): Boolean {
            var success = false
            try {
                // hexadecimal encoding of the first 4 bytes of the SHA-256 hash of the key.
                val dataDigest = HashManager.hashSha256(Hex.decodeHex(data.toCharArray()))
                    .copyOfRange(0, 4).toHexString()
                success = dataDigest == hash
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return success
        }

        private const val XML_NODE_ROOT_NAME = "KeyFile"
        private const val XML_NODE_META_NAME = "Meta"
        private const val XML_NODE_VERSION_NAME = "Version"
        private const val XML_NODE_KEY_NAME = "Key"
        private const val XML_NODE_DATA_NAME = "Data"
        private const val XML_ATTRIBUTE_DATA_HASH = "Hash"
    }
}
