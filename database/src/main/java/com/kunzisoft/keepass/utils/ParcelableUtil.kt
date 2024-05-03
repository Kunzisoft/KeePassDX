/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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

import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable
import java.util.*

// -------- Intent --------
inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(key: String?): T? = when {
    key == null -> null
    SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

inline fun <reified T : Serializable> Intent.getSerializableExtraCompat(key: String?): T? = when {
    key == null -> null
    SDK_INT >= 33 -> getSerializableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializableExtra(key) as? T
}

inline fun <reified E : Parcelable> Intent.putParcelableList(key: String?, list: MutableList<E>) {
    putExtra(key, list.toTypedArray())
}

inline fun <reified E : Parcelable> Intent.getParcelableList(key: String?): MutableList<E>? = when {
    SDK_INT >= 33 -> getParcelableArrayExtra(key, E::class.java)?.toMutableList()
    else -> @Suppress("DEPRECATION", "UNCHECKED_CAST") (getParcelableArrayExtra(key) as? Array<E>)?.toMutableList()
}

inline fun <reified T : Enum<T>> Intent.getEnumExtra(key: String?) =
    getStringExtra(key)?.let { enumValueOf<T>(it) }

fun <T : Enum<T>> Intent.putEnumExtra(key: String?, value: T?) =
    putExtra(key, value?.name)

// -------- Bundle --------
inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String?): T? = when {
    key == null -> null
    SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

inline fun <reified T : Serializable> Bundle.getSerializableCompat(key: String?): T? = when {
    SDK_INT >= 33 -> getSerializable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializable(key) as? T
}

inline fun <reified T : Parcelable> Bundle.getParcelableArrayCompat(key: String?): Array<out T>? {
    return when {
        SDK_INT >= 33 -> getParcelableArray(key, T::class.java)
        else -> @Suppress("DEPRECATION", "UNCHECKED_CAST") (getParcelableArray(key) as? Array<T>)
    }
}

inline fun <reified E : Parcelable> Bundle.putParcelableList(key: String?, list: List<E>) {
    putParcelableArray(key, list.toTypedArray())
}

inline fun <reified E : Parcelable> Bundle.getParcelableList(key: String?): MutableList<E>? = when {
    SDK_INT >= 33 -> getParcelableArray(key, E::class.java)?.toMutableList()
    else -> @Suppress("DEPRECATION", "UNCHECKED_CAST") (getParcelableArray(key) as? Array<E>)?.toMutableList()
}

// -------- Parcel --------

inline fun <reified T : Parcelable> Parcel.readParcelableCompat(): T? = when {
    SDK_INT >= 33 -> readParcelable(T::class.java.classLoader, T::class.java)
    else -> @Suppress("DEPRECATION") readParcelable(T::class.java.classLoader) as? T
}

fun <T> Parcel.readParcelableCompat(clazz: Class<T>): T? = when {
    SDK_INT >= 33 -> readParcelable(clazz.classLoader, clazz)
    else -> @Suppress("DEPRECATION") readParcelable(clazz.classLoader) as? T
}

inline fun <reified T : Serializable> Parcel.readSerializableCompat(): T? = when {
    SDK_INT >= 33 -> readSerializable(T::class.java.classLoader, T::class.java)
    else -> @Suppress("DEPRECATION") readSerializable() as? T
}

inline fun <reified T> Parcel.readListCompat(outVal: MutableList<T>) {
    when {
        SDK_INT >= 33 -> readList(outVal, T::class.java.classLoader, T::class.java)
        else -> @Suppress("DEPRECATION") readList(outVal, T::class.java.classLoader)
    }
}

// For writing to a Parcel
fun <K : Parcelable, V : Parcelable> Parcel.writeParcelableMap(map: Map<K, V>, flags: Int) {
    writeInt(map.size)
    for ((key, value) in map) {
        writeParcelable(key, flags)
        writeParcelable(value, flags)
    }
}

// For reading from a Parcel
inline fun <reified K : Parcelable, reified V : Parcelable> Parcel.readParcelableMap(): Map<K, V> {
    val size = readInt()
    val map = HashMap<K, V>(size)
    for (i in 0 until size) {
        val key: K? = try {
            when {
                SDK_INT >= 33 -> readParcelable(K::class.java.classLoader, K::class.java)
                else -> @Suppress("DEPRECATION") readParcelable(K::class.java.classLoader)
            }
        } catch (e: Exception) { null }
        val value: V? = try {
            when {
                SDK_INT >= 33 -> readParcelable(V::class.java.classLoader, V::class.java)
                else -> @Suppress("DEPRECATION") readParcelable(V::class.java.classLoader)
            }
        } catch (e: Exception) { null }
        if (key != null && value != null)
            map[key] = value
    }
    return map
}

// For writing map with string key to a Parcel
fun <V : Parcelable> Parcel.writeStringParcelableMap(map: HashMap<String, V>, flags: Int) {
    writeInt(map.size)
    for ((key, value) in map) {
        writeString(key)
        writeParcelable(value, flags)
    }
}

// For reading map with string key from a Parcel
inline fun <reified V : Parcelable> Parcel.readStringParcelableMap(): LinkedHashMap<String, V> {
    val size = readInt()
    val map = LinkedHashMap<String, V>(size)
    for (i in 0 until size) {
        val key: String? = readString()
        val value: V? = try {
            when {
                SDK_INT >= 33 -> readParcelable(V::class.java.classLoader, V::class.java)
                else -> @Suppress("DEPRECATION") readParcelable(V::class.java.classLoader)
            }
        } catch (e: Exception) { null }
        if (key != null && value != null)
            map[key] = value
    }
    return map
}

// For writing map with string key and Int value to a Parcel
fun Parcel.writeStringIntMap(map: LinkedHashMap<String, Int>) {
    writeInt(map.size)
    for ((key, value) in map) {
        writeString(key)
        writeInt(value)
    }
}

// For reading map with string key and Int value from a Parcel
fun Parcel.readStringIntMap(): LinkedHashMap<String, Int> {
    val size = readInt()
    val map = LinkedHashMap<String, Int>(size)
    for (i in 0 until size) {
        val key: String? = readString()
        val value: Int = readInt()
        if (key != null)
            map[key] = value
    }
    return map
}


// For writing map with string key and string value to a Parcel
fun Parcel.writeStringStringMap(map: MutableMap<String, String>) {
    writeInt(map.size)
    for ((key, value) in map) {
        writeString(key)
        writeString(value)
    }
}

fun Parcel.readStringStringMap(): LinkedHashMap<String, String> {
    val size = readInt()
    val map = LinkedHashMap<String, String>(size)
    for (i in 0 until size) {
        val key: String? = readString()
        val value: String? = readString()
        if (key != null && value != null)
            map[key] = value
    }
    return map
}

fun Parcel.readByteArrayCompat(): ByteArray? {
    val dataLength = readInt()
    return if (dataLength >= 0) {
        val data = ByteArray(dataLength)
        readByteArray(data)
        data
    } else {
        null
    }
}

fun Parcel.writeByteArrayCompat(data: ByteArray?) {
    if (data != null) {
        writeInt(data.size)
        writeByteArray(data)
    } else {
        writeInt(-1)
    }
}

inline fun <reified T : Enum<T>> Parcel.readEnum() =
    readString()?.let { enumValueOf<T>(it) }

fun <T : Enum<T>> Parcel.writeEnum(value: T?) =
    writeString(value?.name)

fun Parcel.readBooleanCompat(): Boolean = readByte().toInt() != 0

fun Parcel.writeBooleanCompat(value: Boolean) = writeByte((if (value) 1 else 0).toByte())