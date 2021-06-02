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
package com.kunzisoft.keepass.database.file

import java.text.SimpleDateFormat
import java.util.*

object DatabaseKDBXXML {

    const val ElemDocNode = "KeePassFile"
    const val ElemMeta = "Meta"
    const val ElemRoot = "Root"
    const val ElemGroup = "Group"
    const val ElemEntry = "Entry"

    const val ElemGenerator = "Generator"
    const val ElemHeaderHash = "HeaderHash"
    const val ElemSettingsChanged = "SettingsChanged"
    const val ElemDbName = "DatabaseName"
    const val ElemDbNameChanged = "DatabaseNameChanged"
    const val ElemDbDesc = "DatabaseDescription"
    const val ElemDbDescChanged = "DatabaseDescriptionChanged"
    const val ElemDbDefaultUser = "DefaultUserName"
    const val ElemDbDefaultUserChanged = "DefaultUserNameChanged"
    const val ElemDbMntncHistoryDays = "MaintenanceHistoryDays"
    const val ElemDbColor = "Color"
    const val ElemDbKeyChanged = "MasterKeyChanged"
    const val ElemDbKeyChangeRec = "MasterKeyChangeRec"
    const val ElemDbKeyChangeForce = "MasterKeyChangeForce"
    const val ElemDbKeyChangeForceOnce = "MasterKeyChangeForceOnce"
    const val ElemRecycleBinEnabled = "RecycleBinEnabled"
    const val ElemRecycleBinUuid = "RecycleBinUUID"
    const val ElemRecycleBinChanged = "RecycleBinChanged"
    const val ElemEntryTemplatesGroup = "EntryTemplatesGroup"
    const val ElemEntryTemplatesGroupChanged = "EntryTemplatesGroupChanged"
    const val ElemHistoryMaxItems = "HistoryMaxItems"
    const val ElemHistoryMaxSize = "HistoryMaxSize"
    const val ElemLastSelectedGroup = "LastSelectedGroup"
    const val ElemLastTopVisibleGroup = "LastTopVisibleGroup"

    const val ElemMemoryProt = "MemoryProtection"
    const val ElemProtTitle = "ProtectTitle"
    const val ElemProtUserName = "ProtectUserName"
    const val ElemProtPassword = "ProtectPassword"
    const val ElemProtURL = "ProtectURL"
    const val ElemProtNotes = "ProtectNotes"
    const val ElemProtAutoHide = "AutoEnableVisualHiding"

    const val ElemCustomIcons = "CustomIcons"
    const val ElemCustomIconItem = "Icon"
    const val ElemCustomIconItemID = "UUID"
    const val ElemCustomIconItemData = "Data"

    const val ElemAutoType = "AutoType"
    const val ElemHistory = "History"

    const val ElemName = "Name"
    const val ElemNotes = "Notes"
    const val ElemUuid = "UUID"
    const val ElemIcon = "IconID"
    const val ElemCustomIconID = "CustomIconUUID"
    const val ElemFgColor = "ForegroundColor"
    const val ElemBgColor = "BackgroundColor"
    const val ElemOverrideUrl = "OverrideURL"
    const val ElemQualityCheck = "QualityCheck"
    const val ElemTimes = "Times"
    const val ElemTags = "Tags"
    const val ElemPreviousParentGroup = "PreviousParentGroup"

    const val ElemCreationTime = "CreationTime"
    const val ElemLastModTime = "LastModificationTime"
    const val ElemLastAccessTime = "LastAccessTime"
    const val ElemExpiryTime = "ExpiryTime"
    const val ElemExpires = "Expires"
    const val ElemUsageCount = "UsageCount"
    const val ElemLocationChanged = "LocationChanged"

    const val ElemGroupDefaultAutoTypeSeq = "DefaultAutoTypeSequence"
    const val ElemEnableAutoType = "EnableAutoType"
    const val ElemEnableSearching = "EnableSearching"

    const val ElemString = "String"
    const val ElemBinary = "Binary"
    const val ElemKey = "Key"
    const val ElemValue = "Value"

    const val ElemAutoTypeEnabled = "Enabled"
    const val ElemAutoTypeObfuscation = "DataTransferObfuscation"
    const val ElemAutoTypeDefaultSeq = "DefaultSequence"
    const val ElemAutoTypeItem = "Association"
    const val ElemWindow = "Window"
    const val ElemKeystrokeSequence = "KeystrokeSequence"

    const val ElemBinaries = "Binaries"

    const val AttrId = "ID"
    const val AttrRef = "Ref"
    const val AttrProtected = "Protected"
    const val AttrCompressed = "Compressed"

    const val ElemIsExpanded = "IsExpanded"
    const val ElemLastTopVisibleEntry = "LastTopVisibleEntry"

    const val ElemDeletedObjects = "DeletedObjects"
    const val ElemDeletedObject = "DeletedObject"
    const val ElemDeletionTime = "DeletionTime"

    const val ValFalse = "False"
    const val ValTrue = "True"
    const val ValNull = "Null"

    const val ElemCustomData = "CustomData"
    const val ElemStringDictExItem = "Item"

    val DateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
}
