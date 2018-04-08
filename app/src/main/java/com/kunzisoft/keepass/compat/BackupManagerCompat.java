/*
 * Copyright 2018 Brian Pellin, Jeremy Jamet / Kunzisoft, Justin Gross.
 *     
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass Libre is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass Libre.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.compat;

import android.content.Context;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;


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
