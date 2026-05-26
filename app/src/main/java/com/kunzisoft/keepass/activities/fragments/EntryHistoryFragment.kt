package com.kunzisoft.keepass.activities.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.EntryHistoryAdapter
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.viewmodels.EntryViewModel
import kotlinx.coroutines.launch

class EntryHistoryFragment: Fragment() {

    private lateinit var historyContainerView: View
    private lateinit var historyListView: RecyclerView
    private var historyAdapter: EntryHistoryAdapter? = null

    private val mEntryViewModel: EntryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_entry_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        context?.let { context ->
            historyAdapter = EntryHistoryAdapter(context)
        }

        historyContainerView = view.findViewById(R.id.entry_history_container)
        historyListView = view.findViewById(R.id.entry_history_list)
        historyListView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true)
            adapter = historyAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                mEntryViewModel.entryHistoryState.collect { entryState ->
                    when(entryState) {
                        is EntryViewModel.EntryHistoryState -> {
                            assignHistory(entryState.entryHistory)
                        }
                    }
                }
            }
        }
    }

    /* -------------
     * History
     * -------------
     */
    private fun assignHistory(history: List<EntryInfo>?) {
        historyAdapter?.clear()
        history?.let {
            historyAdapter?.entryHistoryList?.addAll(history)
        }
        historyAdapter?.onItemClickListener = { item, position ->
            mEntryViewModel.onHistorySelected(item, position)
        }
        historyContainerView.visibility = if (historyAdapter?.entryHistoryList?.isEmpty() != false)
            View.GONE
        else
            View.VISIBLE
        historyAdapter?.notifyDataSetChanged()
    }
}