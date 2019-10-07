package com.kunzisoft.keepass.activities.helpers

import android.app.Activity
import android.app.assist.AssistStructure
import android.content.Intent

object EntrySelectionHelper {

    private const val EXTRA_ENTRY_SELECTION_MODE = "com.kunzisoft.keepass.extra.ENTRY_SELECTION_MODE"
    private const val DEFAULT_ENTRY_SELECTION_MODE = false

    fun startActivityForEntrySelection(activity: Activity, intent: Intent) {
        addEntrySelectionModeExtraInIntent(intent)
        // only to avoid visible flickering when redirecting
        activity.startActivity(intent)
    }

    fun addEntrySelectionModeExtraInIntent(intent: Intent) {
        intent.putExtra(EXTRA_ENTRY_SELECTION_MODE, true)
    }

    fun removeEntrySelectionModeFromIntent(intent: Intent) {
        intent.removeExtra(EXTRA_ENTRY_SELECTION_MODE)
    }

    fun retrieveEntrySelectionModeFromIntent(intent: Intent): Boolean {
        return intent.getBooleanExtra(EXTRA_ENTRY_SELECTION_MODE, DEFAULT_ENTRY_SELECTION_MODE)
    }

    fun doEntrySelectionAction(intent: Intent, standardAction: () -> Unit) {
                standardAction.invoke()
    }
}
