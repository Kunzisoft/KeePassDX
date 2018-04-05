package com.keepassdroid.adapters;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import tech.jgross.keepass.R;

class EntryViewHolder extends BasicViewHolder {

    EntryViewHolder(View itemView) {
        super(itemView);
        container = itemView.findViewById(R.id.entry_container);
        icon = (ImageView) itemView.findViewById(R.id.entry_icon);
        text = (TextView) itemView.findViewById(R.id.entry_text);
        username = (TextView) itemView.findViewById(R.id.entry_user_name);
    }
}
