package com.keepassdroid.groupentity;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

// TODO Refactor
public abstract class BasicViewHolder extends RecyclerView.ViewHolder {

    View container;
    ImageView icon;
    TextView text;

    BasicViewHolder(View itemView) {
        super(itemView);
    }
}
