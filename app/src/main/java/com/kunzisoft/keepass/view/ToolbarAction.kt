package com.kunzisoft.keepass.view

import android.content.Context
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.appcompat.view.ActionMode
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.widget.Toolbar
import com.kunzisoft.keepass.R

class ToolbarAction @JvmOverloads constructor(context: Context,
                                              attrs: AttributeSet? = null,
                                              defStyle: Int = androidx.appcompat.R.attr.toolbarStyle)
    : Toolbar(context, attrs, defStyle) {

    private var mActionModeCallback: ActionMode.Callback? = null
    private val actionMode = NodeActionMode(this)

    init {
        visibility = View.GONE
    }

    fun startSupportActionMode(actionModeCallback: ActionMode.Callback): ActionMode {
        mActionModeCallback?.onDestroyActionMode(actionMode)
        mActionModeCallback = actionModeCallback
        mActionModeCallback?.onCreateActionMode(actionMode, menu)
        mActionModeCallback?.onPrepareActionMode(actionMode, menu)

        setOnMenuItemClickListener {
            mActionModeCallback?.onActionItemClicked(actionMode, it) ?: false
        }
        setNavigationOnClickListener{
            close()
        }

        setNavigationIcon(R.drawable.ic_close_white_24dp)

        open()

        return actionMode
    }

    fun invalidateMenu() {
        open()
        mActionModeCallback?.onPrepareActionMode(actionMode, menu)
    }

    fun open() {
        visibility = View.VISIBLE
        //expand()
    }

    fun close() {
        //collapse()
        visibility = View.GONE
        mActionModeCallback?.onDestroyActionMode(actionMode)
    }

    private class NodeActionMode(var toolbarAction: ToolbarAction): ActionMode() {

        override fun finish() {
            menu.clear()
            toolbarAction.close()
        }

        override fun getMenu(): Menu {
            return toolbarAction.menu
        }

        override fun getCustomView(): View {
            return toolbarAction
        }

        override fun setCustomView(view: View?) {}

        override fun getMenuInflater(): MenuInflater {
            return SupportMenuInflater(toolbarAction.context)
        }

        override fun invalidate() {
            toolbarAction.invalidateMenu()
        }

        override fun getSubtitle(): CharSequence {
            return toolbarAction.subtitle
        }

        override fun setTitle(title: CharSequence?) {
            toolbarAction.title = title
        }

        override fun setTitle(resId: Int) {
            toolbarAction.setTitle(resId)
        }

        override fun getTitle(): CharSequence {
            return toolbarAction.title
        }

        override fun setSubtitle(subtitle: CharSequence?) {
            toolbarAction.subtitle = subtitle
        }

        override fun setSubtitle(resId: Int) {
            toolbarAction.setSubtitle(resId)
        }
    }
}