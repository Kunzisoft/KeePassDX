package com.kunzisoft.keepass.database.action

import androidx.fragment.app.FragmentActivity
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater

class ProgressDialogSaveDatabaseThread(activity: FragmentActivity,
                                       actionRunnable: (ProgressTaskUpdater?)-> ActionRunnable)
    : ProgressDialogThread(activity,
        actionRunnable,
        R.string.saving_database,
        null,
        R.string.do_not_kill_app)