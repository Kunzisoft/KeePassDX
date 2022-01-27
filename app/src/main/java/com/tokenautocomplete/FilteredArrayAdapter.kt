package com.tokenautocomplete

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import java.util.*

/**
 * Simplified custom filtered ArrayAdapter
 * override keepObject with your test for filtering
 *
 *
 * Based on gist [
 * FilteredArrayAdapter](https://gist.github.com/tobiasschuerg/3554252/raw/30634bf9341311ac6ad6739ef094222fc5f07fa8/FilteredArrayAdapter.java) by Tobias Schürg
 *
 *
 * Created on 9/17/13.
 * @author mgod
 */
abstract class FilteredArrayAdapter<T>
/**
 * Constructor
 *
 * @param context The current context.
 * @param resource The resource ID for a layout file containing a layout to use when
 * instantiating views.
 * @param textViewResourceId The id of the TextView within the layout resource to be populated
 * @param objects The objects to represent in the ListView.
 */(
    context: Context,
    resource: Int,
    textViewResourceId: Int,
    objects: List<T>
) : ArrayAdapter<T>(
    context, resource, textViewResourceId, ArrayList(objects)
) {
    private val originalObjects: List<T> = objects
    private var filter: Filter? = null

    /**
     * Constructor
     *
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a TextView to use when
     * instantiating views.
     * @param objects The objects to represent in the ListView.
     */
    constructor(context: Context, resource: Int, objects: Array<T>) : this(
        context,
        resource,
        0,
        objects
    )

    /**
     * Constructor
     *
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a layout to use when
     * instantiating views.
     * @param textViewResourceId The id of the TextView within the layout resource to be populated
     * @param objects The objects to represent in the ListView.
     */
    constructor(
        context: Context,
        resource: Int,
        textViewResourceId: Int,
        objects: Array<T>
    ) : this(context, resource, textViewResourceId, ArrayList<T>(listOf(*objects)))

    /**
     * Constructor
     *
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a TextView to use when
     * instantiating views.
     * @param objects The objects to represent in the ListView.
     */
    @Suppress("unused")
    constructor(context: Context, resource: Int, objects: List<T>) : this(
        context,
        resource,
        0,
        objects
    )

    override fun getFilter(): Filter {
        if (filter == null) filter = AppFilter()
        return filter!!
    }

    /**
     * Filter method used by the adapter. Return true if the object should remain in the list
     *
     * @param obj object we are checking for inclusion in the adapter
     * @param mask current text in the edit text we are completing against
     * @return true if we should keep the item in the adapter
     */
    protected abstract fun keepObject(obj: T, mask: String?): Boolean

    /**
     * Class for filtering Adapter, relies on keepObject in FilteredArrayAdapter
     *
     * based on gist by Tobias Schürg
     * in turn inspired by inspired by Alxandr
     * (http://stackoverflow.com/a/2726348/570168)
     */
    private inner class AppFilter : Filter() {
        override fun performFiltering(chars: CharSequence?): FilterResults {
            val sourceObjects = ArrayList(originalObjects)
            val result = FilterResults()
            if (chars != null && chars.isNotEmpty()) {
                val mask = chars.toString()
                val keptObjects = ArrayList<T>()
                for (sourceObject in sourceObjects) {
                    if (keepObject(sourceObject, mask)) keptObjects.add(sourceObject)
                }
                result.count = keptObjects.size
                result.values = keptObjects
            } else {
                // add all objects
                result.values = sourceObjects
                result.count = sourceObjects.size
            }
            return result
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            clear()
            if (results.count > 0) {
                @Suppress("unchecked_cast")
                this@FilteredArrayAdapter.addAll(results.values as Collection<T>)
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }
    }
}