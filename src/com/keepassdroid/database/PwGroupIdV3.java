package com.keepassdroid.database;

public class PwGroupIdV3 extends PwGroupId {

	private int id;
	
	public PwGroupIdV3(int i) {
		id = i;
	}
	
	@Override
	public boolean equals(Object compare) {
		if ( ! (compare instanceof PwGroupIdV3) ) {
			return false;
		}
		
		PwGroupIdV3 cmp = (PwGroupIdV3) compare;
		return id == cmp.id;
	}

	@Override
	public int hashCode() {
		Integer i = new Integer(id);
		return i.hashCode();
	}
	
	

}
