package com.kunzisoft.keepass.database.element

import android.os.Parcelable

interface NodeVersionedInterface<ParentGroup> : NodeTimeInterface, Parcelable {

    var title: String

    /**
     * @return Visual icon
     */
    var icon: IconImage

    /**
     * @return Type of Node
     */
    val type: Type

    /**
     * Retrieve the parent node
     */
    var parent: ParentGroup?

    fun containsParent(): Boolean

    fun afterAssignNewParent()

    fun isContainedIn(container: ParentGroup): Boolean

    fun touch(modified: Boolean, touchParents: Boolean)
}