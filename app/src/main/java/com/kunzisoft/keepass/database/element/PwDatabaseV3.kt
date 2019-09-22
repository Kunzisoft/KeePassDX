/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.kunzisoft.keepass.database.element

import com.kunzisoft.keepass.crypto.finalkey.FinalKeyFactory
import com.kunzisoft.keepass.crypto.keyDerivation.KdfEngine
import com.kunzisoft.keepass.crypto.keyDerivation.KdfFactory
import com.kunzisoft.keepass.database.exception.InvalidKeyFileException
import com.kunzisoft.keepass.stream.NullOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class PwDatabaseV3 : PwDatabase<PwGroupV3, PwEntryV3>() {

    private var numKeyEncRounds: Int = 0

    private var kdfListV3: MutableList<KdfEngine> = ArrayList()

    override val version: String
        get() = "KeePass 1"

    init {
        kdfListV3.add(KdfFactory.aesKdf)
    }

    override val kdfEngine: KdfEngine?
        get() = kdfListV3[0]

    override val kdfAvailableList: List<KdfEngine>
        get() = kdfListV3

    override val availableEncryptionAlgorithms: List<PwEncryptionAlgorithm>
        get() {
            val list = ArrayList<PwEncryptionAlgorithm>()
            list.add(PwEncryptionAlgorithm.AESRijndael)
            return list
        }

    val rootGroups: List<PwGroupV3>
        get() {
            val kids = ArrayList<PwGroupV3>()
            doForEachGroupInIndex { group ->
                if (group.level == 0)
                    kids.add(group)
            }
            return kids
        }

    override val passwordEncoding: String
        get() = "ISO-8859-1"

    override var numberKeyEncryptionRounds: Long
        get() = numKeyEncRounds.toLong()
        @Throws(NumberFormatException::class)
        set(rounds) {
            if (rounds > Integer.MAX_VALUE || rounds < Integer.MIN_VALUE) {
                throw NumberFormatException()
            }
            numKeyEncRounds = rounds.toInt()
        }

    init {
        algorithm = PwEncryptionAlgorithm.AESRijndael
        numKeyEncRounds = DEFAULT_ENCRYPTION_ROUNDS
    }

    /**
     * Generates an unused random tree id
     *
     * @return new tree id
     */
    override fun newGroupId(): PwNodeIdInt {
        var newId: PwNodeIdInt
        do {
            newId = PwNodeIdInt()
        } while (isGroupIdUsed(newId))

        return newId
    }

    /**
     * Generates an unused random tree id
     *
     * @return new tree id
     */
    override fun newEntryId(): PwNodeIdUUID {
        var newId: PwNodeIdUUID
        do {
            newId = PwNodeIdUUID()
        } while (isEntryIdUsed(newId))

        return newId
    }

    @Throws(InvalidKeyFileException::class, IOException::class)
    override fun getMasterKey(key: String?, keyInputStream: InputStream?): ByteArray {

        return if (key != null && keyInputStream != null) {
            getCompositeKey(key, keyInputStream)
        } else if (key != null) { // key.length() >= 0
            getPasswordKey(key)
        } else if (keyInputStream != null) { // key == null
            getFileKey(keyInputStream)
        } else {
            throw IllegalArgumentException("Key cannot be empty.")
        }
    }

    @Throws(IOException::class)
    fun makeFinalKey(masterSeed: ByteArray, masterSeed2: ByteArray, numRounds: Long) {

        // Write checksum Checksum
        val messageDigest: MessageDigest
        try {
            messageDigest = MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw IOException("SHA-256 not implemented here.")
        }

        val nos = NullOutputStream()
        val dos = DigestOutputStream(nos, messageDigest)

        val transformedMasterKey = transformMasterKey(masterSeed2, masterKey, numRounds)
        dos.write(masterSeed)
        dos.write(transformedMasterKey)

        finalKey = messageDigest.digest()
    }

    override fun loadXmlKeyFile(keyInputStream: InputStream): ByteArray? {
        return null
    }

    override fun createGroup(): PwGroupV3 {
        return PwGroupV3()
    }

    override fun createEntry(): PwEntryV3 {
        return PwEntryV3()
    }

    override fun rootCanContainsEntry(): Boolean {
        return false
    }

    override fun containsCustomData(): Boolean {
        return false
    }

    override fun isBackup(group: PwGroupV3): Boolean {
        var currentGroup: PwGroupV3? = group
        while (currentGroup != null) {
            if (currentGroup.level == 0 && currentGroup.title.equals("Backup", ignoreCase = true)) {
                return true
            }
            currentGroup = currentGroup.parent
        }
        return false
    }

    companion object {

        private const val DEFAULT_ENCRYPTION_ROUNDS = 300

        /**
         * Encrypt the master key a few times to make brute-force key-search harder
         * @throws IOException
         */
        @Throws(IOException::class)
        private fun transformMasterKey(pKeySeed: ByteArray, pKey: ByteArray, rounds: Long): ByteArray {
            val key = FinalKeyFactory.createFinalKey()

            return key.transformMasterKey(pKeySeed, pKey, rounds)
        }
    }
}
