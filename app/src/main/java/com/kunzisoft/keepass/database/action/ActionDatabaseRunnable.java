package com.kunzisoft.keepass.database.action;

import android.content.Context;

import com.kunzisoft.keepass.database.Database;

public abstract class ActionDatabaseRunnable extends RunnableOnFinish {

    protected Database mDb;
    protected Context mContext;
    protected boolean mDontSave;

    public ActionDatabaseRunnable(Context context, Database db, OnFinishRunnable finish, boolean dontSave) {
        super(finish);

        this.mDb = db;
        this.mContext = context;
        this.mDontSave = dontSave;
        this.mFinish = new AfterActionRunnable(finish);
    }

    @Override
    public void run() {
        // Commit to disk
        SaveDatabaseRunnable save = new SaveDatabaseRunnable(mContext, mDb, mFinish, mDontSave);
        save.run();
    }

    abstract protected void onFinish(boolean success, String message);

    private class AfterActionRunnable extends OnFinishRunnable {

        AfterActionRunnable(OnFinishRunnable finish) {
            super(finish);
        }

        @Override
        public void run() {
            onFinish(mSuccess, mMessage);
        }
    }
}
