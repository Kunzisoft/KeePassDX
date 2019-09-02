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
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import android.view.View
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.biometric.FingerPrintAnimatedVector
import com.kunzisoft.keepass.settings.SettingsAdvancedUnlockActivity

@RequiresApi(api = Build.VERSION_CODES.M)
class FingerPrintExplanationDialog : DialogFragment() {

    private var fingerPrintAnimatedVector: FingerPrintAnimatedVector? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            val builder = AlertDialog.Builder(activity)
            val inflater = activity.layoutInflater

            val rootView = inflater.inflate(R.layout.fragment_fingerprint_explanation, null)

            rootView.findViewById<View>(R.id.fingerprint_setting_way_text).setOnClickListener {
                startActivity(Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS))
            }

            rootView.findViewById<View>(R.id.auto_open_biometric_prompt_button).setOnClickListener {
                startActivity(Intent(activity, SettingsAdvancedUnlockActivity::class.java))
            }

            fingerPrintAnimatedVector = FingerPrintAnimatedVector(activity,
                    rootView.findViewById(R.id.biometric_image))

            builder.setView(rootView)
                    .setPositiveButton(android.R.string.ok) { _, _ -> }
            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        fingerPrintAnimatedVector?.startScan()
    }

    override fun onPause() {
        super.onPause()
        fingerPrintAnimatedVector?.stopScan()
    }
}
