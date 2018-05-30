package com.kunzisoft.keepass.database.action.node;

import android.content.Context;

import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.database.PwNode;
import com.kunzisoft.keepass.database.action.ActionDatabaseRunnable;

abstract class ActionNodeDatabaseRunnable extends ActionDatabaseRunnable {

    private AfterActionNodeOnFinish callbackRunnable;

    public ActionNodeDatabaseRunnable(Context context, Database db, AfterActionNodeOnFinish finish, boolean dontSave) {
        super(context, db, finish, dontSave);
        this.callbackRunnable = finish;
    }

    /**
     * Callback method who return the node(s) modified after an action
     * - Add : @param oldNode NULL, @param newNode CreatedNode
     * - Copy : @param oldNode NodeToCopy, @param newNode NodeCopied
     * - Delete : @param oldNode NodeToDelete, @param NULL
     * - Move : @param oldNode NULL, @param NodeToMove
     * - Update : @param oldNode NodeToUpdate, @param NodeUpdated
     */
    protected void callbackNodeAction(boolean success, String message, PwNode oldNode, PwNode newNode) {
        if (callbackRunnable != null) {
            callbackRunnable.setSuccess(success);
            callbackRunnable.setMessage(message);
            callbackRunnable.run(oldNode, newNode);
        }
    }
}
