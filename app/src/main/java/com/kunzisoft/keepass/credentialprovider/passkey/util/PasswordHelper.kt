package com.kunzisoft.keepass.credentialprovider.passkey.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.credentials.provider.ProviderGetCredentialRequest
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.credentialprovider.passkey.util.PassHelper.getOrigin
import com.kunzisoft.keepass.model.AppOrigin
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.PasswordInfo
import com.kunzisoft.keepass.utils.getParcelableExtraCompat

/**
 * Utility class to manage the password elements,
 * allows to add and retrieve intent values with preconfigured keys,
 * and makes it easy to create creation and usage requests
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
object PasswordHelper {

    private const val EXTRA_PASSWORD_INFO = "com.kunzisoft.keepass.passkey.extra.passwordInfo"


    /**
     * Add the password info to the intent
     */
    fun Intent.addPasswordInfo(passwordInfo: PasswordInfo?) {
        passwordInfo?.let {
            putExtra(EXTRA_PASSWORD_INFO, passwordInfo)
        }
    }

    /**
     * Retrieve the password info from the intent
     */
    fun Intent.retrievePasswordInfo(): PasswordInfo? {
        return this.getParcelableExtraCompat(EXTRA_PASSWORD_INFO)
    }

    /**
     * Remove the password info from the intent
     */
    fun Intent.removePasswordInfo() {
        return this.removeExtra(EXTRA_PASSWORD_INFO)
    }

    /**
     * Build the Password response for one entry
     */
    fun Activity.buildPasswordResponseAndSetResult(
        entryInfo: EntryInfo,
        extras: Bundle? = null
    ) {
        try {
            val mReplyIntent = Intent()
            Log.d(javaClass.name, "Success Passkey manual selection")
            mReplyIntent.addPasswordInfo(
                PasswordInfo(
                    username = entryInfo.username,
                    password = entryInfo.password,
                    appOrigin = entryInfo.appOrigin
                )
            )
            extras?.let {
                mReplyIntent.putExtras(it)
            }
            setResult(Activity.RESULT_OK, mReplyIntent)
        } catch (e: Exception) {
            Log.e(javaClass.name, "Unable to add the password as result", e)
            Toast.makeText(
                this,
                getString(R.string.error_password_result),
                Toast.LENGTH_SHORT
            ).show()
            setResult(Activity.RESULT_CANCELED)
        }
    }

    /**
     * Utility method to create a password and the associated creation request parameters
     * [intent] allows to retrieve the request
     * [context] context to manage package verification files
     * [passwordCreated] is called asynchronously when the password has been created
     */
    suspend fun retrievePasswordCreationRequestParameters(
        intent: Intent,
        context: Context,
        passwordCreated: suspend (PasswordInfo) -> Unit
    ) {
        val createCredentialRequest =
            PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)
            ?: throw CreateCredentialUnknownException("could not retrieve request from intent")
        val callingAppInfo = createCredentialRequest.callingAppInfo
        val creationOptions = createCredentialRequest.retrievePasswordCreationComponent()

        getOrigin(
            callingAppInfo = callingAppInfo,
            context = context,
            onOriginRetrieved = { appInfoToStore ->
                passwordCreated.invoke(
                    PasswordInfo(
                        username = creationOptions.id,
                        password = creationOptions.password.toCharArray(),
                        appOrigin = appInfoToStore
                    )
                )
            }
        )
    }

    /**
     * Retrieve the [androidx.credentials.CreatePasswordRequest] from the intent
     */
    fun ProviderCreateCredentialRequest.retrievePasswordCreationComponent(): CreatePasswordRequest {
        val request = this
        if (request.callingRequest !is CreatePasswordRequest) {
            throw CreateCredentialUnknownException("callingRequest is of wrong type: ${request.callingRequest.type}")
        }
        return request.callingRequest as CreatePasswordRequest
    }

    /**
     * Retrieve the [androidx.credentials.GetPasswordOption] from the intent
     */
    fun ProviderGetCredentialRequest.retrievePasswordUsageComponent(): GetPasswordOption {
        val request = this
        if (request.credentialOptions.size != 1) {
            throw GetCredentialUnknownException("not exact one credentialOption")
        }
        if (request.credentialOptions[0] !is GetPasswordOption) {
            throw CreateCredentialUnknownException("credentialOptions is of wrong type: ${request.credentialOptions[0]}")
        }
        return request.credentialOptions[0] as GetPasswordOption
    }

    /**
     * Utility method to use a password and create the associated usage request parameters
     * [intent] allows to retrieve the request
     * [context] context to manage package verification files
     * [result] is called asynchronously after the creation of AppOrigin
     */
    suspend fun retrievePasswordUsageRequestParameters(
        intent: Intent,
        context: Context,
        result: suspend (appOrigin: AppOrigin) -> Unit
    ) {
        val getCredentialRequest =
            PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
                ?: throw CreateCredentialUnknownException("could not retrieve request from intent")
        val callingAppInfo = getCredentialRequest.callingAppInfo
        val credentialOption = getCredentialRequest.retrievePasswordUsageComponent()
        // TODO Get options
        getOrigin(
            callingAppInfo = callingAppInfo,
            context = context,
            onOriginRetrieved = { appOrigin ->
                result.invoke(appOrigin)
            }
        )
    }
}