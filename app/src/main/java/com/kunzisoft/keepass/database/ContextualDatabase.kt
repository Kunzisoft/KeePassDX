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
package com.kunzisoft.keepass.database

import android.net.Uri
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.EntryId
import com.kunzisoft.keepass.database.element.GroupId
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.icons.IconDrawableFactory
import com.kunzisoft.keepass.model.AttachmentState
import com.kunzisoft.keepass.model.DatabaseInfo
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.model.StreamDirection
import com.kunzisoft.keepass.utils.SingletonHolder
import java.io.File

class ContextualDatabase: DatabaseInfo() {

    var fileUri: Uri? = null

    val iconDrawableFactory = IconDrawableFactory(
        retrieveBinaryCache = { binaryCache },
        retrieveCustomIconBinary = { iconId -> getBinaryForCustomIcon(iconId) }
    )

    private val tempAttachments = mutableListOf<EntryAttachmentState>()

    /**
     * Build entry info to create.
     * @param parentId The parent ID to use.
     * @param registerInfo The registration info to use.
     * @return The created entry info if successful, null otherwise.
     */
    suspend fun buildNewEntryInfo(
        parentId: GroupId,
        registerInfo: RegisterInfo? = null
    ): EntryInfo? {
        getGroupById(parentId)?.let { parentGroup ->
            val entry = createEntry()?.apply {
                // Add the default icon from parent if not a folder
                val parentIcon = parentGroup.icon
                // Set default icon
                if (parentIcon.custom.isUnknown
                    && parentIcon.standard.id != IconImageStandard.FOLDER_ID
                ) {
                    icon = IconImage(parentIcon.standard)
                }
                if (!parentIcon.custom.isUnknown) {
                    icon = IconImage(parentIcon.custom)
                }
                // Set default username
                username = defaultUsername
            }
            var entryInfo: EntryInfo? = null
            // Decode the entry / load entry info
            entry?.let {
                getEntryInfoFrom(entry = entry, raw = true).let { tempEntryInfo ->
                    // Retrieve data from registration
                    registerInfo?.let { regInfo ->
                        saveRegisterInfoIn(tempEntryInfo, regInfo)
                    }
                    entryInfo = tempEntryInfo
                }
            }
            return entryInfo
        }
        return null
    }

    /**
     * Build entry info to update.
     * @param entryId The entry ID to update.
     * @param registerInfo The registration info to use.
     * @param actionDataOverwrite The action to perform when data is overwritten.
     */
    suspend fun buildEntryInfoFrom(
        entryId: EntryId,
        registerInfo: RegisterInfo? = null,
        actionDataOverwrite: suspend () -> Unit = {}
    ): EntryInfo? {
        getEntryInfoById(entryId = entryId, raw = true)?.let { tempEntryInfo ->
            // Retrieve data from registration
            registerInfo?.let { regInfo ->
                if (saveRegisterInfoIn(tempEntryInfo, regInfo))
                    actionDataOverwrite()
            }
            return tempEntryInfo
        }
        return null
    }

    override fun removeCustomIcon(customIcon: IconImageCustom) {
        iconDrawableFactory.clearFromCache(customIcon)
        super.removeCustomIcon(customIcon)
    }

    fun addTempAttachment(entryAttachmentState: EntryAttachmentState) {
        if (tempAttachments.contains(entryAttachmentState)) {
            tempAttachments.remove(entryAttachmentState)
        }
        tempAttachments.add(entryAttachmentState)
    }

    fun removeTempAttachmentsNotCompleted(entryInfo: EntryInfo) {
        // Do not save entry in upload progression
        tempAttachments.forEach { attachmentState ->
            if (attachmentState.streamDirection == StreamDirection.UPLOAD) {
                when (attachmentState.downloadState) {
                    AttachmentState.START,
                    AttachmentState.IN_PROGRESS,
                    AttachmentState.CANCELED,
                    AttachmentState.ERROR -> {
                        // Remove attachment not finished from info
                        entryInfo.attachments = entryInfo.attachments.toMutableList().apply {
                            remove(attachmentState.attachment)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun removeTempAttachmentsNotUsed(entry: Entry) {
        tempAttachments.forEach { tempAttachmentState ->
            val tempAttachment = tempAttachmentState.attachment
            attachmentPool.let { binaryPool ->
                if (!entry.getAttachments(binaryPool).contains(tempAttachment)) {
                    removeAttachmentIfNotUsed(tempAttachment)
                }
            }
        }
    }

    override fun clearIndexesAndBinaries(filesDirectory: File?) {
        iconDrawableFactory.clearCache()
        super.clearIndexesAndBinaries(filesDirectory)
    }

    override fun clearAndClose(filesDirectory: File?) {
        super.clearAndClose(filesDirectory)
        this.fileUri = null
    }

    companion object : SingletonHolder<ContextualDatabase>(::ContextualDatabase) {
        private val TAG = ContextualDatabase::class.java.name
    }
}