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
package com.kunzisoft.keepass.database.action.node

import android.content.Context
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.node.Node

class UpdateEntryRunnable constructor(
        context: Context,
        database: Database,
        private val mOldEntry: Entry,
        private val mNewEntry: Entry,
        save: Boolean,
        afterActionNodesFinish: AfterActionNodesFinish?)
    : ActionNodeDatabaseRunnable(context, database, afterActionNodesFinish, save) {

    // Keep backup of original values in case save fails
    private var mBackupEntryHistory: Entry = Entry(mOldEntry)

    override fun nodeAction() {
        // WARNING : Re attribute parent removed in entry edit activity to save memory
        mNewEntry.addParentFrom(mOldEntry)

        // Build oldest attachments
        val oldEntryAttachments = mOldEntry.getAttachments(database.attachmentPool, true)
        val newEntryAttachments = mNewEntry.getAttachments(database.attachmentPool, true)
        val attachmentsToRemove = ArrayList<Attachment>(oldEntryAttachments)
        // Not use equals because only check name
        newEntryAttachments.forEach { newAttachment ->
            oldEntryAttachments.forEach { oldAttachment ->
                if (oldAttachment.name == newAttachment.name
                        && oldAttachment.binaryData == newAttachment.binaryData)
                    attachmentsToRemove.remove(oldAttachment)
            }
        }

        // Update entry with new values
        mOldEntry.updateWith(mNewEntry)
        mNewEntry.touch(modified = true, touchParents = true)

        // Create an entry history (an entry history don't have history)
        mOldEntry.addEntryToHistory(Entry(mBackupEntryHistory, copyHistory = false))
        database.removeOldestEntryHistory(mOldEntry, database.attachmentPool)

        // Only change data in index
        database.updateEntry(mOldEntry)

        // Remove oldest attachments
        attachmentsToRemove.forEach {
            database.removeAttachmentIfNotUsed(it)
        }
    }

    override fun nodeFinish(): ActionNodesValues {
        if (!result.isSuccess) {
            mOldEntry.updateWith(mBackupEntryHistory)
            // If we fail to save, back out changes to global structure
            database.updateEntry(mOldEntry)
        }

        val oldNodesReturn = ArrayList<Node>()
        oldNodesReturn.add(mBackupEntryHistory)
        val newNodesReturn = ArrayList<Node>()
        newNodesReturn.add(mOldEntry)
        return ActionNodesValues(oldNodesReturn, newNodesReturn)
    }
}
