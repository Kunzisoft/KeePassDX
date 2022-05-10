package com.kunzisoft.keepass.database.element

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.kunzisoft.encrypt.HashManager
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.utils.StringUtil.removeSpaceChars
import com.kunzisoft.keepass.utils.StringUtil.toHexString
import com.kunzisoft.keepass.utils.UriUtil
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

data class CompositeKey(var passwordData: ByteArray? = null,
                        var keyFileData: ByteArray? = null,
                        var hardwareKeyData: ByteArray? = null) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CompositeKey

        if (passwordData != null) {
            if (other.passwordData == null) return false
            if (!passwordData.contentEquals(other.passwordData)) return false
        } else if (other.passwordData != null) return false
        if (keyFileData != null) {
            if (other.keyFileData == null) return false
            if (!keyFileData.contentEquals(other.keyFileData)) return false
        } else if (other.keyFileData != null) return false
        if (hardwareKeyData != null) {
            if (other.hardwareKeyData == null) return false
            if (!hardwareKeyData.contentEquals(other.hardwareKeyData)) return false
        } else if (other.hardwareKeyData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = passwordData?.contentHashCode() ?: 0
        result = 31 * result + (keyFileData?.contentHashCode() ?: 0)
        result = 31 * result + (hardwareKeyData?.contentHashCode() ?: 0)
        return result
    }

    companion object {

        private val TAG = CompositeKey::class.java.simpleName

        @Throws(IOException::class)
        fun retrievePasswordKey(key: String,
                                encoding: Charset): ByteArray {
            val bKey: ByteArray = try {
                key.toByteArray(encoding)
            } catch (e: UnsupportedEncodingException) {
                key.toByteArray()
            }
            return HashManager.hashSha256(bKey)
        }

        @Throws(IOException::class)
        fun retrieveFileKey(contentResolver: ContentResolver,
                            keyFileUri: Uri?,
                            allowXML: Boolean): ByteArray {
            if (keyFileUri == null)
                throw IOException("Keyfile URI is null")
            val keyData = getKeyFileData(contentResolver, keyFileUri)
                ?: throw IOException("No data retrieved")
            try {
                // Check XML key file
                val xmlKeyByteArray = if (allowXML)
                    loadXmlKeyFile(ByteArrayInputStream(keyData))
                else
                    null
                if (xmlKeyByteArray != null) {
                    return xmlKeyByteArray
                }

                // Check 32 bytes key file
                when (keyData.size) {
                    32 -> return keyData
                    64 -> try {
                        return Hex.decodeHex(String(keyData).toCharArray())
                    } catch (ignoredException: Exception) {
                        // Key is not base 64, treat it as binary data
                    }
                }
                // Hash file as binary data
                return HashManager.hashSha256(keyData)
            } catch (e: Exception) {
                throw IOException("Unable to load the keyfile.", e)
            }
        }

        @Throws(IOException::class)
        fun retrieveHardwareKey(keyData: ByteArray): ByteArray {
            return HashManager.hashSha256(keyData)
        }

        @Throws(Exception::class)
        private fun getKeyFileData(contentResolver: ContentResolver,
                                   keyFileUri: Uri): ByteArray? {
            UriUtil.getUriInputStream(contentResolver, keyFileUri)?.use { keyFileInputStream ->
                return keyFileInputStream.readBytes()
            }
            return null
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
                                                return Base64.decode(dataString,
                                                    DatabaseKDBX.BASE_64_FLAG
                                                )
                                            }
                                            2F -> {
                                                return if (hashString != null
                                                    && checkKeyFileHash(dataString, hashString)) {
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