/*
 * Copyright 2026 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.model

import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.EntryId
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.GroupId
import com.kunzisoft.keepass.database.element.node.EmptyNodeFilter
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.NodeFilter
import com.kunzisoft.keepass.database.element.node.Nodes
import com.kunzisoft.keepass.database.search.SearchHelper
import com.kunzisoft.keepass.database.search.SearchParameters
import com.kunzisoft.keepass.model.CreditCardEntryFields.setCreditCard
import com.kunzisoft.keepass.model.PasskeyEntryFields.setPasskey
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpEntryFields
import com.kunzisoft.keepass.otp.OtpEntryFields.setOtp

/**
 * Database utility class to manage database information and conversion between elements and info objects.
 */
open class DatabaseInfo: Database() {

    private var mSearchHelper: SearchHelper = SearchHelper()

    /**
     * Get entry info by its ID.
     * @param entryId Unique identifier of the entry.
     * @param raw Whether to retrieve raw data or processed data (e.g. OTP fields).
     * @return The entry info if found, null otherwise.
     */
    fun getEntryInfoById(entryId: EntryId, raw: Boolean = false): EntryInfo? {
        return getEntryById(entryId)?.let { entry -> getEntryInfoFrom(entry, raw) }
    }

    /**
     * Get group info by its ID.
     * @param groupId Unique identifier of the group.
     * @return The group info if found, null otherwise.
     */
    fun getGroupInfoById(groupId: GroupId): GroupInfo? {
        return getGroupById(groupId)?.let { group -> getGroupInfoFrom(group) }
    }

    /**
     * Get info of the root group.
     * @return The root group info if found, null otherwise.
     */
    fun getRootGroupInfo(): GroupInfo? {
        return rootGroup?.let { group -> getGroupInfoFrom(group) }
    }

    /*
      ------------
      Converter
      ------------
     */

    /**
     * Retrieve generated entry info.
     * If are not [raw] data, remove parameter fields and add auto generated elements in auto custom fields.
     * @param entry The entry to convert.
     * @param raw Whether to retrieve raw data or processed data.
     * @return The converted entry info.
     */
    fun getEntryInfoFrom(entry: Entry, raw: Boolean = false): EntryInfo {
        val entryInfo = EntryInfo()
        // Fetch template and remove unwanted template fields
        getTemplate(entry)?.let {
            entryInfo.template = it
        }
        val baseInfo = decodeEntryWithTemplateConfiguration(entry = entry)
        baseInfo.apply {
            if (raw)
                stopManageEntry(this)
            else
                startManageEntry(this)

            entryInfo.nodeId = nodeId
            entryInfo.title = title
            entryInfo.icon = icon
            entryInfo.username = username
            entryInfo.password = password
            entryInfo.creationTime = creationTime
            entryInfo.lastModificationTime = lastModificationTime
            entryInfo.lastAccessTime = lastAccessTime
            entryInfo.expires = expires
            entryInfo.expiryTime = expiryTime
            entryInfo.url = url
            entryInfo.notes = notes
            entryInfo.tags = tags
            entryInfo.backgroundColor = backgroundColor
            entryInfo.foregroundColor = foregroundColor
            entryInfo.customData = customData
            entryInfo.autoType = autoType
            entryInfo.customFields = getExtraFields().toMutableList()
            // Add otpElement to generate token
            entryInfo.otpModel = getOtpElement()?.otpModel
            // Add Credit Card
            entryInfo.creditCard = getCreditCard()
            // Add Passkey
            entryInfo.passkey = getPasskey()
            entryInfo.appOrigin = getAppOrigin()
            if (!raw) {
                // Replace parameter fields by generated OTP fields
                entryInfo.customFields = OtpEntryFields.generateAutoFields(entryInfo.customFields)
                entryInfo.customFields = PasskeyEntryFields.generateAutoFields(entryInfo.customFields)
            }
            entryInfo.attachments = getAttachments(attachmentPool).toMutableList()

            if (!raw)
                stopManageEntry(this)
        }
        return entryInfo
    }

    /**
     * Create a new entry from its info.
     * @param newEntryInfo The info for the new entry.
     * @return The created entry if successful, null otherwise.
     */
    fun createEntry(newEntryInfo: EntryInfo): Entry? {
        return createEntry()?.apply {
            saveEntryInfo(this@DatabaseInfo, newEntryInfo)
        }
    }

    /**
     * Update an entry with new info.
     * @param entry The entry to update.
     * @param entryInfo The new info for the entry.
     * @return The updated entry.
     */
    fun updateEntry(entry: Entry, entryInfo: EntryInfo): Entry {
        return entry.apply { saveEntryInfo(this@DatabaseInfo, entryInfo) }
    }

    /**
     * Convert a group to group info.
     * @param group The group to convert.
     * @return The converted group info.
     */
    fun getGroupInfoFrom(group: Group): GroupInfo {
        val groupInfo = GroupInfo()
        group.apply {
            groupInfo.nodeId = nodeId
            groupInfo.title = title
            groupInfo.icon = icon
            groupInfo.creationTime = creationTime
            groupInfo.lastModificationTime = lastModificationTime
            groupInfo.lastAccessTime = lastAccessTime
            groupInfo.expires = expires
            groupInfo.expiryTime = expiryTime
            groupInfo.customData = customData

            groupInfo.notes = notes
            groupInfo.searchable = searchable
            groupInfo.enableAutoType = enableAutoType
            groupInfo.defaultAutoTypeSequence = defaultAutoTypeSequence
            groupInfo.tags = tags
        }

        return groupInfo
    }

    /**
     * Create a new group from its info.
     * @param newGroupInfo The info for the new group.
     * @return The created group if successful, null otherwise.
     */
    fun createGroup(newGroupInfo: GroupInfo): Group? {
        return createGroup()?.apply {
            saveGroupInfo(newGroupInfo)
        }
    }

    /**
     * Update a group with new info.
     * @param group The group to update.
     * @param groupInfo The new info for the group.
     * @return The updated group.
     */
    fun updateGroup(group: Group, groupInfo: GroupInfo): Group {
        return group.apply { saveGroupInfo(groupInfo) }
    }

    /**
     * Get history of an entry as a list of entry info.
     * @param entry The entry to get history from.
     * @return List of entry info representing the history.
     */
    fun getHistoryEntryInfoFrom(entry: Entry): List<EntryInfo> {
        return entry.getHistory().map { entryHistory ->
            getEntryInfoFrom(entryHistory)
        }
    }

    /**
     * Get history of an entry by the EntryId, as a list of entry info.
     * @param entryId The entryId to get history from.
     * @return List of entry info representing the history.
     */
    fun getHistoryEntryInfoFrom(entryId: EntryId): List<EntryInfo>? {
        return getEntryById(entryId)?.let { mainEntry ->
            getHistoryEntryInfoFrom(mainEntry)
        }
    }

    /**
     * Convert a node to node info.
     * @param node The node (entry or group) to convert.
     * @return The converted node info.
     */
    fun getNodeInfoFrom(node: Node): NodeInfo {
        return if (node is Entry) getEntryInfoFrom(node)
        else getGroupInfoFrom(node as Group)
    }

    /**
     * Get node from node info.
     * @param nodeInfo The node info to look for.
     * @return The node if found in the database, null otherwise.
     */
    fun getNodeFrom(nodeInfo: NodeInfo): Node? {
        return if (nodeInfo is EntryInfo) this.getEntryById(nodeInfo.nodeId)
        else this.getGroupById((nodeInfo as GroupInfo).nodeId)
    }

    /**
     * Save register info in entry info.
     * @param entryInfo The entry info to save in.
     * @param registerInfo The register info to save.
     * @return True if data has been overwritten, false otherwise.
     */
    fun saveRegisterInfoIn(entryInfo: EntryInfo, registerInfo: RegisterInfo): Boolean {
        return entryInfo.saveRegisterInfo(registerInfo, allowEntryCustomFields())
    }

    /**
     * Get the number of children entries in a group.
     * @param node The group info.
     * @param recursiveNumberOfEntries Whether to count entries in subgroups recursively.
     * @param filter Filter to apply to entries.
     * @return The number of child entries, or null if group not found.
     */
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

    /**
     * Get the breadcrumb of a group.
     * @param groupId The group ID.
     * @return List of group info representing the path from root to the group.
     */
    fun getBreadcrumbsFrom(
        groupId: GroupId,
        recursiveNumberOfEntries: Boolean,
        nodeFilter: NodeFilter
    ): List<SortedGroupInfo> {
        val listBreadcrumb = mutableListOf<SortedGroupInfo>()
        getGroupById(groupId)?.let {
            var currentNode = it
            listBreadcrumb.add(0,
               SortedGroupInfo(
                   groupToCopy = getGroupInfoFrom(currentNode),
                   numberChildrenEntries = it.getNumberOfChildEntries(
                       recursive = recursiveNumberOfEntries,
                       filter = nodeFilter.filter
                    ),
                   indexInParent = it.indexInParent(),
                   path = it.getPathString()
                )
            )
            while (currentNode.containsParent()) {
                currentNode.parent?.let { parent ->
                    currentNode = parent
                    listBreadcrumb.add(0,
                        SortedGroupInfo(
                            groupToCopy = getGroupInfoFrom(currentNode),
                            numberChildrenEntries = parent.getNumberOfChildEntries(
                                recursive = recursiveNumberOfEntries,
                                filter = nodeFilter.filter
                            ),
                            indexInParent = it.indexInParent(),
                            path = it.getPathString()
                        )
                    )
                }
            }
        }
        return listBreadcrumb
    }

    /**
     * Get the children of a group represented by its ID.
     * @param parentId The group ID.
     * @param recursiveNumberOfEntries Whether to count entries in subgroups recursively.
     * @param nodeFilter Filter to apply to entries.
     */
    fun getSortedChildrenOf(
        parentId: GroupId,
        recursiveNumberOfEntries: Boolean,
        nodeFilter: NodeFilter
    ): List<SortedNodeInfo> {
        return getGroupById(parentId)
            ?.getChildren(nodeFilter.filter)
            ?.mapNotNull {
                when (it) {
                    is Group -> SortedGroupInfo(
                        groupToCopy = getGroupInfoFrom(it),
                        numberChildrenEntries = it.getNumberOfChildEntries(
                            recursive = recursiveNumberOfEntries,
                            filter = nodeFilter.filter
                        ),
                        // No path here | path = it.getPathString(),
                        indexInParent = it.indexInParent()
                    )
                    is Entry -> SortedEntryInfo(
                        entryToCopy = getEntryInfoFrom(it),
                        // No path here | path = it.getPathString(),
                        indexInParent = it.indexInParent()
                    )
                    else -> null
                }
            } ?: listOf()
    }

    /**
     * Get the children of a group represented by its ID.
     * @param parentId The group ID.
     * @param nodeFilter Filter to apply to entries.
     */
    fun getChildrenNodes(
        parentId: GroupId,
        nodeFilter: NodeFilter = EmptyNodeFilter()
    ): Nodes {
        val groupsIds = mutableListOf<GroupId>()
        val entriesIds = mutableListOf<EntryId>()
        getGroupById(parentId)
            ?.getChildren(nodeFilter.filter)
            ?.mapNotNull {
                when (it) {
                    is Group -> groupsIds.add(it.nodeId)
                    is Entry -> entriesIds.add(it.nodeId)
                    else -> null
                }
            }
        return Nodes(
            listGroupsIds = groupsIds,
            listEntriesIds = entriesIds
        )
    }

    /**
     * Get the Nodes in the recycle bin.
     */
    fun getNodesInRecycleBin(): Nodes {
        recycleBin?.nodeId?.let { recycleBinId ->
            return getChildrenNodes(recycleBinId)
        }
        return Nodes()
    }

    /**
     * Check if adding a group is allowed in the specified group.
     * @param group The parent group info.
     * @return True if allowed, false otherwise.
     */
    fun allowAddGroupIn(group: GroupInfo?): Boolean {
        if (group == null)
            return false
        if (isReadOnly)
            return false
        if (group is SearchGroupInfo)
            return false
        return true
    }

    /**
     * Check if adding an entry is allowed in the specified group.
     * @param group The parent group info.
     * @return True if allowed, false otherwise.
     */
    fun allowAddEntryIn(group: GroupInfo?): Boolean {
        if (group == null)
            return false
        if (isReadOnly)
            return false
        if (group is SearchGroupInfo)
            return false
        if (allowAddEntryInRoot.not()) {
            return group.nodeId != rootGroup?.nodeId
        }
        return true
    }

    /**
     * Check if a group can be moved to the recycle bin.
     * @param groupId The group ID to check.
     * @return True if it can be recycled, false otherwise.
     */
    fun canRecycleGroup(groupId: GroupId): Boolean {
        var canRecycle = false
        getGroupById(groupId)?.let {
            canRecycle = canRecycle(it)
        }
        return canRecycle
    }

    /**
     * Check if an entry can be moved to the recycle bin.
     * @param entryId The entry ID to check.
     * @return True if it can be recycled, false otherwise.
     */
    fun canRecycleEntry(entryId: EntryId): Boolean {
        var canRecycle = false
        getEntryById(entryId)?.let {
            canRecycle = canRecycle(it)
        }
        return canRecycle
    }

    /**
     * Check if all nodes in the list are recyclable.
     * @param nodes List of nodes to check.
     * @return True if all nodes can be recycled, false otherwise.
     */
    fun eachNodeRecyclable(nodes: Nodes): Boolean {
        if (nodes.listGroupsIds.any { !canRecycleGroup(it) })
            return false
        if (nodes.listEntriesIds.any { !canRecycleEntry(it) })
            return false
        return true
    }

    /**
     * Create search group info with results based on search parameters.
     * @param searchParameters Parameters for the search.
     * @param fromGroup Optional node ID to start search from.
     * @param max Maximum number of results.
     * @return Search group info containing the results.
     */
    fun createSearchGroupInfo(
        searchParameters: SearchParameters,
        fromGroup: GroupId? = null,
        max: Int = Integer.MAX_VALUE
    ): SearchGroupInfo {
        return mSearchHelper.createGroupInfoWithSearchResult(
            database = this,
            searchParameters = searchParameters,
            fromGroup = fromGroup,
            max = max)
    }

    /**
     * Check if an entry is a template.
     * @param entry The entry to check.
     */
    fun entryIsTemplate(entry: EntryInfo?): Boolean {
        return if (entry == null)
            false
        else
            entryIsTemplate(getEntryById(entry.nodeId))
    }

    companion object {
        private fun Entry.saveEntryInfo(database: DatabaseInfo, newEntryInfo: EntryInfo) {
            database.startManageEntry(this)

            // Ensure custom fields are populated from custom objects if they are missing
            newEntryInfo.passkey?.let {
                newEntryInfo.setPasskey(it)
            }
            newEntryInfo.creditCard?.let {
                newEntryInfo.setCreditCard(it)
            }
            newEntryInfo.otpModel?.let {
                newEntryInfo.setOtp(OtpEntryFields.buildOtpField(OtpElement(it)).protectedValue.toString())
            }
            newEntryInfo.appOrigin?.let {
                newEntryInfo.saveAppOrigin(it, database.allowEntryCustomFields())
            }

            removeAllFields()
            removeAllAttachments()
            // NodeId stay as is
            title = newEntryInfo.title
            icon = newEntryInfo.icon
            username = newEntryInfo.username
            password = newEntryInfo.password
            // Update date time, creation time stay as is
            lastModificationTime = newEntryInfo.lastModificationTime
            lastAccessTime = newEntryInfo.lastAccessTime
            expires = newEntryInfo.expires
            expiryTime = newEntryInfo.expiryTime
            url = newEntryInfo.url
            notes = newEntryInfo.notes
            tags = newEntryInfo.tags
            backgroundColor = newEntryInfo.backgroundColor
            foregroundColor = newEntryInfo.foregroundColor
            customData = newEntryInfo.customData
            autoType = newEntryInfo.autoType
            addExtraFields(newEntryInfo.customFields)

            newEntryInfo.attachments.forEach { attachment ->
                putAttachment(attachment, database.attachmentPool)
            }

            database.stopManageEntry(this)

            // Encode entry properties for template
            entryKDBX = database.encodeEntryWithTemplateConfiguration(
                entry = this,
                template = newEntryInfo.template
            ).entryKDBX
        }

        private fun Group.saveGroupInfo(groupInfo: GroupInfo) {
            // Do not set id
            title = groupInfo.title
            icon = groupInfo.icon
            creationTime = groupInfo.creationTime
            lastModificationTime = groupInfo.lastModificationTime
            lastAccessTime = groupInfo.lastAccessTime
            expires = groupInfo.expires
            expiryTime = groupInfo.expiryTime
            customData = groupInfo.customData

            notes = groupInfo.notes
            searchable = groupInfo.searchable
            enableAutoType = groupInfo.enableAutoType
            defaultAutoTypeSequence = groupInfo.defaultAutoTypeSequence
            tags = groupInfo.tags
        }
    }
}