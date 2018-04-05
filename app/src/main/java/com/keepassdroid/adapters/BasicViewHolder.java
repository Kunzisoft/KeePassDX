package com.keepassdroid.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

abstract class BasicViewHolder extends RecyclerView.ViewHolder {

    View container;
    ImageView icon;
    TextView text;
    TextView username;

    BasicViewHolder(View itemView) {
        super(itemView);
    }
}
