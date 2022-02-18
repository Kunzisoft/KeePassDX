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
import android.view.View
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import com.kunzisoft.keepass.R

class GroupActivityEducation(activity: Activity)
    : Education(activity) {

    fun checkAndPerformedAddNodeButtonEducation(educationView: View,
                                                onEducationViewClick: ((TapTargetView?) -> Unit)? = null,
                                                onOuterViewClick: ((TapTargetView?) -> Unit)? = null): Boolean {
        return checkAndPerformedEducation(isEducationNewNodePerformed(activity),
                TapTarget.forView(educationView,
                        activity.getString(R.string.education_new_node_title),
                        activity.getString(R.string.education_new_node_summary))
                        .outerCircleColorInt(getCircleColor())
                        .outerCircleAlpha(getCircleAlpha())
                        .textColorInt(getTextColor())
                        .tintTarget(false)
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
                R.string.education_new_node_key)
    }

    fun checkAndPerformedSearchMenuEducation(educationView: View,
                                             onEducationViewClick: ((TapTargetView?) -> Unit)? = null,
                                             onOuterViewClick: ((TapTargetView?) -> Unit)? = null): Boolean {
        return checkAndPerformedEducation(isEducationSearchPerformed(activity),
                TapTarget.forView(educationView,
                        activity.getString(R.string.education_search_title),
                        activity.getString(R.string.education_search_summary))
                        .outerCircleColorInt(getCircleColor())
                        .outerCircleAlpha(getCircleAlpha())
                        .textColorInt(getTextColor())
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
                R.string.education_search_key)
    }

    fun checkAndPerformedSortMenuEducation(educationView: View,
                                           onEducationViewClick: ((TapTargetView?) -> Unit)? = null,
                                           onOuterViewClick: ((TapTargetView?) -> Unit)? = null): Boolean {
        return checkAndPerformedEducation(isEducationSortPerformed(activity),
                TapTarget.forView(educationView,
                        activity.getString(R.string.education_sort_title),
                        activity.getString(R.string.education_sort_summary))
                        .outerCircleColorInt(getCircleColor())
                        .outerCircleAlpha(getCircleAlpha())
                        .textColorInt(getTextColor())
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
                R.string.education_sort_key)
    }

    fun checkAndPerformedLockMenuEducation(educationView: View,
                                           onEducationViewClick: ((TapTargetView?) -> Unit)? = null,
                                           onOuterViewClick: ((TapTargetView?) -> Unit)? = null): Boolean {
        return checkAndPerformedEducation(isEducationLockPerformed(activity),
                TapTarget.forView(educationView,
                        activity.getString(R.string.education_lock_title),
                        activity.getString(R.string.education_lock_summary))
                        .outerCircleColorInt(getCircleColor())
                        .outerCircleAlpha(getCircleAlpha())
                        .textColorInt(getTextColor())
                        .tintTarget(false)
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
                R.string.education_lock_key)
    }
}