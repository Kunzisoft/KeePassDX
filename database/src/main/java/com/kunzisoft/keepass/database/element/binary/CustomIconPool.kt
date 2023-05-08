package com.kunzisoft.keepass.database.element.binary

import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import java.util.*

class CustomIconPool : BinaryPool<UUID>() {

    private val customIcons = HashMap<UUID, IconImageCustom>()

    fun put(key: UUID? = null,
            name: String,
            lastModificationTime: DateInstant?,
            builder: (uniqueBinaryId: String) -> BinaryData,
            result: (IconImageCustom, BinaryData?) -> Unit) {
        val keyBinary = super.put(key, builder)
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

    fun getCustomIcon(key: UUID): IconImageCustom? {
        return customIcons[key]
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