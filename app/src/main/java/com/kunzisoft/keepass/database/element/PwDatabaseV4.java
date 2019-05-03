/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.database.element;

import android.util.Log;
import biz.source_code.base64Coder.Base64Coder;
import com.kunzisoft.keepass.collections.VariantDictionary;
import com.kunzisoft.keepass.crypto.CryptoUtil;
import com.kunzisoft.keepass.crypto.engine.AesEngine;
import com.kunzisoft.keepass.crypto.engine.CipherEngine;
import com.kunzisoft.keepass.crypto.keyDerivation.KdfEngine;
import com.kunzisoft.keepass.crypto.keyDerivation.KdfFactory;
import com.kunzisoft.keepass.crypto.keyDerivation.KdfParameters;
import com.kunzisoft.keepass.database.*;
import com.kunzisoft.keepass.database.exception.InvalidKeyFileException;
import com.kunzisoft.keepass.database.exception.UnknownKDF;
import org.w3c.dom.*;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;


public class PwDatabaseV4 extends PwDatabase<PwGroupV4, PwEntryV4> {
    private static final String TAG = PwDatabaseV4.class.getName();

	private static final int DEFAULT_HISTORY_MAX_ITEMS = 10; // -1 unlimited
	private static final long DEFAULT_HISTORY_MAX_SIZE = 6 * 1024 * 1024; // -1 unlimited
	private static final String RECYCLEBIN_NAME = "RecycleBin";

    private PwGroupV4 rootGroup;

	private byte[] hmacKey;
	private UUID dataCipher = AesEngine.CIPHER_UUID;
	private CipherEngine dataEngine = new AesEngine();
	private PwCompressionAlgorithm compressionAlgorithm = PwCompressionAlgorithm.Gzip;
    private KdfParameters kdfParameters;
	private long numKeyEncRounds;
    private VariantDictionary publicCustomData = new VariantDictionary();

	private String name = "KeePass DX database";
    private PwDate nameChanged = new PwDate();
    private PwDate settingsChanged = new PwDate();
    private String description = "";
    private PwDate descriptionChanged = new PwDate();
    private String defaultUserName = "";
    private PwDate defaultUserNameChanged = new PwDate();
    
    private PwDate keyLastChanged = new PwDate();
    private long keyChangeRecDays = -1;
    private long keyChangeForceDays = 1;
	private boolean keyChangeForceOnce = false;
    
    private long maintenanceHistoryDays = 365;
    private String color = "";
    private boolean recycleBinEnabled = true;
    private UUID recycleBinUUID = UUID_ZERO;
    private Date recycleBinChanged = new Date();
    private UUID entryTemplatesGroup = UUID_ZERO;
    private PwDate entryTemplatesGroupChanged = new PwDate();
    private int historyMaxItems = DEFAULT_HISTORY_MAX_ITEMS;
    private long historyMaxSize = DEFAULT_HISTORY_MAX_SIZE;
    private UUID lastSelectedGroup = UUID_ZERO;
    private UUID lastTopVisibleGroup = UUID_ZERO;
    private MemoryProtectionConfig memoryProtection = new MemoryProtectionConfig();
    private List<PwDeletedObject> deletedObjects = new ArrayList<>();
    private List<PwIconCustom> customIcons = new ArrayList<>();
    private Map<String, String> customData = new HashMap<>();

    private BinaryPool binPool = new BinaryPool();

    public String localizedAppName = "KeePassDX"; // TODO resource

    public PwDatabaseV4() {}

	@Override
	public PwVersion getVersion() {
		return PwVersion.V4;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

    public byte[] getHmacKey() {
        return hmacKey;
    }

    public UUID getDataCipher() {
        return dataCipher;
    }

    public void setDataCipher(UUID dataCipher) {
        this.dataCipher = dataCipher;
    }

    public void setDataEngine(CipherEngine dataEngine) {
        this.dataEngine = dataEngine;
    }

	@Override
	public List<PwEncryptionAlgorithm> getAvailableEncryptionAlgorithms() {
		List<PwEncryptionAlgorithm> list = new ArrayList<>();
		list.add(PwEncryptionAlgorithm.AES_Rijndael);
		list.add(PwEncryptionAlgorithm.Twofish);
		list.add(PwEncryptionAlgorithm.ChaCha20);
		return list;
	}

    public PwCompressionAlgorithm getCompressionAlgorithm() {
        return compressionAlgorithm;
    }

    public void setCompressionAlgorithm(PwCompressionAlgorithm compressionAlgorithm) {
        this.compressionAlgorithm = compressionAlgorithm;
    }

	public @Nullable KdfEngine getKdfEngine() {
        try {
            return KdfFactory.getEngineV4(kdfParameters);
        } catch (UnknownKDF unknownKDF) {
            Log.i(TAG, "Unable to retrieve KDF engine", unknownKDF);
            return null;
        }
    }

    public KdfParameters getKdfParameters() {
	    return kdfParameters;
    }

    public void setKdfParameters(KdfParameters kdfParameters) {
        this.kdfParameters = kdfParameters;
    }

    @Override
    public long getNumberKeyEncryptionRounds() {
	    if (getKdfEngine() != null && getKdfParameters() != null)
            numKeyEncRounds = getKdfEngine().getKeyRounds(getKdfParameters());
        return numKeyEncRounds;
    }

    @Override
    public void setNumberKeyEncryptionRounds(long rounds) throws NumberFormatException {
        if (getKdfEngine() != null && getKdfParameters() != null)
	        getKdfEngine().setKeyRounds(getKdfParameters(), rounds);
        numKeyEncRounds = rounds;
    }

	public long getMemoryUsage() {
        if (getKdfEngine() != null && getKdfParameters() != null) {
            return getKdfEngine().getMemoryUsage(getKdfParameters());
        }
        return KdfEngine.UNKNOW_VALUE;
	}

	public void setMemoryUsage(long memory) {
        if (getKdfEngine() != null && getKdfParameters() != null)
            getKdfEngine().setMemoryUsage(getKdfParameters(), memory);
    }

	public int getParallelism() {
        if (getKdfEngine() != null && getKdfParameters() != null) {
            return getKdfEngine().getParallelism(getKdfParameters());
        }
        return KdfEngine.UNKNOW_VALUE;
	}

    public void setParallelism(int parallelism) {
        if (getKdfEngine() != null && getKdfParameters() != null)
            getKdfEngine().setParallelism(getKdfParameters(), parallelism);
    }

    public PwDate getNameChanged() {
        return nameChanged;
    }

    public void setNameChanged(PwDate nameChanged) {
        this.nameChanged = nameChanged;
    }

    public PwDate getSettingsChanged() {
        return settingsChanged;
    }

    public void setSettingsChanged(PwDate settingsChanged) {
        // TODO change setting date
        this.settingsChanged = settingsChanged;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PwDate getDescriptionChanged() {
        return descriptionChanged;
    }

    public void setDescriptionChanged(PwDate descriptionChanged) {
        this.descriptionChanged = descriptionChanged;
    }

    public String getDefaultUserName() {
        return defaultUserName;
    }

    public void setDefaultUserName(String defaultUserName) {
        this.defaultUserName = defaultUserName;
    }

    public PwDate getDefaultUserNameChanged() {
        return defaultUserNameChanged;
    }

    public void setDefaultUserNameChanged(PwDate defaultUserNameChanged) {
        this.defaultUserNameChanged = defaultUserNameChanged;
    }

    public PwDate getKeyLastChanged() {
        return keyLastChanged;
    }

    public void setKeyLastChanged(PwDate keyLastChanged) {
	    // TODO date
        this.keyLastChanged = keyLastChanged;
    }

    public long getKeyChangeRecDays() {
        return keyChangeRecDays;
    }

    public void setKeyChangeRecDays(long keyChangeRecDays) {
        this.keyChangeRecDays = keyChangeRecDays;
    }

    public long getKeyChangeForceDays() {
        return keyChangeForceDays;
    }

    public void setKeyChangeForceDays(long keyChangeForceDays) {
        this.keyChangeForceDays = keyChangeForceDays;
    }

    public boolean isKeyChangeForceOnce() {
        return keyChangeForceOnce;
    }

    public void setKeyChangeForceOnce(boolean keyChangeForceOnce) {
        this.keyChangeForceOnce = keyChangeForceOnce;
    }

    public long getMaintenanceHistoryDays() {
        return maintenanceHistoryDays;
    }

    public void setMaintenanceHistoryDays(long maintenanceHistoryDays) {
        this.maintenanceHistoryDays = maintenanceHistoryDays;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public UUID getEntryTemplatesGroup() {
        return entryTemplatesGroup;
    }

    public void setEntryTemplatesGroup(UUID entryTemplatesGroup) {
        this.entryTemplatesGroup = entryTemplatesGroup;
    }

    public PwDate getEntryTemplatesGroupChanged() {
        return entryTemplatesGroupChanged;
    }

    public void setEntryTemplatesGroupChanged(PwDate entryTemplatesGroupChanged) {
        this.entryTemplatesGroupChanged = entryTemplatesGroupChanged;
    }

    public int getHistoryMaxItems() {
        return historyMaxItems;
    }

    public void setHistoryMaxItems(int historyMaxItems) {
        this.historyMaxItems = historyMaxItems;
    }

    public long getHistoryMaxSize() {
        return historyMaxSize;
    }

    public void setHistoryMaxSize(long historyMaxSize) {
        this.historyMaxSize = historyMaxSize;
    }

    public UUID getLastSelectedGroup() {
        return lastSelectedGroup;
    }

    public void setLastSelectedGroup(UUID lastSelectedGroup) {
        this.lastSelectedGroup = lastSelectedGroup;
    }

    public UUID getLastTopVisibleGroup() {
        return lastTopVisibleGroup;
    }

    public void setLastTopVisibleGroup(UUID lastTopVisibleGroup) {
        this.lastTopVisibleGroup = lastTopVisibleGroup;
    }

    public MemoryProtectionConfig getMemoryProtection() {
        return memoryProtection;
    }

    public void setMemoryProtection(MemoryProtectionConfig memoryProtection) {
        this.memoryProtection = memoryProtection;
    }

    public List<PwIconCustom> getCustomIcons() {
        return customIcons;
    }

    public void addCustomIcon(PwIconCustom customIcon) {
        this.customIcons.add(customIcon);
    }

    public Map<String, String> getCustomData() {
        return customData;
    }

    public void putCustomData(String label, String value) {
        this.customData.put(label, value);
    }

    @Override
	public byte[] getMasterKey(String key, InputStream keyInputStream)
			throws InvalidKeyFileException, IOException {
		
		byte[] fKey = new byte[]{};
		
		if (key != null && keyInputStream != null) {
			return getCompositeKey(key, keyInputStream);
		} else if (key != null) { // key.length() >= 0
			fKey =  getPasswordKey(key);
		} else if (keyInputStream != null) { // key == null
			fKey = getFileKey(keyInputStream);
		}
		
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("No SHA-256 implementation");
		}
		
		return md.digest(fKey);
	}

	public void makeFinalKey(byte[] masterSeed) throws IOException {

        KdfEngine kdfEngine = KdfFactory.getEngineV4(kdfParameters);

		byte[] transformedMasterKey = kdfEngine.transform(masterKey, kdfParameters);
		if (transformedMasterKey.length != 32) {
			transformedMasterKey = CryptoUtil.hashSha256(transformedMasterKey);
		}

        byte[] cmpKey = new byte[65];
		System.arraycopy(masterSeed, 0, cmpKey, 0, 32);
        System.arraycopy(transformedMasterKey, 0, cmpKey, 32, 32);
		finalKey = CryptoUtil.resizeKey(cmpKey, 0, 64, dataEngine.keyLength());

        MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-512");
			cmpKey[64] = 1;
			hmacKey = md.digest(cmpKey);
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("No SHA-512 implementation");
		} finally {
			Arrays.fill(cmpKey, (byte)0);
		}
	}

	@Override
	protected String getPasswordEncoding() {
		return "UTF-8";
	}
    
	private static final String RootElementName = "KeyFile";
	//private static final String MetaElementName = "Meta";
	//private static final String VersionElementName = "Version";
	private static final String KeyElementName = "Key";
	private static final String KeyDataElementName = "Data";
	
	@Override
	protected byte[] loadXmlKeyFile(InputStream keyInputStream) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(keyInputStream);
			
			Element el = doc.getDocumentElement();
			if (el == null || ! el.getNodeName().equalsIgnoreCase(RootElementName)) {
				return null;
			}
			
			NodeList children = el.getChildNodes();
			if (children.getLength() < 2) {
				return null;
			}
			
			for ( int i = 0; i < children.getLength(); i++ ) {
				Node child = children.item(i);
				
				if ( child.getNodeName().equalsIgnoreCase(KeyElementName) ) {
					NodeList keyChildren = child.getChildNodes();
					for ( int j = 0; j < keyChildren.getLength(); j++ ) {
						Node keyChild = keyChildren.item(j);
						if ( keyChild.getNodeName().equalsIgnoreCase(KeyDataElementName) ) {
							NodeList children2 = keyChild.getChildNodes();
							for ( int k = 0; k < children2.getLength(); k++) {
								Node text = children2.item(k);
								if (text.getNodeType() == Node.TEXT_NODE) {
									Text txt = (Text) text;
									return Base64Coder.decode(txt.getNodeValue());
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}

    @Override
    public void populateNodesIndexes() {
        getRootGroup().doForEachChildAndForIt(
                new NodeHandler<PwEntryV4>() {
                    @Override
                    public boolean operate(PwEntryV4 entry) {
                        addEntryIndex(entry);
                        return true;
                    }
                },
                new NodeHandler<PwGroupV4>() {
                    @Override
                    public boolean operate(PwGroupV4 group) {
                        addGroupIndex(group);
                        return true;
                    }
                });
    }

	@Override
	public PwNodeIdUUID newGroupId() {
		PwNodeIdUUID newId;
        do {
            newId = new PwNodeIdUUID(UUID.randomUUID());
        } while (isGroupIdUsed(newId));
		
		return newId;
	}

    @Override
    public PwNodeIdUUID newEntryId() {
        PwNodeIdUUID newId;
        do {
            newId = new PwNodeIdUUID(UUID.randomUUID());
        } while (isEntryIdUsed(newId));

        return newId;
    }

	@Override
	public PwGroupV4 createGroup() {
		return new PwGroupV4();
	}

    public void setRootGroup(PwGroupV4 rootGroup) {
        this.rootGroup = rootGroup;
    }

    @Override
    public PwGroupV4 getRootGroup() {
        return rootGroup;
    }

    @Override
	public boolean isBackup(PwGroupV4 group) {
		if (!recycleBinEnabled) {
			return false;
		}
		
		return group.isContainedIn(getRecycleBin());
	}
	
	/**
     * Ensure that the recycle bin tree exists, if enabled and create it
     * if it doesn't exist
	 */
	private void ensureRecycleBin() {
		if (getRecycleBin() == null) {
			// Create recycle bin
				
			PwGroupV4 recycleBin = new PwGroupV4();
            recycleBin.setTitle(RECYCLEBIN_NAME);
            recycleBin.setIconStandard(iconFactory.getTrashIcon());
			recycleBin.setEnableAutoType(false);
			recycleBin.setEnableSearching(false);
			recycleBin.setExpanded(false);
			addGroupTo(recycleBin, rootGroup);
			
			recycleBinUUID = recycleBin.getNodeId().getId();
		}
	}

    public UUID getRecycleBinUUID() {
        return recycleBinUUID;
    }

    public void setRecycleBinUUID(UUID recycleBinUUID) {
        this.recycleBinUUID = recycleBinUUID;
    }

	/**
	 * Determine if RecycleBin is enable or not
	 * @return true if RecycleBin enable, false if is not available or not enable
	 */
	public boolean isRecycleBinEnabled() {
		return recycleBinEnabled;
	}

	public void setRecycleBinEnabled(boolean recycleBinEnabled) {
	    this.recycleBinEnabled = recycleBinEnabled;
    }

    public Date getRecycleBinChanged() {
        return recycleBinChanged;
    }

    public void setRecycleBinChanged(Date recycleBinChanged) {
	    // TODO recyclebin Date
        this.recycleBinChanged = recycleBinChanged;
    }

	/**
	 * Define if a Group must be delete or recycle
	 * @param group Group to remove
	 * @return true if group can be recycle, false elsewhere
	 */
	public boolean canRecycle(PwGroupV4 group) {
		if (!recycleBinEnabled) {
			return false;
		}
		PwGroupV4 recycle = getRecycleBin();
		return (recycle == null) || (!group.isContainedIn(recycle));
	}

	/**
	 * Define if an Entry must be delete or recycle
	 * @param entry Entry to remove
	 * @return true if entry can be recycle, false elsewhere
	 */
	public boolean canRecycle(PwEntryV4 entry) {
		if (!recycleBinEnabled) {
			return false;
		}
		PwGroupV4 parent = entry.getParent();
		return (parent != null) && canRecycle(parent);
	}

	public void recycle(PwGroupV4 group) {
		ensureRecycleBin();

		removeGroupFrom(group, group.getParent());

		addGroupTo(group, getRecycleBin());

        // TODO ? group.touchLocation();
	}

	public void recycle(PwEntryV4 entry) {
		ensureRecycleBin();

		removeEntryFrom(entry, entry.getParent());

		addEntryTo(entry, getRecycleBin());

		entry.touchLocation();
	}

    public void undoRecycle(PwGroupV4 group, PwGroupV4 origParent) {

        removeGroupFrom(group, getRecycleBin());

        addGroupTo(group, origParent);
    }

	public void undoRecycle(PwEntryV4 entry, PwGroupV4 origParent) {

		removeEntryFrom(entry, getRecycleBin());
		
		addEntryTo(entry, origParent);
	}

    public List<PwDeletedObject> getDeletedObjects() {
        return deletedObjects;
    }

    public void addDeletedObject(PwDeletedObject deletedObject) {
        this.deletedObjects.add(deletedObject);
    }

	@Override
	protected void removeEntryFrom(PwEntryV4 entryToRemove, PwGroupV4 parent) {
		super.removeEntryFrom(entryToRemove, parent);
		deletedObjects.add(new PwDeletedObject((UUID) entryToRemove.getNodeId().getId()));
	}

	@Override
	public void undoDeleteEntryFrom(PwEntryV4 entry, PwGroupV4 origParent) {
		super.undoDeleteEntryFrom(entry, origParent);
        deletedObjects.remove(new PwDeletedObject((UUID) entry.getNodeId().getId()));
	}

	public PwGroupV4 getRecycleBin() { // TODO delete recycle bin preference
		if (recycleBinUUID == null) {
			return null;
		}
		
		PwNodeId recycleId = new PwNodeIdUUID(recycleBinUUID);
		return groupIndexes.get(recycleId);
	}

    public VariantDictionary getPublicCustomData() {
        return publicCustomData;
    }

    public boolean containsPublicCustomData() {
	    return publicCustomData.size() > 0;
    }

    public void setPublicCustomData(VariantDictionary publicCustomData) {
        this.publicCustomData = publicCustomData;
    }

    public BinaryPool getBinPool() {
        return binPool;
    }

    public void setBinPool(BinaryPool binPool) {
        this.binPool = binPool;
    }

    @Override
	public boolean isGroupSearchable(PwGroupV4 group, boolean omitBackup) {
		if (!super.isGroupSearchable(group, omitBackup)) {
			return false;
		}
		return group.isSearchingEnabled();
	}

	@Override
	public boolean validatePasswordEncoding(String key) {
		return true;
	}

    public void clearCache() {
        binPool.clear();
    }
}