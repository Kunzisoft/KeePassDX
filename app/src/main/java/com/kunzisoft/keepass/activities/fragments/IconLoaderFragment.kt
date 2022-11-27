/*
 * Copyright 2021 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.activities.fragments

import android.widget.ProgressBar
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.IconPickerActivity
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.binary.BinaryData
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.icons.IconDrawableFactory
import com.kunzisoft.keepass.icons.KeePassIconsProviderClient
import com.kunzisoft.keepass.tasks.BinaryDatabaseManager
import com.kunzisoft.keepass.view.hideByFading
import com.kunzisoft.keepass.view.showByFading
import com.kunzisoft.keepass.viewmodels.IconPickerViewModel.IconCustomState
import kotlinx.coroutines.*
import kotlin.coroutines.resume

class IconLoaderFragment : IconFragment<IconImageCustom>() {

    private val mainScope = CoroutineScope(Dispatchers.Main)

    private val client by lazy {
        KeePassIconsProviderClient(requireContext().contentResolver)
    }

    override fun retrieveMainLayoutId(): Int {
        return R.layout.fragment_icon_grid
    }

    override fun onDatabaseRetrieved(database: Database?) {
        super.onDatabaseRetrieved(database)

        // Change IconDrawableFactory for IconLoader
        iconPickerAdapter.iconDrawableFactory = IconDrawableFactory(
            retrieveBinaryCache = { client.cache },
            retrieveCustomIconBinary = { iconId -> client.loadIcon(iconId) },
        )
    }

    override suspend fun defineIconList(database: Database?) {
        val iconProviderData = iconPickerViewModel.iconProviderData
        if (iconProviderData != null) {
            showLoading(true)
            client.queryIcons(iconProviderData).forEach {
                iconPickerAdapter.addIcon(it, false)
            }
            showLoading(false)
        }
    }

    override fun onIconClickListener(icon: IconImageCustom) {
        mDatabase?.let { database ->
            mainScope.launch {
                val iconCustomState = addCustomIcon(database, icon)
                val iconCustom = iconCustomState.iconCustom
                if (iconCustom != null) {
                    iconPickerViewModel.pickCustomIcon(iconCustom)
                } else {
                    iconPickerViewModel.addCustomIcon(iconCustomState)
                }
            }
        }
    }

    override fun onIconLongClickListener(icon: IconImageCustom) {}

    private suspend fun showLoading(show: Boolean) = Dispatchers.Main {
        val loading = requireView().findViewById<ProgressBar>(R.id.loading)
        if (show) loading.showByFading() else loading.hideByFading()
    }

    private suspend fun addCustomIcon(database: Database, icon: IconImageCustom) = Dispatchers.IO {
        val binaryData = client.loadIcon(icon.uuid)

        val iconCustomState = when {
            binaryData == null ->
                IconCustomState(errorStringId = R.string.error_upload_file)

            binaryData.getSize() > IconPickerActivity.MAX_ICON_SIZE ->
                IconCustomState(errorStringId = R.string.error_file_to_big)

            else ->
                addCustomIcon(database, icon, binaryData)
        }
        iconCustomState
    }

    private suspend fun addCustomIcon(
        database: Database,
        icon: IconImageCustom,
        binaryData: BinaryData,
    ): IconCustomState = suspendCancellableCoroutine { continuation ->
        database.buildNewCustomIcon { customIcon, binary ->
            BinaryDatabaseManager.resizeBitmapAndStoreDataInBinaryFile(
                database = database,
                inputStream = binaryData.getInputDataStream(client.cache),
                binaryData = binary,
            )

            val iconCustomState = when {
                binary.getSize() <= 0 ->
                    IconCustomState(errorStringId = R.string.error_upload_file)

                database.isCustomIconBinaryDuplicate(binary) ->
                    IconCustomState(errorStringId = R.string.error_duplicate_file)

                else ->
                    IconCustomState(
                        iconCustom = customIcon.apply {
                            name = icon.name
                        },
                        error = false,
                    )
            }

            if (iconCustomState.error) {
                database.removeCustomIcon(customIcon)
            }

            continuation.resume(iconCustomState)
        }
    }
}
