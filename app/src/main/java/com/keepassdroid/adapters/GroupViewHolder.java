package com.keepassdroid.adapters;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.kunzisoft.keepass.R;

class GroupViewHolder extends BasicViewHolder {

    GroupViewHolder(View itemView) {
        super(itemView);
        container = itemView.findViewById(R.id.group_container);
        icon = (ImageView) itemView.findViewById(R.id.group_icon);
        text = (TextView) itemView.findViewById(R.id.group_text);
    }
}
