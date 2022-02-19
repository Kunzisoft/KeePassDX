/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.education

import android.app.Activity
import android.graphics.Color
import android.view.View
import androidx.core.content.ContextCompat
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import com.kunzisoft.keepass.R

class FileDatabaseSelectActivityEducation(activity: Activity)
    : Education(activity) {

    /**
     * Check and display learning views
     * Displays the explanation for a database creation then a database selection
     */
    fun checkAndPerformedCreateDatabaseEducation(educationView: View,
                                                 onEducationViewClick: ((TapTargetView?) -> Unit)? = null,
                                                 onOuterViewClick: ((TapTargetView?) -> Unit)? = null): Boolean {

        // Try to open the creation base education
        return checkAndPerformedEducation(isEducationCreateDatabasePerformed(activity),
                TapTarget.forView(educationView,
                        activity.getString(R.string.education_create_database_title),
                        activity.getString(R.string.education_create_database_summary))
                        .outerCircleColorInt(getCircleColor())
                        .outerCircleAlpha(getCircleAlpha())
                        .textColorInt(getTextColor())
                        .icon(ContextCompat.getDrawable(activity, R.drawable.ic_database_plus_white_24dp))
                        .tintTarget(true)
                        .cancelable(true),
                object : TapTargetView.Listener() {
                    override fun onTargetClick(view: TapTargetView) {
                        super.onTargetClick(view)
                        onEducationViewClick?.invoke(view)
                    }

                    override fun onOuterCircleClick(view: TapTargetView?) {
                        super.onOuterCircleClick(view)
                        view?.dismiss(false)
                        onOuterViewClick?.invoke(view)
                    }
                },
                R.string.education_create_db_key)
    }

    /**
     * Check and display learning views
     * Displays the explanation for a database selection
     */
    fun checkAndPerformedSelectDatabaseEducation(educationView: View,
                                                 onEducationViewClick: ((TapTargetView?) -> Unit)? = null,
                                                 onOuterViewClick: ((TapTargetView?) -> Unit)? = null): Boolean {
        return checkAndPerformedEducation(isEducationSelectDatabasePerformed(activity),
                TapTarget.forView(educationView,
                        activity.getString(R.string.education_select_database_title),
                        activity.getString(R.string.education_select_database_summary))
                        .outerCircleColorInt(getCircleColor())
                        .outerCircleAlpha(getCircleAlpha())
                        .textColorInt(getTextColor())
                        .icon(ContextCompat.getDrawable(activity, R.drawable.ic_folder_white_24dp))
                        .tintTarget(true)
                        .cancelable(true),
                object : TapTargetView.Listener() {
                    override fun onTargetClick(view: TapTargetView) {
                        super.onTargetClick(view)
                        onEducationViewClick?.invoke(view)
                    }

                    override fun onOuterCircleClick(view: TapTargetView?) {
                        super.onOuterCircleClick(view)
                        view?.dismiss(false)
                        onOuterViewClick?.invoke(view)
                    }
                },
                R.string.education_select_db_key)
    }
}