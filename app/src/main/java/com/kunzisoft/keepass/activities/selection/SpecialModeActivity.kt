package com.kunzisoft.keepass.activities.selection

import android.os.Build
import android.view.View
import android.widget.Toast
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.stylish.StylishActivity
import com.kunzisoft.keepass.autofill.AutofillHelper
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.view.SpecialModeView

/**
 * Activity to manage special mode (ie: selection mode)
 */
abstract class SpecialModeActivity : StylishActivity() {

    protected var mSelectionMode: Boolean = false

    protected var mAutofillSelection: Boolean = false

    private var specialModeView: SpecialModeView? = null

    open fun onCancelSpecialMode() {
        onBackPressed()
    }

    override fun onResume() {
        super.onResume()

        mSelectionMode = EntrySelectionHelper.retrieveEntrySelectionModeFromIntent(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAutofillSelection = AutofillHelper.retrieveAssistStructure(intent) != null
        }

        // To show the selection mode
        specialModeView = findViewById(R.id.special_mode_view)
        specialModeView?.apply {
            searchInfo = intent.getParcelableExtra(EntrySelectionHelper.KEY_SEARCH_INFO)
            visible = mSelectionMode
            onCancelButtonClickListener = View.OnClickListener {
                onCancelSpecialMode()
            }
            menu.clear()
            if (mAutofillSelection) {
                menuInflater.inflate(R.menu.autofill, menu)
                setOnMenuItemClickListener {  menuItem ->
                    when (menuItem.itemId) {
                        R.id.menu_autofill -> {
                            val webDomain = searchInfo?.webDomain
                            val applicationId = searchInfo?.applicationId
                            if (webDomain != null) {
                                PreferencesUtil.addWebDomainToBlocklist(this@SpecialModeActivity,
                                        webDomain)
                            } else if (applicationId != null) {
                                PreferencesUtil.addApplicationIdToBlocklist(this@SpecialModeActivity,
                                        applicationId)
                            }
                            onCancelSpecialMode()
                            Toast.makeText(this@SpecialModeActivity,
                                    R.string.autofill_block_restart,
                                    Toast.LENGTH_LONG).show()
                        }
                    }
                    true
                }
            }
        }
    }
}