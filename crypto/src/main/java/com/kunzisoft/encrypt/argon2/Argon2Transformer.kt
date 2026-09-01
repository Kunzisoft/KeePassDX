package com.kunzisoft.encrypt.argon2

import com.kunzisoft.encrypt.NativeLib

object Argon2Transformer {

    fun transformKey(
        type: Argon2Type,
        password: ByteArray?,
        salt: ByteArray?,
        parallelism: UInt,
        memory: UInt,
        iterations: UInt,
        version: Int
    ): ByteArray {
        if (password == null || salt == null) {
            throw IllegalArgumentException("Password and salt must not be null")
        }

        NativeLib.init()
        val argon2Type = when(type) {
            Argon2Type.ARGON2_I -> NativeArgon2KeyTransformer.CType.ARGON2_I
            Argon2Type.ARGON2_D -> NativeArgon2KeyTransformer.CType.ARGON2_D
            Argon2Type.ARGON2_ID -> NativeArgon2KeyTransformer.CType.ARGON2_ID
        }

        return NativeArgon2KeyTransformer.nTransformKey(
                argon2Type.cValue,
                password,
                salt,
                parallelism.toInt(),
                memory.toInt(),
                iterations.toInt(),
                ByteArray(0),
                ByteArray(0),
                version
        )
    }
}