package com.kunzisoft.keepass.database.element

interface NodeVersioned: PwNodeInterface<GroupVersioned> {

    val nodePositionInParent: Int
        get() {
            parent?.getChildren(false)?.let { children ->
                for ((i, child) in children.withIndex()) {
                    if (child == this)
                        return i
                }
            }
            return -1
        }
}

/**
 * Type of available Nodes
 */
enum class Type {
    GROUP, ENTRY
}


