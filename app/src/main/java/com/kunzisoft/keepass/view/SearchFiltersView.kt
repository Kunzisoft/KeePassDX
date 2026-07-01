package com.kunzisoft.keepass.view

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.TagsAdapter
import com.kunzisoft.keepass.database.element.Tag
import com.kunzisoft.keepass.database.element.Tags
import com.kunzisoft.keepass.database.search.SearchParameters
import com.kunzisoft.keepass.settings.PreferencesUtil

class SearchFiltersView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    private var searchContainer: ViewGroup
    private var searchAdvanceFiltersContainer: ViewGroup? = null
    private var searchExpandButton: ImageView
    private var searchTags: RecyclerView
    private var searchTagsContainer: View
    private var searchTagGroup: ViewGroup
    private var searchTag: CompoundButton
    private var tagsAdapter: TagsAdapter = TagsAdapter(
        context,
        TagsAdapter.TagViewType.CHIP
    ).apply {
        onItemClickListener = object : TagsAdapter.OnItemClickListener {
            override fun onItemClick(item: Tag) {
                toggleSelection(item)
                val selectedTags = tagsAdapter.getSelectedStringTags()
                val atLeastOneSelectedTag = selectedTags.isNotEmpty()
                searchParameters.apply {
                    searchInTags = atLeastOneSelectedTag
                    tagsToSearch = selectedTags
                }
                if (searchTag.isChecked != atLeastOneSelectedTag) {
                    searchTag.isChecked = atLeastOneSelectedTag
                } else {
                    mOnParametersChangeListener?.invoke(searchParameters)
                }
            }
            override fun onItemLongClick(item: Tag): Boolean {
                return false
            }
        }
    }
    private var searchCurrentGroup: CompoundButton
    private var searchCaseSensitive: CompoundButton
    private var searchRegex: CompoundButton
    private var searchTitle: CompoundButton
    private var searchUsername: CompoundButton
    private var searchPassword: CompoundButton
    private var searchApplicationId: CompoundButton
    private var searchURL: CompoundButton
    private var searchByURLDomain: Boolean = false
    private var searchByURLSubDomain: Boolean = false
    private var searchExpired: CompoundButton
    private var searchNotes: CompoundButton
    private var searchOther: CompoundButton
    private var searchUUID: CompoundButton
    private var searchGroupSearchable: CompoundButton
    private var searchRecycleBin: CompoundButton
    private var searchTemplate: CompoundButton

    private var isExpanded = false
    var onExpansionChanged: ((expanded: Boolean) -> Unit)? = null

    var searchParameters = SearchParameters()
        get() {
            return field.copy().apply {
                this.searchInCurrentGroup = searchCurrentGroup.isChecked
                this.caseSensitive = searchCaseSensitive.isChecked
                this.isRegex = searchRegex.isChecked
                this.searchInTags = searchTag.isChecked
                this.tagsToSearch = tagsAdapter.getSelectedStringTags()
                this.searchInTitles = searchTitle.isChecked
                this.searchInUsernames = searchUsername.isChecked
                this.searchInPasswords = searchPassword.isChecked
                this.searchInAppIds = searchApplicationId.isChecked
                this.searchInUrls = searchURL.isChecked
                this.searchByDomain = searchByURLDomain
                this.searchBySubDomain = searchByURLSubDomain
                this.searchInExpired = searchExpired.isChecked
                this.searchInNotes = searchNotes.isChecked
                this.searchInOther = searchOther.isChecked
                this.searchInUUIDs = searchUUID.isChecked
                this.searchInRecycleBin = searchRecycleBin.isChecked
                this.searchInTemplates = searchTemplate.isChecked
            }
        }
        set(value) {
            field = value.copy()
            val tempListener = mOnParametersChangeListener
            mOnParametersChangeListener = null
            searchCurrentGroup.isChecked = field.searchInCurrentGroup
            searchTag.isChecked = field.searchInTags
            tagsAdapter.selectTags(field.tagsToSearch)
            searchCaseSensitive.isChecked = field.caseSensitive
            searchRegex.isChecked = field.isRegex
            searchTitle.isChecked = field.searchInTitles
            searchUsername.isChecked = field.searchInUsernames
            searchPassword.isChecked = field.searchInPasswords
            searchApplicationId.isChecked = field.searchInAppIds
            searchURL.isChecked = field.searchInUrls
            searchByURLDomain = field.searchByDomain
            searchByURLSubDomain = field.searchBySubDomain
            searchExpired.isChecked = field.searchInExpired
            searchNotes.isChecked = field.searchInNotes
            searchOther.isChecked = field.searchInOther
            searchUUID.isChecked = field.searchInUUIDs
            searchGroupSearchable.isChecked = field.searchInSearchableGroup
            searchRecycleBin.isChecked = field.searchInRecycleBin
            searchTemplate.isChecked = field.searchInTemplates
            mOnParametersChangeListener = tempListener
        }

    var onParametersChangeListener: ((searchParameters: SearchParameters) -> Unit)? = null
    private var mOnParametersChangeListener: ((searchParameters: SearchParameters) -> Unit)? = {
        // To recalculate height
        if (searchAdvanceFiltersContainer?.visibility == VISIBLE) {
            searchAdvanceFiltersContainer?.expand(
                false,
                searchAdvanceFiltersContainer?.getFullHeight()
            )
        }
        onParametersChangeListener?.invoke(searchParameters)
    }

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_search_filters, this)

        searchContainer = findViewById(R.id.search_container)
        searchAdvanceFiltersContainer = findViewById(R.id.search_advance_filters)
        searchExpandButton = findViewById(R.id.search_expand)
        searchCurrentGroup = findViewById(R.id.search_chip_current_group)
        searchTagsContainer = findViewById(R.id.search_tags_container)
        searchTagGroup = findViewById(R.id.search_chip_tag_group)
        searchTag = findViewById(R.id.search_chip_tag)
        searchTags = findViewById(R.id.search_tags_list)
        searchCaseSensitive = findViewById(R.id.search_chip_case_sensitive)
        searchRegex = findViewById(R.id.search_chip_regex)
        searchTitle = findViewById(R.id.search_chip_title)
        searchUsername = findViewById(R.id.search_chip_username)
        searchPassword = findViewById(R.id.search_chip_password)
        searchApplicationId = findViewById(R.id.search_chip_application_id)
        searchURL = findViewById(R.id.search_chip_url)
        searchExpired = findViewById(R.id.search_chip_expires)
        searchNotes = findViewById(R.id.search_chip_note)
        searchUUID = findViewById(R.id.search_chip_uuid)
        searchOther = findViewById(R.id.search_chip_other)
        searchGroupSearchable = findViewById(R.id.search_chip_group_searchable)
        searchRecycleBin = findViewById(R.id.search_chip_recycle_bin)
        searchTemplate = findViewById(R.id.search_chip_template)

        searchContainer.visibility = GONE
        searchAdvanceFiltersContainer?.visibility = GONE

        // Set search
        searchParameters = PreferencesUtil.getDefaultSearchParameters(context)

        // Expand menu with button
        searchExpandButton.setOnClickListener {
            val isExpanded = searchAdvanceFiltersContainer?.visibility == VISIBLE
            if (isExpanded)
                closeAdvancedFilters()
            else
                openAdvancedFilters()
        }

        searchCurrentGroup.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.searchInCurrentGroup = isChecked
            mOnParametersChangeListener?.invoke(searchParameters)
        }
        searchTags.apply {
            layoutManager = LinearLayoutManager(
                context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = tagsAdapter
        }
        searchTag.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.apply {
                searchInTags = isChecked
                tagsToSearch = tagsAdapter.getSelectedStringTags()
            }
            mOnParametersChangeListener?.invoke(searchParameters)
        }
        searchCaseSensitive.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.caseSensitive = isChecked
            mOnParametersChangeListener?.invoke(searchParameters)
        }
        searchRegex.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.isRegex = isChecked
            mOnParametersChangeListener?.invoke(searchParameters)
        }
        searchTitle.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.searchInTitles = isChecked
            mOnParametersChangeListener?.invoke(searchParameters)
        }
        searchUsername.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.searchInUsernames = isChecked
            mOnParametersChangeListener?.invoke(searchParameters)
        }
        searchPassword.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.searchInPasswords = isChecked
            mOnParametersChangeListener?.invoke(searchParameters)
        }
        searchApplicationId.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.searchInAppIds = isChecked
            mOnParametersChangeListener?.invoke(searchParameters)
        }
        searchURL.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.searchInUrls = isChecked
            mOnParametersChangeListener?.invoke(searchParameters)
        }
        searchExpired.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.searchInExpired = isChecked
            mOnParametersChangeListener?.invoke(searchParameters)
        }
        searchNotes.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.searchInNotes = isChecked
            mOnParametersChangeListener?.invoke(searchParameters)
        }
        searchUUID.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.searchInUUIDs = isChecked
            mOnParametersChangeListener?.invoke(searchParameters)
        }
        searchOther.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.searchInOther = isChecked
            mOnParametersChangeListener?.invoke(searchParameters)
        }
        searchGroupSearchable.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.searchInSearchableGroup = isChecked
            mOnParametersChangeListener?.invoke(searchParameters)
        }
        searchRecycleBin.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.searchInRecycleBin = isChecked
            mOnParametersChangeListener?.invoke(searchParameters)
        }
        searchTemplate.setOnCheckedChangeListener { _, isChecked ->
            searchParameters.searchInTemplates = isChecked
            mOnParametersChangeListener?.invoke(searchParameters)
        }
    }

    fun setCurrentGroupText(text: String?) {
        val maxChars = 12
        searchCurrentGroup.text = when {
            text.isNullOrEmpty() -> context.getString(R.string.current_group)
            text.length > maxChars -> text.substring(0, maxChars) + "…"
            else -> text
        }
    }

    fun setSelectableTags(tags: Tags) {
        if (tags.isEmpty()) {
            searchTagGroup.isVisible = false
            searchParameters.searchInTags = false
        } else {
            searchTagGroup.isVisible = true
            tagsAdapter.setTags(tags)
        }
    }

    fun availableOther(available: Boolean) {
        searchOther.isVisible = available
    }

    fun availableApplicationIds(available: Boolean) {
        searchApplicationId.isVisible = available
    }

    fun availableTags(available: Boolean) {
        searchTag.isVisible = available
    }

    fun enableTags(enable: Boolean) {
        searchTag.isEnabled = enable
    }

    fun availableSearchableGroup(available: Boolean) {
        searchGroupSearchable.isVisible = available
    }

    fun availableTemplates(available: Boolean) {
        searchTemplate.isVisible = available
    }

    fun enableTemplates(enable: Boolean) {
        searchTemplate.isEnabled = enable
    }

    fun closeAdvancedFilters(animate: Boolean = true) {
        isExpanded = false
        searchAdvanceFiltersContainer?.collapse(animate) {
            onExpansionChanged?.invoke(false)
        }
    }

    fun openAdvancedFilters(animate: Boolean = true) {
        isExpanded = true
        searchAdvanceFiltersContainer?.expand(animate,
            searchAdvanceFiltersContainer?.getFullHeight()
        ) {
            onExpansionChanged?.invoke(true)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable("superState", super.onSaveInstanceState())
        bundle.putBoolean("isExpanded", isExpanded)
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        var superState = state
        if (state is Bundle) {
            isExpanded = state.getBoolean("isExpanded")
            if (isExpanded) {
                searchAdvanceFiltersContainer?.post {
                    openAdvancedFilters(animate = false)
                }
            }
            superState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                state.getParcelable("superState", Parcelable::class.java)
            } else {
                @Suppress("DEPRECATION")
                state.getParcelable("superState")
            }
        }
        super.onRestoreInstanceState(superState)
    }

    fun allowAdvancedSearch(show: Boolean) {
        val expandButtonVisibility = if (show) VISIBLE else GONE
        if (searchExpandButton.visibility != expandButtonVisibility)
            searchExpandButton.visibility = expandButtonVisibility
        val searchTagsContainerVisibility = if (show) VISIBLE else GONE
        if (searchTagsContainer.visibility != searchTagsContainerVisibility)
            searchTagsContainer.visibility = searchTagsContainerVisibility
    }

    override fun setVisibility(visibility: Int) {
        when (visibility) {
            VISIBLE -> {
                if (searchContainer.visibility != VISIBLE) {
                    searchContainer.showByFading()
                }
            }
            else -> {
                closeAdvancedFilters()
                if (searchContainer.visibility != GONE) {
                    searchContainer.hideByFading()
                }
            }
        }
    }
}