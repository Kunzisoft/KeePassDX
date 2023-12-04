package com.kunzisoft.keepass.activities.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.EntryHistoryAdapter
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.view.hideByFading
import com.kunzisoft.keepass.view.showByFading
import com.kunzisoft.keepass.viewmodels.EntryViewModel

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

        val viewHistoryButton = view.findViewById<View>(R.id.entry_show_history)
        val viewHistoryList = view.findViewById<View>(R.id.history_list)
        val viewHistoryLabel = view.findViewById<TextView>(R.id.view_history_label)
        val viewHistoryArrow = view.findViewById<ImageView>(R.id.view_history_arrow)
        viewHistoryButton.setOnClickListener {
            val currentVisibility = viewHistoryList.isVisible
            if (currentVisibility) {
                viewHistoryList.hideByFading()
                viewHistoryArrow.animate()
                    .rotation(0f)
                    .setDuration(200)
                    .start()
                viewHistoryLabel.setText(R.string.entry_show_history)
            } else {
                viewHistoryList.showByFading()
                viewHistoryArrow.animate()
                    .rotation(90f)
                    .setDuration(200)
                    .start()
                viewHistoryLabel.setText(R.string.entry_hide_history)
            }
        }

        mEntryViewModel.entryHistory.observe(viewLifecycleOwner) {
            assignHistory(it)
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