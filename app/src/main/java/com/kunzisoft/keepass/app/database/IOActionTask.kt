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
package com.kunzisoft.keepass.app.database

import kotlinx.coroutines.*

/**
 * Class to invoke action in a separate IO thread
 */
class IOActionTask<T>(
        private val action: suspend () -> T ,
        private val afterActionDatabaseListener: ((T?) -> Unit)? = null) {

    private val mainScope = CoroutineScope(Dispatchers.Main)

    fun execute() {
        mainScope.launch {
            withContext(Dispatchers.IO) {
                val asyncResult: Deferred<T?> = async {
                        try {
                            action.invoke()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                }
                withContext(Dispatchers.Main) {
                    afterActionDatabaseListener?.invoke(asyncResult.await())
                }
            }
        }
    }
}