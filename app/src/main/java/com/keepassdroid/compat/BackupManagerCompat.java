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
package com.keepassdroid.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import android.content.Context;


@SuppressWarnings({"unchecked", "rawtypes"})
public class BackupManagerCompat {
	private static Class classBackupManager;
	private static Constructor constructorBackupManager;
	private static Method dataChanged;
	
	private Object backupManager;
	
	static {
		try {
			classBackupManager = Class.forName("android.app.backup.BackupManager");
			constructorBackupManager = classBackupManager.getConstructor(Context.class);
			dataChanged = classBackupManager.getMethod("dataChanged", (Class[]) null);
		} catch (Exception e) {
			// Do nothing, class does not exist
		}
	}
	
	public BackupManagerCompat(Context ctx) {
		if (constructorBackupManager != null) {
			try {
				backupManager = constructorBackupManager.newInstance(ctx);
			} catch (Exception e) {
				// Do nothing
			}
		}
	}
	
	public void dataChanged() {
		if (backupManager != null && dataChanged != null) {
			try {
				dataChanged.invoke(backupManager, (Object[]) null);
			} catch (Exception e) {
				// Do nothing
			}
		}
	}

}
