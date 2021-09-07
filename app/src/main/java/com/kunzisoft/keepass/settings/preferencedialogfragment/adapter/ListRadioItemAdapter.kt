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
package com.kunzisoft.keepass.settings.preferencedialogfragment.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import java.util.*

class ListRadioItemAdapter<T>(private val context: Context)
    : RecyclerView.Adapter<ListRadioItemAdapter.ListRadioViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    private val radioItemList: MutableList<T> = ArrayList()
    private var radioItemUsed: T? = null

    private var radioItemSelectedCallback: RadioItemSelectedCallback<T>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListRadioViewHolder {
        val view = inflater.inflate(R.layout.pref_dialog_list_radio_item, parent, false)
        return ListRadioViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListRadioViewHolder, position: Int) {
        val item = this.radioItemList[position]
        holder.radioButton.text = item.toString()
        holder.radioButton.isChecked = radioItemUsed != null && radioItemUsed == item
        holder.radioButton.setOnClickListener(OnItemClickListener(item))
    }

    override fun getItemCount(): Int {
        return radioItemList.size
    }

    fun setItems(items: List<T>, itemUsed: T?) {
        this.radioItemList.clear()
        this.radioItemList.addAll(items)
        this.radioItemUsed = itemUsed
    }

    private fun setRadioItemUsed(radioItemUsed: T) {
        this.radioItemUsed = radioItemUsed
    }

    private inner class OnItemClickListener(private val itemClicked: T) : View.OnClickListener {

        override fun onClick(view: View) {
            radioItemSelectedCallback?.onItemSelected(itemClicked)
            setRadioItemUsed(itemClicked)
            notifyDataSetChanged()
        }
    }

    fun setRadioItemSelectedCallback(radioItemSelectedCallback: RadioItemSelectedCallback<T>) {
        this.radioItemSelectedCallback = radioItemSelectedCallback
    }

    interface RadioItemSelectedCallback<T> {
        fun onItemSelected(item: T)
    }

    class ListRadioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var radioButton: RadioButton = itemView.findViewById(R.id.pref_dialog_list_radio)
    }
}
