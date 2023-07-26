package com.kunzisoft.keepass.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.template.Template
import com.kunzisoft.keepass.database.element.template.TemplateField
import com.kunzisoft.keepass.database.helper.getLocalizedName
import com.kunzisoft.keepass.icons.IconDrawableFactory


class TemplatesSelectorAdapter(
    private val context: Context,
    private var templates: List<Template>): BaseAdapter() {

    var iconDrawableFactory: IconDrawableFactory? = null
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var mTextColor = Color.BLACK

    init {
        val taTextColor = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.textColor))
        mTextColor = taTextColor.getColor(0, Color.BLACK)
        taTextColor.recycle()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val template: Template = getItem(position)

        val holder: TemplateSelectorViewHolder
        var templateView = convertView
        if (templateView == null) {
            holder = TemplateSelectorViewHolder()
            templateView = inflater
                .cloneInContext(context)
                .inflate(R.layout.item_template, parent, false)
            holder.background = templateView?.findViewById(R.id.template_background)
            holder.icon = templateView?.findViewById(R.id.template_image)
            holder.name = templateView?.findViewById(R.id.template_name)
            templateView?.tag = holder
        } else {
            holder = templateView.tag as TemplateSelectorViewHolder
        }

        holder.background?.setBackgroundColor(template.backgroundColor ?: Color.TRANSPARENT)
        val textColor = template.foregroundColor ?: mTextColor
        holder.icon?.let { icon ->
            iconDrawableFactory?.assignDatabaseIcon(icon, template.icon, textColor)
        }
        holder.name?.apply {
            setTextColor(textColor)
            text = TemplateField.getLocalizedName(context, template.title)
        }

        return templateView!!
    }

    override fun getCount(): Int {
        return templates.size
    }

    override fun getItem(position: Int): Template {
        return templates[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    inner class TemplateSelectorViewHolder {
        var background: View? = null
        var icon: ImageView? = null
        var name: TextView? = null
    }
}
