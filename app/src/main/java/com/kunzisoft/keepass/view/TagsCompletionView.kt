package com.kunzisoft.keepass.view

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.kunzisoft.keepass.R
import com.tokenautocomplete.TokenCompleteTextView


class TagsCompletionView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : TokenCompleteTextView<String>(context, attrs) {

    private val layoutInflater = context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE)
            as? LayoutInflater?

    override fun defaultObject(completionText: String): String {
        return completionText
    }

    override fun getViewForObject(obj: String): View? {
        val viewGroup = layoutInflater?.inflate(R.layout.item_tag_edit, parent as ViewGroup, false)
                as? ViewGroup?
        viewGroup?.findViewById<TextView>(R.id.tag_name)?.apply {
            text = obj
        }
        return viewGroup
    }
}