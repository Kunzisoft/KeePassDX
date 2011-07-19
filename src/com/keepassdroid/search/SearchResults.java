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

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;

import com.android.keepass.KeePass;
import com.keepassdroid.Database;
import com.keepassdroid.GroupBaseActivity;
import com.keepassdroid.PwGroupListAdapter;
import com.keepassdroid.app.App;
import com.keepassdroid.view.GroupEmptyView;
import com.keepassdroid.view.GroupViewOnlyView;

public class SearchResults extends GroupBaseActivity {
	
	private Database mDb;
	//private String mQuery;
	
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

		performSearch(getSearchStr(getIntent()));
		
	}
	
	private void performSearch(String query) {
		query(query);
	}
	
	private void query(String query) {
		mGroup = mDb.Search(query);

		if ( mGroup == null || mGroup.childEntries.size() < 1 ) {
			setContentView(new GroupEmptyView(this));
		} else {
			setContentView(new GroupViewOnlyView(this));
		}
		
		setGroupTitle();
		
		setListAdapter(new PwGroupListAdapter(this, mGroup));
	}
	
	/*
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		
		mQuery = getSearchStr(intent);
		performSearch();
		//mGroup = processSearchIntent(intent);
		//assert(mGroup != null);
	}
	*/

	private String getSearchStr(Intent queryIntent) {
        // get and process search query here
        final String queryAction = queryIntent.getAction();
        if ( Intent.ACTION_SEARCH.equals(queryAction) ) {
        	return queryIntent.getStringExtra(SearchManager.QUERY);
        }
        
        return "";
		
	}
	
}
