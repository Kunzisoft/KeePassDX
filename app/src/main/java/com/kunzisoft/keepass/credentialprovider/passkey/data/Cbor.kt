/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kunzisoft.keepass.credentialprovider.passkey.data

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
class Cbor {
    data class Item(val item: Any, val len: Int)

    data class Arg(val arg: Long, val len: Int)

    val TYPE_UNSIGNED_INT = 0x00
    val TYPE_NEGATIVE_INT = 0x01
    val TYPE_BYTE_STRING = 0x02
    val TYPE_TEXT_STRING = 0x03
    val TYPE_ARRAY = 0x04
    val TYPE_MAP = 0x05
    val TYPE_TAG = 0x06
    val TYPE_FLOAT = 0x07

    fun decode(data: ByteArray): Any {
        val ret = parseItem(data, 0)
        return ret.item
    }

    fun encode(data: Any): ByteArray {
        if (data is Number) {
            if (data is Double) {
                throw IllegalArgumentException("Don't support doubles yet")
            } else {
                val value = data.toLong()
                if (value >= 0) {
                    return createArg(TYPE_UNSIGNED_INT, value)
                } else {
                    return createArg(TYPE_NEGATIVE_INT, -1 - value)
                }
            }
        }
        if (data is ByteArray) {
            return createArg(TYPE_BYTE_STRING, data.size.toLong()) + data
        }
        if (data is String) {
            return createArg(TYPE_TEXT_STRING, data.length.toLong()) + data.encodeToByteArray()
        }
        if (data is List<*>) {
            var ret = createArg(TYPE_ARRAY, data.size.toLong())
            for (i in data) {
                ret += encode(i!!)
            }
            return ret
        }
        if (data is Map<*, *>) {
            // See:
            // https://fidoalliance.org/specs/fido-v2.1-ps-20210615/fido-client-to-authenticator-protocol-v2.1-ps-20210615.html#ctap2-canonical-cbor-encoding-form
            var ret = createArg(TYPE_MAP, data.size.toLong())
            var byteMap: MutableMap<ByteArray, ByteArray> = mutableMapOf()
            for (i in data) {
                // Convert to byte arrays so we can sort them.
                byteMap.put(encode(i.key!!), encode(i.value!!))
            }

            var keysList = ArrayList<ByteArray>(byteMap.keys)
            keysList.sortedWith(
                Comparator<ByteArray> { a, b ->
                    // If two keys have different lengths, the shorter one sorts earlier;
                    // If two keys have the same length, the one with the lower value in (byte-wise)
                    // lexical order sorts earlier.
                    var aBytes = byteMap.get(a)!!
                    var bBytes = byteMap.get(b)!!
                    when {
                        a.size > b.size -> 1
                        a.size < b.size -> -1
                        aBytes.size > bBytes.size -> 1
                        aBytes.size < bBytes.size -> -1
                        else -> 0
                    }
                }
            )

            for (key in keysList) {
                ret += key
                ret += byteMap.get(key)!!
            }
            return ret
        }
        throw IllegalArgumentException("Bad type")
    }

    private fun getType(data: ByteArray, offset: Int): Int {
        val d = data[offset].toInt()
        return (d and 0xFF) shr 5
    }

    private fun getArg(data: ByteArray, offset: Int): Arg {
        val arg = data[offset].toLong() and 0x1F
        if (arg < 24) {
            return Arg(arg, 1)
        }
        if (arg == 24L) {
            return Arg(data[offset + 1].toLong() and 0xFF, 2)
        }
        if (arg == 25L) {
            var ret = (data[offset + 1].toLong() and 0xFF) shl 8
            ret = ret or (data[offset + 2].toLong() and 0xFF)
            return Arg(ret, 3)
        }
        if (arg == 26L) {
            var ret = (data[offset + 1].toLong() and 0xFF) shl 24
            ret = ret or ((data[offset + 2].toLong() and 0xFF) shl 16)
            ret = ret or ((data[offset + 3].toLong() and 0xFF) shl 8)
            ret = ret or (data[offset + 4].toLong() and 0xFF)
            return Arg(ret, 5)
        }
        throw IllegalArgumentException("Bad arg")
    }

    private fun parseItem(data: ByteArray, offset: Int): Item {
        val itemType = getType(data, offset)
        val arg = getArg(data, offset)
        println("Type $itemType ${arg.arg} ${arg.len}")

        when (itemType) {
            TYPE_UNSIGNED_INT -> {
                return Item(arg.arg, arg.len)
            }
            TYPE_NEGATIVE_INT -> {
                return Item(-1 - arg.arg, arg.len)
            }
            TYPE_BYTE_STRING -> {
                val ret =
                    data.sliceArray(offset + arg.len.toInt() until offset + arg.len.toInt() + arg.arg.toInt())
                return Item(ret, arg.len + arg.arg.toInt())
            }
            TYPE_TEXT_STRING -> {
                val ret =
                    data.sliceArray(offset + arg.len.toInt() until offset + arg.len.toInt() + arg.arg.toInt())
                return Item(ret.toString(Charsets.UTF_8), arg.len + arg.arg.toInt())
            }
            TYPE_ARRAY -> {
                val ret = mutableListOf<Any>()
                var consumed = arg.len
                for (i in 0 until arg.arg.toInt()) {
                    val item = parseItem(data, offset + consumed)
                    ret.add(item.item)
                    consumed += item.len
                }
                return Item(ret.toList(), consumed)
            }
            TYPE_MAP -> {
                val ret = mutableMapOf<Any, Any>()
                var consumed = arg.len
                for (i in 0 until arg.arg.toInt()) {
                    val key = parseItem(data, offset + consumed)
                    consumed += key.len
                    val value = parseItem(data, offset + consumed)
                    consumed += value.len
                    ret[key.item] = value.item
                }
                return Item(ret.toMap(), consumed)
            }
            else -> {
                throw IllegalArgumentException("Bad type")
            }
        }
    }

    private fun createArg(type: Int, arg: Long): ByteArray {
        val t = type shl 5
        val a = arg.toInt()
        if (arg < 24) {
            return byteArrayOf(((t or a) and 0xFF).toByte())
        }
        if (arg <= 0xFF) {
            return byteArrayOf(((t or 24) and 0xFF).toByte(), (a and 0xFF).toByte())
        }
        if (arg <= 0xFFFF) {
            return byteArrayOf(
                ((t or 25) and 0xFF).toByte(),
                ((a shr 8) and 0xFF).toByte(),
                (a and 0xFF).toByte()
            )
        }
        if (arg <= 0xFFFFFFFF) {
            return byteArrayOf(
                ((t or 26) and 0xFF).toByte(),
                ((a shr 24) and 0xFF).toByte(),
                ((a shr 16) and 0xFF).toByte(),
                ((a shr 8) and 0xFF).toByte(),
                (a and 0xFF).toByte()
            )
        }
        throw IllegalArgumentException("bad Arg")
    }
}
