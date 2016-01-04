/*
 * Copyright 2011 Brian Pellin.
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
package com.keepassdroid.fileselect;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.android.keepass.R;
import com.keepassdroid.utils.Util;

public class BrowserDialog extends Dialog {

	public BrowserDialog(Context context) {
		super(context);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.browser_install);
		setTitle(R.string.file_browser);
		
		Button cancel = (Button) findViewById(R.id.cancel);
		cancel.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				BrowserDialog.this.cancel();
			}
		});
		
		Button market = (Button) findViewById(R.id.install_market);
		market.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				Util.gotoUrl(getContext(), R.string.oi_filemanager_market);
				BrowserDialog.this.cancel();
			}
		});
		if (!isMarketInstalled()) {
			market.setVisibility(View.GONE);
		}
		
		Button web = (Button) findViewById(R.id.install_web);
		web.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				Util.gotoUrl(getContext(), R.string.oi_filemanager_web);
				BrowserDialog.this.cancel();
			}
		});
	}
	
	private boolean isMarketInstalled() {
		PackageManager pm = getContext().getPackageManager();
		
		try {
			pm.getPackageInfo("com.android.vending", 0);
		} catch (NameNotFoundException e) {
			return false;
		}
		
		return true;
		
	}

}
