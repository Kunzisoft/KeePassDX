package com.kunzisoft.keepass.education

import android.app.Activity
import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.view.View
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
        return checkAndPerformedEducation(!isEducationCreateDatabasePerformed(activity),
                TapTarget.forView(educationView,
                        activity.getString(R.string.education_create_database_title),
                        activity.getString(R.string.education_create_database_summary))
                        .icon(ContextCompat.getDrawable(activity, R.drawable.ic_database_plus_white_24dp))
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
                R.string.education_create_db_key)
    }

    /**
     * Check and display learning views
     * Displays the explanation for a database selection
     */
    fun checkAndPerformedSelectDatabaseEducation(educationView: View,
                                                 onEducationViewClick: ((TapTargetView?) -> Unit)? = null,
                                                 onOuterViewClick: ((TapTargetView?) -> Unit)? = null): Boolean {
        return checkAndPerformedEducation(!isEducationSelectDatabasePerformed(activity),
                TapTarget.forView(educationView,
                        activity.getString(R.string.education_select_database_title),
                        activity.getString(R.string.education_select_database_summary))
                        .icon(ContextCompat.getDrawable(activity, R.drawable.ic_folder_white_24dp))
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
                R.string.education_select_db_key)
    }


    fun checkAndPerformedOpenLinkDatabaseEducation(educationView: View,
                                                   onEducationViewClick: ((TapTargetView?) -> Unit)? = null,
                                                   onOuterViewClick: ((TapTargetView?) -> Unit)? = null): Boolean {
        return checkAndPerformedEducation(!isEducationOpenLinkDatabasePerformed(activity),
                TapTarget.forView(educationView,
                        activity.getString(R.string.education_open_link_database_title),
                        activity.getString(R.string.education_open_link_database_summary))
                        .icon(ContextCompat.getDrawable(activity, R.drawable.ic_link_white_24dp))
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
                R.string.education_open_link_db_key)
    }
}