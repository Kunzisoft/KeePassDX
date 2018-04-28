/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.settings.preferenceDialogFragment.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.database.ObjectNameResource;

import java.util.ArrayList;
import java.util.List;

public class ListRadioItemAdapter<T extends ObjectNameResource> extends RecyclerView.Adapter<ListRadioViewHolder> {

    private Context context;
    private LayoutInflater inflater;

    private List<T> radioItemList;
    private T radioItemUsed;

    private RadioItemSelectedCallback<T> radioItemSelectedCallback;

    public ListRadioItemAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.radioItemList = new ArrayList<>();
        this.radioItemUsed = null;
    }

    @NonNull
    @Override
    public ListRadioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.pref_dialog_list_radio_item, parent, false);
        return new ListRadioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListRadioViewHolder holder, int position) {
        T item = this.radioItemList.get(position);
        holder.radioButton.setText(item.getName(context.getResources()));
        if (radioItemUsed != null && radioItemUsed.equals(item))
            holder.radioButton.setChecked(true);
        else
            holder.radioButton.setChecked(false);
        holder.radioButton.setOnClickListener(new OnItemClickListener(item));
    }

    @Override
    public int getItemCount() {
        return radioItemList.size();
    }

    public void setItems(List<T> algorithms, T algorithmUsed) {
        this.radioItemList.clear();
        this.radioItemList.addAll(algorithms);
        this.radioItemUsed = algorithmUsed;
    }

    private void setRadioItemUsed(T radioItemUsed) {
        this.radioItemUsed = radioItemUsed;
    }

    private class OnItemClickListener implements View.OnClickListener {

        private T itemClicked;

        OnItemClickListener(T item) {
            this.itemClicked = item;
        }

        @Override
        public void onClick(View view) {
            if (radioItemSelectedCallback != null)
                radioItemSelectedCallback.onItemSelected(itemClicked);
            setRadioItemUsed(itemClicked);
            notifyDataSetChanged();
        }
    }

    public void setRadioItemSelectedCallback(RadioItemSelectedCallback<T> radioItemSelectedCallback) {
        this.radioItemSelectedCallback = radioItemSelectedCallback;
    }

    public interface RadioItemSelectedCallback<T> {
        void onItemSelected(T item);
    }
}
