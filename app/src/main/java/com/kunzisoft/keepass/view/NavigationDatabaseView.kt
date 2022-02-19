package com.kunzisoft.keepass.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.isVisible
import com.google.android.material.navigation.NavigationView
import com.kunzisoft.keepass.R

class NavigationDatabaseView @JvmOverloads constructor(context: Context,
                                                       attrs: AttributeSet? = null,
                                                       defStyle: Int = 0)
    : NavigationView(context, attrs, defStyle) {

    private var databaseNavContainerView: View? = null
    private var databaseNavIconView: ImageView? = null
    private var databaseNavModifiedView: ImageView? = null
    private var databaseNavColorView: ImageView? = null
    private var databaseNavNameView: TextView? = null
    private var databaseNavPathView: TextView? = null
    private var databaseNavVersionView: TextView? = null

    init {
        inflateHeaderView(R.layout.nav_header_database)
        databaseNavIconView = databaseNavContainerView?.findViewById(R.id.nav_database_icon)
        databaseNavModifiedView = databaseNavContainerView?.findViewById(R.id.nav_database_modified)
        databaseNavColorView = databaseNavContainerView?.findViewById(R.id.nav_database_color)
        databaseNavNameView = databaseNavContainerView?.findViewById(R.id.nav_database_name)
        databaseNavPathView = databaseNavContainerView?.findViewById(R.id.nav_database_path)
        databaseNavVersionView = databaseNavContainerView?.findViewById(R.id.nav_database_version)
    }

    override fun inflateHeaderView(res: Int): View {
        val headerView = super.inflateHeaderView(res)
        databaseNavContainerView = headerView
        return headerView
    }

    fun setDatabaseName(name: String) {
        databaseNavNameView?.text = name
    }

    fun setDatabasePath(path: String?) {
        if (path != null) {
            databaseNavPathView?.text = path
            databaseNavPathView?.visibility = View.VISIBLE
        } else {
            databaseNavPathView?.visibility = View.GONE
        }
    }

    fun setDatabaseVersion(version: String) {
        databaseNavVersionView?.text = version
    }

    fun setDatabaseModifiedSinceLastLoading(modified: Boolean) {
        databaseNavModifiedView?.isVisible = modified
    }

    fun setDatabaseColor(color: Int?) {
        if (color != null) {
            databaseNavColorView?.drawable?.colorFilter = BlendModeColorFilterCompat
                .createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_IN)
            databaseNavColorView?.visibility = View.VISIBLE
        } else {
            databaseNavColorView?.visibility = View.GONE
        }
    }
}