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
import android.util.Xml
import com.kunzisoft.encrypt.HashManager
import com.kunzisoft.keepass.database.element.binary.BinaryData.Companion.BASE64_FLAG
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.utils.CodecUtil
import com.kunzisoft.keepass.utils.StringUtil.removeSpaceChars
import com.kunzisoft.keepass.utils.StringUtil.toHexString
import com.kunzisoft.keepass.utils.clear
import com.kunzisoft.keepass.utils.readByteArrayCompat
import com.kunzisoft.keepass.utils.readCharArrayCompat
import com.kunzisoft.keepass.utils.readEnum
import com.kunzisoft.keepass.utils.writeByteArrayCompat
import com.kunzisoft.keepass.utils.writeCharArrayCompat
import com.kunzisoft.keepass.utils.writeEnum
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.security.SecureRandom
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import kotlin.math.min

data class MasterCredential(
    private var mPassword: CharArray? = null,
    private var mKeyFileData: ByteArray? = null,
    var hardwareKey: HardwareKey? = null
): Parcelable {

    var password: CharArray?
        get() = mPassword
        set(value) {
            mPassword?.clear()
            mPassword = value?.copyOf()
        }

    var keyFileData: ByteArray?
        get() = mKeyFileData
        set(value) {
            mKeyFileData?.fill(0)
            mKeyFileData = value?.copyOf()
        }

    init {
        mPassword = mPassword?.copyOf()
        mKeyFileData = mKeyFileData?.copyOf()
    }

    constructor(parcel: Parcel) : this(
        parcel.readCharArrayCompat(),
        parcel.readByteArrayCompat(),
        parcel.readEnum<HardwareKey>()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeCharArrayCompat(password)
        parcel.writeByteArrayCompat(keyFileData)
        parcel.writeEnum(hardwareKey)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun getCheckKey(encoding: Charset): ByteArray {
        return getCheckKey(password, encoding)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MasterCredential

        if (password != null) {
            if (other.password == null) return false
            if (!password!!.contentEquals(other.password!!)) return false
        } else if (other.password != null) return false

        if (keyFileData != null) {
            if (other.keyFileData == null) return false
            if (!keyFileData!!.contentEquals(other.keyFileData!!)) return false
        } else if (other.keyFileData != null) return false

        if (hardwareKey != other.hardwareKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = password?.contentHashCode() ?: 0
        result = 31 * result + (keyFileData?.contentHashCode() ?: 0)
        result = 31 * result + (hardwareKey?.hashCode() ?: 0)
        return result
    }

    fun clear() {
        password?.clear()
        password = null
        keyFileData?.clear()
        keyFileData = null
        hardwareKey = null
    }

    companion object CREATOR : Parcelable.Creator<MasterCredential> {
        override fun createFromParcel(parcel: Parcel): MasterCredential {
            return MasterCredential(parcel)
        }

        override fun newArray(size: Int): Array<MasterCredential?> {
            return arrayOfNulls(size)
        }

        private val TAG = MasterCredential::class.java.simpleName

        /**
         * Get a check key for the given password.
         * Only the first few characters of the password are used.
         * @param password The password.
         * @param encoding The character encoding.
         * @return A hash of the first few characters.
         */
        fun getCheckKey(password: CharArray?, encoding: Charset): ByteArray {
            val shortPass = password?.copyOfRange(0, min(password.size, CHECK_KEY_PASSWORD_LENGTH))
                    ?: charArrayOf()
            val res = retrievePasswordKey(shortPass, encoding)
            shortPass.clear()
            return res
        }

        /**
         * Retrieve the key from a password.
         * @param key The password to hash.
         * @param encoding The character encoding to use.
         * @return The SHA-256 hash of the password.
         * @throws IOException If the encoding or hashing fails.
         */
        @Throws(IOException::class)
        fun retrievePasswordKey(
            key: CharArray,
            encoding: Charset
        ): ByteArray {
            val byteBuffer = encoding.encode(CharBuffer.wrap(key))
            val bKey = ByteArray(byteBuffer.remaining())
            byteBuffer.get(bKey)

            val hash = HashManager.hashSha256(bKey)
            bKey.clear()
            if (byteBuffer.hasArray()) {
                byteBuffer.array().clear()
            }
            return hash
        }

        /**
         * Retrieve the key from key file data.
         * Supports raw 32-byte binary keys, 64-byte hex-encoded keys, and KeePass XML key files (v1 and v2).
         * @param keyFileData The raw bytes of the key file.
         * @param allowXML Whether to attempt parsing the data as XML.
         * @return The decoded 32-byte key.
         * @throws IOException If the key file is invalid or cannot be decoded.
         */
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
                        return CodecUtil.decodeHex(String(keyFileData))
                    } catch (_: Exception) {
                        // Key is not hex, treat it as binary data
                    }
                }
                // Hash file as binary data
                return HashManager.hashSha256(keyFileData)
            } catch (e: Exception) {
                throw IOException("Unable to load the keyfile.", e)
            }
        }

        /**
         * Retrieve the key from hardware key data.
         * @param keyData The data provided by the hardware key.
         * @return The SHA-256 hash of the key data.
         */
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
                } catch (_ : ParserConfigurationException) {
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
                                            Log.e(TAG, "XML Keyfile version cannot be read : $versionText", e)
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
                                                    CodecUtil.decodeHex(dataString)
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
            } catch (_: Exception) {
                return null
            }
            return null
        }

        private fun checkKeyFileHash(data: String, hash: String): Boolean {
            var success = false
            try {
                // hexadecimal encoding of the first 4 bytes of the SHA-256 hash of the key.
                val dataDigest = HashManager.hashSha256(CodecUtil.decodeHex(data))
                    .copyOfRange(0, 4).toHexString()
                success = dataDigest == hash
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return success
        }

        /**
         * Create a key file.
         * @param outputStream The output stream to write the key file to
         * @param keySize The size of the random key to generate
         * @param format The format of the key file
         */
        @Throws(IOException::class)
        fun createKeyFile(
            outputStream: OutputStream,
            keySize: Int = DEFAULT_KEYFILE_SIZE,
            format: KeyFileFormat = KeyFileFormat.XML_2_0
        ) {
            val randomBytes = ByteArray(keySize)
            SecureRandom().nextBytes(randomBytes)

            when (format) {
                KeyFileFormat.RANDOM_BYTES -> {
                    outputStream.write(randomBytes)
                }
                KeyFileFormat.XML_2_0 -> {
                    val hexData = randomBytes.toHexString()
                    val hash = HashManager.hashSha256(randomBytes)
                        .copyOfRange(0, 4).toHexString()

                    val xmlSerializer = Xml.newSerializer()
                    xmlSerializer.setOutput(outputStream, DEFAULT_KEYFILE_ENCODING)
                    xmlSerializer.startDocument(DEFAULT_KEYFILE_ENCODING, true)
                    xmlSerializer.startTag(null, XML_NODE_ROOT_NAME)

                    xmlSerializer.startTag(null, XML_NODE_META_NAME)
                    xmlSerializer.startTag(null, XML_NODE_VERSION_NAME)
                    xmlSerializer.text("2.0")
                    xmlSerializer.endTag(null, XML_NODE_VERSION_NAME)
                    xmlSerializer.endTag(null, XML_NODE_META_NAME)

                    xmlSerializer.startTag(null, XML_NODE_KEY_NAME)
                    xmlSerializer.startTag(null, XML_NODE_DATA_NAME)
                    xmlSerializer.attribute(null, XML_ATTRIBUTE_DATA_HASH, hash)
                    xmlSerializer.text(hexData)
                    xmlSerializer.endTag(null, XML_NODE_DATA_NAME)
                    xmlSerializer.endTag(null, XML_NODE_KEY_NAME)

                    xmlSerializer.endTag(null, XML_NODE_ROOT_NAME)
                    xmlSerializer.endDocument()
                }
            }
        }

        /**
         * Supported formats for KeePass key files.
         * @property defaultFileExtension The default file extension for this format
         */
        enum class KeyFileFormat(
            val defaultFileExtension: String,
        ) {
            /**
             * Raw random bytes.
             */
            RANDOM_BYTES("bin"),

            /**
             * KeePass v2 XML format.
             */
            XML_2_0("key")
        }

        private const val XML_NODE_ROOT_NAME = "KeyFile"
        private const val XML_NODE_META_NAME = "Meta"
        private const val XML_NODE_VERSION_NAME = "Version"
        private const val XML_NODE_KEY_NAME = "Key"
        private const val XML_NODE_DATA_NAME = "Data"
        private const val XML_ATTRIBUTE_DATA_HASH = "Hash"

        private const val DEFAULT_KEYFILE_SIZE = 128
        private const val DEFAULT_KEYFILE_ENCODING = "UTF-8"
        const val CHECK_KEY_PASSWORD_LENGTH = 4
    }
}
