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
package com.kunzisoft.keepass.keeshare

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log

/** Watches SAF URIs for changes to KeeShare container files via ContentObserver. */
class KeeShareObserver(private val onChange: (Uri) -> Unit) {

    private val observers = mutableMapOf<Uri, ContentObserver>()

    /** Start observing [uri]. Replaces any existing observer for the same URI. */
    fun observe(contentResolver: ContentResolver, uri: Uri) {
        stop(contentResolver, uri)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                onChange(uri)
            }
        }
        try {
            contentResolver.registerContentObserver(uri, true, observer)
            observers[uri] = observer
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register ContentObserver for $uri", e)
        }
    }

    fun stop(contentResolver: ContentResolver, uri: Uri) {
        observers.remove(uri)?.let {
            try {
                contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister ContentObserver", e)
            }
        }
    }

    fun stopAll(contentResolver: ContentResolver) {
        observers.values.forEach {
            try {
                contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister ContentObserver", e)
            }
        }
        observers.clear()
    }

    companion object {
        private val TAG = KeeShareObserver::class.java.name
    }
}
