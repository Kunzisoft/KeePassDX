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
package com.kunzisoft.keepass.credentialprovider

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.credentialprovider.autofill.AutofillComponent
import com.kunzisoft.keepass.credentialprovider.autofill.AutofillHelper
import com.kunzisoft.keepass.credentialprovider.autofill.AutofillHelper.addAutofillComponent
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.LOCK_ACTION
import com.kunzisoft.keepass.utils.getEnumExtra
import com.kunzisoft.keepass.utils.getParcelableExtraCompat
import com.kunzisoft.keepass.utils.putEnumExtra

object EntrySelectionHelper {

    private const val KEY_SPECIAL_MODE = "com.kunzisoft.keepass.extra.SPECIAL_MODE"
    private const val KEY_TYPE_MODE = "com.kunzisoft.keepass.extra.TYPE_MODE"
    private const val KEY_SEARCH_INFO = "com.kunzisoft.keepass.extra.SEARCH_INFO"
    private const val KEY_REGISTER_INFO = "com.kunzisoft.keepass.extra.REGISTER_INFO"

    /**
     * Finish the activity by passing the result code and by locking the database if necessary
     */
    fun Activity.setActivityResult(
        typeMode: TypeMode,
        lockDatabase: Boolean = false,
        resultCode: Int,
        data: Intent? = null,
    ) {
        when (resultCode) {
            Activity.RESULT_OK ->
                this.setResult(resultCode, data)
            Activity.RESULT_CANCELED ->
                this.setResult(resultCode)
        }
        this.finish()

        if (lockDatabase) {
            when (typeMode) {
                TypeMode.DEFAULT -> // Close the database
                    this.sendBroadcast(Intent(LOCK_ACTION))
                TypeMode.MAGIKEYBOARD -> { }
                TypeMode.AUTOFILL -> if (PreferencesUtil.isAutofillCloseDatabaseEnable(this))
                    this.sendBroadcast(Intent(LOCK_ACTION))
                TypeMode.PASSKEY -> { }
            }
        }
    }

    /**
     * Utility method to build a registerForActivityResult,
     * Used recursively, close each activity with return data
     */
    fun AppCompatActivity.buildActivityResultLauncher(
        typeMode: TypeMode,
        lockDatabase: Boolean = false,
        dataTransformation: (data: Intent?) -> Intent? = { it },
    ): ActivityResultLauncher<Intent> {
        return this.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            setActivityResult(
                typeMode,
                lockDatabase,
                it.resultCode,
                dataTransformation(it.data)
            )
        }
    }

    fun startActivityForSearchModeResult(context: Context,
                                         intent: Intent,
                                         searchInfo: SearchInfo) {
        addSpecialModeInIntent(intent, SpecialMode.SEARCH)
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

    /**
     * Utility method to start an activity with an Autofill for result
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun startActivityForAutofillSelectionModeResult(
        context: Context,
        intent: Intent,
        activityResultLauncher: ActivityResultLauncher<Intent>?,
        autofillComponent: AutofillComponent,
        searchInfo: SearchInfo?
    ) {
        addSpecialModeInIntent(intent, SpecialMode.SELECTION)
        addTypeModeInIntent(intent, TypeMode.AUTOFILL)
        intent.addAutofillComponent(context, autofillComponent)
        addSearchInfoInIntent(intent, searchInfo)
        activityResultLauncher?.launch(intent)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun startActivityForPasskeySelectionModeResult(
        context: Context,
        intent: Intent,
        activityResultLauncher: ActivityResultLauncher<Intent>?,
        searchInfo: SearchInfo?
    ) {
        addSpecialModeInIntent(intent, SpecialMode.SELECTION)
        addTypeModeInIntent(intent, TypeMode.PASSKEY)
        addSearchInfoInIntent(intent, searchInfo)
        activityResultLauncher?.launch(intent)
    }

    fun startActivityForRegistrationModeResult(
        context: Context?,
        activityResultLauncher: ActivityResultLauncher<Intent>?,
        intent: Intent,
        registerInfo: RegisterInfo?,
        typeMode: TypeMode
    ) {
        addSpecialModeInIntent(intent, SpecialMode.REGISTRATION)
        addTypeModeInIntent(intent, typeMode)
        addRegisterInfoInIntent(intent, registerInfo)
        if (activityResultLauncher == null) {
            intent.flags = intent.flags or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        activityResultLauncher?.launch(intent) ?: context?.startActivity(intent) ?:
            throw IllegalStateException("At least Context or ActivityResultLauncher must not be null")
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
        // TODO Replace by Intent.addSpecialMode
        intent.putEnumExtra(KEY_SPECIAL_MODE, specialMode)
    }
    fun Intent.addSpecialMode(specialMode: SpecialMode): Intent {
        this.putEnumExtra(KEY_SPECIAL_MODE, specialMode)
        return this
    }

    fun retrieveSpecialModeFromIntent(intent: Intent): SpecialMode {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (AutofillHelper.retrieveAutofillComponent(intent) != null)
                return SpecialMode.SELECTION
        }
        return intent.getEnumExtra<SpecialMode>(KEY_SPECIAL_MODE) ?: SpecialMode.DEFAULT
    }

    private fun addTypeModeInIntent(intent: Intent, typeMode: TypeMode) {
        // TODO Replace by Intent.addTypeMode
        intent.putEnumExtra(KEY_TYPE_MODE, typeMode)
    }
    fun Intent.addTypeMode(typeMode: TypeMode): Intent {
        this.putEnumExtra(KEY_TYPE_MODE, typeMode)
        return this
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

    /**
     * Intent sender uses special retains data in callback
     */
    fun isIntentSenderMode(specialMode: SpecialMode, typeMode: TypeMode): Boolean {
        return (specialMode == SpecialMode.SELECTION
                && (typeMode == TypeMode.AUTOFILL || typeMode == TypeMode.PASSKEY))
                // TODO Autofill Registration callback #765 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                || (specialMode == SpecialMode.REGISTRATION
                && typeMode == TypeMode.PASSKEY)
    }

    fun doSpecialAction(
        intent: Intent,
        defaultAction: () -> Unit,
        searchAction: (searchInfo: SearchInfo) -> Unit,
        registrationAction: (registerInfo: RegisterInfo?) -> Unit,
        keyboardSelectionAction: (searchInfo: SearchInfo?) -> Unit,
        autofillSelectionAction: (searchInfo: SearchInfo?,
                                  autofillComponent: AutofillComponent) -> Unit,
        autofillRegistrationAction: (registerInfo: RegisterInfo?) -> Unit,
        passkeySelectionAction: (searchInfo: SearchInfo?) -> Unit,
        passkeyRegistrationAction: (registerInfo: RegisterInfo?) -> Unit
    ) {

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
                            TypeMode.PASSKEY -> passkeySelectionAction.invoke(searchInfo)
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
                if (!isIntentSenderMode(
                        specialMode = retrieveSpecialModeFromIntent(intent),
                        typeMode = retrieveTypeModeFromIntent(intent))
                    ) {
                    removeModesFromIntent(intent)
                    removeInfoFromIntent(intent)
                }
                when (retrieveTypeModeFromIntent(intent)) {
                    TypeMode.AUTOFILL -> {
                        autofillRegistrationAction.invoke(registerInfo)
                    }
                    TypeMode.PASSKEY -> {
                        passkeyRegistrationAction.invoke(registerInfo)
                    }
                    else -> {
                        if (registerInfo != null)
                            registrationAction.invoke(registerInfo)
                        else {
                            defaultAction.invoke()
                        }
                    }
                }
            }
        }
    }

    fun performSelection(items: List<EntryInfo>,
                         actionPopulateCredentialProvider: (entryInfo: EntryInfo) -> Unit,
                         actionEntrySelection: (autoSearch: Boolean) -> Unit) {
        if (items.size == 1) {
            val itemFound = items[0]
            actionPopulateCredentialProvider.invoke(itemFound)
        } else if (items.size > 1) {
            // Select the one we want in the selection
            actionEntrySelection.invoke(true)
        } else {
            // Select an arbitrary one
            actionEntrySelection.invoke(false)
        }
    }

    /**
     * Method to assign a drawable to a new icon from a database icon
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun EntryInfo.buildIcon(
        context: Context,
        database: ContextualDatabase
    ): Icon? {
        try {
            database.iconDrawableFactory.getBitmapFromIcon(context,
                this.icon, ContextCompat.getColor(context, R.color.green))?.let { bitmap ->
                return Icon.createWithBitmap(bitmap)
            }
        } catch (e: Exception) {
            Log.e(RemoteViews::class.java.name, "Unable to assign icon in remote view", e)
        }
        return null
    }
}
