package com.kunzisoft.keepass.database.element.database

import com.kunzisoft.keepass.database.element.binary.BinaryCache
import com.kunzisoft.keepass.database.element.binary.BinaryPool
import java.util.*

class CustomIconPool(binaryCache: BinaryCache) : BinaryPool<UUID>(binaryCache) {

    override fun findUnusedKey(): UUID {
        var newUUID = UUID.randomUUID()
        while (pool.containsKey(newUUID)) {
            newUUID = UUID.randomUUID()
        }
        return newUUID
    }
}