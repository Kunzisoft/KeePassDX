/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
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
package com.keepassdroid.search;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.android.keepass.KeePass;
import com.android.keepass.R;
import com.keepassdroid.Database;
import com.keepassdroid.GroupBaseActivity;
import com.keepassdroid.PwGroupListAdapter;
import com.keepassdroid.app.App;

public class SearchResultsActivity extends GroupBaseActivity {
	
	private Database mDb;

	private View listView;
	private View imageNotFoundView;

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

        setContentView(getLayoutInflater().inflate(R.layout.search_results, null));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.search_label));
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        listView = findViewById(R.id.group_list);
        imageNotFoundView = findViewById(R.id.img_not_found);

		performSearch(getSearchStr(getIntent()));
		
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        // TODO Donation
        inflater.inflate(R.menu.donation, menu);
        inflater.inflate(R.menu.tree, menu);
        inflater.inflate(R.menu.default_menu, menu);

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch ( item.getItemId() ) {
            case android.R.id.home:
                finish();
        }

        return super.onOptionsItemSelected(item);
    }
	
	private void performSearch(String query) {
		mGroup = mDb.Search(query.trim());

		if ( mGroup == null || mGroup.childEntries.size() < 1 ) {
            listView.setVisibility(View.GONE);
            imageNotFoundView.setVisibility(View.VISIBLE);
		} else {
            listView.setVisibility(View.VISIBLE);
            imageNotFoundView.setVisibility(View.GONE);
        }

		setGroupTitle();
		
		setListAdapter(new PwGroupListAdapter(this, mGroup));
	}

	private String getSearchStr(Intent queryIntent) {
        // get and process search query here
        final String queryAction = queryIntent.getAction();
        if ( Intent.ACTION_SEARCH.equals(queryAction) ) {
        	return queryIntent.getStringExtra(SearchManager.QUERY);
        }
        return "";
	}
	
}
