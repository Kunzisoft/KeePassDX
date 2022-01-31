package com.kunzisoft.keepass.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.search.SearchParameters

class SearchFiltersView @JvmOverloads constructor(context: Context,
                                                  attrs: AttributeSet? = null,
                                                  defStyle: Int = 0)
    : LinearLayout(context, attrs, defStyle) {

    private var searchContainer: ViewGroup
    private var searchAdvanceFiltersContainer: ViewGroup
    private var searchExpandButton: ImageView
    private var searchNumbers: TextView
    private var searchCaseSensitive: CompoundButton
    private var searchExpires: CompoundButton
    private var searchTitle: CompoundButton
    private var searchUsername: CompoundButton
    private var searchPassword: CompoundButton
    private var searchURL: CompoundButton
    private var searchNotes: CompoundButton
    private var searchOther: CompoundButton
    private var searchUUID: CompoundButton
    private var searchTag: CompoundButton

    var searchParameters = SearchParameters()
        get() {
            return field.apply {
                this.caseSensitive = searchCaseSensitive.isChecked
                this.excludeExpired = !(searchExpires.isChecked)
                this.searchInTitles = searchTitle.isChecked
                this.searchInUsernames = searchUsername.isChecked
                this.searchInPasswords = searchPassword.isChecked
                this.searchInUrls = searchURL.isChecked
                this.searchInNotes = searchNotes.isChecked
                this.searchInOther = searchOther.isChecked
                this.searchInUUIDs = searchUUID.isChecked
                this.searchInTags = searchTag.isChecked
            }
        }
        private set

    var onParametersChangeListener: ((searchParameters: SearchParameters) -> Unit)? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_search_filters, this)

        searchContainer = findViewById(R.id.search_container)
        searchAdvanceFiltersContainer = findViewById(R.id.search_advance_filters)
        searchExpandButton = findViewById(R.id.search_expand)
        searchNumbers = findViewById(R.id.search_numbers)
        searchCaseSensitive = findViewById(R.id.search_chip_case_sensitive)
        searchExpires = findViewById(R.id.search_chip_expires)
        searchTitle = findViewById(R.id.search_chip_title)
        searchUsername = findViewById(R.id.search_chip_username)
        searchPassword = findViewById(R.id.search_chip_password)
        searchURL = findViewById(R.id.search_chip_url)
        searchNotes = findViewById(R.id.search_chip_note)
        searchUUID = findViewById(R.id.search_chip_uuid)
        searchOther = findViewById(R.id.search_chip_other)
        searchTag = findViewById(R.id.search_chip_tag)

        // Expand menu with button
        searchExpandButton.setOnClickListener {
            val isVisible = searchAdvanceFiltersContainer.visibility == View.VISIBLE
            if (isVisible)
                searchAdvanceFiltersContainer.collapse()
            else {
                searchAdvanceFiltersContainer.expand(true,
                    resources.getDimension(R.dimen.advanced_search_height).toInt()
                )
            }
        }

        searchCaseSensitive.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.caseSensitive = isChecked
            onParametersChangeListener?.invoke(searchParameters)
        }
        searchExpires.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.excludeExpired = !isChecked
            onParametersChangeListener?.invoke(searchParameters)
        }
        searchTitle.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.searchInTitles = isChecked
            onParametersChangeListener?.invoke(searchParameters)
        }
        searchUsername.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.searchInUsernames = isChecked
            onParametersChangeListener?.invoke(searchParameters)
        }
        searchPassword.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.searchInPasswords = isChecked
            onParametersChangeListener?.invoke(searchParameters)
        }
        searchURL.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.searchInUrls = isChecked
            onParametersChangeListener?.invoke(searchParameters)
        }
        searchNotes.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.searchInNotes = isChecked
            onParametersChangeListener?.invoke(searchParameters)
        }
        searchUUID.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.searchInUUIDs = isChecked
            onParametersChangeListener?.invoke(searchParameters)
        }
        searchOther.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.searchInOther = isChecked
            onParametersChangeListener?.invoke(searchParameters)
        }
        searchTag.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.searchInTags = isChecked
            onParametersChangeListener?.invoke(searchParameters)
        }
    }

    fun setQuery(query: String) {
        searchParameters.searchQuery = query
        onParametersChangeListener?.invoke(searchParameters)
    }

    fun setNumbers(stringNumbers: String) {
        searchNumbers.text = stringNumbers
    }

    override fun setVisibility(visibility: Int) {
        //super.setVisibility(visibility)
        when (visibility) {
            View.VISIBLE -> {
                searchAdvanceFiltersContainer.visibility = View.GONE
                searchContainer.showByFading()
            }
            else -> {
                searchContainer.hideByFading()
                if (searchAdvanceFiltersContainer.visibility == View.VISIBLE) {
                    searchAdvanceFiltersContainer.visibility = View.INVISIBLE
                    searchAdvanceFiltersContainer.collapse()
                }
            }
        }
    }
}