package com.kunzisoft.keepass.database.action;

import android.content.Context;

import com.kunzisoft.keepass.database.Database;

public abstract class ActionWithSaveDatabaseRunnable extends RunnableOnFinish {

    protected Context mContext;
    protected boolean mDontSave;
    protected Database mDatabase;

    public ActionWithSaveDatabaseRunnable(Context context, Database database, OnFinishRunnable finish, boolean dontSave) {
        super(finish);

        this.mDatabase = database;
        this.mContext = context;
        this.mDontSave = dontSave;
        this.mFinish = new AfterActionRunnable(finish);
    }

    @Override
    public void run() {
        // Commit to disk
        SaveDatabaseRunnable save = new SaveDatabaseRunnable(mContext, mDatabase, mFinish, mDontSave);
        save.run();
    }

    public void runWithoutSaveDatabase() {
        mFinish.run();
    }

    abstract protected void onFinish(boolean success, String message);

    private class AfterActionRunnable extends OnFinishRunnable {

        AfterActionRunnable(OnFinishRunnable finish) {
            super(finish);
        }

        @Override
        public void run() {
            onFinish(mSuccess, mMessage);
            super.run();
        }
    }
}
