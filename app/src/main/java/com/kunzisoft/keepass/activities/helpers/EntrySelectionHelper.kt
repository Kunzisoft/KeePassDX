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

import android.content.Context
import android.content.Intent
import android.os.Build
import com.kunzisoft.keepass.autofill.AutofillComponent
import com.kunzisoft.keepass.autofill.AutofillHelper
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.utils.getParcelableExtraCompat
import com.kunzisoft.keepass.utils.getEnumExtra
import com.kunzisoft.keepass.utils.putEnumExtra

object EntrySelectionHelper {

    private const val KEY_SPECIAL_MODE = "com.kunzisoft.keepass.extra.SPECIAL_MODE"
    private const val KEY_TYPE_MODE = "com.kunzisoft.keepass.extra.TYPE_MODE"
    private const val KEY_SEARCH_INFO = "com.kunzisoft.keepass.extra.SEARCH_INFO"
    private const val KEY_REGISTER_INFO = "com.kunzisoft.keepass.extra.REGISTER_INFO"

    fun startActivityForSearchModeResult(context: Context,
                                         intent: Intent,
                                         searchInfo: SearchInfo) {
        addSpecialModeInIntent(intent, SpecialMode.SEARCH)
        addSearchInfoInIntent(intent, searchInfo)
        intent.flags = intent.flags or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }

    fun startActivityForSaveModeResult(context: Context,
                                             intent: Intent,
                                             searchInfo: SearchInfo) {
        addSpecialModeInIntent(intent, SpecialMode.SAVE)
        addTypeModeInIntent(intent, TypeMode.DEFAULT)
        addSearchInfoInIntent(intent, searchInfo)
        intent.flags = intent.flags or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }

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
        return intent.getParcelableExtraCompat(KEY_SEARCH_INFO)
    }

    private fun addRegisterInfoInIntent(intent: Intent, registerInfo: RegisterInfo?) {
        registerInfo?.let {
            intent.putExtra(KEY_REGISTER_INFO, it)
        }
    }

    fun retrieveRegisterInfoFromIntent(intent: Intent): RegisterInfo? {
        return intent.getParcelableExtraCompat(KEY_REGISTER_INFO)
    }

    fun removeInfoFromIntent(intent: Intent) {
        intent.removeExtra(KEY_SEARCH_INFO)
        intent.removeExtra(KEY_REGISTER_INFO)
    }

    fun addSpecialModeInIntent(intent: Intent, specialMode: SpecialMode) {
        intent.putEnumExtra(KEY_SPECIAL_MODE, specialMode)
    }

    fun retrieveSpecialModeFromIntent(intent: Intent): SpecialMode {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (AutofillHelper.retrieveAutofillComponent(intent) != null)
                return SpecialMode.SELECTION
        }
        return intent.getEnumExtra<SpecialMode>(KEY_SPECIAL_MODE) ?: SpecialMode.DEFAULT
    }

    private fun addTypeModeInIntent(intent: Intent, typeMode: TypeMode) {
        intent.putEnumExtra(KEY_TYPE_MODE, typeMode)
    }

    fun retrieveTypeModeFromIntent(intent: Intent): TypeMode {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (AutofillHelper.retrieveAutofillComponent(intent) != null)
                return TypeMode.AUTOFILL
        }
        return intent.getEnumExtra<TypeMode>(KEY_TYPE_MODE) ?: TypeMode.DEFAULT
    }

    fun removeModesFromIntent(intent: Intent) {
        intent.removeExtra(KEY_SPECIAL_MODE)
        intent.removeExtra(KEY_TYPE_MODE)
    }

    fun doSpecialAction(intent: Intent,
                        defaultAction: () -> Unit,
                        searchAction: (searchInfo: SearchInfo) -> Unit,
                        saveAction: (searchInfo: SearchInfo) -> Unit,
                        keyboardSelectionAction: (searchInfo: SearchInfo?) -> Unit,
                        autofillSelectionAction: (searchInfo: SearchInfo?,
                                                  autofillComponent: AutofillComponent) -> Unit,
                        autofillRegistrationAction: (registerInfo: RegisterInfo?) -> Unit) {

        when (retrieveSpecialModeFromIntent(intent)) {
            SpecialMode.DEFAULT -> {
                removeModesFromIntent(intent)
                removeInfoFromIntent(intent)
                defaultAction.invoke()
            }
            SpecialMode.SEARCH -> {
                val searchInfo = retrieveSearchInfoFromIntent(intent)
                removeModesFromIntent(intent)
                removeInfoFromIntent(intent)
                if (searchInfo != null)
                    searchAction.invoke(searchInfo)
                else {
                    defaultAction.invoke()
                }
            }
            SpecialMode.SAVE -> {
                val searchInfo = retrieveSearchInfoFromIntent(intent)
                removeModesFromIntent(intent)
                removeInfoFromIntent(intent)
                if (searchInfo != null)
                    saveAction.invoke(searchInfo)
                else {
                    defaultAction.invoke()
                }
            }
            SpecialMode.SELECTION -> {
                val searchInfo: SearchInfo? = retrieveSearchInfoFromIntent(intent)
                var autofillComponentInit = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    AutofillHelper.retrieveAutofillComponent(intent)?.let { autofillComponent ->
                        autofillSelectionAction.invoke(searchInfo, autofillComponent)
                        autofillComponentInit = true
                    }
                }
                if (!autofillComponentInit) {
                    if (intent.getEnumExtra<SpecialMode>(KEY_SPECIAL_MODE) != null) {
                        when (retrieveTypeModeFromIntent(intent)) {
                            TypeMode.DEFAULT -> {
                                removeModesFromIntent(intent)
                                if (searchInfo != null)
                                    searchAction.invoke(searchInfo)
                                else
                                    defaultAction.invoke()
                            }
                            TypeMode.MAGIKEYBOARD -> keyboardSelectionAction.invoke(searchInfo)
                            else -> {
                                // In this case, error
                                removeModesFromIntent(intent)
                                removeInfoFromIntent(intent)
                            }
                        }
                    } else {
                        if (searchInfo != null)
                            searchAction.invoke(searchInfo)
                        else
                            defaultAction.invoke()
                    }
                }
            }
            SpecialMode.REGISTRATION -> {
                val registerInfo: RegisterInfo? = retrieveRegisterInfoFromIntent(intent)
                removeModesFromIntent(intent)
                removeInfoFromIntent(intent)
                autofillRegistrationAction.invoke(registerInfo)
            }
        }
    }
}
