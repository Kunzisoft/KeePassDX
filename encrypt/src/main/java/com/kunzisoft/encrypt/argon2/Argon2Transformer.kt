package com.kunzisoft.encrypt.argon2

import com.kunzisoft.encrypt.UnsignedInt

class Argon2Transformer {

    companion object {
        fun transformKey(type: Argon2Type,
                         password: ByteArray?,
                         salt: ByteArray?,
                         parallelism: UnsignedInt,
                         memory: UnsignedInt,
                         iterations: UnsignedInt,
                         secretKey: ByteArray?,
                         associatedData: ByteArray?,
                         version: UnsignedInt): ByteArray {
            return NativeArgon2.transformKey(type,
                    password,
                    salt,
                    parallelism,
                    memory,
                    iterations,
                    secretKey,
                    associatedData,
                    version)
        }
    }
}