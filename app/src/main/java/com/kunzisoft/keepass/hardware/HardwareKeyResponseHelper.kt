package com.kunzisoft.keepass.hardware

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.utils.UriUtil.openExternalApp
import kotlinx.coroutines.launch

class HardwareKeyResponseHelper {

    private var activity: FragmentActivity? = null
    private var fragment: Fragment? = null

    private var getChallengeResponseResultLauncher: ActivityResultLauncher<Intent>? = null

    constructor(context: FragmentActivity) {
        this.activity = context
        this.fragment = null
    }

    constructor(context: Fragment) {
        this.activity = context.activity
        this.fragment = context
    }

    fun buildHardwareKeyResponse(onChallengeResponded: (challengeResponse: ByteArray?,
                                                        extra: Bundle?) -> Unit) {
        val resultCallback = ActivityResultCallback<ActivityResult> { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val challengeResponse: ByteArray? = result.data?.getByteArrayExtra(HARDWARE_KEY_RESPONSE_KEY)
                Log.d(TAG, "Response form challenge")
                onChallengeResponded.invoke(challengeResponse,
                    result.data?.getBundleExtra(EXTRA_BUNDLE_KEY))
            } else {
                Log.e(TAG, "Response from challenge error")
                onChallengeResponded.invoke(null,
                    result.data?.getBundleExtra(EXTRA_BUNDLE_KEY))
            }
        }

        getChallengeResponseResultLauncher = if (fragment != null) {
            fragment?.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
                resultCallback
            )
        } else {
            activity?.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
                resultCallback
            )
        }
    }

    fun launchChallengeForResponse(hardwareKey: HardwareKey, seed: ByteArray?) {
        when (hardwareKey) {
            /*
            HardwareKey.FIDO2_SECRET -> {
                // TODO FIDO2 under development
                throw Exception("FIDO2 not implemented")
            }
            */
            HardwareKey.CHALLENGE_RESPONSE_YUBIKEY -> {
                // Transform the seed before sending
                var challenge: ByteArray? = null
                if (seed != null) {
                    challenge = ByteArray(64)
                    seed.copyInto(challenge, 0, 0, 32)
                    challenge.fill(32, 32, 64)
                }
                // Send to the driver
                getChallengeResponseResultLauncher!!.launch(
                    Intent(YUBIKEY_CHALLENGE_RESPONSE_INTENT).apply {
                        putExtra(HARDWARE_KEY_CHALLENGE_KEY, challenge)
                    }
                )
                Log.d(TAG, "Challenge sent")
            }
        }
    }

    companion object {
        private val TAG = HardwareKeyResponseHelper::class.java.simpleName

        private const val YUBIKEY_CHALLENGE_RESPONSE_INTENT = "android.yubikey.intent.action.CHALLENGE_RESPONSE"
        private const val HARDWARE_KEY_CHALLENGE_KEY = "challenge"
        private const val HARDWARE_KEY_RESPONSE_KEY = "response"
        private const val EXTRA_BUNDLE_KEY = "EXTRA_BUNDLE_KEY"

        fun isHardwareKeyAvailable(
            activity: FragmentActivity,
            hardwareKey: HardwareKey,
            showDialog: Boolean = true
        ): Boolean {
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
                            .resolveActivity(activity.packageManager) != null
                    if (showDialog && !yubikeyDriverAvailable)
                        showHardwareKeyDriverNeeded(activity, hardwareKey)
                    yubikeyDriverAvailable
                }
            }
        }

        private fun showHardwareKeyDriverNeeded(
            activity: FragmentActivity,
            hardwareKey: HardwareKey
        ) {
            activity.lifecycleScope.launch {
                val builder = AlertDialog.Builder(activity)
                builder
                    .setMessage(
                        activity.getString(R.string.error_driver_required, hardwareKey.toString())
                    )
                    .setPositiveButton(R.string.download) { _, _ ->
                        activity.openExternalApp(activity.getString(R.string.key_driver_app_id))
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                builder.create().show()
            }
        }
    }
}
