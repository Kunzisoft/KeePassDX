/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 * 
 * This file is part of KeePassDX.
 *
 * KeePassDX is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * KeePassDX is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with KeePassDX. If not,
 * see <http://www.gnu.org/licenses/>.
 *
 * This code is based on andOTP code
 * https://github.com/andOTP/andOTP/blob/master/app/src/main/java/org/shadowice/flocke/andotp/
 * Utilities/TokenCalculator.java
 */
package com.kunzisoft.keepass.otp

import java.nio.ByteBuffer
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.text.NumberFormat
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

object TokenCalculator {
    const val TOTP_DEFAULT_PERIOD: Int = 30
    const val HOTP_INITIAL_COUNTER: Long = 1
    const val OTP_DEFAULT_DIGITS: Int = 6
    const val STEAM_DEFAULT_DIGITS: Int = 5
    val OTP_DEFAULT_ALGORITHM: HashAlgorithm = HashAlgorithm.SHA1

    private val STEAM_CHARS = charArrayOf(
        '2', '3', '4', '5', '6', '7', '8', '9', 'B', 'C',
        'D', 'F', 'G', 'H', 'J', 'K', 'M', 'N', 'P', 'Q',
        'R', 'T', 'V', 'W', 'X', 'Y'
    )

    @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class)
    private fun generateHash(
        algorithm: HashAlgorithm,
        key: ByteArray?,
        data: ByteArray?
    ): ByteArray {
        val algo = "Hmac$algorithm"

        val mac = Mac.getInstance(algo)
        mac.init(SecretKeySpec(key, algo))

        return mac.doFinal(data)
    }

    fun getTotpRfc6238Token(
        secret: ByteArray?,
        period: Int,
        time: Long,
        digits: Int,
        algorithm: HashAlgorithm
    ): Int {
        val fullToken = getTotpToken(secret, period, time, algorithm)
        val div = 10.0.pow(digits.toDouble()).toInt()

        return fullToken % div
    }

    fun getTotpRfc6238Token(
        secret: ByteArray?,
        period: Int,
        digits: Int,
        algorithm: HashAlgorithm
    ): CharArray {
        return formatTokenString(
            getTotpRfc6238Token(
                secret,
                period,
                System.currentTimeMillis() / 1000,
                digits,
                algorithm
            ), digits
        )
    }

    fun getTotpSteamToken(
        secret: ByteArray?,
        period: Int,
        digits: Int,
        algorithm: HashAlgorithm
    ): CharArray {
        var fullToken = getTotpToken(secret, period, System.currentTimeMillis() / 1000, algorithm)

        val token = CharArray(digits)
        for (i in 0..<digits) {
            token[i] = STEAM_CHARS[fullToken % STEAM_CHARS.size]
            fullToken /= STEAM_CHARS.size
        }

        return token
    }

    fun getHotpToken(secret: ByteArray?, counter: Long, digits: Int, algorithm: HashAlgorithm): CharArray {
        val fullToken = getHotpToken(secret, counter, algorithm)
        val div = 10.0.pow(digits.toDouble()).toInt()

        return formatTokenString(fullToken % div, digits)
    }

    private fun getTotpToken(key: ByteArray?, period: Int, time: Long, algorithm: HashAlgorithm): Int {
        return getHotpToken(key, time / period, algorithm)
    }

    private fun getHotpToken(key: ByteArray?, counter: Long, algorithm: HashAlgorithm): Int {
        var r = 0
        try {
            val data = ByteBuffer.allocate(8).putLong(counter).array()
            val hash = generateHash(algorithm, key, data)

            val offset = hash[hash.size - 1].toInt() and 0xF

            var binary = (hash[offset].toInt() and 0x7F) shl 0x18
            binary = binary or ((hash[offset + 1].toInt() and 0xFF) shl 0x10)
            binary = binary or ((hash[offset + 2].toInt() and 0xFF) shl 0x08)
            binary = binary or (hash[offset + 3].toInt() and 0xFF)

            r = binary
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return r
    }

    fun formatTokenString(token: Int, digits: Int): CharArray {
        val numberFormat = NumberFormat.getInstance(Locale.ENGLISH)
        numberFormat.minimumIntegerDigits = digits
        numberFormat.isGroupingUsed = false

        return numberFormat.format(token.toLong()).toCharArray()
    }

    enum class HashAlgorithm {
        SHA1, SHA256, SHA512;

        companion object {
            fun fromString(hashString: String): HashAlgorithm {
                val hash = hashString.replace(Regex("[^a-zA-Z0-9]"), "").uppercase(Locale.getDefault())
                return try {
                    valueOf(hash)
                } catch (_: Exception) {
                    OTP_DEFAULT_ALGORITHM
                }
            }
        }
    }
}