package com.kunzisoft.keepass.hardware

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

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
            Log.d(TAG, "resultCode from ykdroid: " + result.resultCode)
            if (result.resultCode == Activity.RESULT_OK) {
                val challengeResponse: ByteArray? = result.data?.getByteArrayExtra("response")
                Log.d(TAG, "Response: " + challengeResponse.contentToString())
                onChallengeResponded.invoke(challengeResponse,
                    result.data?.getBundleExtra(EXTRA_BUNDLE_KEY))
            } else {
                Log.e(TAG, "Response error")
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

    fun launchChallengeForResponse(hardwareKey: HardwareKey?, seed: ByteArray?) {
        try {
            when (hardwareKey) {
                HardwareKey.CHALLENGE_RESPONSE_YUBIKEY -> {
                    // Transform the seed before sending
                    var challenge: ByteArray? = null
                    if (seed != null) {
                        challenge = ByteArray(64)
                        seed.copyInto(challenge, 0, 0, 32)
                        challenge.fill(32, 32, 64)
                    }
                    // Send to the driver
                    getChallengeResponseResultLauncher?.launch(Intent(YKDROID_CHALLENGE_RESPONSE_INTENT).apply {
                        putExtra(YKDROID_SEED_KEY, challenge)
                    })
                    Log.d(TAG, "Challenge sent : " + challenge.contentToString())
                }
                else -> {
                    // TODO other algorithm
                }
            }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Unable to retrieve the challenge response",
                e
            )
            e.message?.let { message ->
                Toast.makeText(
                    activity,
                    message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    companion object {
        private val TAG = HardwareKeyResponseHelper::class.java.simpleName

        private const val YKDROID_CHALLENGE_RESPONSE_INTENT = "net.pp3345.ykdroid.intent.action.CHALLENGE_RESPONSE"
        private const val YKDROID_SEED_KEY = "challenge"
        private const val EXTRA_BUNDLE_KEY = "EXTRA_BUNDLE_KEY"

    }
}