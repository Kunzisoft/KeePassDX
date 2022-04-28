package com.kunzisoft.keepass.hardware

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.kunzisoft.keepass.database.element.Database

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

    fun buildHardwareKeyResponse(onChallengeResponded: ((challengeResponse:ByteArray?,
                                                         extra: Bundle?) -> Unit)?) {
        val resultCallback = ActivityResultCallback<ActivityResult> { result ->
            Log.d(TAG, "resultCode from ykdroid: " + result.resultCode)
            if (result.resultCode == Activity.RESULT_OK) {
                val challengeResponse: ByteArray? = result.data?.getByteArrayExtra("response")
                Log.d(TAG, "Response: " + challengeResponse.contentToString())
                challengeResponse?.let {
                    onChallengeResponded?.invoke(challengeResponse,
                        result.data?.getBundleExtra(EXTRA_BUNDLE_KEY))
                }
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

    fun launchChallengeForResponse(databaseUri: Uri, extra: Bundle? = null) {
        try {
            fragment?.context?.contentResolver ?: activity?.contentResolver ?.let { contentResolver ->
                Database.getTransformSeed(contentResolver, databaseUri) { seed ->
                    getChallengeResponseResultLauncher?.launch(Intent(YKDROID_CHALLENGE_RESPONSE_INTENT).apply {
                        putExtra(YKDROID_SEED_KEY, seed)
                        putExtra(EXTRA_BUNDLE_KEY, extra)
                    })
                    Log.d(TAG, "Challenge sent : " + seed.contentToString())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not launch challenge for response", e)
        }
    }

    companion object {
        private val TAG = HardwareKeyResponseHelper::class.java.simpleName

        private const val YKDROID_CHALLENGE_RESPONSE_INTENT = "net.pp3345.ykdroid.intent.action.CHALLENGE_RESPONSE"
        private const val YKDROID_SEED_KEY = "challenge"
        private const val EXTRA_BUNDLE_KEY = "EXTRA_BUNDLE_KEY"

    }
}