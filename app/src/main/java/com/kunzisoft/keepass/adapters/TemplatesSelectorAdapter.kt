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
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.template.Template
import com.kunzisoft.keepass.database.element.template.TemplateField


class TemplatesSelectorAdapter(private val context: Context,
                               private val database: Database?,
                               private var templates: List<Template>): BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var mIconColor = Color.BLACK

    init {
        val taIconColor = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.textColor))
        mIconColor = taIconColor.getColor(0, Color.BLACK)
        taIconColor.recycle()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val template: Template = getItem(position)

        val holder: TemplateSelectorViewHolder
        var templateView = convertView
        if (templateView == null) {
            holder = TemplateSelectorViewHolder()
            templateView = inflater.inflate(R.layout.item_template, parent, false)
            holder.icon = templateView?.findViewById(R.id.template_image)
            holder.name = templateView?.findViewById(R.id.template_name)
            templateView?.tag = holder
        } else {
            holder = templateView.tag as TemplateSelectorViewHolder
        }

        holder.icon?.let { icon ->
            database?.iconDrawableFactory?.assignDatabaseIcon(icon, template.icon, mIconColor)
        }
        holder.name?.text = TemplateField.getLocalizedName(context, template.title)

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
        var icon: ImageView? = null
        var name: TextView? = null
    }
}