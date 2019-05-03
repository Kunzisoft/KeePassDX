package com.kunzisoft.keepass.database.element

import android.os.Parcelable
import com.kunzisoft.keepass.database.SmallTimeInterface

interface PwNodeInterface<ParentGroup> : SmallTimeInterface, Parcelable {

    var title: String

    /**
     * @return Visual icon
     */
    var icon: PwIcon

    /**
     * @return Type of Node
     */
    val type: Type

    /**
     * Retrieve the parent node
     */
    var parent: ParentGroup?

    val isSearchingEnabled: Boolean?

    fun containsParent(): Boolean

    fun touch(modified: Boolean, touchParents: Boolean)

    fun isContainedIn(container: ParentGroup): Boolean
}