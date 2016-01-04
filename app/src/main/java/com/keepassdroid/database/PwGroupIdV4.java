package com.keepassdroid.database;

import java.util.UUID;

public class PwGroupIdV4 extends PwGroupId {
	private UUID uuid;
	
	public PwGroupIdV4(UUID u) {
		uuid = u;
	}
	
	@Override
	public boolean equals(Object id) {
		if ( ! (id instanceof PwGroupIdV4) ) {
			return false;
		}
		
		PwGroupIdV4 v4 = (PwGroupIdV4) id;
		
		return uuid.equals(v4.uuid);
	}
	
	@Override
	public int hashCode() {
		return uuid.hashCode();
	}
	
	public UUID getId() {
		return uuid;
	}

}
