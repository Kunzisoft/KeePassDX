package com.kunzisoft.keepass.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.kunzisoft.encrypt.Signature.getAllFingerprints
import com.kunzisoft.keepass.BuildConfig
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.credentialprovider.passkey.data.AndroidPrivilegedApp
import com.kunzisoft.keepass.education.Education

object AppUtil {

    fun randomRequestCode(): Int {
        return (Math.random() * Integer.MAX_VALUE).toInt()
    }

    fun Context.isExternalAppInstalled(packageName: String, showError: Boolean = true): Boolean {
        try {
            this.applicationContext.packageManager.getPackageInfoCompat(
                packageName,
                PackageManager.GET_ACTIVITIES
            )
            Education.setEducationScreenReclickedPerformed(this)
            return true
        } catch (e: Exception) {
            if (showError)
                Log.e(AppUtil::class.simpleName, "App not accessible", e)
        }
        return false
    }

    fun Context.openExternalApp(packageName: String, sourcesURL: String? = null) {
        var launchIntent: Intent? = null
        try {
            launchIntent = this.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } catch (ignored: Exception) { }
        try {
            if (launchIntent == null) {
                this.startActivity(
                    Intent(Intent.ACTION_VIEW)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .setData(
                            if (sourcesURL != null
                                && !BuildConfig.CLOSED_STORE
                            ) {
                                sourcesURL
                            } else {
                                this.getString(
                                    if (BuildConfig.CLOSED_STORE)
                                        R.string.play_store_url
                                    else
                                        R.string.f_droid_url,
                                    packageName
                                )
                            }.toUri()
                        )
                )
            } else {
                this.startActivity(launchIntent)
            }
        } catch (e: Exception) {
            Log.e(AppUtil::class.simpleName, "App cannot be open", e)
        }
    }

    fun Context.isContributingUser(): Boolean {
        return (Education.isEducationScreenReclickedPerformed(this)
                || isExternalAppInstalled(this.getString(R.string.keepro_app_id), false)
                )
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun getInstalledBrowsersWithSignatures(context: Context): List<AndroidPrivilegedApp> {
        val packageManager = context.packageManager
        val browserList = mutableListOf<AndroidPrivilegedApp>()

        // Create a generic web intent
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = context.getString(R.string.homepage_url).toUri()

        // Query for apps that can handle this intent
        val resolveInfoList: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }

        val processedPackageNames = mutableSetOf<String>()
        for (resolveInfo in resolveInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            if (packageName != null && !processedPackageNames.contains(packageName)) {
                buildAndroidPrivilegedApp(packageManager, packageName)?.let { privilegedApp ->
                    browserList.add(privilegedApp)
                    processedPackageNames.add(packageName)
                }
            }
        }

        // Add the Play Service
        val gServices = "com.google.android.gms"
        buildAndroidPrivilegedApp(packageManager, gServices)?.let { privilegedApp ->
            browserList.add(privilegedApp)
            processedPackageNames.add(gServices)
        }

        return browserList.distinctBy { it.packageName } // Ensure uniqueness just in case
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun buildAndroidPrivilegedApp(
        packageManager: PackageManager,
        packageName: String
    ): AndroidPrivilegedApp? {
        return try {
            val packageInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
            val signatureFingerprints = packageInfo.signingInfo?.getAllFingerprints()
            signatureFingerprints?.let {
                 AndroidPrivilegedApp(packageName, signatureFingerprints)
            }
        } catch (e: Exception) {
            Log.e(AppUtil::class.simpleName, "Error processing package: $packageName", e)
            null
        }
    }
}