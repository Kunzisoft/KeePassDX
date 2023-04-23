package com.kunzisoft.keepass.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class IconProviderData(
    val packageNames: List<String>,
    val hosts: List<String>,
) : Parcelable

fun EntryInfo.toIconProviderData(): IconProviderData {
    val packageNames = customFields
        .asSequence()
        .filter { it.name == "AndroidApp" || it.name.matches("AndroidApp_\\d+".toRegex()) }
        .map { it.protectedValue.stringValue }
        .filter(String::isNotBlank)

    val urls = sequenceOf(url) + customFields
        .asSequence()
        .filter { it.name.matches("URL_\\d+".toRegex()) }
        .map { it.protectedValue.stringValue }

    val hosts = urls
        .mapNotNull { url ->
            Uri.parse(url).host ?: Uri.parse("//$url").host
        }
        .filter(String::isNotBlank)

    return IconProviderData(
        packageNames = packageNames.toList(),
        hosts = hosts.toList(),
    )
}
