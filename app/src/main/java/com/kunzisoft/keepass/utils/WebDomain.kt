package com.kunzisoft.keepass.utils

import android.content.Context
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.settings.PreferencesUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.lib.publicsuffixlist.PublicSuffixList

object WebDomain {

    /**
     * Get the concrete web domain AKA without sub domain if needed
     */
    fun getConcreteWebDomain(context: Context,
                             webDomain: String?,
                             concreteWebDomain: (String?) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            if (webDomain != null) {
                // Warning, web domain can contains IP, don't crop in this case
                if (PreferencesUtil.searchSubdomains(context)
                    || Regex(SearchInfo.WEB_IP_REGEX).matches(webDomain)) {
                    concreteWebDomain.invoke(webDomain)
                } else {
                    val publicSuffixList = PublicSuffixList(context)
                    concreteWebDomain.invoke(publicSuffixList
                        .getPublicSuffixPlusOne(webDomain).await())
                }
            } else {
                concreteWebDomain.invoke(null)
            }
        }
    }
}