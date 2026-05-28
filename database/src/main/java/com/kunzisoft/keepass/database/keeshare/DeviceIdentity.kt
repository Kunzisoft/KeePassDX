/*
 * Copyright 2026 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.keeshare

import java.util.UUID

/**
 * Generates device identities for per-device KeeShare sync.
 *
 * Resolution chain:
 * 1. User-configured ID from preferences (handled by caller)
 * 2. Generate random 7-char hex UUID, persist, return it
 */
object DeviceIdentity {

    private const val DEVICE_ID_SHORT_LENGTH = 7

    /**
     * Generate a fallback device ID when no user-configured ID is available.
     * Uses a short hash of a random UUID.
     *
     * The caller should persist this value (e.g., in SharedPreferences) so it
     * remains stable across app restarts.
     */
    fun generateFallbackDeviceId(): String {
        return UUID.randomUUID().toString()
            .replace("-", "")
            .take(DEVICE_ID_SHORT_LENGTH)
            .uppercase()
    }
}
