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
package com.kunzisoft.keepass.adapters

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.Field

import java.util.ArrayList

class FieldsAdapter(context: Context) : RecyclerView.Adapter<FieldsAdapter.FieldViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var fields: MutableList<Field> = ArrayList()
    var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FieldViewHolder {
        val view = inflater.inflate(R.layout.keyboard_popup_fields_item, parent, false)
        return FieldViewHolder(view)
    }

    override fun onBindViewHolder(holder: FieldViewHolder, position: Int) {
        val field = fields[position]
        holder.name.text = field.name
        holder.bind(field, onItemClickListener)
    }

    override fun getItemCount(): Int {
        return fields.size
    }

    fun setFields(fieldsToAdd: List<Field>) {
        fields.clear()
        fields.addAll(fieldsToAdd)
    }

    fun clear() {
        fields.clear()
    }

    interface OnItemClickListener {
        fun onItemClick(item: Field)
    }

    inner class FieldViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var name: TextView = itemView.findViewById(R.id.keyboard_popup_field_item_name)

        fun bind(item: Field, listener: OnItemClickListener?) {
            itemView.setOnClickListener { listener?.onItemClick(item) }
        }
    }
}
