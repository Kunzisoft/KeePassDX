package com.kunzisoft.keepass.activities.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.IconAdapter
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.icon.IconImageDraw
import com.kunzisoft.keepass.viewmodels.IconPickerViewModel

abstract class IconFragment<T: IconImageDraw> : Fragment() {

    protected lateinit var iconsGridView: RecyclerView
    protected lateinit var iconAdapter: IconAdapter<T>

    protected val database = Database.getInstance()

    protected val iconPickerViewModel: IconPickerViewModel by activityViewModels()

    abstract fun retrieveMainLayoutId(): Int

    abstract fun defineIconList(database: Database): List<T>

    override fun onAttach(context: Context) {
        super.onAttach(context)

        iconAdapter = IconAdapter<T>(requireActivity()).apply {
            iconDrawableFactory = database.iconDrawableFactory
        }

        iconAdapter.setList(defineIconList(database))
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val root = inflater.inflate(retrieveMainLayoutId(), container, false)
        iconsGridView = root.findViewById(R.id.icons_grid_view)
        iconsGridView.adapter = iconAdapter
        return root
    }
}