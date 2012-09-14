package com.keepassdroid.database;

import java.util.UUID;

import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceMap;

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
