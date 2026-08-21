package com.kunzisoft.keepass.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.util.Log
import android.view.Window
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.kunzisoft.encrypt.Signature.getAllFingerprints
import com.kunzisoft.keepass.BuildConfig
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.credentialprovider.passkey.data.AndroidPrivilegedApp
import com.kunzisoft.keepass.database.crypto.kdf.KdfLimits
import com.kunzisoft.keepass.database.crypto.kdf.Limits
import com.kunzisoft.keepass.education.Education
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.settings.PreferencesUtil
import java.security.SecureRandom
import kotlin.math.min

object AppUtil {

    private val secureRandom = SecureRandom()

    fun randomRequestCode(): Int {
        return secureRandom.nextInt(Int.MAX_VALUE)
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
        } catch (_: Exception) { }
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
    fun getInstalledBrowsersWithSignatures(
        context: Context,
        withGServices: Boolean = true
    ): List<AndroidPrivilegedApp> {
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

        // Add the Play Service if needed
        if (withGServices) {
            val gServices = BuildConfig.GOOGLE_PLAY_SERVICES_PACKAGE
            buildAndroidPrivilegedApp(packageManager, gServices)?.let { privilegedApp ->
                browserList.add(privilegedApp)
                processedPackageNames.add(gServices)
            }
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

    /**
     * Set the screenshot mode for the window
     * @param window The window to set the screenshot mode for
     * @param isEnabled True if the screenshot mode is enabled
     */
    fun setScreenshotMode(window: Window?, isEnabled: Boolean) {
        window?.let {
            if (isEnabled) {
                it.clearFlags(FLAG_SECURE)
            } else {
                it.setFlags(FLAG_SECURE, FLAG_SECURE)
            }
        }
    }

    /**
     * Indicates whether the [element] is allowed according to the [blockList]
     */
    fun isElementAllowed(element: String?, blockList: Set<String>?): Boolean {
        element?.let { elementNotNull ->
            if (blockList?.any { blocked ->
                    elementNotNull.contains(blocked)
                } == true
            ) {
                return false
            }
        }
        return true
    }

    /**
     * Create a new SearchInfo according to the browser package and blocklists
     */
    fun SearchInfo.withoutBrowserOrAppBlocked(context: Context): SearchInfo? {
        val searchInfo = SearchInfo(this)
        val applicationId = searchInfo.applicationId
        val isWebBrowser = if (!applicationId.isNullOrEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getInstalledBrowsersWithSignatures(context, withGServices = false).any {
                it.packageName == applicationId
            }
        } else false

        if (isWebBrowser
            || !isElementAllowed(
                applicationId,
                PreferencesUtil.applicationIdBlocklist(context))) {
            searchInfo.applicationId = null
        }
        if (!isElementAllowed(
                searchInfo.webDomain,
                PreferencesUtil.webDomainBlocklist(context))) {
            searchInfo.webDomain = null
        }
        return if (searchInfo.applicationId == null && searchInfo.webDomain == null) null
        else searchInfo
    }

    /**
     * Get a safe memory limit for the device in bytes.
     * The limit is calculated based on available RAM and a ratio.
     *
     * @param maxMemory Optional manual maximum memory limit.
     * @param maxRatio Ratio of available RAM to allow (0.0 to 1.0).
     * @return Safe memory limit in bytes.
     */
    fun Context.getSafeMemoryLimit(
        maxMemory: ULong? = null,
        maxRatio: Float = 1.0f,
    ): ULong {
        val memoryInfo = ActivityManager.MemoryInfo()
        (this.getSystemService(Context.ACTIVITY_SERVICE)
                as? ActivityManager)?.getMemoryInfo(memoryInfo)
        val availableMemory = memoryInfo.availMem
        // Limit KDF by ratio of available RAM to avoid OOM
        val deviceSafeLimit = (availableMemory * maxRatio.coerceIn(0.0f, 1.0f)).toULong()
        if (maxMemory == null)
            return deviceSafeLimit
        return min(maxMemory, deviceSafeLimit)
    }

    /**
     * Get a safe memory limit for the device in bytes based on an operation type.
     *
     * @param type The type of operation requiring memory (KDF, BINARY, etc.).
     * @return Safe memory limit in bytes.
     */
    fun Context.getSafeMemoryLimit(type: Limits.LimitOperationType): ULong {
        return this.getSafeMemoryLimit(type.maxMemory, type.ratioAvailableMemory)
    }

    /**
     * Get a safe parallelism limit based on available processors.
     *
     * @return Number of available processors.
     */
    fun getSafeParallelismLimit(): Long {
        return Runtime.getRuntime().availableProcessors().toLong()
    }

    /**
     * Determine if a memory allocation for a binary element is safe given current resources.
     *
     * @param memoryWanted Memory wanted in bytes.
     * @param maxMemory Manual memory limit.
     * @param maxRatio Ratio of memory deemed secure to the device's available memory.
     * @return true if memory can be allocated in RAM, false otherwise.
     */
    fun Context.isMemorySufficient(
        memoryWanted: ULong,
        maxMemory: ULong,
        maxRatio: Float = 0.5f,
    ): Boolean {
        if (memoryWanted > maxMemory)
            return false
        return memoryWanted <= getSafeMemoryLimit(maxMemory, maxRatio)
    }

    /**
     * Determine if a memory allocation for a binary element is safe given an operation type.
     *
     * @param memoryWanted Memory wanted in bytes.
     * @param operationType The type of operation requiring memory.
     * @return true if memory can be allocated in RAM, false otherwise.
     */
    fun Context.isMemorySufficient(
        memoryWanted: ULong,
        operationType: Limits.LimitOperationType,
    ): Boolean {
        return this.isMemorySufficient(
            memoryWanted,
            maxMemory = operationType.maxMemory,
            maxRatio = operationType.ratioAvailableMemory
        )
    }

    /**
     * Get hardware and resource limits for Key Derivation Function (KDF) operations.
     *
     * @return A [KdfLimits] object containing safe memory and parallelism values.
     */
    fun Context.getKdfLimits(): KdfLimits {
        return KdfLimits(
            memory = this.getSafeMemoryLimit(Limits.LimitOperationType.KDF),
            parallelism = getSafeParallelismLimit()
        )
    }

    /**
     * Get fresh local resource limits for memory operations.
     *
     * @return A [Limits] object containing memory check function and parallelism limit.
     */
    fun Context.getLimits(): Limits {
        return Limits(
            isMemorySufficient = { memoryWanted, type ->
                this.isMemorySufficient(memoryWanted, type)
            },
            parallelism = getSafeParallelismLimit()
        )
    }
}