/*
 * Copyright 2009 Brian Pellin.
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
package com.keepassdroid.search;

import org.phoneid.keepassj2me.PwGroup;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;

import com.android.keepass.KeePass;
import com.android.keepass.R;
import com.keepassdroid.Database;
import com.keepassdroid.GroupBaseActivity;
import com.keepassdroid.PwListAdapter;
import com.keepassdroid.app.App;

public class SearchResults extends GroupBaseActivity {
	
	private Database mDb;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if ( isFinishing() ) {
			return;
		}
		
		setResult(KeePass.EXIT_NORMAL);
		
		mDb = App.getDB();
		
		// Likely the app has been killed exit the activity 
		if ( ! mDb.Loaded() ) {
			finish();
		}

		mGroup = processSearchIntent(getIntent());
		assert(mGroup != null);
		
		if ( mGroup == null || mGroup.childEntries.size() < 1 ) {
			setContentView(R.layout.group_empty);
		} else {
			setContentView(R.layout.group_view_only);
		}
		
		setGroupTitle();
		
		setListAdapter(new PwListAdapter(this, mGroup));
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		
		mGroup = processSearchIntent(intent);
		assert(mGroup != null);
	}

	private PwGroup processSearchIntent(Intent queryIntent) {
        // get and process search query here
        final String queryAction = queryIntent.getAction();
        if ( Intent.ACTION_SEARCH.equals(queryAction) ) {
        	final String queryString = queryIntent.getStringExtra(SearchManager.QUERY);
        
			return mDb.Search(queryString);
        }
        
        return null;
		
	}

}
