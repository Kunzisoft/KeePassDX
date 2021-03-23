package com.kunzisoft.keepass.database.element.binary

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