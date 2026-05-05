package com.kunzisoft.keepass.view

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.ViewCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.Tags
import com.tokenautocomplete.CharacterTokenizer
import com.tokenautocomplete.TokenCompleteTextView


class TagsCompletionView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : TokenCompleteTextView<String>(context, attrs) {

    @ColorInt
    private val mColorSecondary: Int
    private val layoutInflater = context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE)
            as? LayoutInflater?

    init {
        allowCollapse(false)
        setTokenizer(CharacterTokenizer(Tags.DELIMITERS, Tags.DELIMITER.toString()))
        setTokenClickStyle(TokenClickStyle.Delete)

        context.obtainStyledAttributes(intArrayOf(R.attr.colorSecondary)).also { taColorSecondary ->
            this.mColorSecondary = taColorSecondary.getColor(0, Color.GRAY)
        }.recycle()
    }

    override fun defaultObject(completionText: String): String {
        return completionText
    }

    override fun getViewForObject(obj: String): View? {
        val viewGroup = layoutInflater?.inflate(R.layout.item_tag_edit, parent as ViewGroup, false)
                as? ViewGroup?
        viewGroup?.findViewById<AppCompatTextView>(R.id.tag_name)?.apply {
            text = obj
            ViewCompat.setBackgroundTintList(this, ColorStateList.valueOf(mColorSecondary))
        }
        return viewGroup
    }

    override fun shouldIgnoreToken(token: String): Boolean {
        return objects.contains(token)
    }

    fun getTags(): Tags {
        performCompletion()
        val tags = Tags()
        objects.forEach { tag ->
            tags.put(tag)
        }
        return tags
    }
}