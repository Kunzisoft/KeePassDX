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

class EntryEditActivityEducation(activity: Activity)
    : Education(activity) {

    /**
     * Check and display learning views
     * Displays the explanation for the password generator
     */
    fun checkAndPerformedGeneratePasswordEducation(educationView: View,
                                                   onEducationViewClick: ((TapTargetView?) -> Unit)? = null,
                                                   onOuterViewClick: ((TapTargetView?) -> Unit)? = null): Boolean {
        return checkAndPerformedEducation(isEducationPasswordGeneratorPerformed(activity),
                TapTarget.forView(educationView,
                        activity.getString(R.string.education_generate_password_title),
                        activity.getString(R.string.education_generate_password_summary))
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
                R.string.education_password_generator_key)
    }

    /**
     * Check and display learning views
     * Displays the explanation to create a new field
     */
    fun checkAndPerformedEntryNewFieldEducation(educationView: View,
                                                onEducationViewClick: ((TapTargetView?) -> Unit)? = null,
                                                onOuterViewClick: ((TapTargetView?) -> Unit)? = null): Boolean {
        return checkAndPerformedEducation(isEducationEntryNewFieldPerformed(activity),
                TapTarget.forView(educationView,
                        activity.getString(R.string.education_entry_new_field_title),
                        activity.getString(R.string.education_entry_new_field_summary))
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
                R.string.education_entry_new_field_key)
    }

    /**
     * Check and display learning views
     * Displays the explanation for to upload attachment
     */
    fun checkAndPerformedAttachmentEducation(educationView: View,
                                             onEducationViewClick: ((TapTargetView?) -> Unit)? = null,
                                             onOuterViewClick: ((TapTargetView?) -> Unit)? = null): Boolean {
        return checkAndPerformedEducation(isEducationAddAttachmentPerformed(activity),
                TapTarget.forView(educationView,
                        activity.getString(R.string.education_add_attachment_title),
                        activity.getString(R.string.education_add_attachment_summary))
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
                R.string.education_add_attachment_key)
    }

    /**
     * Check and display learning views
     * Displays the explanation to setup OTP
     */
    fun checkAndPerformedSetUpOTPEducation(educationView: View,
                                           onEducationViewClick: ((TapTargetView?) -> Unit)? = null,
                                           onOuterViewClick: ((TapTargetView?) -> Unit)? = null): Boolean {
        return checkAndPerformedEducation(isEducationSetupOTPPerformed(activity),
                TapTarget.forView(educationView,
                                activity.getString(R.string.education_setup_OTP_title),
                                activity.getString(R.string.education_setup_OTP_summary))
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
                R.string.education_setup_OTP_key)
    }

    /**
     * Check and display learning views
     * Displays the explanation for the entry validation
     */
    fun checkAndPerformedValidateEntryEducation(educationView: View,
                                                onEducationViewClick: ((TapTargetView?) -> Unit)? = null,
                                                onOuterViewClick: ((TapTargetView?) -> Unit)? = null): Boolean {
        return checkAndPerformedEducation(isEducationValidateEntryPerformed(activity),
            TapTarget.forView(educationView,
                activity.getString(R.string.education_validate_entry_title),
                activity.getString(R.string.education_validate_entry_summary))
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
            R.string.education_validate_entry_key)
    }
}