/*
 * Copyright 2009-2013 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.database;

public class PwDatabaseV4XML {

    public static final String ElemDocNode = "KeePassFile";
    public static final String ElemMeta = "Meta";
    public static final String ElemRoot = "Root";
    public static final String ElemGroup = "Group";
    public static final String ElemEntry = "Entry";

    public static final String ElemGenerator = "Generator";
    public static final String ElemHeaderHash = "HeaderHash";
    public static final String ElemDbName = "DatabaseName";
    public static final String ElemDbNameChanged = "DatabaseNameChanged";
    public static final String ElemDbDesc = "DatabaseDescription";
    public static final String ElemDbDescChanged = "DatabaseDescriptionChanged";
    public static final String ElemDbDefaultUser = "DefaultUserName";
    public static final String ElemDbDefaultUserChanged = "DefaultUserNameChanged";
    public static final String ElemDbMntncHistoryDays = "MaintenanceHistoryDays";
    public static final String ElemDbColor = "Color";
    public static final String ElemDbKeyChanged = "MasterKeyChanged";
    public static final String ElemDbKeyChangeRec = "MasterKeyChangeRec";
    public static final String ElemDbKeyChangeForce = "MasterKeyChangeForce";
    public static final String ElemRecycleBinEnabled = "RecycleBinEnabled";
    public static final String ElemRecycleBinUuid = "RecycleBinUUID";
    public static final String ElemRecycleBinChanged = "RecycleBinChanged";
    public static final String ElemEntryTemplatesGroup = "EntryTemplatesGroup";
    public static final String ElemEntryTemplatesGroupChanged = "EntryTemplatesGroupChanged";
    public static final String ElemHistoryMaxItems = "HistoryMaxItems";
    public static final String ElemHistoryMaxSize = "HistoryMaxSize";
    public static final String ElemLastSelectedGroup = "LastSelectedGroup";
    public static final String ElemLastTopVisibleGroup = "LastTopVisibleGroup";

    public static final String ElemMemoryProt = "MemoryProtection";
    public static final String ElemProtTitle = "ProtectTitle";
    public static final String ElemProtUserName = "ProtectUserName";
    public static final String ElemProtPassword = "ProtectPassword";
    public static final String ElemProtURL = "ProtectURL";
    public static final String ElemProtNotes = "ProtectNotes";
    public static final String ElemProtAutoHide = "AutoEnableVisualHiding";

    public static final String ElemCustomIcons = "CustomIcons";
    public static final String ElemCustomIconItem = "Icon";
    public static final String ElemCustomIconItemID = "UUID";
    public static final String ElemCustomIconItemData = "Data";

    public static final String ElemAutoType = "AutoType";
    public static final String ElemHistory = "History";

    public static final String ElemName = "Name";
    public static final String ElemNotes = "Notes";
    public static final String ElemUuid = "UUID";
    public static final String ElemIcon = "IconID";
    public static final String ElemCustomIconID = "CustomIconUUID";
    public static final String ElemFgColor = "ForegroundColor";
    public static final String ElemBgColor = "BackgroundColor";
    public static final String ElemOverrideUrl = "OverrideURL";
    public static final String ElemTimes = "Times";
    public static final String ElemTags = "Tags";

    public static final String ElemCreationTime = "CreationTime";
    public static final String ElemLastModTime = "LastModificationTime";
    public static final String ElemLastAccessTime = "LastAccessTime";
    public static final String ElemExpiryTime = "ExpiryTime";
    public static final String ElemExpires = "Expires";
    public static final String ElemUsageCount = "UsageCount";
    public static final String ElemLocationChanged = "LocationChanged";

    public static final String ElemGroupDefaultAutoTypeSeq = "DefaultAutoTypeSequence";
    public static final String ElemEnableAutoType = "EnableAutoType";
    public static final String ElemEnableSearching = "EnableSearching";

    public static final String ElemString = "String";
    public static final String ElemBinary = "Binary";
    public static final String ElemKey = "Key";
    public static final String ElemValue = "Value";

    public static final String ElemAutoTypeEnabled = "Enabled";
    public static final String ElemAutoTypeObfuscation = "DataTransferObfuscation";
    public static final String ElemAutoTypeDefaultSeq = "DefaultSequence";
    public static final String ElemAutoTypeItem = "Association";
    public static final String ElemWindow = "Window";
    public static final String ElemKeystrokeSequence = "KeystrokeSequence";
    
    public static final String ElemBinaries = "Binaries";

    public static final String AttrId = "ID";
    public static final String AttrRef = "Ref";
    public static final String AttrProtected = "Protected";
    public static final String AttrCompressed = "Compressed";

    public static final String ElemIsExpanded = "IsExpanded";
    public static final String ElemLastTopVisibleEntry = "LastTopVisibleEntry";

    public static final String ElemDeletedObjects = "DeletedObjects";
    public static final String ElemDeletedObject = "DeletedObject";
    public static final String ElemDeletionTime = "DeletionTime";

	public static final String ValFalse = "False";
    public static final String ValTrue = "True";

    public static final String ElemCustomData = "CustomData";
    public static final String ElemStringDictExItem = "Item";
}
