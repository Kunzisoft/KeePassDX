package com.kunzisoft.keepass.database

import android.net.Uri
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.icons.IconDrawableFactory
import com.kunzisoft.keepass.model.AttachmentState
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.model.StreamDirection
import com.kunzisoft.keepass.utils.SingletonHolder
import java.io.File

// TODO Change to DatabaseInfo
class ContextualDatabase: Database() {

    var fileUri: Uri? = null

    val iconDrawableFactory = IconDrawableFactory(
        retrieveBinaryCache = { binaryCache },
        retrieveCustomIconBinary = { iconId -> getBinaryForCustomIcon(iconId) }
    )

    val tempAttachments = mutableListOf<EntryAttachmentState>()

    fun getEntryInfoFrom(entry: Entry, raw: Boolean = false): EntryInfo {
        return entry.getEntryInfo(database = this, raw = raw)
    }

    fun getHistoryEntryInfoFrom(entry: Entry): List<EntryInfo> {
        return entry.getHistory().map { entryHistory ->
            entryHistory.getEntryInfo(database = this)
        }
    }

    /**
     * Build a new Entry from an EntryInfo
     */
    fun getEntryFrom(entryInfo: EntryInfo): Entry? {
        // Delete temp attachment if not completely downloaded
        removeTempAttachmentsNotCompleted(entryInfo)
        // Create the new entry
        val newEntry = getEntryById(NodeIdUUID(entryInfo.id))?.let { oldEntry ->
            // Create a clone
            Entry(oldEntry)
        } ?: createEntry()
        newEntry?.apply {
            // Build info
            setEntryInfo(this@ContextualDatabase, entryInfo)
            // Delete temp attachment from entry if not used
            removeTempAttachmentsNotUsed()
        }
        // Return entry to save
        return newEntry
    }

    /**
     * Build a new Group from a GroupInfo
     */
    fun getGroupFrom(groupInfo: GroupInfo): Group? {
        // Create the new entry
        val newGroup = groupInfo.id?.let { groupId ->
            getGroupById(NodeIdUUID(groupId))
        }?.let { oldGroup ->
            Group(oldGroup)
        } ?: createGroup()
        newGroup?.apply {
            // WARNING remove parent and children to keep memory
            removeParent()
            removeChildren()
            setGroupInfo(groupInfo)
        }
        return newGroup
    }

    override fun removeCustomIcon(customIcon: IconImageCustom) {
        iconDrawableFactory.clearFromCache(customIcon)
        super.removeCustomIcon(customIcon)
    }

    private fun removeTempAttachmentsNotCompleted(entryInfo: EntryInfo) {
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

    private fun Entry.removeTempAttachmentsNotUsed() {
        tempAttachments.forEach { tempAttachmentState ->
            val tempAttachment = tempAttachmentState.attachment
            attachmentPool.let { binaryPool ->
                if (!this.getAttachments(binaryPool)
                        .contains(tempAttachment)
                ) {
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