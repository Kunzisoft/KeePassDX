/*
 * Copyright 2017 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.selection;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.kunzisoft.keepass.fileselect.FileSelectActivity;

public class EntrySelectionAuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        startFileSelectActivity();
        super.onCreate(savedInstanceState);
    }
    
    protected void startFileSelectActivity() {
        // Pass extra to get entry
        FileSelectActivity.launchForKeyboardResult(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        EntrySelectionHelper.onActivityResultSetResultAndFinish(this, requestCode, resultCode, data);
    }
}
