package com.kunzisoft.keepass.activities.selection

import android.view.View
import android.widget.Toast
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.helpers.SpecialMode
import com.kunzisoft.keepass.activities.helpers.TypeMode
import com.kunzisoft.keepass.activities.stylish.StylishActivity
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.view.SpecialModeView

/**
 * Activity to manage special mode (ie: selection mode)
 */
abstract class SpecialModeActivity : StylishActivity() {

    protected var mSpecialMode: SpecialMode = SpecialMode.DEFAULT
    protected var mTypeMode: TypeMode = TypeMode.DEFAULT

    private var mSpecialModeView: SpecialModeView? = null

    open fun onCancelSpecialMode() {
        onBackPressed()
    }

    override fun onResume() {
        super.onResume()

        mSpecialMode = EntrySelectionHelper.retrieveSpecialModeFromIntent(intent)
        mTypeMode = EntrySelectionHelper.retrieveTypeModeFromIntent(intent)
        val searchInfo: SearchInfo? = EntrySelectionHelper.retrieveRegisterInfoFromIntent(intent)?.searchInfo
                ?: EntrySelectionHelper.retrieveSearchInfoFromIntent(intent)

        // To show the selection mode
        mSpecialModeView = findViewById(R.id.special_mode_view)
        mSpecialModeView?.apply {
            // Populate title
            val selectionModeStringId = when (mSpecialMode) {
                SpecialMode.DEFAULT, // Not important because hidden
                SpecialMode.SELECTION -> R.string.selection_mode
                SpecialMode.REGISTRATION -> R.string.registration_mode
            }
            val typeModeStringId = when (mTypeMode) {
                TypeMode.DEFAULT, // Not important because hidden
                TypeMode.MAGIKEYBOARD -> R.string.magic_keyboard_title
                TypeMode.AUTOFILL -> R.string.autofill
            }
            title = "${getString(selectionModeStringId)} (${getString(typeModeStringId)})"
            // Populate subtitle
            subtitle = searchInfo?.getName(resources)

            // Show the toolbar or not
            visible = when (mSpecialMode) {
                SpecialMode.DEFAULT -> false
                SpecialMode.SELECTION -> true
                SpecialMode.REGISTRATION -> true
            }

            // Add back listener
            onCancelButtonClickListener = View.OnClickListener {
                onCancelSpecialMode()
            }

            // Create menu
            menu.clear()
            if (mTypeMode == TypeMode.AUTOFILL) {
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

    private fun blockAutofill(searchInfo: SearchInfo?) {
        val webDomain = searchInfo?.webDomain
        val applicationId = searchInfo?.applicationId
        if (webDomain != null) {
            PreferencesUtil.addWebDomainToBlocklist(this,
                    webDomain)
        } else if (applicationId != null) {
            PreferencesUtil.addApplicationIdToBlocklist(this,
                    applicationId)
        }
        onCancelSpecialMode()
        Toast.makeText(this.applicationContext,
                R.string.autofill_block_restart,
                Toast.LENGTH_LONG).show()
    }
}