package com.kunzisoft.encrypt.argon2

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import com.lambdapioneer.argon2kt.Argon2Version

class Argon2Transformer {

    companion object {
        fun transformKey(type: Argon2Type,
                         password: ByteArray,
                         salt: ByteArray,
                         parallelism: Long,
                         memory: Long,
                         iterations: Long,
                         version: Int): ByteArray {

            val argon2Type = when(type) {
                Argon2Type.ARGON2_I -> Argon2Mode.ARGON2_I
                Argon2Type.ARGON2_D -> Argon2Mode.ARGON2_D
                Argon2Type.ARGON2_ID -> Argon2Mode.ARGON2_ID
            }

            val argon2Version = when(version) {
                0x10 -> Argon2Version.V10
                0x13 -> Argon2Version.V13
                else -> Argon2Version.V13
            }

            return Argon2Kt().hash(
                    argon2Type,
                    password,
                    salt,
                    iterations.toInt(),
                    memory.toInt(),
                    parallelism.toInt(),
                    32,
                    argon2Version).rawHashAsByteArray()
        }
    }
}