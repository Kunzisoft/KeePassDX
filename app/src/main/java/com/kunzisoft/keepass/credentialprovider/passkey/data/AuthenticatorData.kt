/*
 * Copyright 2025 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.credentialprovider.passkey.data

import com.kunzisoft.encrypt.HashManager

class AuthenticatorData {

    companion object {
        fun buildAuthenticatorData(
            relyingPartyId: ByteArray,
            userPresent: Boolean,
            userVerified: Boolean,
            backupEligibility: Boolean,
            backupState: Boolean,
            attestedCredentialData: Boolean = false
        ): ByteArray {
            // https://www.w3.org/TR/webauthn-3/#table-authData
            var flags = 0
            if (userPresent)
                flags = flags or 0x01
            // bit at index 1 is reserved
            if (userVerified)
                flags = flags or 0x04
            if (backupEligibility)
                flags = flags or 0x08
            if (backupState)
                flags = flags or 0x10
            // bit at index 5 is reserved
            if (attestedCredentialData) {
                flags = flags or 0x40
            }
            // bit at index 7: Extension data included == false

            return HashManager.hashSha256(relyingPartyId) +
                    byteArrayOf(flags.toByte()) +
                    byteArrayOf(0, 0, 0, 0)
        }
    }
}