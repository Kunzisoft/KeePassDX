/*
 * Copyright 2020 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.os.Bundle
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.model.SearchInfo

class AutofillBlocklistWebDomainPreferenceDialogFragmentCompat
    : AutofillBlocklistPreferenceDialogFragmentCompat() {

    override fun buildSearchInfoFromString(searchInfoString: String): SearchInfo? {
        val newSearchInfo = searchInfoString
                // remove prefix https://
                .replace(Regex("^.*://"), "")
                // Remove suffix /login...
                .replace(Regex("/.*$"), "")
        return SearchInfo().apply { webDomain = newSearchInfo }
    }

    override fun getDefaultValues(): Set<String> {
        return context?.resources
                ?.getStringArray(R.array.autofill_web_domain_blocklist_default)
                ?.toMutableSet()
                ?: emptySet()
    }

    companion object {
        fun newInstance(key: String): AutofillBlocklistWebDomainPreferenceDialogFragmentCompat {
            val fragment = AutofillBlocklistWebDomainPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
