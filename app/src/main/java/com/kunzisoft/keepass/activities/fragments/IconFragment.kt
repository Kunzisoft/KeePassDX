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
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.viewmodels.IconPickerViewModel

abstract class IconFragment : Fragment() {

    private lateinit var iconsGridView: RecyclerView
    protected lateinit var iconAdapter: IconAdapter

    protected val iconPickerViewModel: IconPickerViewModel by activityViewModels()

    abstract fun retrieveMainLayoutId(): Int

    abstract fun defineIconList(database: Database): List<IconImage>

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val database = Database.getInstance()

        iconAdapter = IconAdapter(requireActivity()).apply {
            iconDrawableFactory = database.iconDrawableFactory
            setList(defineIconList(database))
        }
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