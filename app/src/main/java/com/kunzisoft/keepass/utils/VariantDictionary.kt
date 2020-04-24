/*
 * Copyright 2020 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.utils

import com.kunzisoft.keepass.stream.LittleEndianDataInputStream
import com.kunzisoft.keepass.stream.LittleEndianDataOutputStream
import com.kunzisoft.keepass.stream.bytes4ToUInt
import com.kunzisoft.keepass.stream.bytes64ToLong
import java.io.IOException
import java.nio.charset.Charset
import java.util.*

open class VariantDictionary {

    private val dict: MutableMap<String, VdType> = HashMap()

    private fun getValue(name: String): Any? {
        return dict[name]?.value ?: return null
    }

    private fun putType(type: Byte, name: String, value: Any) {
        dict[name] = VdType(type, value)
    }

    fun setUInt32(name: String, value: UnsignedInt) {
        putType(VdType.UInt32, name, value)
    }

    fun getUInt32(name: String): UnsignedInt? {
        return dict[name]?.value as UnsignedInt?
    }

    fun setUInt64(name: String, value: Long) {
        putType(VdType.UInt64, name, value)
    }

    fun getUInt64(name: String): Long? {
        return dict[name]?.value as Long?
    }

    fun setBool(name: String, value: Boolean) {
        putType(VdType.Bool, name, value)
    }

    fun getBool(name: String): Boolean? {
        return dict[name]?.value as Boolean?
    }

    fun setInt32(name: String, value: Int) {
        putType(VdType.Int32, name, value)
    }

    fun getInt32(name: String): Int? {
        return dict[name]?.value as Int?
    }

    fun setInt64(name: String, value: Long) {
        putType(VdType.Int64, name, value)
    }

    fun getInt64(name: String): Long? {
        return dict[name]?.value as Long?
    }

    fun setString(name: String, value: String) {
        putType(VdType.String, name, value)
    }

    fun getString(name: String): String? {
        return getValue(name) as String?
    }

    fun setByteArray(name: String, value: ByteArray) {
        putType(VdType.ByteArray, name, value)
    }

    fun getByteArray(name: String): ByteArray? {
        return getValue(name) as ByteArray?
    }

    fun copyTo(d: VariantDictionary) {
        for ((key, value) in d.dict) {
            dict[key] = value
        }
    }

    fun size(): Int {
        return dict.size
    }

    companion object {
        private const val VdVersion = 0x0100
        private const val VdmCritical = 0xFF00
        private const val VdmInfo = 0x00FF
        private val UTF8Charset = Charset.forName("UTF-8")

        @Throws(IOException::class)
        fun deserialize(inputStream: LittleEndianDataInputStream): VariantDictionary {
            val dictionary = VariantDictionary()
            val version = inputStream.readUShort()
            if (version and VdmCritical > VdVersion and VdmCritical) {
                throw IOException("Invalid format")
            }
            while (true) {
                val type = inputStream.read()
                if (type < 0) {
                    throw IOException("Invalid format")
                }
                val bType = type.toByte()
                if (bType == VdType.None) {
                    break
                }
                val nameLen = inputStream.readUInt().toInt()
                val nameBuf = inputStream.readBytes(nameLen)
                if (nameLen != nameBuf.size) {
                    throw IOException("Invalid format")
                }
                val name = String(nameBuf, UTF8Charset)
                val valueLen = inputStream.readUInt().toInt()
                val valueBuf = inputStream.readBytes(valueLen)
                if (valueLen != valueBuf.size) {
                    throw IOException("Invalid format")
                }
                when (bType) {
                    VdType.UInt32 -> if (valueLen == 4) {
                        dictionary.setUInt32(name, bytes4ToUInt(valueBuf))
                    }
                    VdType.UInt64 -> if (valueLen == 8) {
                        dictionary.setUInt64(name, bytes64ToLong(valueBuf))
                    }
                    VdType.Bool -> if (valueLen == 1) {
                        dictionary.setBool(name, valueBuf[0] != 0.toByte())
                    }
                    VdType.Int32 -> if (valueLen == 4) {
                        dictionary.setInt32(name, bytes4ToUInt(valueBuf).toInt())
                    }
                    VdType.Int64 -> if (valueLen == 8) {
                        dictionary.setInt64(name, bytes64ToLong(valueBuf))
                    }
                    VdType.String -> dictionary.setString(name, String(valueBuf, UTF8Charset))
                    VdType.ByteArray -> dictionary.setByteArray(name, valueBuf)
                    else -> {
                    }
                }
            }
            return dictionary
        }

        @Throws(IOException::class)
        fun serialize(variantDictionary: VariantDictionary,
                      outputStream: LittleEndianDataOutputStream?) {
            if (outputStream == null) {
                return
            }
            outputStream.writeUShort(VdVersion)
            for ((name, vd) in variantDictionary.dict) {
                val nameBuf = name.toByteArray(UTF8Charset)
                outputStream.write(vd.type.toInt())
                outputStream.writeInt(nameBuf.size)
                outputStream.write(nameBuf)
                var buf: ByteArray
                when (vd.type) {
                    VdType.UInt32 -> {
                        outputStream.writeInt(4)
                        outputStream.writeUInt((vd.value as UnsignedInt))
                    }
                    VdType.UInt64 -> {
                        outputStream.writeInt(8)
                        outputStream.writeLong(vd.value as Long)
                    }
                    VdType.Bool -> {
                        outputStream.writeInt(1)
                        val bool = if (vd.value as Boolean) 1.toByte() else 0.toByte()
                        outputStream.write(bool.toInt())
                    }
                    VdType.Int32 -> {
                        outputStream.writeInt(4)
                        outputStream.writeInt(vd.value as Int)
                    }
                    VdType.Int64 -> {
                        outputStream.writeInt(8)
                        outputStream.writeLong(vd.value as Long)
                    }
                    VdType.String -> {
                        val value = vd.value as String
                        buf = value.toByteArray(UTF8Charset)
                        outputStream.writeInt(buf.size)
                        outputStream.write(buf)
                    }
                    VdType.ByteArray -> {
                        buf = vd.value as ByteArray
                        outputStream.writeInt(buf.size)
                        outputStream.write(buf)
                    }
                    else -> {
                    }
                }
            }
            outputStream.write(VdType.None.toInt())
        }
    }

    class VdType(val type: Byte, val value: Any) {

        companion object {
            const val None: Byte = 0x00
            const val UInt32: Byte = 0x04
            const val UInt64: Byte = 0x05
            const val Bool: Byte = 0x08
            const val Int32: Byte = 0x0C
            const val Int64: Byte = 0x0D
            const val String: Byte = 0x18
            const val ByteArray: Byte = 0x42
        }
    }
}