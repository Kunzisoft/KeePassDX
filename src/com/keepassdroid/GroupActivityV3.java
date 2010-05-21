package com.keepassdroid;

import android.content.Intent;

import com.keepassdroid.database.PwGroupIdV3;
import com.keepassdroid.database.PwGroupV3;

public class GroupActivityV3 extends GroupActivity {

	@Override
	protected PwGroupV3 getGroup() {
		
		return (PwGroupV3) mGroup;
	}

	@Override
	protected PwGroupIdV3 retrieveGroupId(Intent i) {
		int id = i.getIntExtra(KEY_ENTRY, -1);
		
		if ( id == -1 ) {
			return null;
		}
		
		return new PwGroupIdV3(id);
	}


}
