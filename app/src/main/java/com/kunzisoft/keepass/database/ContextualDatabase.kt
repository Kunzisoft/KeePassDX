package com.kunzisoft.keepass.database

import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.icons.IconDrawableFactory
import com.kunzisoft.keepass.utils.SingletonHolder
import java.io.File

class ContextualDatabase: Database() {

    val iconDrawableFactory = IconDrawableFactory(
        retrieveBinaryCache = { binaryCache },
        retrieveCustomIconBinary = { iconId -> getBinaryForCustomIcon(iconId) }
    )

    override fun removeCustomIcon(customIcon: IconImageCustom) {
        iconDrawableFactory.clearFromCache(customIcon)
        super.removeCustomIcon(customIcon)
    }

    override fun clearIndexesAndBinaries(filesDirectory: File?) {
        iconDrawableFactory.clearCache()
        super.clearIndexesAndBinaries(filesDirectory)
    }

    companion object : SingletonHolder<ContextualDatabase>(::ContextualDatabase) {
        private val TAG = ContextualDatabase::class.java.name
    }
}