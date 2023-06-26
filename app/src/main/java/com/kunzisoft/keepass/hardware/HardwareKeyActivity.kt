package com.kunzisoft.keepass.hardware

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.legacy.DatabaseModeActivity
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.utils.UriUtil.openExternalApp

/**
 * Special activity to deal with hardware key drivers,
 * return the response to the database service once finished
 */
class HardwareKeyActivity: DatabaseModeActivity(){

    // To manage hardware key challenge response
    private val resultCallback = ActivityResultCallback<ActivityResult> { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val challengeResponse: ByteArray? = result.data?.getByteArrayExtra(HARDWARE_KEY_RESPONSE_KEY)
            Log.d(TAG, "Response form challenge")
            mDatabaseTaskProvider?.startChallengeResponded(challengeResponse ?: ByteArray(0))
        } else {
            Log.e(TAG, "Response from challenge error")
            mDatabaseTaskProvider?.startChallengeResponded(ByteArray(0))
        }
        finish()
    }

    private var activityResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        resultCallback
    )

    override fun applyCustomStyle(): Boolean {
        return false
    }

    override fun showDatabaseDialog(): Boolean {
        return false
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)

        val hardwareKey = HardwareKey.getHardwareKeyFromString(
            intent.getStringExtra(DATA_HARDWARE_KEY)
        )
        if (isHardwareKeyAvailable(this, hardwareKey, true) {
                mDatabaseTaskProvider?.startChallengeResponded(ByteArray(0))
            }) {
            when (hardwareKey) {
                /*
                HardwareKey.FIDO2_SECRET -> {
                    // TODO FIDO2 under development
                    throw Exception("FIDO2 not implemented")
                }
                */
                HardwareKey.CHALLENGE_RESPONSE_YUBIKEY -> {
                    launchYubikeyChallengeForResponse(intent.getByteArrayExtra(DATA_SEED))
                }
                else -> {
                    finish()
                }
            }
        }
    }

    private fun launchYubikeyChallengeForResponse(seed: ByteArray?) {
        // Transform the seed before sending
        var challenge: ByteArray? = null
        if (seed != null) {
            challenge = ByteArray(64)
            seed.copyInto(challenge, 0, 0, 32)
            challenge.fill(32, 32, 64)
        }
        // Send to the driver
        activityResultLauncher.launch(
            Intent(YUBIKEY_CHALLENGE_RESPONSE_INTENT).apply {
                putExtra(HARDWARE_KEY_CHALLENGE_KEY, challenge)
            }
        )
        Log.d(TAG, "Challenge sent")
    }

    companion object {
        private val TAG = HardwareKeyActivity::class.java.simpleName

        private const val DATA_HARDWARE_KEY = "DATA_HARDWARE_KEY"
        private const val DATA_SEED = "DATA_SEED"
        private const val YUBIKEY_CHALLENGE_RESPONSE_INTENT = "android.yubikey.intent.action.CHALLENGE_RESPONSE"
        private const val HARDWARE_KEY_CHALLENGE_KEY = "challenge"
        private const val HARDWARE_KEY_RESPONSE_KEY = "response"

        fun launchHardwareKeyActivity(
            context: Context,
            hardwareKey: HardwareKey,
            seed: ByteArray?
        ) {
            context.startActivity(Intent(context, HardwareKeyActivity::class.java).apply {
                flags = FLAG_ACTIVITY_NEW_TASK
                putExtra(DATA_HARDWARE_KEY, hardwareKey.value)
                putExtra(DATA_SEED, seed)
            })
        }

        fun isHardwareKeyAvailable(
            context: Context,
            hardwareKey: HardwareKey?,
            showDialog: Boolean = true,
            onDialogDismissed: DialogInterface.OnDismissListener? = null
        ): Boolean {
            if (hardwareKey == null)
                return false
            return when (hardwareKey) {
                /*
                HardwareKey.FIDO2_SECRET -> {
                    // TODO FIDO2 under development
                    if (showDialog)
                        UnderDevelopmentFeatureDialogFragment()
                            .show(activity.supportFragmentManager, "underDevFeatureDialog")
                    false
                }
                */
                HardwareKey.CHALLENGE_RESPONSE_YUBIKEY -> {
                    // Check available intent
                    val yubikeyDriverAvailable =
                        Intent(YUBIKEY_CHALLENGE_RESPONSE_INTENT)
                            .resolveActivity(context.packageManager) != null
                    if (showDialog && !yubikeyDriverAvailable
                        && context is Activity)
                        showHardwareKeyDriverNeeded(context, hardwareKey) {
                            onDialogDismissed?.onDismiss(it)
                            context.finish()
                        }
                    yubikeyDriverAvailable
                }
            }
        }

        private fun showHardwareKeyDriverNeeded(
            context: Context,
            hardwareKey: HardwareKey,
            onDialogDismissed: DialogInterface.OnDismissListener
        ) {
            val builder = AlertDialog.Builder(context)
            builder
                .setMessage(
                    context.getString(R.string.error_driver_required, hardwareKey.toString())
                )
                .setPositiveButton(R.string.download) { _, _ ->
                    context.openExternalApp(
                        context.getString(R.string.key_driver_app_id),
                        context.getString(R.string.key_driver_url)
                    )
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .setOnDismissListener(onDialogDismissed)
            builder.create().show()
        }
    }
}