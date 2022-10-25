package com.kunzisoft.keepass.icons

import android.content.Context

interface InterfaceIconPackChooser {
    fun build(context: Context)
    fun addDefaultIconPack(context: Context)
    fun addOrCatchNewIconPack(context: Context, iconPackString: String)
    fun setSelectedIconPack(iconPackIdString: String?)
    fun getSelectedIconPack(context: Context): IconPack?
    fun getIconPackList(context: Context): List<IconPack>
}
