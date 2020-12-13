package com.kunzisoft.keepass.biometric

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.fragment.app.FragmentActivity
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.stylish.StylishFragment
import com.kunzisoft.keepass.settings.PreferencesUtil

class AdvancedUnlockFragment: StylishFragment() {

    private var advancedUnlockManager: AdvancedUnlockManager? = null

    private var advancedUnlockEnabled = false

    override fun onAttach(context: Context) {
        super.onAttach(context)

        advancedUnlockEnabled = PreferencesUtil.isAdvancedUnlockEnable(context)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (advancedUnlockEnabled) {
                    advancedUnlockManager?.builderListener = context as AdvancedUnlockManager.BuilderListener
                }
            }
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString()
                    + " must implement " + AdvancedUnlockManager.BuilderListener::class.java.name)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (advancedUnlockEnabled) {
                if (advancedUnlockManager == null) {
                    advancedUnlockManager = AdvancedUnlockManager { context as FragmentActivity }
                } else {
                    advancedUnlockManager?.retrieveContext = { context as FragmentActivity }
                }
            }
        }

        retainInstance = true
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val rootView = inflater.cloneInContext(contextThemed)
                .inflate(R.layout.fragment_advanced_unlock, container, false)

        advancedUnlockManager?.advancedUnlockInfoView = rootView.findViewById(R.id.advanced_unlock_view)

        return rootView
    }

    private data class ActivityResult(var requestCode: Int, var resultCode: Int, var data: Intent?)
    private var activityResult: ActivityResult? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // To wait resume
        activityResult = ActivityResult(requestCode, resultCode, data)

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onResume() {
        super.onResume()
        advancedUnlockEnabled = PreferencesUtil.isAdvancedUnlockEnable(requireContext())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // biometric menu
            advancedUnlockManager?.inflateOptionsMenu(inflater, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_keystore_remove_key -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                advancedUnlockManager?.deleteEncryptedDatabaseKey()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    fun loadDatabase(databaseUri: Uri?, autoOpenPrompt: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // To get device credential unlock result, only if same database uri
            activityResult?.let {
                if (databaseUri != null && advancedUnlockManager?.databaseFileUri == databaseUri) {
                    advancedUnlockManager?.onActivityResult(it.requestCode, it.resultCode, it.data)
                }
            } ?: run {
                if (databaseUri != null && advancedUnlockEnabled) {
                    advancedUnlockManager?.connect(databaseUri)
                    advancedUnlockManager?.autoOpenPrompt = autoOpenPrompt
                } else {
                    advancedUnlockManager?.disconnect()
                }
            }
            activityResult = null
        }
    }

    /**
     * Check unlock availability and change the current mode depending of device's state
     */
    fun checkUnlockAvailability() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            advancedUnlockManager?.checkUnlockAvailability()
        }
    }

    fun initAdvancedUnlockMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (advancedUnlockEnabled) {
                advancedUnlockManager?.initAdvancedUnlockMode()
            }
        }
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            advancedUnlockManager?.disconnect()
            advancedUnlockManager?.builderListener = null
            advancedUnlockManager = null
        }

        super.onDestroy()
    }

    override fun onDetach() {
        advancedUnlockManager?.builderListener = null

        super.onDetach()
    }
}