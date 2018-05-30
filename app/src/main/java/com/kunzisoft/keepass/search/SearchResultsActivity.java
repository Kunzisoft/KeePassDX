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
package com.kunzisoft.keepass.search;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.activities.ListNodesActivity;
import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.database.PwGroup;
import com.kunzisoft.keepass.utils.MenuUtil;

import javax.annotation.Nullable;

public class SearchResultsActivity extends ListNodesActivity {

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(getLayoutInflater().inflate(R.layout.search_results, null));

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.search_label));
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        groupNameView = findViewById(R.id.group_name);

        attachFragmentToContentView();

        View notFoundView = findViewById(R.id.not_found_container);
        View listContainer = findViewById(R.id.nodes_list_fragment_container);

        if ( mCurrentGroup == null || mCurrentGroup.numbersOfChildEntries() < 1 ) {
            listContainer.setVisibility(View.GONE);
            notFoundView.setVisibility(View.VISIBLE);
        } else {
            listContainer.setVisibility(View.VISIBLE);
            notFoundView.setVisibility(View.GONE);
        }
	}

    @Override
    protected PwGroup retrieveCurrentGroup(@Nullable Bundle savedInstanceState) {
        Database mDb = App.getDB();
        // Likely the app has been killed exit the activity
        if ( ! mDb.getLoaded() ) {
            finish();
        }
        return mDb.search(getSearchStr(getIntent()).trim());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        MenuUtil.contributionMenuInflater(inflater, menu);
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

	private String getSearchStr(Intent queryIntent) {
        // get and process search query here
        final String queryAction = queryIntent.getAction();
        if ( Intent.ACTION_SEARCH.equals(queryAction) ) {
        	return queryIntent.getStringExtra(SearchManager.QUERY);
        }
        return "";
	}
	
}
