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
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.RemoteViews
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.utils.LOCK_ACTION
import com.kunzisoft.keepass.utils.getEnum
import com.kunzisoft.keepass.utils.getEnumExtra
import com.kunzisoft.keepass.utils.getParcelableCompat
import com.kunzisoft.keepass.utils.getParcelableExtraCompat
import com.kunzisoft.keepass.utils.getParcelableList
import com.kunzisoft.keepass.utils.putEnum
import com.kunzisoft.keepass.utils.putEnumExtra
import com.kunzisoft.keepass.utils.putParcelableList
import java.io.IOException
import java.util.UUID

object EntrySelectionHelper {

    private const val KEY_SPECIAL_MODE = "com.kunzisoft.keepass.extra.SPECIAL_MODE"
    private const val KEY_TYPE_MODE = "com.kunzisoft.keepass.extra.TYPE_MODE"
    private const val KEY_SEARCH_INFO = "com.kunzisoft.keepass.extra.SEARCH_INFO"
    private const val KEY_REGISTER_INFO = "com.kunzisoft.keepass.extra.REGISTER_INFO"
    private const val EXTRA_NODES_IDS = "com.kunzisoft.keepass.extra.NODES_IDS"
    private const val EXTRA_NODE_ID = "com.kunzisoft.keepass.extra.NODE_ID"

    /**
     * Finish the activity by passing the result code and by locking the database if necessary
     */
    fun Activity.setActivityResult(
        lockDatabase: Boolean = false,
        resultCode: Int,
        data: Intent? = null
    ) {
        when (resultCode) {
            Activity.RESULT_OK ->
                this.setResult(resultCode, data)
            Activity.RESULT_CANCELED ->
                this.setResult(resultCode)
        }
        this.finish()

        if (lockDatabase) {
            // Close the database
            this.sendBroadcast(Intent(LOCK_ACTION))
        }
    }

    fun startActivityForSearchModeResult(
        context: Context,
        intent: Intent,
        searchInfo: SearchInfo
    ) {
        intent.addSpecialMode(SpecialMode.SEARCH)
        intent.addSearchInfo(searchInfo)
        intent.flags = intent.flags or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }

    fun startActivityForSelectionModeResult(
        context: Context,
        intent: Intent,
        typeMode: TypeMode,
        searchInfo: SearchInfo?,
        activityResultLauncher: ActivityResultLauncher<Intent>? = null,
    ) {
        intent.addSpecialMode(SpecialMode.SELECTION)
        intent.addTypeMode(typeMode)
        intent.addSearchInfo(searchInfo)
        if (activityResultLauncher == null) {
            intent.flags = intent.flags or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        activityResultLauncher?.launch(intent) ?: context.startActivity(intent)
    }

    fun startActivityForRegistrationModeResult(
        context: Context,
        activityResultLauncher: ActivityResultLauncher<Intent>?,
        intent: Intent,
        registerInfo: RegisterInfo?,
        typeMode: TypeMode
    ) {
        intent.addSpecialMode(SpecialMode.REGISTRATION)
        intent.addTypeMode(typeMode)
        intent.addRegisterInfo(registerInfo)
        if (activityResultLauncher == null) {
            intent.flags = intent.flags or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        activityResultLauncher?.launch(intent) ?: context.startActivity(intent)
    }

    /**
     * Build the special mode response for internal entry selection for one entry
     */
    fun Activity.buildSpecialModeResponseAndSetResult(
        entryInfo: EntryInfo,
        extras: Bundle? = null
    ) {
        this.buildSpecialModeResponseAndSetResult(listOf(entryInfo), extras)
    }

    /**
     * Build the special mode response for internal entry selection for multiple entries
     */
    fun Activity.buildSpecialModeResponseAndSetResult(
        entriesInfo: List<EntryInfo>,
        extras: Bundle? = null
    ) {
        try {
            val mReplyIntent = Intent()
            Log.d(javaClass.name, "Success special mode manual selection")
            mReplyIntent.addNodesIds(entriesInfo.map { it.id })
            extras?.let {
                mReplyIntent.putExtras(it)
            }
            setResult(Activity.RESULT_OK, mReplyIntent)
        } catch (e: Exception) {
            Log.e(javaClass.name, "Unable to add the result", e)
            setResult(Activity.RESULT_CANCELED)
        }
    }

    fun Intent.addSearchInfo(searchInfo: SearchInfo?): Intent {
        searchInfo?.let {
            putExtra(KEY_SEARCH_INFO, it)
        }
        return this
    }

    fun Bundle.addSearchInfo(searchInfo: SearchInfo?): Bundle {
        searchInfo?.let {
            putParcelable(KEY_SEARCH_INFO, it)
        }
        return this
    }

    fun Intent.retrieveSearchInfo(): SearchInfo? {
        return getParcelableExtraCompat(KEY_SEARCH_INFO)
    }

    fun Bundle.getSearchInfo(): SearchInfo? {
        return getParcelableCompat(KEY_SEARCH_INFO)
    }

    fun Intent.addRegisterInfo(registerInfo: RegisterInfo?): Intent {
        registerInfo?.let {
            putExtra(KEY_REGISTER_INFO, it)
        }
        return this
    }

    fun Bundle.addRegisterInfo(registerInfo: RegisterInfo?): Bundle {
        registerInfo?.let {
            putParcelable(KEY_REGISTER_INFO, it)
        }
        return this
    }

    fun Intent.retrieveRegisterInfo(): RegisterInfo? {
        return getParcelableExtraCompat(KEY_REGISTER_INFO)
    }

    fun Bundle.getRegisterInfo(): RegisterInfo? {
        return getParcelableCompat(KEY_REGISTER_INFO)
    }

    fun Intent.removeInfo() {
        removeExtra(KEY_SEARCH_INFO)
        removeExtra(KEY_REGISTER_INFO)
    }

    fun Intent.addSpecialMode(specialMode: SpecialMode): Intent {
        this.putEnumExtra(KEY_SPECIAL_MODE, specialMode)
        return this
    }

    fun Bundle.addSpecialMode(specialMode: SpecialMode): Bundle {
        this.putEnum(KEY_SPECIAL_MODE, specialMode)
        return this
    }

    fun Intent.retrieveSpecialMode(): SpecialMode {
        return this.getEnumExtra<SpecialMode>(KEY_SPECIAL_MODE) ?: SpecialMode.DEFAULT
    }

    fun Bundle.getSpecialMode(): SpecialMode {
        return this.getEnum<SpecialMode>(KEY_SPECIAL_MODE) ?: SpecialMode.DEFAULT
    }

    fun Intent.addTypeMode(typeMode: TypeMode): Intent {
        this.putEnumExtra(KEY_TYPE_MODE, typeMode)
        return this
    }

    fun Intent.retrieveTypeMode(): TypeMode {
        return getEnumExtra<TypeMode>(KEY_TYPE_MODE) ?: TypeMode.DEFAULT
    }

    fun Intent.removeModes() {
        removeExtra(KEY_SPECIAL_MODE)
        removeExtra(KEY_TYPE_MODE)
    }

    fun Intent.addNodesIds(nodesIds: List<UUID>): Intent {
        this.putParcelableList(EXTRA_NODES_IDS, nodesIds.map { ParcelUuid(it) })
        return this
    }

    fun Intent.retrieveNodesIds(): List<UUID>? {
        return getParcelableList<ParcelUuid>(EXTRA_NODES_IDS)?.map { it.uuid }
    }

    fun Intent.removeNodesIds() {
        removeExtra(EXTRA_NODES_IDS)
    }

    /**
     * Add the node id to the intent
     */
    fun Intent.addNodeId(nodeId: UUID?) {
        nodeId?.let {
            putExtra(EXTRA_NODE_ID, ParcelUuid(nodeId))
        }
    }

    /**
     * Retrieve the node id from the intent
     */
    fun Intent.retrieveNodeId(): UUID? {
        return getParcelableExtraCompat<ParcelUuid>(EXTRA_NODE_ID)?.uuid
    }

    fun Intent.removeNodeId() {
        removeExtra(EXTRA_NODE_ID)
    }

    /**
     * Retrieve nodes ids from intent and get the corresponding entry info list in [database]
     */
    fun Intent.retrieveAndRemoveEntries(database: ContextualDatabase): List<EntryInfo> {
        val nodesIds = retrieveNodesIds()
            ?: throw IOException("NodesIds is null")
        removeNodesIds()
        return nodesIds.mapNotNull { nodeId ->
            database
                .getEntryById(NodeIdUUID(nodeId))
                ?.getEntryInfo(database)
        }
    }

    /**
     * Intent sender uses special retains data in callback
     */
    fun isIntentSenderMode(specialMode: SpecialMode, typeMode: TypeMode): Boolean {
        return (specialMode == SpecialMode.SELECTION
                && (typeMode == TypeMode.MAGIKEYBOARD
                    || typeMode == TypeMode.AUTOFILL
                    || typeMode == TypeMode.PASSWORD
                    || typeMode == TypeMode.PASSKEY
                    )
                )
                || (specialMode == SpecialMode.REGISTRATION
                && (typeMode == TypeMode.AUTOFILL
                    || typeMode == TypeMode.PASSWORD
                    || typeMode == TypeMode.PASSKEY
                    )
                )
    }

    fun doSpecialAction(
        intent: Intent,
        defaultAction: () -> Unit,
        searchAction: (searchInfo: SearchInfo) -> Unit,
        selectionAction: (
            intentSenderMode: Boolean,
            typeMode: TypeMode,
            searchInfo: SearchInfo?
        ) -> Unit,
        registrationAction: (
            intentSenderMode: Boolean,
            typeMode: TypeMode,
            registerInfo: RegisterInfo?
        ) -> Unit
    ) {
        when (val specialMode = intent.retrieveSpecialMode()) {
            SpecialMode.DEFAULT -> {
                intent.removeModes()
                intent.removeInfo()
                defaultAction.invoke()
            }
            SpecialMode.SEARCH -> {
                val searchInfo = intent.retrieveSearchInfo()
                intent.removeModes()
                intent.removeInfo()
                if (searchInfo != null)
                    searchAction.invoke(searchInfo)
                else {
                    defaultAction.invoke()
                }
            }
            SpecialMode.SELECTION -> {
                val searchInfo: SearchInfo? = intent.retrieveSearchInfo()
                if (intent.getEnumExtra<SpecialMode>(KEY_SPECIAL_MODE) != null) {
                    when (val typeMode = intent.retrieveTypeMode()) {
                        TypeMode.DEFAULT -> {
                            intent.removeModes()
                            if (searchInfo != null)
                                searchAction.invoke(searchInfo)
                            else
                                defaultAction.invoke()
                        }
                        TypeMode.MAGIKEYBOARD -> selectionAction.invoke(
                            isIntentSenderMode(specialMode, typeMode),
                            typeMode,
                            searchInfo
                        )
                        TypeMode.AUTOFILL -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                selectionAction.invoke(
                                    isIntentSenderMode(specialMode, typeMode),
                                    typeMode,
                                    searchInfo
                                )
                            } else
                                defaultAction.invoke()
                        }
                        TypeMode.PASSWORD,
                        TypeMode.PASSKEY ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                selectionAction.invoke(
                                    isIntentSenderMode(specialMode, typeMode),
                                    typeMode,
                                    searchInfo
                                )
                            } else
                                defaultAction.invoke()
                    }
                } else {
                    if (searchInfo != null)
                        searchAction.invoke(searchInfo)
                    else
                        defaultAction.invoke()
                }
            }
            SpecialMode.REGISTRATION -> {
                val registerInfo: RegisterInfo? = intent.retrieveRegisterInfo()
                val typeMode = intent.retrieveTypeMode()
                val intentSenderMode = isIntentSenderMode(specialMode, typeMode)
                if (!intentSenderMode) {
                    intent.removeModes()
                    intent.removeInfo()
                }
                if (registerInfo != null)
                    registrationAction.invoke(
                        intentSenderMode,
                        typeMode,
                        registerInfo
                    )
                else {
                    defaultAction.invoke()
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
                return IconCompat.createWithBitmap(bitmap).toIcon(context)
            }
        } catch (e: Exception) {
            Log.e(RemoteViews::class.java.name, "Unable to assign icon in remote view", e)
        }
        return null
    }
}
