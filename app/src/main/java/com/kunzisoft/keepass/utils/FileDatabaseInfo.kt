package com.kunzisoft.keepass.utils

import android.content.Context
import android.net.Uri
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.settings.PreferencesUtil

class FileDatabaseInfo : FileInfo {

    constructor(context: Context, fileUri: Uri): super(context, fileUri)

    constructor(context: Context, filePath: String): super(context, filePath)

    fun retrieveDatabaseAlias(alias: String): String {
        return when {
            alias.isNotEmpty() -> alias
            PreferencesUtil.isFullFilePathEnable(context) -> filePath ?: ""
            else -> fileName ?: ""
        }
    }

    fun retrieveDatabaseTitle(titleCallback: (String)->Unit) {

        FileDatabaseHistoryAction.getInstance(context.applicationContext).getFileDatabaseHistory(fileUri) {
            fileDatabaseHistoryEntity ->

            titleCallback.invoke(retrieveDatabaseAlias(fileDatabaseHistoryEntity?.databaseAlias ?: ""))
        }
    }

}