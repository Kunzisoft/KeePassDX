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
                // seed: 32 byte transform seed, needs to be padded before sent to the hardware
                val challenge = ByteArray(64)
                System.arraycopy(seed, 0, challenge, 0, 32)
                challenge.fill(32, 32, 64)
                val intent = Intent("net.pp3345.ykdroid.intent.action.CHALLENGE_RESPONSE")
                Log.d(TAG, "Challenge sent to yubikey: " + challenge.contentToString())
                intent.putExtra("challenge", challenge)
                try {
                    getChallengeResponseResultLauncher?.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    // TODO better error
                    throw IOException("No activity to handle CHALLENGE_RESPONSE intent")
                }
            }
        }
    }

    private fun getTransformSeedFromHeader(uri: Uri, contentResolver: ContentResolver): ByteArray? {
        // TODO better implementation
        var databaseInputStream: InputStream? = null
        var challenge: ByteArray? = null

        try {
            // Load Data, pass Uris as InputStreams
            val databaseStream = UriUtil.getUriInputStream(contentResolver, uri)
                ?: throw IOException("Database input stream cannot be retrieve")

            databaseInputStream = BufferedInputStream(databaseStream)
            if (!databaseInputStream.markSupported()) {
                throw IOException("Input stream does not support mark.")
            }

            // We'll end up reading 8 bytes to identify the header. Might as well use two extra.
            databaseInputStream.mark(10)

            // Return to the start
            databaseInputStream.reset()

            val header = DatabaseHeaderKDBX(DatabaseKDBX())

            header.loadFromFile(databaseInputStream)

            challenge = ByteArray(64)
            System.arraycopy(header.transformSeed, 0, challenge, 0, 32)
            challenge.fill(32, 32, 64)

        } catch (e: Exception) {
            Log.e(TAG, "Could not read transform seed from file")
        } finally {
            databaseInputStream?.close()
        }

        return challenge
    }

    companion object {
        private val TAG = HardwareKeyResponseHelper::class.java.simpleName
    }
}