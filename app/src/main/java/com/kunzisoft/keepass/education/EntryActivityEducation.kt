package com.kunzisoft.keepass.education

import android.app.Activity
import android.graphics.Color
import android.view.View
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import com.kunzisoft.keepass.R


class EntryActivityEducation(activity: Activity)
    : Education(activity) {

    fun checkAndPerformedEntryCopyEducation(educationView: View,
                                            onEducationViewClick: ((TapTargetView?) -> Unit)? = null,
                                            onOuterViewClick: ((TapTargetView?) -> Unit)? = null): Boolean {
        return checkAndPerformedEducation(
                !isEducationCopyUsernamePerformed(activity),
                TapTarget.forView(educationView,
                        activity.getString(R.string.education_field_copy_title),
                        activity.getString(R.string.education_field_copy_summary))
                        .textColorInt(Color.WHITE)
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
                R.string.education_copy_username_key)
    }

    /**
     * Check and display learning views
     * Displays the explanation for copying a field and editing an entry
     */
    fun checkAndPerformedEntryEditEducation(educationView: View,
                                            onEducationViewClick: ((TapTargetView?) -> Unit)? = null,
                                            onOuterViewClick: ((TapTargetView?) -> Unit)? = null): Boolean {
        return checkAndPerformedEducation(
                !isEducationEntryEditPerformed(activity),
                TapTarget.forView(educationView,
                        activity.getString(R.string.education_entry_edit_title),
                        activity.getString(R.string.education_entry_edit_summary))
                        .textColorInt(Color.WHITE)
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
                R.string.education_entry_edit_key)
    }
}