package com.kunzisoft.keepass.activities.selection

import android.os.Build
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.stylish.StylishActivity
import com.kunzisoft.keepass.autofill.AutofillHelper

/**
 * Activity to manage special mode (ie: selection mode)
 */
abstract class SpecialModeActivity : StylishActivity() {

    protected var mSelectionMode: Boolean = false

    protected var mAutofillSelection: Boolean = false

    override fun onResume() {
        super.onResume()

        mSelectionMode = EntrySelectionHelper.retrieveEntrySelectionModeFromIntent(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAutofillSelection = AutofillHelper.retrieveAssistStructure(intent) != null
        }
    }
}