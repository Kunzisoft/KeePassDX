package com.kunzisoft.keepass.activities.selection

import android.os.Build
import android.view.View
import android.widget.Toast
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.stylish.StylishActivity
import com.kunzisoft.keepass.autofill.AutofillHelper
import com.kunzisoft.keepass.model.SearchInfo
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

        val searchInfo: SearchInfo? = intent.getParcelableExtra(EntrySelectionHelper.KEY_SEARCH_INFO)

        // To show the selection mode
        specialModeView = findViewById(R.id.special_mode_view)
        specialModeView?.apply {
            // Populate title
            title = if (mAutofillSelection) {
                "${resources.getString(R.string.selection_mode)} (${getString(R.string.autofill)})"
            } else {
                getString(R.string.magic_keyboard_title)
            }
            // Populate subtitle
            subtitle = searchInfo?.getName(resources)

            // Show the toolbar or not
            visible = mSelectionMode

            // Add back listener
            onCancelButtonClickListener = View.OnClickListener {
                onCancelSpecialMode()
            }

            // Create menu
            menu.clear()
            if (mAutofillSelection) {
                menuInflater.inflate(R.menu.autofill, menu)
                setOnMenuItemClickListener {  menuItem ->
                    when (menuItem.itemId) {
                        R.id.menu_block_autofill -> {
                            blockAutofill(searchInfo)
                        }
                    }
                    true
                }
            }
        }
    }

    fun blockAutofill(searchInfo: SearchInfo?) {
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