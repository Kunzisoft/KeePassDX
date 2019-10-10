package com.kunzisoft.keepass.database.element

interface NodeVersioned: PwNodeInterface<GroupVersioned> {

    val nodePositionInParent: Int
        get() {
            parent?.getChildren(true)?.let { children ->
                children.forEachIndexed { index, nodeVersioned ->
                    if (nodeVersioned == this)
                        return index
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


