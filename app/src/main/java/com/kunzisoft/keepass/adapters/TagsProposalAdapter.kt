package com.kunzisoft.keepass.adapters

import android.content.Context
import com.kunzisoft.keepass.database.element.Tags
import com.tokenautocomplete.FilteredArrayAdapter

class TagsProposalAdapter(context: Context, proposal: Tags?)
    : FilteredArrayAdapter<String>(
    context,
    android.R.layout.simple_list_item_1,
    (proposal ?: Tags()).toList()
) {

    override fun keepObject(obj: String, mask: String?): Boolean {
        if (mask == null)
            return false
        return obj.contains(mask, true)
    }
}