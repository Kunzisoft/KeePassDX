package com.kunzisoft.keepass.credentialprovider.viewmodel

import android.app.Activity.RESULT_OK
import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResult
import com.kunzisoft.keepass.credentialprovider.SpecialMode
import com.kunzisoft.keepass.credentialprovider.activity.HardwareKeyActivity.Companion.isHardwareKeyAvailable
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.hardware.ChallengeRequest
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.utils.getParcelableExtraCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HardwareKeyLauncherViewModel(application: Application): CredentialLauncherViewModel(application) {

    private val mUiState = MutableStateFlow<UIState>(UIState.Loading)
    val uiState: StateFlow<UIState> = mUiState

    override suspend fun launchAction(
        intent: Intent,
        specialMode: SpecialMode,
        database: ContextualDatabase?
    ) {
        val challengeRequest: ChallengeRequest? = intent.getChallengeRequest()
        challengeRequest?.hardwareKey?.let { hardwareKey ->
            if (isHardwareKeyAvailable(getApplication(), hardwareKey)) {
                launchChallengeForResponse(
                    challengeRequest = challengeRequest
                )
            } else {
                mUiState.value = UIState.ShowHardwareKeyDriverNeeded(hardwareKey)
            }
        } ?: UIState.OnChallengeResponded(null)
    }

    private fun launchChallengeForResponse(
        challengeRequest: ChallengeRequest
    ) {
        mUiState.value = UIState.LaunchChallengeActivityForResponse(challengeRequest)
        Log.d(TAG, "Challenge sent")
    }

    override fun manageSelectionResult(
        database: ContextualDatabase,
        activityResult: ActivityResult
    ) {
        super.manageSelectionResult(database, activityResult)

        if (activityResult.resultCode == RESULT_OK) {
            // check if request is the same
            val data = activityResult.data
            val challengeRequest = data?.getChallengeRequest()
            if (challengeRequest == null) {
                Log.e(TAG, "Response from challenge without request validation")
                mUiState.value = UIState.OnChallengeResponded(null)
                return
            } else {
                Log.d(TAG, "Response form challenge")
                activityResult.data?.getByteArrayExtra(RESPONSE_KEY)?.let { challengeResponse ->
                    val challengeResponseList = mutableListOf(challengeResponse)
                    when (challengeRequest.hardwareKey) {
                        HardwareKey.FIDO2_HMAC_SECRET -> {
                            val usedClientData = data.getByteArrayExtra(RESPONSE_CLIENT_DATA_KEY)
                            val signature = data.getByteArrayExtra(RESPONSE_SIGNATURE_KEY)
                            val credentialId = data.getByteArrayExtra(RESPONSE_CREDENTIAL_ID_KEY)
                            // TODO Add clientData to verify signature
                            if (verifySignature(usedClientData, usedClientData, signature)) {
                                // To get data from "response_x"
                                (2..4)
                                    .mapNotNull { i -> data.getByteArrayExtra(RESPONSE_X_KEY + i)
                                    }.let { challengeResponseList.addAll(it) }
                                mUiState.value = UIState.OnChallengeResponded(challengeResponseList)
                            } else {
                                // Response didn't correspond to the request
                                Log.e(TAG, "Response from challenge is not correctly signed")
                                mUiState.value = UIState.OnChallengeResponded(null)
                            }
                        }
                        HardwareKey.YUBIKEY_HMAC_SHA1 -> {
                            mUiState.value = UIState.OnChallengeResponded(challengeResponseList)
                        }
                    }
                } ?: run {
                    Log.e(TAG, "Response from challenge cannot be null")
                    mUiState.value = UIState.OnChallengeResponded(null)
                }
            }
        } else {
            Log.e(TAG, "Response from challenge error")
            mUiState.value = UIState.OnChallengeResponded(null)
        }
    }

    private fun verifySignature(
        clientData: ByteArray?,
        usedClientData: ByteArray?,
        signature: ByteArray?
    ): Boolean {
        // TODO implement signature verification
        return true
    }

    sealed class UIState {
        object Loading : UIState()
        data class ShowHardwareKeyDriverNeeded(
            val hardwareKey: HardwareKey?
        ): UIState()
        data class LaunchChallengeActivityForResponse(
            val challengeRequest: ChallengeRequest
        ): UIState()
        data class OnChallengeResponded(
            val response: List<ByteArray>?
        ): UIState()
    }

    companion object {
        private val TAG = HardwareKeyLauncherViewModel::class.java.name

        private const val CHALLENGE_REQUEST_KEY = "CHALLENGE_REQUEST_KEY"

        // Yubikey Driver call
        private const val YUBIKEY_CHALLENGE_RESPONSE_INTENT = "android.yubikey.intent.action.CHALLENGE_RESPONSE"
        private const val YUBIKEY_CHALLENGE_KEY = "challenge"

        // FIDO2 Hmac-secret driver call
        private const val HMAC_SECRET_CREATE_INTENT = "android.fido.intent.action.HMAC_SECRET_CREATE"
        private const val HMAC_SECRET_CHALLENGE_RESPONSE_INTENT = "android.fido.intent.action.HMAC_SECRET_CHALLENGE_RESPONSE"
        private const val HMAC_SECREY_RELYING_PARTY_ID_KEY = "rpId"
        private const val HMAC_SECREY_NUM_CREDENTIALS_KEY = "numCredentials"
        private const val HMAC_SECREY_CREDENTIAL_ID_KEY = "credential_0"
        private const val HMAC_SECREY_SALT_KEY = "salt"

        // Response
        private const val RESPONSE_CLIENT_DATA_KEY = "clientDataId"
        private const val RESPONSE_SIGNATURE_KEY = "signature"
        private const val RESPONSE_CREDENTIAL_ID_KEY = "credentialId"
        private const val RESPONSE_KEY = "response"
        private const val RESPONSE_X_KEY = "response_"

        fun isHardwareDriverAvailable(
            context: Context,
            hardwareKey: HardwareKey
        ): Boolean {
            return when (hardwareKey) {
                HardwareKey.FIDO2_HMAC_SECRET -> {
                    Intent(HMAC_SECRET_CREATE_INTENT)
                        .resolveActivity(context.packageManager) != null
                            &&
                            Intent(HMAC_SECRET_CHALLENGE_RESPONSE_INTENT)
                                .resolveActivity(context.packageManager) != null
                }
                HardwareKey.YUBIKEY_HMAC_SHA1 -> {
                    Intent(YUBIKEY_CHALLENGE_RESPONSE_INTENT)
                        .resolveActivity(context.packageManager) != null
                }
            }
        }

        fun buildSecretChallengeRequest(
            challengeRequest: ChallengeRequest
        ): Intent {
            return when (challengeRequest.hardwareKey) {
                HardwareKey.FIDO2_HMAC_SECRET -> {
                    if (challengeRequest.saveOperation) {
                        Intent(HMAC_SECRET_CREATE_INTENT).apply {
                            putExtra(HMAC_SECREY_RELYING_PARTY_ID_KEY, challengeRequest.relyingPartyId)
                        }
                    } else {
                        Intent(HMAC_SECRET_CHALLENGE_RESPONSE_INTENT).apply {
                            putExtra(HMAC_SECREY_RELYING_PARTY_ID_KEY, challengeRequest.relyingPartyId)
                            putExtra(HMAC_SECREY_NUM_CREDENTIALS_KEY, 1)
                            putExtra(HMAC_SECREY_CREDENTIAL_ID_KEY, challengeRequest.credentialId)
                            putExtra(HMAC_SECREY_SALT_KEY, challengeRequest.seed)
                        }
                    }
                }
                HardwareKey.YUBIKEY_HMAC_SHA1 -> {
                    // Transform the seed to 64 bytes Yubikey challenge before sending
                    var challenge: ByteArray? = null
                    challengeRequest.seed?.let { seed ->
                        challenge = ByteArray(64)
                        seed.copyInto(challenge, 0, 0, 32)
                        challenge.fill(32, 32, 64)
                    }
                    Intent(YUBIKEY_CHALLENGE_RESPONSE_INTENT).apply {
                        putExtra(YUBIKEY_CHALLENGE_KEY, challenge)
                    }
                }
            }
        }

        fun Intent.addChallengeRequest(
            challengeRequest: ChallengeRequest
        ) {
            putExtra(CHALLENGE_REQUEST_KEY, challengeRequest)
        }

        fun Intent.getChallengeRequest(): ChallengeRequest? {
            return getParcelableExtraCompat(CHALLENGE_REQUEST_KEY)
        }
    }
}