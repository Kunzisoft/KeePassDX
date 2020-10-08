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
package com.kunzisoft.keepass.activities.helpers

import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.os.Build
import com.kunzisoft.keepass.autofill.AutofillHelper
import com.kunzisoft.keepass.model.SearchInfo
import java.io.Serializable

object EntrySelectionHelper {

    private const val KEY_SPECIAL_MODE = "com.kunzisoft.keepass.extra.SPECIAL_MODE"
    private const val KEY_TYPE_MODE = "com.kunzisoft.keepass.extra.TYPE_MODE"
    private const val KEY_SEARCH_INFO = "com.kunzisoft.keepass.extra.SEARCH_INFO"

    fun startActivityForSpecialModeResult(context: Context,
                                          intent: Intent,
                                          specialMode: SpecialMode,
                                          searchInfo: SearchInfo?) {
        addSpecialModeInIntent(intent, specialMode)
        // At the moment, only autofill for registration
        addTypeModeInIntent(intent, TypeMode.AUTOFILL)
        addSearchInfoInIntent(intent, searchInfo)
        context.startActivity(intent)
    }

    fun addSearchInfoInIntent(intent: Intent, searchInfo: SearchInfo?) {
        searchInfo?.let {
            intent.putExtra(KEY_SEARCH_INFO, it)
        }
    }

    fun retrieveSearchInfoFromIntent(intent: Intent): SearchInfo? {
        return intent.getParcelableExtra(KEY_SEARCH_INFO)
    }

    fun removeSearchInfoFromIntent(intent: Intent) {
        intent.removeExtra(KEY_SEARCH_INFO)
    }

    fun addSpecialModeInIntent(intent: Intent, specialMode: SpecialMode) {
        intent.putExtra(KEY_SPECIAL_MODE, specialMode as Serializable)
    }

    fun retrieveSpecialModeFromIntent(intent: Intent): SpecialMode {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (AutofillHelper.retrieveAssistStructure(intent) != null)
                return SpecialMode.SELECTION
        }
        return intent.getSerializableExtra(KEY_SPECIAL_MODE) as SpecialMode?
                ?: SpecialMode.DEFAULT
    }

    fun addTypeModeInIntent(intent: Intent, typeMode: TypeMode) {
        intent.putExtra(KEY_TYPE_MODE, typeMode as Serializable)
    }

    fun retrieveTypeModeFromIntent(intent: Intent): TypeMode {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (AutofillHelper.retrieveAssistStructure(intent) != null)
                return TypeMode.AUTOFILL
        }
        return intent.getSerializableExtra(KEY_TYPE_MODE) as TypeMode? ?: TypeMode.DEFAULT
    }

    fun removeModesFromIntent(intent: Intent) {
        intent.removeExtra(KEY_SPECIAL_MODE)
        intent.removeExtra(KEY_TYPE_MODE)
    }

    fun doSpecialAction(intent: Intent,
                        defaultAction: (searchInfo: SearchInfo?) -> Unit,
                        keyboardSelectionAction: (searchInfo: SearchInfo?) -> Unit,
                        autofillSelectionAction: (searchInfo: SearchInfo?,
                                                  assistStructure: AssistStructure?) -> Unit,
                        registrationAction: (searchInfo: SearchInfo?) -> Unit) {

        val searchInfo: SearchInfo? = retrieveSearchInfoFromIntent(intent)
        when (retrieveSpecialModeFromIntent(intent)) {
            SpecialMode.DEFAULT -> {
                defaultAction.invoke(searchInfo)
            }
            SpecialMode.SELECTION -> {
                var assistStructureInit = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    AutofillHelper.retrieveAssistStructure(intent)?.let { assistStructure ->
                        autofillSelectionAction.invoke(searchInfo, assistStructure)
                        assistStructureInit = true
                    }
                }
                if (!assistStructureInit) {
                    if (intent.getSerializableExtra(KEY_SPECIAL_MODE) != null) {
                        removeModesFromIntent(intent)
                        when (retrieveTypeModeFromIntent(intent)) {
                            TypeMode.DEFAULT -> defaultAction.invoke(searchInfo)
                            TypeMode.MAGIKEYBOARD -> keyboardSelectionAction.invoke(searchInfo)
                            TypeMode.AUTOFILL -> autofillSelectionAction.invoke(searchInfo, null)
                        }
                    } else {
                        defaultAction.invoke(searchInfo)
                    }
                }
            }
            SpecialMode.REGISTRATION -> {
                registrationAction.invoke(searchInfo)
            }
        }
    }
}
