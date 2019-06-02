package com.kunzisoft.keepass.database.element

import android.os.Parcelable

interface PwNodeInterface<ParentGroup> : NodeTimeInterface, Parcelable {

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

    val isSearchingEnabled: Boolean

    fun containsParent(): Boolean

    fun afterAssignNewParent()

    fun isContainedIn(container: ParentGroup): Boolean

    fun touch(modified: Boolean, touchParents: Boolean)
}