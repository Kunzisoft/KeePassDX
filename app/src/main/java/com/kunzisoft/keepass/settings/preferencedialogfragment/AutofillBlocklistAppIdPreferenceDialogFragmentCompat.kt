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
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.settings.PreferencesUtil

class AutofillBlocklistAppIdPreferenceDialogFragmentCompat
    : AutofillBlocklistPreferenceDialogFragmentCompat() {

    override fun buildSearchInfoFromString(searchInfoString: String): SearchInfo? {
        val newSearchInfo = searchInfoString
                // remove chars not allowed in application ID
                .replace(Regex("[^a-zA-Z0-9_.]+"), "")
        return SearchInfo().apply { this.applicationId = newSearchInfo }
    }

    override fun getDefaultValues(): Set<String> {
        return PreferencesUtil.getDefaultApplicationIdBlocklist(this.resources)
    }

    companion object {
        fun newInstance(key: String): AutofillBlocklistAppIdPreferenceDialogFragmentCompat {
            val fragment = AutofillBlocklistAppIdPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
