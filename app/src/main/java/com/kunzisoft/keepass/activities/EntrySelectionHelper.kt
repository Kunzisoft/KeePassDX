package com.kunzisoft.keepass.activities

import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Build
import com.kunzisoft.keepass.autofill.AutofillHelper

object EntrySelectionHelper {

    private const val EXTRA_ENTRY_SELECTION_MODE = "com.kunzisoft.keepass.extra.ENTRY_SELECTION_MODE"
    private const val DEFAULT_ENTRY_SELECTION_MODE = false

    fun addEntrySelectionModeExtraInIntent(intent: Intent) {
        intent.putExtra(EXTRA_ENTRY_SELECTION_MODE, true)
    }

    fun removeEntrySelectionModeFromIntent(intent: Intent) {
        intent.removeExtra(EXTRA_ENTRY_SELECTION_MODE)
    }

    fun retrieveEntrySelectionModeFromIntent(intent: Intent): Boolean {
        return intent.getBooleanExtra(EXTRA_ENTRY_SELECTION_MODE, DEFAULT_ENTRY_SELECTION_MODE)
    }

    fun doEntrySelectionAction(intent: Intent,
                               standardAction: () -> Unit,
                               keyboardAction: () -> Unit,
                               autofillAction: (assistStructure: AssistStructure) -> Unit) {
        var assistStructure: AssistStructure? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assistStructure = AutofillHelper.retrieveAssistStructure(intent)
            assistStructure?.let {
                autofillAction.invoke(assistStructure)
            }
        }
        if (assistStructure == null) {
            if (intent.getBooleanExtra(EXTRA_ENTRY_SELECTION_MODE, DEFAULT_ENTRY_SELECTION_MODE)) {
                intent.removeExtra(EXTRA_ENTRY_SELECTION_MODE)
                keyboardAction.invoke()
            } else {
                standardAction.invoke()
            }
        }
    }
}
