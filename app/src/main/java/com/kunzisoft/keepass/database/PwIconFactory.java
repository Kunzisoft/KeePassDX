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
package com.kunzisoft.keepass.database;

import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceMap;

import java.util.UUID;

public class PwIconFactory {
	/** customIconMap
	 *  Cache for icon drawable. 
	 *  Keys: Integer, Values: PwIconStandard
	 */
	private ReferenceMap cache = new ReferenceMap(AbstractReferenceMap.HARD, AbstractReferenceMap.WEAK);
	
	/** standardIconMap
	 *  Cache for icon drawable. 
	 *  Keys: UUID, Values: PwIconCustom
	 */
	private ReferenceMap customCache = new ReferenceMap(AbstractReferenceMap.HARD, AbstractReferenceMap.WEAK);

	public PwIconStandard getKeyIcon() {
		return getIcon(PwIconStandard.KEY);
	}

	public PwIconStandard getTrashIcon() {
		return getIcon(PwIconStandard.TRASH);
	}

    public PwIconStandard getFolderIcon() {
        return getIcon(PwIconStandard.FOLDER);
    }

	public PwIconStandard getIcon(int iconId) {
		PwIconStandard icon = (PwIconStandard) cache.get(iconId);
		
		if (icon == null) {
			icon = new PwIconStandard(iconId);
			cache.put(iconId, icon);
		}
		
		return icon;
	}
	
	public PwIconCustom getIcon(UUID iconUuid) {
		PwIconCustom icon = (PwIconCustom) customCache.get(iconUuid);
		
		if (icon == null) {
			icon = new PwIconCustom(iconUuid, null);
			customCache.put(iconUuid, icon);
		}
		
		return icon;
	}
	
	public PwIconCustom getIcon(UUID iconUuid, byte[] data) {
		PwIconCustom icon = (PwIconCustom) customCache.get(iconUuid);
		
		if (icon == null) {
			icon = new PwIconCustom(iconUuid, data);
			customCache.put(iconUuid, icon);
		} else {
			icon.imageData = data;
		}
		
		return icon;
	}
	
	public void setIconData(UUID iconUuid, byte[] data) {
		getIcon(iconUuid, data);
	}
	
	public void put(PwIconCustom icon) {
		customCache.put(icon.uuid, icon);
	}

}
