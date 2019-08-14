package com.kunzisoft.keepass.activities.helpers

import android.content.Context
import android.content.Intent
import android.os.Bundle

import com.kunzisoft.keepass.settings.PreferencesUtil

object ReadOnlyHelper {

    private const val READ_ONLY_KEY = "READ_ONLY_KEY"

    const val READ_ONLY_DEFAULT = false

    fun retrieveReadOnlyFromIntent(intent: Intent): Boolean {
        return intent.getBooleanExtra(READ_ONLY_KEY, READ_ONLY_DEFAULT)
    }

    fun retrieveReadOnlyFromInstanceStateOrPreference(context: Context, savedInstanceState: Bundle?): Boolean {
        return if (savedInstanceState != null && savedInstanceState.containsKey(READ_ONLY_KEY)) {
            savedInstanceState.getBoolean(READ_ONLY_KEY)
        } else {
            PreferencesUtil.enableReadOnlyDatabase(context)
        }
    }

    fun retrieveReadOnlyFromInstanceStateOrArguments(savedInstanceState: Bundle?, arguments: Bundle?): Boolean {
        var readOnly = READ_ONLY_DEFAULT
        if (savedInstanceState != null && savedInstanceState.containsKey(READ_ONLY_KEY)) {
            readOnly = savedInstanceState.getBoolean(READ_ONLY_KEY)
        } else if (arguments != null && arguments.containsKey(READ_ONLY_KEY)) {
            readOnly = arguments.getBoolean(READ_ONLY_KEY)
        }
        return readOnly
    }

    fun retrieveReadOnlyFromInstanceStateOrIntent(savedInstanceState: Bundle?, intent: Intent?): Boolean {
        var readOnly = READ_ONLY_DEFAULT
        if (savedInstanceState != null && savedInstanceState.containsKey(READ_ONLY_KEY)) {
            readOnly = savedInstanceState.getBoolean(READ_ONLY_KEY)
        } else {
            if (intent != null)
                readOnly = intent.getBooleanExtra(READ_ONLY_KEY, READ_ONLY_DEFAULT)
        }
        return readOnly
    }

    fun putReadOnlyInIntent(intent: Intent, readOnly: Boolean) {
        intent.putExtra(READ_ONLY_KEY, readOnly)
    }

    fun putReadOnlyInBundle(bundle: Bundle, readOnly: Boolean) {
        bundle.putBoolean(READ_ONLY_KEY, readOnly)
    }

    fun onSaveInstanceState(outState: Bundle, readOnly: Boolean) {
        outState.putBoolean(READ_ONLY_KEY, readOnly)
    }
}
