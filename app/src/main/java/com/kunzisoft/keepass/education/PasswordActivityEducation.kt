package com.kunzisoft.keepass.education

import android.app.Activity
import android.graphics.Color
import androidx.core.content.ContextCompat
import android.view.View
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import com.kunzisoft.keepass.R

class PasswordActivityEducation(activity: Activity)
    : Education(activity) {

    fun checkAndPerformedUnlockEducation(educationView: View,
                                         onEducationViewClick: ((TapTargetView?) -> Unit)? = null,
                                         onOuterViewClick: ((TapTargetView?) -> Unit)? = null): Boolean {
        return checkAndPerformedEducation(!isEducationUnlockPerformed(activity),
                TapTarget.forView(educationView,
                        activity.getString(R.string.education_unlock_title),
                        activity.getString(R.string.education_unlock_summary))
                        .icon(ContextCompat.getDrawable(activity, R.mipmap.ic_launcher_round))
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
                R.string.education_unlock_key)
    }

    fun checkAndPerformedReadOnlyEducation(educationView: View,
                                           onEducationViewClick: ((TapTargetView?) -> Unit)? = null,
                                           onOuterViewClick: ((TapTargetView?) -> Unit)? = null): Boolean {
        return checkAndPerformedEducation(!isEducationReadOnlyPerformed(activity),
                TapTarget.forView(educationView,
                        activity.getString(R.string.education_read_only_title),
                        activity.getString(R.string.education_read_only_summary))
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
                R.string.education_read_only_key)
    }

    fun checkAndPerformedBiometricEducation(educationView: View,
                                            onEducationViewClick: ((TapTargetView?) -> Unit)? = null,
                                            onOuterViewClick: ((TapTargetView?) -> Unit)? = null): Boolean {
        return checkAndPerformedEducation(!isEducationBiometricPerformed(activity),
                TapTarget.forView(educationView,
                        activity.getString(R.string.education_biometric_title),
                        activity.getString(R.string.education_biometric_summary))
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
                R.string.education_biometric_key)
    }
}