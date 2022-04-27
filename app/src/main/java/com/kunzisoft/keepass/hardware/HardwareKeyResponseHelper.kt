package com.kunzisoft.keepass.hardware

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDBX
import com.kunzisoft.keepass.utils.UriUtil
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream

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

    fun buildHardwareKeyResponse(onChallengeResponded: ((challengeResponse:ByteArray?) -> Unit)?) {
        val resultCallback = ActivityResultCallback<ActivityResult> { result ->
            Log.d(TAG, "resultCode from ykdroid: " + result.resultCode)
            if (result.resultCode == Activity.RESULT_OK) {
                val challengeResponse: ByteArray? = result.data?.getByteArrayExtra("response")
                Log.d(TAG, "Response: " + challengeResponse.contentToString())
                challengeResponse?.let {
                    onChallengeResponded?.invoke(challengeResponse)
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

    fun launchChallengeForResponse(databaseUri: Uri) {
        fragment?.context?.contentResolver ?: activity?.contentResolver ?.let { contentResolver ->
            getTransformSeedFromHeader(databaseUri, contentResolver)?.let { seed ->
                try {
                    getChallengeResponseResultLauncher?.launch(Intent(YKDROID_CHALLENGE_RESPONSE_INTENT).apply {
                        putExtra(YKDROID_SEED_KEY, seed)
                    })
                    Log.d(TAG, "Challenge sent : " + seed.contentToString())
                } catch (e: ActivityNotFoundException) {
                    // TODO better error
                    throw IOException("No activity to handle $YKDROID_CHALLENGE_RESPONSE_INTENT intent")
                }
            }
        }
    }

    private fun getTransformSeedFromHeader(uri: Uri, contentResolver: ContentResolver): ByteArray? {
        try {
            BufferedInputStream(UriUtil.getUriInputStream(contentResolver, uri)).use { databaseInputStream ->
                val header = DatabaseHeaderKDBX(DatabaseKDBX())
                header.loadFromFile(databaseInputStream)
                val challenge = ByteArray(64)
                header.transformSeed?.copyInto(challenge, 0, 0, 32)
                // seed: 32 byte transform seed, needs to be padded before sent to the hardware
                challenge.fill(32, 32, 64)
                return challenge
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not read transform seed from file", e)
        }
        return null
    }

    companion object {
        private val TAG = HardwareKeyResponseHelper::class.java.simpleName

        private const val YKDROID_CHALLENGE_RESPONSE_INTENT = "net.pp3345.ykdroid.intent.action.CHALLENGE_RESPONSE"
        private const val YKDROID_SEED_KEY = "challenge"

    }
}