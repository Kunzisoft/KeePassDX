package com.keepassdroid.database;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class PwIconFactory {
	private Map<Integer, PwIconStandard> cache = new WeakHashMap<Integer, PwIconStandard>();
	private Map<UUID, PwIconCustom> customCache = new WeakHashMap<UUID, PwIconCustom>();
	
	public PwIconStandard getIcon(int iconId) {
		PwIconStandard icon = cache.get(iconId);
		
		if (icon == null) {
			icon = new PwIconStandard(iconId);
			cache.put(iconId, icon);
		}
		
		return icon;
	}
	
	public PwIconCustom getIcon(UUID iconUuid) {
		PwIconCustom icon = customCache.get(iconUuid);
		
		if (icon == null) {
			icon = new PwIconCustom(iconUuid, null);
			customCache.put(iconUuid, icon);
		}
		
		return icon;
	}
	
	public PwIconCustom getIcon(UUID iconUuid, byte[] data) {
		PwIconCustom icon = customCache.get(iconUuid);
		
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
