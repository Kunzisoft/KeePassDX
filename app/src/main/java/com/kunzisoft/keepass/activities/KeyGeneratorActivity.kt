package com.kunzisoft.keepass.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.fragments.KeyGeneratorFragment
import com.kunzisoft.keepass.activities.legacy.DatabaseLockActivity
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.view.updateLockPaddingLeft
import com.kunzisoft.keepass.viewmodels.KeyGeneratorViewModel

class KeyGeneratorActivity : DatabaseLockActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var validationButton: View
    private var lockView: View? = null

    private val keyGeneratorViewModel: KeyGeneratorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_key_generator)

        toolbar = findViewById(R.id.toolbar)
        toolbar.title = " "
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        coordinatorLayout = findViewById(R.id.key_generator_coordinator)

        lockView = findViewById(R.id.lock_button)
        lockView?.setOnClickListener {
            lockAndExit()
        }

        validationButton = findViewById(R.id.key_generator_validation)
        validationButton.setOnClickListener {
            keyGeneratorViewModel.validateKeyGenerated()
        }

        supportFragmentManager.commit {
            replace(R.id.key_generator_fragment, KeyGeneratorFragment.getInstance(
                    // Default selection tab
                    KeyGeneratorFragment.KeyGeneratorTab.PASSWORD
                ), KEY_GENERATED_FRAGMENT_TAG
            )
        }

        keyGeneratorViewModel.keyGenerated.observe(this) { keyGenerated ->
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra(KEY_GENERATED, keyGenerated)
            })
            finish()
        }
    }

    override fun viewToInvalidateTimeout(): View? {
        return findViewById<ViewGroup>(R.id.key_generator_container)
    }

    override fun onResume() {
        super.onResume()

        // Show the lock button
        lockView?.visibility = if (PreferencesUtil.showLockDatabaseButton(this)) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // Padding if lock button visible
        toolbar.updateLockPaddingLeft()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.key_generator, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onDatabaseBackPressed()
            }
            R.id.menu_generate -> {
                keyGeneratorViewModel.requireKeyGeneration()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onDatabaseBackPressed() {
        setResult(Activity.RESULT_CANCELED, Intent())
        super.onDatabaseBackPressed()
    }

    companion object {
        private const val KEY_GENERATED = "KEY_GENERATED"
        private const val KEY_GENERATED_FRAGMENT_TAG = "KEY_GENERATED_FRAGMENT_TAG"

        fun registerForGeneratedKeyResult(activity: FragmentActivity,
                                          keyGeneratedListener: (String?) -> Unit): ActivityResultLauncher<Intent> {
            return activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    keyGeneratedListener.invoke(
                        result.data?.getStringExtra(KEY_GENERATED)
                    )
                } else {
                    keyGeneratedListener.invoke(null)
                }
            }
        }

        fun launch(context: FragmentActivity,
                   resultLauncher: ActivityResultLauncher<Intent>) {
            // Create an instance to return the picker icon
            resultLauncher.launch(
                Intent(context, KeyGeneratorActivity::class.java)
            )
        }
    }
}