package com.android.keepass.database;

import java.io.IOException;
import java.lang.ref.WeakReference;

import org.phoneid.keepassj2me.PwGroup;

import android.content.Context;
import android.os.Handler;

import com.android.keepass.Database;
import com.android.keepass.UIToastTask;
import com.android.keepass.keepasslib.PwManagerOutputException;

public class DeleteGroup implements Runnable {
	
	private Database mDb;
	private PwGroup mGroup;
	private Handler uiHandler;
	private Context mCtx;
	
	public DeleteGroup(Database db, PwGroup group, Context ctx, Handler handler) {
		mDb = db;
		mGroup = group;
		uiHandler = handler;
		mCtx = ctx;
	}
	
	@Override
	public void run() {
		// Remove from parent
		PwGroup parent = mGroup.parent;
		if ( parent != null ) {
			parent.childGroups.remove(mGroup);
		}
		
		// Remove from PwManager
		mDb.mPM.groups.remove(mGroup);
		
		// Save
		try {
			mDb.SaveData();
		} catch (IOException e) {
			saveError(e.getMessage());
			return;
		} catch (PwManagerOutputException e) {
			saveError(e.getMessage());
			return;
		}
		
		// Remove from group global
		mDb.gGroups.remove(mGroup.groupId);
		
		// Mark parent dirty
		mDb.gDirty.put(parent, new WeakReference<PwGroup>(mGroup));

	}
	
	private void saveError(String msg) {
		undoRemoveGroup();
		uiHandler.post(new UIToastTask(mCtx, msg));
	}
	
	private void undoRemoveGroup() {
		mDb.mPM.groups.add(mGroup);
		
		PwGroup parent = mGroup.parent;
		if ( parent != null ) {
			parent.childGroups.add(mGroup);
		}
		
	}
}
