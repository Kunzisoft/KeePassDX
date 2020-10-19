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
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.model.SearchInfo
import java.io.Serializable

object EntrySelectionHelper {

    private const val KEY_SPECIAL_MODE = "com.kunzisoft.keepass.extra.SPECIAL_MODE"
    private const val KEY_TYPE_MODE = "com.kunzisoft.keepass.extra.TYPE_MODE"
    private const val KEY_SEARCH_INFO = "com.kunzisoft.keepass.extra.SEARCH_INFO"
    private const val KEY_REGISTER_INFO = "com.kunzisoft.keepass.extra.REGISTER_INFO"

    fun startActivityForKeyboardSelectionModeResult(context: Context,
                                                    intent: Intent,
                                                    searchInfo: SearchInfo?) {
        addSpecialModeInIntent(intent, SpecialMode.SELECTION)
        addTypeModeInIntent(intent, TypeMode.MAGIKEYBOARD)
        addSearchInfoInIntent(intent, searchInfo)
        intent.flags = intent.flags or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }

    fun startActivityForRegistrationModeResult(context: Context,
                                               intent: Intent,
                                               registerInfo: RegisterInfo?) {
        addSpecialModeInIntent(intent, SpecialMode.REGISTRATION)
        // At the moment, only autofill for registration
        addTypeModeInIntent(intent, TypeMode.AUTOFILL)
        addRegisterInfoInIntent(intent, registerInfo)
        intent.flags = intent.flags or Intent.FLAG_ACTIVITY_CLEAR_TASK
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

    fun addRegisterInfoInIntent(intent: Intent, registerInfo: RegisterInfo?) {
        registerInfo?.let {
            intent.putExtra(KEY_REGISTER_INFO, it)
        }
    }

    fun retrieveRegisterInfoFromIntent(intent: Intent): RegisterInfo? {
        return intent.getParcelableExtra(KEY_REGISTER_INFO)
    }

    fun removeInfoFromIntent(intent: Intent) {
        intent.removeExtra(KEY_SEARCH_INFO)
        intent.removeExtra(KEY_REGISTER_INFO)
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
                                                  assistStructure: AssistStructure) -> Unit,
                        registrationAction: (registerInfo: RegisterInfo?) -> Unit) {

        when (retrieveSpecialModeFromIntent(intent)) {
            SpecialMode.DEFAULT -> {
                removeModesFromIntent(intent)
                removeInfoFromIntent(intent)
                defaultAction.invoke(retrieveSearchInfoFromIntent(intent))
            }
            SpecialMode.SELECTION -> {
                val searchInfo: SearchInfo? = retrieveSearchInfoFromIntent(intent)
                var assistStructureInit = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    AutofillHelper.retrieveAssistStructure(intent)?.let { assistStructure ->
                        autofillSelectionAction.invoke(searchInfo, assistStructure)
                        assistStructureInit = true
                    }
                }
                if (!assistStructureInit) {
                    if (intent.getSerializableExtra(KEY_SPECIAL_MODE) != null) {
                        val typeMode = retrieveTypeModeFromIntent(intent)
                        removeModesFromIntent(intent)
                        when (typeMode) {
                            TypeMode.DEFAULT -> defaultAction.invoke(searchInfo)
                            TypeMode.MAGIKEYBOARD -> keyboardSelectionAction.invoke(searchInfo)
                            else -> {
                                // In this case, error
                                removeModesFromIntent(intent)
                                removeInfoFromIntent(intent)
                            }
                        }
                    } else {
                        defaultAction.invoke(searchInfo)
                    }
                }
            }
            SpecialMode.REGISTRATION -> {
                val registerInfo: RegisterInfo? = retrieveRegisterInfoFromIntent(intent)
                removeModesFromIntent(intent)
                removeInfoFromIntent(intent)
                registrationAction.invoke(registerInfo)
            }
        }
    }
}
