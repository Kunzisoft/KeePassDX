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

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Class to invoke action in a separate IO thread
 */
class IOActionTask<T>(
    private val action: () -> T,
    private val onActionComplete: ((T?) -> Unit)? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val exceptionHandler: CoroutineExceptionHandler? = null
) {
    fun execute() {
        scope.launch(exceptionHandler ?: EmptyCoroutineContext) {
            withContext(Dispatchers.IO) {
                val asyncResult: Deferred<T?> = async {
                    exceptionHandler?.let {
                        action.invoke()
                    } ?: try {
                        action.invoke()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                withContext(Dispatchers.Main) {
                    onActionComplete?.invoke(asyncResult.await())
                }
            }
        }
    }
}