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
import com.kunzisoft.keepass.hardware.HardwareKey
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
        val hardwareKey = HardwareKey.Companion.getHardwareKeyFromString(
            intent.getStringExtra(DATA_HARDWARE_KEY)
        )
        if (isHardwareKeyAvailable(getApplication(), hardwareKey)) {
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
                    UIState.OnChallengeResponded(null)
                }
            }
        } else {
            mUiState.value = UIState.ShowHardwareKeyDriverNeeded(hardwareKey)
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
        mUiState.value = UIState.LaunchChallengeActivityForResponse(challenge)
        Log.d(TAG, "Challenge sent")
    }

    override fun manageSelectionResult(
        database: ContextualDatabase,
        activityResult: ActivityResult
    ) {
        super.manageSelectionResult(database, activityResult)

        if (activityResult.resultCode == RESULT_OK) {
            val challengeResponse: ByteArray? =
                activityResult.data?.getByteArrayExtra(HARDWARE_KEY_RESPONSE_KEY)
            Log.d(TAG, "Response form challenge")
            mUiState.value = UIState.OnChallengeResponded(challengeResponse)
        } else {
            Log.e(TAG, "Response from challenge error")
            mUiState.value = UIState.OnChallengeResponded(null)
        }
    }

    sealed class UIState {
        object Loading : UIState()
        data class ShowHardwareKeyDriverNeeded(
            val hardwareKey: HardwareKey?
        ): UIState()
        data class LaunchChallengeActivityForResponse(
            val challenge: ByteArray?,
        ): UIState() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as LaunchChallengeActivityForResponse

                return challenge.contentEquals(other.challenge)
            }

            override fun hashCode(): Int {
                return challenge?.contentHashCode() ?: 0
            }
        }
        data class OnChallengeResponded(
            val response: ByteArray?
        ): UIState() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as OnChallengeResponded

                return response.contentEquals(other.response)
            }

            override fun hashCode(): Int {
                return response?.contentHashCode() ?: 0
            }
        }
    }

    companion object {
        private val TAG = HardwareKeyLauncherViewModel::class.java.name

        private const val DATA_HARDWARE_KEY = "DATA_HARDWARE_KEY"
        private const val DATA_SEED = "DATA_SEED"

        // Driver call
        private const val YUBIKEY_CHALLENGE_RESPONSE_INTENT = "android.yubikey.intent.action.CHALLENGE_RESPONSE"
        private const val HARDWARE_KEY_CHALLENGE_KEY = "challenge"
        private const val HARDWARE_KEY_RESPONSE_KEY = "response"

        fun isYubikeyDriverAvailable(context: Context): Boolean {
            return Intent(YUBIKEY_CHALLENGE_RESPONSE_INTENT)
                .resolveActivity(context.packageManager) != null
        }

        fun buildHardwareKeyChallenge(challenge: ByteArray?): Intent {
            return Intent(YUBIKEY_CHALLENGE_RESPONSE_INTENT).apply {
                putExtra(HARDWARE_KEY_CHALLENGE_KEY, challenge)
            }
        }

        fun Intent.addHardwareKey(hardwareKey: HardwareKey) {
            putExtra(DATA_HARDWARE_KEY, hardwareKey.value)
        }

        fun Intent.addSeed(seed: ByteArray?) {
            putExtra(DATA_SEED, seed)
        }
    }
}