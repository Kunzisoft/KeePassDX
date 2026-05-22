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
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.database.element.node.EmptyNodeFilter
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.NodeFilter
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.search.SearchHelper
import com.kunzisoft.keepass.database.search.SearchParameters
import com.kunzisoft.keepass.icons.IconDrawableFactory
import com.kunzisoft.keepass.model.AttachmentState
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.model.NodeInfo
import com.kunzisoft.keepass.model.SearchGroupInfo
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

    private val tempAttachments = mutableListOf<EntryAttachmentState>()

    private var mSearchHelper: SearchHelper = SearchHelper()

    fun getEntryInfoFrom(entry: Entry, raw: Boolean = false): EntryInfo {
        return entry.getEntryInfo(database = this, raw = raw)
    }

    fun getHistoryEntryInfoFrom(entry: Entry): List<EntryInfo> {
        return entry.getHistory().map { entryHistory ->
            entryHistory.getEntryInfo(database = this)
        }
    }

    fun getNodeInfoFrom(node: Node): NodeInfo {
        return if (node is Entry) node.getEntryInfo(this)
        else (node as Group).getGroupInfo()
    }

    fun getNodeFrom(nodeInfo: NodeInfo): Node? {
        return if (nodeInfo is EntryInfo) this.getEntryById(nodeInfo.nodeId)
        else this.getGroupById((nodeInfo as GroupInfo).nodeId)
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

    fun getNumberChildrenEntries(
        node: GroupInfo,
        recursiveNumberOfEntries: Boolean = true,
        filter: NodeFilter = EmptyNodeFilter(),
    ): Int? {
        return getGroupById(node.nodeId)?.getNumberOfChildEntries(
            recursive = recursiveNumberOfEntries,
            filter = filter.filter
        )
    }

    fun getBreadcrumb(node: GroupInfo): List<GroupInfo> {
        val listBreadcrumb = mutableListOf<GroupInfo>()
        getGroupById(node.nodeId)?.let {
            var currentNode = it
            listBreadcrumb.add(0, currentNode.getGroupInfo())
            while (currentNode.containsParent()) {
                currentNode.parent?.let { parent ->
                    currentNode = parent
                    listBreadcrumb.add(0, currentNode.getGroupInfo())
                }
            }
        }
        return listBreadcrumb
    }

    fun allowAddGroupIn(group: GroupInfo?, forceReadOnly: Boolean = false): Boolean {
        if (group == null)
            return false
        if (isReadOnly || forceReadOnly)
            return false
        if (group is SearchGroupInfo)
            return false
        return true
    }

    fun allowAddEntryIn(group: GroupInfo?, forceReadOnly: Boolean = false): Boolean {
        if (group == null)
            return false
        if (isReadOnly || forceReadOnly)
            return false
        if (group is SearchGroupInfo)
            return false
        if (allowAddEntryInRoot.not()) {
            return group.nodeId != rootGroup?.nodeId
        }
        return true
    }

    fun canRecycle(entryInfo: EntryInfo): Boolean {
        var canRecycle = false
        getEntryById(entryInfo.nodeId)?.let {
            canRecycle = canRecycle(it)
        }
        return canRecycle
    }

    fun canRecycle(groupInfo: GroupInfo): Boolean {
        var canRecycle = false
        getGroupById(groupInfo.nodeId)?.let {
            canRecycle = canRecycle(it)
        }
        return canRecycle
    }

    fun eachNodeRecyclable(nodes: List<NodeInfo>): Boolean {
        return nodes.find { node ->
            var cannotRecycle = true
            when (node) {
                is EntryInfo -> {
                    cannotRecycle = !canRecycle(node)
                }
                is GroupInfo -> {
                    cannotRecycle = !canRecycle(node)
                }
            }
            cannotRecycle
        } == null
    }

    fun createSearchGroupInfo(
        searchParameters: SearchParameters,
        fromGroup: NodeId<*>? = null,
        max: Int = Integer.MAX_VALUE
    ): SearchGroupInfo {
        return mSearchHelper.createGroupInfoWithSearchResult(
            database = this,
            searchParameters = searchParameters,
            fromGroup = fromGroup,
            max = max)
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