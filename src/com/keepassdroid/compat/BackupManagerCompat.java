/*
 * Copyright 2010 Brian Pellin.
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
package com.keepassdroid.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import android.content.Context;


public class BackupManagerCompat {
	@SuppressWarnings("unchecked")
	private static Class classBackupManager;
	@SuppressWarnings("unchecked")
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
