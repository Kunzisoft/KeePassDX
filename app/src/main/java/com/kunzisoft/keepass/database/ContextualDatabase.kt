package com.kunzisoft.keepass.database

import android.net.Uri
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.icons.IconDrawableFactory
import com.kunzisoft.keepass.utils.SingletonHolder
import java.io.File

class ContextualDatabase: Database() {

    var fileUri: Uri? = null

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

    override fun clearAndClose(filesDirectory: File?) {
        super.clearAndClose(filesDirectory)
        this.fileUri = null
    }

    companion object : SingletonHolder<ContextualDatabase>(::ContextualDatabase) {
        private val TAG = ContextualDatabase::class.java.name
    }
}