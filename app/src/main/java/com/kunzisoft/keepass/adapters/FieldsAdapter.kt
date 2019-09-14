package com.kunzisoft.keepass.adapters

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.model.Field

import java.util.ArrayList

class FieldsAdapter(context: Context) : RecyclerView.Adapter<FieldsAdapter.FieldViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    var fields: MutableList<Field> = ArrayList()
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
