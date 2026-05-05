package com.kunzisoft.keepass.credentialprovider.passkey.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.credentials.provider.ProviderGetCredentialRequest
import com.kunzisoft.encrypt.Base64Helper
import com.kunzisoft.encrypt.Signature
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.addNodeId
import com.kunzisoft.keepass.credentialprovider.passkey.data.AuthenticatorAssertionResponse
import com.kunzisoft.keepass.credentialprovider.passkey.data.AuthenticatorAttestationResponse
import com.kunzisoft.keepass.credentialprovider.passkey.data.Cbor
import com.kunzisoft.keepass.credentialprovider.passkey.data.ClientDataBuildResponse
import com.kunzisoft.keepass.credentialprovider.passkey.data.ClientDataDefinedResponse
import com.kunzisoft.keepass.credentialprovider.passkey.data.ClientDataResponse
import com.kunzisoft.keepass.credentialprovider.passkey.data.FidoPublicKeyCredential
import com.kunzisoft.keepass.credentialprovider.passkey.data.PublicKeyCredentialCreationOptions
import com.kunzisoft.keepass.credentialprovider.passkey.data.PublicKeyCredentialCreationParameters
import com.kunzisoft.keepass.credentialprovider.passkey.data.PublicKeyCredentialRequestOptions
import com.kunzisoft.keepass.credentialprovider.passkey.data.PublicKeyCredentialUsageParameters
import com.kunzisoft.keepass.credentialprovider.passkey.util.PassHelper.addAppOrigin
import com.kunzisoft.keepass.model.AppOrigin
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.Passkey
import com.kunzisoft.keepass.utils.getParcelableExtraCompat
import java.io.IOException

/**
 * Utility class to manage the passkey elements,
 * allows to add and retrieve intent values with preconfigured keys,
 * and makes it easy to create creation and usage requests
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
object PasskeyHelper {

    private const val EXTRA_PASSKEY = "com.kunzisoft.keepass.passkey.extra.passkey"

    /**
     * Add the passkey to the intent
     */
    fun Intent.addPasskey(passkey: Passkey?) {
        passkey?.let {
            putExtra(EXTRA_PASSKEY, passkey)
        }
    }

    /**
     * Retrieve the passkey from the intent
     */
    fun Intent.retrievePasskey(): Passkey? {
        return this.getParcelableExtraCompat(EXTRA_PASSKEY)
    }

    /**
     * Remove the passkey from the intent
     */
    fun Intent.removePasskey() {
        return this.removeExtra(EXTRA_PASSKEY)
    }

    /**
     * Build the Passkey response for one entry
     */
    fun Activity.buildPasskeyResponseAndSetResult(
        entryInfo: EntryInfo,
        extras: Bundle? = null
    ) {
        try {
            entryInfo.passkey?.let { passkey ->
                val mReplyIntent = Intent()
                Log.d(javaClass.name, "Success Passkey manual selection")
                mReplyIntent.addPasskey(passkey)
                mReplyIntent.addAppOrigin(entryInfo.appOrigin)
                mReplyIntent.addNodeId(entryInfo.id)
                extras?.let {
                    mReplyIntent.putExtras(it)
                }
                setResult(Activity.RESULT_OK, mReplyIntent)
            } ?: run {
                throw IOException("No passkey found")
            }
        } catch (e: Exception) {
            Log.e(javaClass.name, "Unable to add the passkey as result", e)
            Toast.makeText(
                this,
                getString(R.string.error_passkey_result),
                Toast.LENGTH_SHORT
            ).show()
            setResult(Activity.RESULT_CANCELED)
        }
    }

    /**
     * Build the Passkey error response
     */
    fun Activity.buildPasskeyErrorAndSetResult(
        resources: Resources,
        relyingPartyId: String?,
        credentialIds: List<String>
    ) {
        val error = resources.getString(
            R.string.error_passkey_credential_id,
            relyingPartyId,
            credentialIds
        )
        Log.e(javaClass.name, error)
        Toast.makeText(
            this,
            error,
            Toast.LENGTH_SHORT
        ).show()
        setResult(Activity.RESULT_CANCELED)
    }

    /**
     * Retrieve the [com.kunzisoft.keepass.credentialprovider.passkey.data.PublicKeyCredentialCreationOptions] from the intent
     */
    private fun ProviderCreateCredentialRequest.retrievePasskeyCreationComponent(): PublicKeyCredentialCreationOptions {
        val request = this
        if (request.callingRequest !is CreatePublicKeyCredentialRequest) {
            throw CreateCredentialUnknownException("callingRequest is of wrong type: ${request.callingRequest.type}")
        }
        val createPublicKeyCredentialRequest = request.callingRequest as CreatePublicKeyCredentialRequest
        return PublicKeyCredentialCreationOptions(
            requestJson = createPublicKeyCredentialRequest.requestJson,
            clientDataHash = createPublicKeyCredentialRequest.clientDataHash
        )
    }

    /**
     * Retrieve the [androidx.credentials.GetPublicKeyCredentialOption] from the intent
     */
    fun ProviderGetCredentialRequest.retrievePasskeyUsageComponent(): GetPublicKeyCredentialOption {
        val request = this
        if (request.credentialOptions.size != 1) {
            throw GetCredentialUnknownException("not exact one credentialOption")
        }
        if (request.credentialOptions[0] !is GetPublicKeyCredentialOption) {
            throw CreateCredentialUnknownException("credentialOptions is of wrong type: ${request.credentialOptions[0]}")
        }
        return request.credentialOptions[0] as GetPublicKeyCredentialOption
    }

    /**
     * Utility method to create a passkey and the associated creation request parameters
     * [intent] allows to retrieve the request
     * [context] context to manage package verification files
     * [defaultBackupEligibility] the default backup eligibility to add the the passkey entry
     * [defaultBackupState] the default backup state to add the the passkey entry
     * [passkeyCreated] is called asynchronously when the passkey has been created
     */
    suspend fun retrievePasskeyCreationRequestParameters(
        intent: Intent,
        context: Context,
        defaultBackupEligibility: Boolean?,
        defaultBackupState: Boolean?,
        passkeyCreated: suspend (Passkey, AppOrigin?, PublicKeyCredentialCreationParameters) -> Unit
    ) {
        val createCredentialRequest =
            PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)
                ?: throw CreateCredentialUnknownException("could not retrieve request from intent")
        val callingAppInfo = createCredentialRequest.callingAppInfo
        val creationOptions = createCredentialRequest.retrievePasskeyCreationComponent()

        val relyingParty = creationOptions.relyingPartyEntity.id
        val username = creationOptions.userEntity.name
        val userHandle = creationOptions.userEntity.id
        val pubKeyCredParams = creationOptions.pubKeyCredParams
        val clientDataHash = creationOptions.clientDataHash

        val credentialId = PassHelper.generateCredentialId()

        val (keyPair, keyTypeId) = Signature.generateKeyPair(
            pubKeyCredParams.map { params -> params.alg }
        ) ?: throw CreateCredentialUnknownException("no known public key type found")
        val privateKeyPem = Signature.convertPrivateKeyToPem(keyPair.private)

        // Create the passkey element
        val passkey = Passkey(
            username = username,
            privateKeyPem = privateKeyPem,
            credentialId = Base64Helper.b64Encode(credentialId),
            userHandle = Base64Helper.b64Encode(userHandle),
            relyingParty = relyingParty,
            backupEligibility = defaultBackupEligibility,
            backupState = defaultBackupState
        )

        // create new entry in database
        PassHelper.getOrigin(
            providedClientDataHash = clientDataHash,
            callingAppInfo = callingAppInfo,
            context = context,
            onOriginRetrieved = { appInfoToStore, clientDataHash ->
                passkeyCreated.invoke(
                    passkey,
                    appInfoToStore,
                    PublicKeyCredentialCreationParameters(
                        publicKeyCredentialCreationOptions = creationOptions,
                        credentialId = credentialId,
                        signatureKey = Pair(keyPair, keyTypeId),
                        clientDataResponse = ClientDataDefinedResponse(clientDataHash)
                    )
                )
            },
            onOriginNotRetrieved = { appInfoToStore, origin ->
                passkeyCreated.invoke(
                    passkey,
                    appInfoToStore,
                    PublicKeyCredentialCreationParameters(
                        publicKeyCredentialCreationOptions = creationOptions,
                        credentialId = credentialId,
                        signatureKey = Pair(keyPair, keyTypeId),
                        clientDataResponse = ClientDataBuildResponse(
                            type = ClientDataBuildResponse.Type.CREATE,
                            challenge = creationOptions.challenge,
                            origin = origin
                        )
                    )
                )
            }
        )
    }

    /**
     * Build the passkey public key credential response,
     * by calling this method the user is always recognized as present and verified
     */
    fun buildCreatePublicKeyCredentialResponse(
        publicKeyCredentialCreationParameters: PublicKeyCredentialCreationParameters,
        userVerified: Boolean,
        backupEligibility: Boolean,
        backupState: Boolean
    ): CreatePublicKeyCredentialResponse {

        val keyPair = publicKeyCredentialCreationParameters.signatureKey.first
        val keyTypeId = publicKeyCredentialCreationParameters.signatureKey.second
        val responseJson = FidoPublicKeyCredential(
            id = Base64Helper.b64Encode(publicKeyCredentialCreationParameters.credentialId),
            response = AuthenticatorAttestationResponse(
                requestOptions = publicKeyCredentialCreationParameters.publicKeyCredentialCreationOptions,
                credentialId = publicKeyCredentialCreationParameters.credentialId,
                credentialPublicKey = Cbor().encode(
                    Signature.convertPublicKeyToMap(
                        publicKeyIn = keyPair.public,
                        keyTypeId = keyTypeId
                    ) ?: mapOf<Int, Any>()
                ),
                userPresent = true,
                userVerified = userVerified,
                backupEligibility = backupEligibility,
                backupState = backupState,
                publicKeyTypeId = keyTypeId,
                publicKeyCbor = Signature.convertPublicKey(keyPair.public, keyTypeId)!!,
                clientDataResponse = publicKeyCredentialCreationParameters.clientDataResponse
            ),
            authenticatorAttachment = "platform"
        ).json()
        // log only the length to prevent logging sensitive information
        Log.d(javaClass.simpleName, "Json response for key creation")
        return CreatePublicKeyCredentialResponse(responseJson)
    }

    /**
     * Utility method to use a passkey and create the associated usage request parameters
     * [intent] allows to retrieve the request
     * [context] context to manage package verification files
     * [result] is called asynchronously after the creation of PublicKeyCredentialUsageParameters, the origin associated with it may or may not be verified
     */
    suspend fun retrievePasskeyUsageRequestParameters(
        intent: Intent,
        context: Context,
        result: suspend (PublicKeyCredentialUsageParameters) -> Unit
    ) {
        val getCredentialRequest =
            PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
                ?: throw CreateCredentialUnknownException("could not retrieve request from intent")
        val callingAppInfo = getCredentialRequest.callingAppInfo
        val credentialOption = getCredentialRequest.retrievePasskeyUsageComponent()
        val clientDataHash = credentialOption.clientDataHash

        val requestOptions = PublicKeyCredentialRequestOptions(credentialOption.requestJson)

        PassHelper.getOrigin(
            providedClientDataHash = clientDataHash,
            callingAppInfo = callingAppInfo,
            context = context,
            onOriginRetrieved = { appOrigin, clientDataHash ->
                result.invoke(
                    PublicKeyCredentialUsageParameters(
                        publicKeyCredentialRequestOptions = requestOptions,
                        clientDataResponse = ClientDataDefinedResponse(clientDataHash),
                        appOrigin = appOrigin
                    )
                )
            },
            onOriginNotRetrieved = { appOrigin, androidOriginString ->
                // By default we crate an usage parameter with Android origin
                result.invoke(
                    PublicKeyCredentialUsageParameters(
                        publicKeyCredentialRequestOptions = requestOptions,
                        clientDataResponse = ClientDataBuildResponse(
                            type = ClientDataBuildResponse.Type.GET,
                            challenge = requestOptions.challenge,
                            origin = androidOriginString
                        ),
                        appOrigin = appOrigin
                    )
                )
            }
        )
    }

    /**
     * Build the passkey public key credential response,
     * by calling this method the user is always recognized as present and verified
     */
    fun buildPasskeyPublicKeyCredential(
        requestOptions: PublicKeyCredentialRequestOptions,
        clientDataResponse: ClientDataResponse,
        passkey: Passkey,
        userVerified: Boolean,
        defaultBackupEligibility: Boolean,
        defaultBackupState: Boolean
    ): PublicKeyCredential {
        val getCredentialResponse = FidoPublicKeyCredential(
            id = passkey.credentialId,
            response = AuthenticatorAssertionResponse(
                requestOptions = requestOptions,
                userPresent = true,
                userVerified = userVerified,
                backupEligibility = passkey.backupEligibility ?: defaultBackupEligibility,
                backupState = passkey.backupState ?: defaultBackupState,
                userHandle = passkey.userHandle,
                privateKey = passkey.privateKeyPem,
                clientDataResponse = clientDataResponse
            ),
            authenticatorAttachment = "platform"
        ).json()
        Log.d(javaClass.simpleName, "Json response for key usage")
        return PublicKeyCredential(getCredentialResponse)
    }


    /**
     * Verify that the application signature is contained in the [appOrigin]
     */
    fun getVerifiedGETClientDataResponse(
        usageParameters: PublicKeyCredentialUsageParameters,
        appOrigin: AppOrigin
    ): ClientDataResponse {
        val appToCheck = usageParameters.appOrigin
        return if (appToCheck.verified) {
            usageParameters.clientDataResponse
        } else {
            // Origin checked by Android app signature
            ClientDataBuildResponse(
                type = ClientDataBuildResponse.Type.GET,
                challenge = usageParameters.publicKeyCredentialRequestOptions.challenge,
                origin = appToCheck.checkAndroidOrigin(appOrigin)
            )
        }
    }
}