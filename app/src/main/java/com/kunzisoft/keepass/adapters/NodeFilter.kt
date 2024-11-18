package com.kunzisoft.keepass.adapters

import android.content.Context
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.settings.PreferencesUtil

class NodeFilter(
    context: Context,
    var database: Database? = null
) {
    var recursiveNumberOfEntries = PreferencesUtil.recursiveNumberEntries(context)
        private set
    private var showExpired = PreferencesUtil.showExpiredEntries(context)
    private var showTemplate = PreferencesUtil.showTemplates(context)

    val filter: (Node) -> Boolean = { node ->
        when (node) {
            is Entry -> {
                node.entryKDB?.isMetaStream() != true
            }
            is Group -> {
                showTemplate || database?.templatesGroup != node
            }
            else -> true
        } && (showExpired || !node.isCurrentlyExpires)
    }
}