package com.kunzisoft.keepass.database.element.binary

import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import java.util.*

class CustomIconPool(private val binaryCache: BinaryCache) : BinaryPool<UUID>(binaryCache) {

    private val customIcons = HashMap<UUID, IconImageCustom>()

    fun put(key: UUID? = null,
            name: String,
            lastModificationTime: DateInstant?,
            smallSize: Boolean,
            result: (IconImageCustom, BinaryData?) -> Unit) {
        val keyBinary = super.put(key) { uniqueBinaryId ->
            // Create a byte array for better performance with small data
            binaryCache.getBinaryData(uniqueBinaryId, smallSize)
        }
        val uuid = keyBinary.keys.first()
        val customIcon = IconImageCustom(uuid, name, lastModificationTime)
        customIcons[uuid] = customIcon
        result.invoke(customIcon, keyBinary.binary)
    }

    override fun findUnusedKey(): UUID {
        var newUUID = UUID.randomUUID()
        while (pool.containsKey(newUUID)) {
            newUUID = UUID.randomUUID()
        }
        return newUUID
    }

    fun any(predicate: (IconImageCustom)-> Boolean): Boolean {
        return customIcons.any { predicate(it.value) }
    }

    fun doForEachCustomIcon(action: (customIcon: IconImageCustom, binary: BinaryData) -> Unit) {
        doForEachBinary { key, binary ->
            action.invoke(customIcons[key] ?: IconImageCustom(key), binary)
        }
    }
}