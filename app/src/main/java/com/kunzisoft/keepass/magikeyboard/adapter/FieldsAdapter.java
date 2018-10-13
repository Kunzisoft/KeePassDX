package com.kunzisoft.keepass.magikeyboard.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.model.Field;

import java.util.ArrayList;
import java.util.List;

public class FieldsAdapter extends RecyclerView.Adapter<FieldsAdapter.FieldViewHolder> {

    private LayoutInflater inflater;
    private List<Field> fields;
    private OnItemClickListener listener;

    public FieldsAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
        this.fields = new ArrayList<>();
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public FieldViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.keyboard_popup_fields_item, parent, false);
        return new FieldViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FieldViewHolder holder, int position) {
        Field field = fields.get(position);
        holder.name.setText(field.getName());
        holder.bind(field, listener);
    }

    @Override
    public int getItemCount() {
        return fields.size();
    }

    public void clear() {
        fields.clear();
    }

    public interface OnItemClickListener {
        void onItemClick(Field item);
    }

    class FieldViewHolder extends RecyclerView.ViewHolder {

        TextView name;

        FieldViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.keyboard_popup_field_item_name);
        }

        void bind(final Field item, final OnItemClickListener listener) {
            itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}
