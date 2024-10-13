package com.kunzisoft.keepass.credentialprovider.util

import android.content.res.AssetManager
import androidx.credentials.provider.CallingAppInfo

class OriginHelper {

    companion object {

        const val DEFAULT_PROTOCOL = "https://"

        fun getWebOrigin(callingAppInfo: CallingAppInfo, assets: AssetManager): String? {
            val privilegedAllowlist = assets.open("trustedPackages.json").bufferedReader().use {
                it.readText()
            }
            // for trusted browsers like Chrome and Firefox
            val origin = callingAppInfo.getOrigin(privilegedAllowlist)?.removeSuffix("/")
            return origin
        }

    }
}