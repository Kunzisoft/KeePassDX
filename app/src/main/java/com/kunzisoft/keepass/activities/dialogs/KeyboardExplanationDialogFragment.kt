/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import android.view.View
import com.kunzisoft.keepass.BuildConfig
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.utils.UriUtil

class KeyboardExplanationDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let {
            val builder = AlertDialog.Builder(activity!!)
            val inflater = activity!!.layoutInflater

            val rootView = inflater.inflate(R.layout.fragment_keyboard_explanation, null)

            rootView.findViewById<View>(R.id.keyboards_activate_device_setting_button)
                    .setOnClickListener { launchActivateKeyboardSetting() }

            val containerKeyboardSwitcher = rootView.findViewById<View>(R.id.container_keyboard_switcher)
            if (BuildConfig.CLOSED_STORE) {
                containerKeyboardSwitcher.setOnClickListener { UriUtil.gotoUrl(context!!, R.string.keyboard_switcher_play_store) }
            } else {
                containerKeyboardSwitcher.setOnClickListener { UriUtil.gotoUrl(context!!, R.string.keyboard_switcher_f_droid) }
            }

            builder.setView(rootView)
                    .setPositiveButton(android.R.string.ok) { _, _ -> }
            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    private fun launchActivateKeyboardSetting() {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}
