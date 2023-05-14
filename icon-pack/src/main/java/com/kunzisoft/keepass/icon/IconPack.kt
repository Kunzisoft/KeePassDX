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
package com.kunzisoft.keepass.icon

import android.content.Context
import android.content.res.Resources
import android.util.SparseIntArray
import java.util.*

/**
 * Class who construct dynamically database icons contains in a separate library
 *
 *
 * It only supports icons with specific nomenclature **[stringId]_[%2d]_32dp**
 * where [stringId] contains in a string xml attribute with id **resource_id** and
 * [%2d] 2 numerical numbers between 00 and 68 included,
 *
 *
 * See *icon-pack-classic* module as sample
 *
 * Construct dynamically the icon pack provide by the string resource id
 *
 * @param packageName Context of the app to retrieve the resources
 * @param packageName Context of the app to retrieve the resources
 * @param resourceId String Id of the pack (ex : com.kunzisoft.keepass.icon.classic.R.string.resource_id)
 */
class IconPack(packageName: String, resources: Resources, resourceId: Int) {

    private val icons: SparseIntArray = SparseIntArray()

    /**
     * Get the id of the IconPack
     *
     * @return String id of the pack
     */
    val id: String?

    /**
     * Get the name of the IconPack
     *
     * @return String visual name of the pack
     */
    val name: String

    private val tintable: Boolean

    /**
     * @return int Get the default icon resource id
     */
    val defaultIconId: Int
        get() = iconToResId(0)

    init {

        id = resources.getString(resourceId).removeSuffix("_")

        // Build the list of icons
        var num = 0
        while (num < NB_DEFAULT_ICONS) {
            // To construct the id with name_ic_XX_32dp (ex : classic_ic_08_32dp )
            val resId = resources.getIdentifier(
                    id + "_" + String.format(Locale.ENGLISH, "%02d", num) + "_32dp",
                    "drawable",
                    packageName)
            icons.put(num, resId)
            num++
        }
        // Get visual name
        name = resources.getString(
                resources.getIdentifier(
                        id + "_" + "name",
                        "string",
                        packageName
                )
        )
        // If icons are tintable
        tintable = resources.getBoolean(
                resources.getIdentifier(
                        id + "_" + "tintable",
                        "bool",
                        packageName
                )
        )
    }

    /**
     * Determine if each icon in the pack can be tint
     *
     * @return true if icons are tintable
     */
    fun tintable(): Boolean {
        return tintable
    }

    /**
     * Get the number of icons in this pack
     *
     * @return int Number of database icons
     */
    fun numberOfIcons(): Int {
        return icons.size()
    }

    /**
     * Icon as a resourceId
     *
     * @param iconId Icon database Id of the icon to retrieve
     * @return int resourceId
     */
    fun iconToResId(iconId: Int): Int {
        return icons.get(iconId, R.drawable.ic_blank_32dp)
    }

    companion object {
        private const val NB_DEFAULT_ICONS = 69

        fun defaultIconSize(context: Context): Int {
            return context.resources.getDimension(R.dimen.icon_size).toInt()
        }
    }
}
